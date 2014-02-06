/*
 * Copyright (c) 2005-2014 LabKey Corporation
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
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.exp.XarContext;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.security.User;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.massSpecDataFileType;
import org.labkey.ms2.protein.FastaDbLoader;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.reader.AbstractQuantAnalysisResult;
import org.labkey.ms2.reader.PepXmlLoader;
import org.labkey.ms2.reader.PepXmlLoader.FractionIterator;
import org.labkey.ms2.reader.PepXmlLoader.PepXmlFraction;
import org.labkey.ms2.reader.PepXmlLoader.PepXmlPeptide;
import org.labkey.ms2.reader.PepXmlLoader.PeptideIterator;
import org.labkey.ms2.reader.PeptideProphetHandler;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.RelativeQuantAnalysisSummary;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PepXmlImporter extends MS2Importer
{
    protected String _gzFileName = null;
    private Collection<String> _scoreColumnNames;
    private List<RelativeQuantAnalysisSummary> _quantSummaries;
    private boolean _scoringAnalysis;
    private MS2Run _run = null;

    private static final int BATCH_SIZE = 100;

    public PepXmlImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(context, user, c, description, fullFileName, log);
    }


    public String getType()
    {
        return _run != null ? _run.getType() : null;
    }


    @Override
    public void importRun(MS2Progress progress) throws SQLException, XMLStreamException, IOException
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
                HashSet<Integer> scans = new HashSet<>(shouldImportSpectra ? 1000 : 0);
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
        finally
        {
            if (null != loader)
                loader.close();
        }
    }


    protected void writePeptideProphetSummary(int runId, PeptideProphetSummary summary) throws SQLException
    {
        if (null != summary)
        {
            summary.setRun(runId);
            Table.insert(_user, MS2Manager.getTableInfoPeptideProphetSummaries(), summary);
            Table.update(_user, MS2Manager.getTableInfoRuns(), PageFlowUtil.map("HasPeptideProphet", true), _runId);
        }
    }

    /**
     * Save relative quantitation summary information (for XPRESS, Q3, etc.)
     */
    protected void writeQuantSummaries(int runId, List<RelativeQuantAnalysisSummary> quantSummaries) throws SQLException
    {
        if (null == quantSummaries)
            return;

        // For now, we only support one set of quantitation results per run
        if (quantSummaries.size() > 1)
            throw new RuntimeException("Cannot import runs that contain more than one set of quantitation results");

        for (RelativeQuantAnalysisSummary summary : quantSummaries)
        {
            summary.setRun(runId);
            Table.insert(_user, MS2Manager.getTableInfoQuantSummaries(), summary);
        }
    }

    protected void writeRunInfo(PepXmlFraction fraction, MS2Progress progress) throws SQLException, IOException
    {
        String databaseLocalPath = fraction.getDatabaseLocalPath();

        _run = MS2Run.getRunFromTypeString(fraction.getSearchEngine(), fraction.getSearchEngineVersion());
        _scoreColumnNames = _run.getPepXmlScoreColumnNames();

        Map<String, Object> m = new HashMap<>();
        m.put("Type", _run.getType());
        m.put("SearchEngine", fraction.getSearchEngine());
        m.put("MassSpecType", fraction.getMassSpecType());
        m.put("SearchEnzyme", fraction.getSearchEnzyme());

        // If path to fasta is relative, prepend current dir
        if (! isAbsolute(databaseLocalPath))
            databaseLocalPath = _path + "/" + databaseLocalPath;

        try
        {
            updateRunStatus("Importing FASTA file");
            progress.getCumulativeTimer().setCurrentTask(Tasks.ImportFASTA, databaseLocalPath);
            int fastaId = FastaDbLoader.loadAnnotations(_path, databaseLocalPath, FastaDbLoader.UNKNOWN_ORGANISM, true, _log, _context);

            _scoringAnalysis = new SqlSelector(ProteinManager.getSchema(),
                    "SELECT ScoringAnalysis " +
                    "FROM " + ProteinManager.getTableInfoFastaFiles() + " " +
                    "WHERE FastaId = ?", fastaId).getObject(Boolean.class);

            m.put("FastaId", fastaId);
        }
        finally
        {
            // The FastaDbLoader may throw an exception, but we still want to update the run with its Type,
            // or we'll never be able to load it again.
            Table.update(_user, MS2Manager.getTableInfoRuns(), m, _runId);
        }

        for (MS2Modification mod : fraction.getModifications())
        {
            mod.setRun(_runId);
            Table.insert(_user, MS2Manager.getTableInfoModifications(), mod);
        }

        prepareStatement();
    }


    private void writeFractionInfo(PepXmlFraction fraction) throws SQLException, IOException
    {
        String dataSuffix = fraction.getDataSuffix();
        String baseName = fraction.getDataBasename();
        String newFilename = new File(baseName).getName();
        // Build the name of the tgz file
        if(fraction.isSequest())
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
            massSpecDataFileType FT_MZXML = new  massSpecDataFileType();
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

    /**
     * Check to see if the given filename is absolute ("c:/foo", "\\fred\foo",
     * "\foo", "/foo"). Note that the last two are relative according to the
     * standard FileSystem implementation for Windows
     */
    protected static boolean isAbsolute(String filename)
    {
        if (filename == null) return false;
        if (filename.startsWith("\\") || filename.startsWith("/") || (filename.length() > 2 && Character.isLetter(filename.charAt(0)) && ':' == filename.charAt(1)))
            return true;
        File f = new File(filename);
        return f.isAbsolute();
    }


    protected void processSpectrumFile(PepXmlFraction fraction, HashSet<Integer> scans, MS2Progress progress, boolean shouldLoadSpectra, boolean shouldLoadRetentionTimes) throws SQLException
    {
        File mzXmlFile = getMzXMLFile(fraction);
        if ((_run.getType().equalsIgnoreCase("mascot")||_run.getType().equalsIgnoreCase("sequest"))   // TODO: Move this check (perhaps all the code) into the appropriate run classes
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


    protected File getMzXMLFile(PepXmlFraction fraction)
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


    protected String getTableColumnNames()
    {
        StringBuilder columnNames = new StringBuilder();

        for (int i = 0; i < _scoreColumnNames.size(); i++)
        {
            columnNames.append(", Score");
            columnNames.append(i + 1);
        }

        return super.getTableColumnNames() + columnNames.toString();
    }


    protected void setPeptideParameters(PreparedStatement stmt, PepXmlPeptide peptide, PeptideProphetSummary peptideProphetSummary) throws SQLException
    {
        int n = 1;

        stmt.setInt(n++, _fractionId);
        stmt.setInt(n++, peptide.getScan());
        if (peptide.getScan() == peptide.getEndScan())
            stmt.setNull(n++, Types.INTEGER);
        else
            stmt.setInt(n++, peptide.getEndScan());

        Double retentionTime = peptide.getRetentionTime();
        if (retentionTime == null)
            stmt.setNull(n++, Types.FLOAT);
        else
            stmt.setFloat(n++, retentionTime.floatValue());

        stmt.setInt(n++, peptide.getCharge());
        stmt.setFloat(n++, peptide.getIonPercent());

        // Convert calculated neutral mass into calculated MH+
        // Store as double so mass + deltaMass returns high mass accuracy precursor
        stmt.setDouble(n++, peptide.getCalculatedNeutralMass() + MS2Peptide.pMass);
        stmt.setFloat(n++, peptide.getDeltaMass());

        PeptideProphetHandler.PeptideProphetResult pp = peptide.getPeptideProphetResult();
        stmt.setFloat(n++, (null == pp ? 0 : pp.getProbability()));

        Float errorRate = null;
        if (peptideProphetSummary != null && pp != null)
        {
            errorRate = peptideProphetSummary.calculateErrorRate(pp.getProbability());
        }

        if (errorRate != null)
        {
            stmt.setFloat(n++, errorRate);
        }
        else
        {
            stmt.setNull(n++, Types.REAL);
        }

        stmt.setString(n++, peptide.getPeptide());
        stmt.setString(n++, peptide.getPrevAA());
        stmt.setString(n++, peptide.getTrimmedPeptide());
        stmt.setString(n++, peptide.getNextAA());
        stmt.setInt(n++, peptide.getProteinHits());
        stmt.setString(n++, peptide.getProtein());

        Map<String, String> scores = peptide.getScores();
        _run.adjustScores(scores);

        for (String scoreColumnName : _scoreColumnNames)
        {
            String value = scores.get(scoreColumnName);  // Get value from the scores parsed from XML
            setAsFloat(stmt, n++, value);
        }
    }


    private void setAsFloat(PreparedStatement stmt, int n, String v) throws SQLException
    {
        if (v == null)
            stmt.setNull(n, java.sql.Types.FLOAT);
        else
        {
            float f = Float.valueOf(v);
            if (f >= 0 && f < 1e-30)
            {
                f = 1e-30f;
            }
            stmt.setFloat(n, f);
        }
    }

    protected void write(PepXmlPeptide peptide, PeptideProphetSummary peptideProphetSummary) throws SQLException
    {
        // If we have quantitation, use the statement that reselects the rowId; otherwise, use the simple insert statement
        PeptideProphetHandler.PeptideProphetResult pp = peptide.getPeptideProphetResult();
        boolean hasProphet = (_scoringAnalysis && pp != null && pp.isSummaryLoaded());
        boolean hasQuant = (null != _quantSummaries && _quantSummaries.size() > 0);

        if (hasProphet || hasQuant)
        {
            // Execute insert with peptideId reselect
            setPeptideParameters(_stmtWithReselect, peptide, peptideProphetSummary);
            ResultSet rs = null;
            long peptideId;

            try
            {
                rs = MS2Manager.getSqlDialect().executeWithResults(_stmtWithReselect);

                if (null == rs)
                    throw new IllegalArgumentException("No peptideID reselected after peptide insert");

                if (!rs.next())
                    throw new IllegalArgumentException("No peptideID found in result set");

                peptideId = rs.getLong(1);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }

            if (hasProphet)
            {
                int index = 1;
                try
                {
                    _prophetStmt.setLong(index++, peptideId);
                    _prophetStmt.setFloat(index++, pp.getProphetFval());
                    _prophetStmt.setFloat(index++, pp.getProphetDeltaMass());
                    _prophetStmt.setInt(index++, pp.getProphetNumTrypticTerm());
                    _prophetStmt.setInt(index++, pp.getProphetNumMissedCleav());
                    _prophetStmt.executeUpdate();
                }
                catch (SQLException e)
                {
                    _log.error("Failed to insert prophet info for scan " + peptide.getScan() + " with charge " + peptide.getCharge() + " from " + _gzFileName);
                    throw e;
                }
            }

            if (hasQuant)
            {
                // Loop over and insert any quantitation analysis results
                for (RelativeQuantAnalysisSummary summary : _quantSummaries)
                {
                    AbstractQuantAnalysisResult result = (AbstractQuantAnalysisResult)peptide.getAnalysisResult(summary.getAnalysisType());
                    if (result != null)
                    {
                        result.setPeptideId(peptideId);
                        result.setQuantId(summary.getQuantId());

                        try
                        {
                            result.insert(this);
                        }
                        catch (SQLException e)
                        {
                            _log.error("Failed to insert quantitation info for scan " + peptide.getScan() + " with charge " + peptide.getCharge() + " from " + _gzFileName);
                            throw e;
                        }
                    }
                }
            }
        }
        else
        {
            try
            {
                setPeptideParameters(_stmt, peptide, peptideProphetSummary);
                _stmt.execute();
            }
            catch (SQLException e)
            {
                _log.error("Failed to insert scan " + peptide.getScan() + " with charge " + peptide.getCharge() + " from " + _gzFileName);
                throw e;
            }

        }
    }

    public static boolean isFractionsFile(File pepXmlFile, String joinedBaseName)
    {
        PepXMLFileType ft = new PepXMLFileType();
        String baseName = ft.getBaseName(pepXmlFile);
        return ft.isType(pepXmlFile) &&
                (AbstractFileAnalysisProtocol.LEGACY_JOINED_BASENAME.equals(baseName) || joinedBaseName.equals(baseName));
    }
}
