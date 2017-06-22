/*
 * Copyright (c) 2005-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.security.User;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.util.massSpecDataFileType;
import org.labkey.ms2.reader.MS2Loader;
import org.labkey.ms2.reader.PepXmlLoader;
import org.labkey.ms2.reader.PepXmlLoader.FractionIterator;
import org.labkey.ms2.reader.PepXmlLoader.PepXmlFraction;
import org.labkey.ms2.reader.PepXmlLoader.PepXmlPeptide;
import org.labkey.ms2.reader.PepXmlLoader.PeptideIterator;
import org.labkey.ms2.reader.PeptideProphetHandler;
import org.labkey.ms2.reader.PeptideProphetSummary;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class PepXmlImporter extends PeptideImporter
{
    private static final int BATCH_SIZE = 100;

    public PepXmlImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(user, c, description, fullFileName, log, context);
    }

    @Override
    public void importRun(MS2Progress progress) throws XMLStreamException, IOException
    {
        PepXmlLoader loader = null;
        int fractionCount = 0;

        try
        {
            boolean runUpdated = false;  // Set to true after we update the run information (after importing the first fraction)

            File f = new File(_path + "/" + _fileName);
            NetworkDrive.ensureDrive(f.getPath());
            loader = new PepXmlLoader(f, _log);

            PeptideProphetSummary summary = loader.getPeptideProphetSummary();
            writePeptideProphetSummary(_runId, summary);

            _quantSummaries = loader.getQuantSummaries();
            writeQuantSummaries(_runId, _quantSummaries);

            FractionIterator fi = loader.getFractionIterator();

            while (fi.hasNext())
            {
                PepXmlFraction fraction = fi.next();

                if (!runUpdated)  // do this for the first fraction only
                {
                    writeRunInfo(fraction, progress);
                    progress.setMs2FileInfo(loader.getFileLength(), loader.getCurrentOffset());
                    runUpdated = true;
                }

                fractionCount++;
                String description = "";
                if (fractionCount > 1)
                {
                    description += " of fraction " + fractionCount + " ";
                }
                if (fraction.getSpectrumPath() != null)
                {
                    description += " from file " + fraction.getSpectrumPath();
                }
                progress.getCumulativeTimer().setCurrentTask(Tasks.ImportPeptides, description);
                progress.setPeptideMode();
                writeFractionInfo(fraction);

                PeptideIterator pi = fraction.getPeptideIterator();
                boolean shouldImportSpectra = fraction.shouldLoadSpectra();
                float importSpectraMinProbability = (null == fraction.getImportSpectraMinProbability() ? -Float.MAX_VALUE : fraction.getImportSpectraMinProbability());
                progress.setImportSpectra(shouldImportSpectra);
                // Initialize scans to a decent size, but only if we're going to load spectra
                Set<Integer> scans = new HashSet<>(shouldImportSpectra ? 1000 : 0);
                _conn.setAutoCommit(false);

                boolean retentionTimesInPepXml = false;
                int count = 0;

                while (pi.hasNext())
                {
                    PepXmlPeptide peptide = pi.next();

                    // If any peptide in the pep.xml file has retention time then don't import retention times from mzXML
                    if (null != peptide.getRetentionTime())
                        retentionTimesInPepXml = true;

                    // Mascot exported pepXML may contain unassigned spectrum
                    // we omit them for import
                    if (null != peptide.getTrimmedPeptide())
                    {
	                    write(peptide, summary);

                        if (shouldImportSpectra)
                        {
                            PeptideProphetHandler.PeptideProphetResult pp = peptide.getPeptideProphetResult();
                            if (null == pp || pp.getProbability() >= importSpectraMinProbability)
                                scans.add(peptide.getScan());
                        }

                        count++;
                        if (count % BATCH_SIZE == 0)
                        {
                            _conn.commit();
                        }
                    }
                    progress.setCurrentMs2FileOffset(loader.getCurrentOffset());
                }
                _conn.commit();
                _conn.setAutoCommit(true);

                progress.setSpectrumMode(scans.size());
                processSpectrumFile(fraction, scans, progress, shouldImportSpectra, !retentionTimesInPepXml);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (null != loader)
                loader.close();
        }
    }


    private void writeFractionInfo(MS2Loader.PeptideFraction fraction) throws IOException
    {
        String dataSuffix = fraction.getDataSuffix();
        String baseName = fraction.getDataBasename();
        String newFilename = new File(baseName).getName();
        // Build the name of the tgz file
        if (fraction.isSequest())
        {
           _gzFileName = newFilename + "." + "pep." + dataSuffix;
        }
        else
        {
            _gzFileName = switchSuffix(_fileName, dataSuffix);
        }
       // No spectrumPath in a sequest or Mascot pepXML file.
        if (fraction.getSpectrumPath() == null)
        {
            // First, check two directories up from the MS2 results. This is where searches done through the CPAS
            // pipeline will be
            File pepXmlDir = new File(_path);
            File mzXMLFile = null;
            massSpecDataFileType FT_MZXML = new massSpecDataFileType();
            if (pepXmlDir.getParentFile() != null && pepXmlDir.getParentFile().getParentFile() != null)
            {
                mzXMLFile = FT_MZXML.getFile(pepXmlDir.getParentFile().getParentFile(), newFilename);
            }

            if (mzXMLFile == null || !NetworkDrive.exists(mzXMLFile))
            {
                // If not there, look in the same directory as the MS2 results
                mzXMLFile = FT_MZXML.getFile(pepXmlDir, newFilename);
            }
            fraction.setSpectrumPath(mzXMLFile.getAbsolutePath());
        }
        if (! NetworkDrive.exists(new File(_path + "/" + _gzFileName)) && baseName != null)
        {
            // Try using the base_name from the input file
            int i = baseName.lastIndexOf("/");
            newFilename =
                    (i < 0 ? baseName : baseName.substring(i + 1));
            //newFilename = switchSuffix(newFilename, dataSuffix);
            newFilename += "." + dataSuffix;
            if (NetworkDrive.exists(new File(_path + "/" + newFilename)))
                _gzFileName = newFilename;
        }

        File mzXMLFile = getMzXMLFile(fraction);
        _fractionId = createFraction(_user, _container, _runId, _path, mzXMLFile);
    }


    /**
     * Switch the suffix of the give filename
     */
    protected static String switchSuffix(String filename, String suffix)
    {
        if (suffix == null)
            return filename;
        int i = filename.lastIndexOf(".");
        return (i < 0 ? filename : filename.substring(0, i)) + "." + suffix;
    }

    protected void processSpectrumFile(PepXmlFraction fraction, Set<Integer> scans, MS2Progress progress, boolean shouldLoadSpectra, boolean shouldLoadRetentionTimes)
    {
        File mzXmlFile = getMzXMLFile(fraction);
        if ((_run.getType().equalsIgnoreCase(MS2RunType.Mascot.name())||_run.getType().equalsIgnoreCase(MS2RunType.Sequest.name()))   // TODO: Move this check (perhaps all the code) into the appropriate run classes
                && null == mzXmlFile)
        {
            // we attempt to load spectra from .mzXML rather than .pep.tgz
            // (that is, the faked-up .out and .dta files from Mascot2XML)
            // generation of .pep.tgz can be turned off via (Mascot2XML -notgz)
            String baseName = _gzFileName;
            baseName = baseName.replaceAll("\\.pep\\.tgz$", "");
            massSpecDataFileType FT_MZXML = new massSpecDataFileType();
            File path = new File(_path,"");
            File engineProtocolMzXMLFile = FT_MZXML.getFile(path, baseName);
            String mzXmlFileName = engineProtocolMzXMLFile.getName();
            File engineProtocolDir = engineProtocolMzXMLFile.getParentFile();
            File engineDir = engineProtocolDir.getParentFile();
            File mzXMLFile = new File(engineDir.getParent(), mzXmlFileName);
            mzXmlFile = mzXMLFile.getAbsoluteFile();
        }
        String gzFileName = _path + "/" + _gzFileName;
        File gzFile = _context.findFile(gzFileName);
        if (gzFile != null)
        {
            gzFileName = gzFile.toString();
        }
        //sequest spectra are imported from the tgz but are deleted after they are imported.
        if(_run.getType().equalsIgnoreCase("sequest") && mzXmlFile != null)   // TODO: Move this check (perhaps all the code) into the appropriate run classes
        {
            if (NetworkDrive.exists(mzXmlFile))
            {
                gzFileName = "";
            }
        }

        SpectrumImporter sl = new SpectrumImporter(gzFileName, "", mzXmlFile, scans, progress, _fractionId, _log, shouldLoadSpectra, shouldLoadRetentionTimes);
        sl.upload();
        updateFractionSpectrumFileName(sl.getFile());
    }


    protected File getMzXMLFile(MS2Loader.PeptideFraction fraction)
    {
        String mzXmlFileName = fraction.getSpectrumPath();

        if (null != mzXmlFileName)
        {
            File dir = new File(_path);
            File f = _context.findFile(mzXmlFileName, dir);
            if (f != null)
            {
                return f;
            }
            File mzXMLFile = new File(mzXmlFileName);
            // Check two directories up from the pepXML file, where the pipeline normally reads the mzXML file.
            if (dir.getParentFile() != null && dir.getParentFile().getParentFile() != null)
            {
                f = new File(dir.getParentFile().getParentFile(), mzXMLFile.getName());
                if (NetworkDrive.exists(f) && f.isFile())
                {
                    return f;
                }

				// Check if it's under an alternate pipeline root instead
				PipeRoot root = PipelineService.get().findPipelineRoot(_container);
				// We wouldn't get this far if we didn't have a pipeline root
				@SuppressWarnings({"ConstantConditions"}) String relativePath = root.relativePath(f);
				if (relativePath != null)
				{
					f = root.resolvePath(relativePath);
				}
				if (NetworkDrive.exists(f) && f.isFile())
				{
					return f;
				}
            }
            f = new File(dir, mzXMLFile.getName());
            if (NetworkDrive.exists(f) && f.isFile())
            {
                return f;
            }
        }

        return mzXmlFileName == null ? null : new File(mzXmlFileName);
    }

    public static boolean isFractionsFile(File pepXmlFile, String joinedBaseName)
    {
        PepXMLFileType ft = new PepXMLFileType();
        String baseName = ft.getBaseName(pepXmlFile);
        return ft.isType(pepXmlFile) &&
                (AbstractFileAnalysisProtocol.LEGACY_JOINED_BASENAME.equals(baseName) || joinedBaseName.equals(baseName));
    }
}
