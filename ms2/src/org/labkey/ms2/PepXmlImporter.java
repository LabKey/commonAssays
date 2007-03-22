/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.data.Table;
import org.labkey.api.exp.XarContext;
import org.labkey.api.security.User;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.common.tools.*;
import org.labkey.common.tools.PepXmlLoader.FractionIterator;
import org.labkey.common.tools.PepXmlLoader.PepXmlFraction;
import org.labkey.common.tools.PepXmlLoader.PepXmlPeptide;
import org.labkey.common.tools.PepXmlLoader.PeptideIterator;
import org.labkey.ms2.protein.FastaDbLoader;
import org.labkey.ms2.protein.ProteinManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class PepXmlImporter extends MS2Importer
{
    protected String _gzFileName = null;
    private String _type = null;
    private Map<String, Integer> _scoreMap;
    private List<RelativeQuantAnalysisSummary> _quantSummaries;
    private boolean _scoringAnalysis;

    private static final int BATCH_SIZE = 100;

    public PepXmlImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(context);
        initFileUpload(user, c, description, fullFileName, log);
    }


    public String getType()
    {
        return _type;
    }


    public void uploadRun() throws SQLException, XMLStreamException, IOException
    {
        PepXmlLoader loader = null;

        try
        {
            boolean runUpdated = false;  // Set to true after we update the run information (after loading the first fraction)

            File f = new File(_path + "/" + _fileName);
            NetworkDrive.ensureDrive(f.getPath());
            loader = new PepXmlLoader(f, _log);

            PeptideProphetSummary summary = loader.getPeptideProphetSummary();
            writePeptideProphetSummary(_runId, summary);

            _quantSummaries = loader.getQuantSummaries();
            writeQuantSummaries(_runId, _quantSummaries);

            FractionIterator fi = loader.getFractionIterator();
            MS2Progress progress = new MS2Progress();

            while (fi.hasNext())
            {
                PepXmlFraction fraction = (PepXmlFraction) fi.next();

                if (!runUpdated)  // do this for the first fraction only
                {
                    writeRunInfo(fraction);
                    progress.setMs2FileInfo(loader.getFileLength(), loader.getCurrentOffset());
                    runUpdated = true;
                }

                progress.setPeptideMode();
                writeFractionInfo(fraction);

                PeptideIterator pi = fraction.getPeptideIterator();
                boolean shouldLoadSpectra = fraction.shouldLoadSpectra();
                progress.setLoadSpectra(shouldLoadSpectra);
                // Initialize scans to a decent size, but only if we're going to load spectra
                HashSet<Integer> scans = new HashSet<Integer>(shouldLoadSpectra ? 1000 : 0);
                _conn.setAutoCommit(false);

                boolean retentionTimesInPepXml = false;
                int count = 0;

                while (pi.hasNext())
                {
                    PepXmlPeptide peptide = pi.next();

                    // If any peptide in the pep.xml file has retention time then don't load retention times from mzXML
                    if (null != peptide.getRetentionTime())
                        retentionTimesInPepXml = true;

                    // Mascot exported pepXML may contain unassigned spectrum
                    // we omit them for the uploading
                    if (null != peptide.getTrimmedPeptide())
                    {
	                    write(peptide, summary);

                        if (shouldLoadSpectra)
                            scans.add(peptide.getScan());

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
                processSpectrumFile(fraction, scans, progress, shouldLoadSpectra, !retentionTimesInPepXml);
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
            Table.update(_user, MS2Manager.getTableInfoRuns(), PageFlowUtil.map("HasPeptideProphet", true), _runId, null);
        }
    }

    /**
     * Save relatative quantitation summary information (for XPRESS, Q3, etc.)
     */
    protected void writeQuantSummaries(int runId, List<RelativeQuantAnalysisSummary> quantSummaries) throws SQLException
    {
        if (null == quantSummaries)
            return;

        // For now, we only support one set of quantitation results per run
        if (quantSummaries.size() > 1)
            throw new RuntimeException("Can not currently upload runs that contain more than one set of quantitation results");

        for (RelativeQuantAnalysisSummary summary : quantSummaries)
        {
            summary.setRun(runId);
            Table.insert(_user, MS2Manager.getTableInfoQuantSummaries(), summary);
        }
    }

    protected void writeRunInfo(PepXmlFraction fraction) throws SQLException, IOException
    {
        String databaseLocalPath = fraction.getDatabaseLocalPath();

        MS2Run ms2run = MS2Run.getRunFromTypeString(fraction.getSearchEngine());

        _type = ms2run.getType();
        _scoreMap = ms2run.getPepXmlScoreMap();

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("Type", _type);
        m.put("SearchEngine", fraction.getSearchEngine());
        m.put("MassSpecType", fraction.getMassSpecType());
        m.put("SearchEnzyme", fraction.getSearchEnzyme());

        // If path to fasta is relative, prepend current dir
        if (! isAbsolute(databaseLocalPath))
            databaseLocalPath = _path + "/" + databaseLocalPath;

        try
        {
            updateRunStatus("Loading FASTA file");
            _log.info("Loading FASTA file");
            int fastaId = FastaDbLoader.loadAnnotations(_path, databaseLocalPath, FastaDbLoader.UNKNOWN_ORGANISM, true, _log, _context);

            _scoringAnalysis = Table.executeSingleton(ProteinManager.getSchema(),
                    "SELECT ScoringAnalysis " +
                    "FROM " + ProteinManager.getTableInfoFastaFiles() + " " +
                    "WHERE FastaId = ?", new Object[] {fastaId}, Boolean.class);

            m.put("FastaId", fastaId);
        }
        finally
        {
            // The FastaDbLoader may throw an exception, but we still want to update the run with its Type,
            // or we'll never be able to load it again.
            Table.update(_user, MS2Manager.getTableInfoRuns(), m, _runId, null);
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

        // Build the name of the tgz file
        if(fraction.getSearchEngine().equalsIgnoreCase("sequest"))
        {
             int i = baseName.lastIndexOf("/");
            String newFilename =
                    (i < 0 ? baseName : baseName.substring(i + 1));
           _gzFileName =  newFilename + "." + "pep." + dataSuffix;
            //No spectrumPath in a sequest pepXML file.
            File sampleDir = new File(_path).getParentFile().getParentFile();
            fraction.setSpectrumPath(new File(sampleDir, newFilename + ".mzXML").getAbsolutePath());
        }
        else
        {
            _gzFileName = switchSuffix(_fileName, dataSuffix);
        }
        if (! NetworkDrive.exists(new File(_path + "/" + _gzFileName)) &&
                baseName != null)
        {
            // Try using the base_name from the input file
            int i = baseName.lastIndexOf("/");
            String newFilename =
                    (i < 0 ? baseName : baseName.substring(i + 1));
            //newFilename = switchSuffix(newFilename, dataSuffix);
            newFilename += "." + dataSuffix;
            if (NetworkDrive.exists(new File(_path + "/" + newFilename)))
                _gzFileName = newFilename;
        }

        String mzXMLFileName = getMzXMLFileName(fraction);
        File mzXMLFile = mzXMLFileName == null ? null : new File(mzXMLFileName);
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
        if (filename.startsWith("\\") || filename.startsWith("/"))
            return true;
        File f = new File(filename);
        return f.isAbsolute();
    }


    protected void processSpectrumFile(PepXmlFraction fraction, HashSet<Integer> scans, MS2Progress progress, boolean shouldLoadSpectra, boolean shouldLoadRetentionTimes) throws SQLException
    {
        String mzXmlFileName = getMzXMLFileName(fraction);
        if ((_type.equalsIgnoreCase("mascot")||_type.equalsIgnoreCase("sequest"))
                && null == mzXmlFileName)
        {
            // we attempt to load spectra from .mzXML rather than .pep.tgz
            // generation of .pep.tgz can be turned off via (Mascot2XML -notgz)
            mzXmlFileName = _gzFileName;
            mzXmlFileName = mzXmlFileName.replaceAll("\\.pep\\.tgz$", ".mzXML");
            File engineProtocolMzXMLFile = new File(_path, mzXmlFileName);
            File engineProtocolDir = engineProtocolMzXMLFile.getParentFile();
            File engineDir = engineProtocolDir.getParentFile();
            File mzXMLFile = new File(engineDir.getParent(), mzXmlFileName);
            mzXmlFileName = mzXMLFile.getAbsolutePath();
        }
        String gzFileName = _path + "/" + _gzFileName;
        File gzFile = _context.findFile(gzFileName);
        if (gzFile != null)
        {
            gzFileName = gzFile.toString();
        }
        //sequest spectra are loaded from the tgz but are deleted after they are loaded.
        if(_type.equalsIgnoreCase("sequest") && mzXmlFileName != null)
        {
            if (NetworkDrive.exists(new File(mzXmlFileName)))
            {
                gzFileName = "";
            }
        }

        SpectrumLoader sl = new SpectrumLoader(gzFileName, "", mzXmlFileName, scans, progress, _fractionId, _log, shouldLoadSpectra, shouldLoadRetentionTimes);
        sl.upload();
        updateFractionSpectrumFileName(sl.getFile());
    }


    protected String getMzXMLFileName(PepXmlFraction fraction)
    {
        String mzXmlFileName = fraction.getSpectrumPath();

        if (null != mzXmlFileName)
        {
            File f = _context.findFile(mzXmlFileName, new File(_path));
            if (f != null)
            {
                return f.toString();
            }
        }

        return mzXmlFileName;
    }


    protected String getTableColumnNames()
    {
        StringBuffer columnNames = new StringBuffer();

        for (int i = 0; i < _scoreMap.size(); i++)
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
            stmt.setFloat(n++, errorRate.floatValue());
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

        Set<Map.Entry<String, Integer>> requiredScores = _scoreMap.entrySet();
        Map<String, String> scores = peptide.getScores();

        for (Map.Entry<String, Integer> score : requiredScores)
        {
            String key = score.getKey();     // Get next desired score
            String value = scores.get(key);  // Get value from the scores parsed from XML
//wch: mascotdev
            // Mascot exported pepXML can exclude "homologyscore"
            if (null == value)
            {
                if (key.equalsIgnoreCase("homologyscore"))
                {
                    value = "-1";
                }
            }
//END-wch: mascotdev
            setAsFloat(stmt, n + score.getValue(), value);
        }
    }


    // TODO: Change this?
    private void setAsFloat(PreparedStatement stmt, int n, String v) throws SQLException
    {
        if (v == null)
            stmt.setNull(n, java.sql.Types.FLOAT);
        else
            stmt.setFloat(n, Float.valueOf(v));
    }

    private long getPeptideId(PreparedStatement stmt) throws SQLException
    {
        ResultSet rs = null;

        try
        {
            if (!stmt.getMoreResults())
                throw new IllegalArgumentException("No peptideID reselected after peptide insert");
            rs = stmt.getResultSet();
            if (!rs.next())
                throw new IllegalArgumentException("No peptideID found in result set");
            
            return rs.getLong(1);
        }
        finally
        {
            if (null != rs)
                rs.close();
        }
    }

    protected void write(PepXmlPeptide peptide, PeptideProphetSummary peptideProphetSummary) throws SQLException
    {
        // If we have relative quantitation (XPress or Q3), use the statement that reselects the rowId; otherwise, use the simple insert statement
        PeptideProphetHandler.PeptideProphetResult pp = peptide.getPeptideProphetResult();
        boolean hasProphet = (_scoringAnalysis && pp != null && pp.isSummaryLoaded());
        boolean hasQuant = (null != _quantSummaries && _quantSummaries.size() > 0);

        PreparedStatement stmt = (hasProphet || hasQuant ? _stmtWithReselect : _stmt);

        setPeptideParameters(stmt, peptide, peptideProphetSummary);
        stmt.execute();

        long peptideId = -1;
        if (hasProphet)
        {
            peptideId = getPeptideId(stmt);
            int index = 1;
            _prophetStmt.setLong(index++, peptideId);
            _prophetStmt.setFloat(index++, pp.getProphetFval());
            _prophetStmt.setFloat(index++, pp.getProphetDeltaMass());
            _prophetStmt.setInt(index++, pp.getProphetNumTrypticTerm());
            _prophetStmt.setInt(index++, pp.getProphetNumMissedCleav());
            _prophetStmt.executeUpdate();
        }
        if (hasQuant)
        {
            if (peptideId == -1)
                peptideId = getPeptideId(stmt);

            // Loop over and insert any quantitation analysis results
            for (RelativeQuantAnalysisSummary summary : _quantSummaries)
            {
                RelativeQuantAnalysisResult quant =
                    (RelativeQuantAnalysisResult)peptide.getAnalysisResult(summary.getAnalysisType());
                if (null != quant)
                {
                    quant.setPeptideId(peptideId);
                    quant.setQuantId(summary.getQuantId());

                    int index = 1;
                    _quantStmt.setLong(index++, quant.getPeptideId());
                    _quantStmt.setInt(index++, quant.getLightFirstscan());
                    _quantStmt.setInt(index++, quant.getLightLastscan());
                    _quantStmt.setFloat(index++, quant.getLightMass());
                    _quantStmt.setInt(index++, quant.getHeavyFirstscan());
                    _quantStmt.setInt(index++, quant.getHeavyLastscan());
                    _quantStmt.setFloat(index++, quant.getHeavyMass());
                    if (quant instanceof XPressHandler.XPressResult)
                    {
                        XPressHandler.XPressResult xpressQuant = (XPressHandler.XPressResult)quant;
                        if (xpressQuant.getRatio() != null)
                        {
                            _quantStmt.setString(index++, xpressQuant.getRatio());
                        }
                        else
                        {
                            _quantStmt.setNull(index++, Types.VARCHAR);
                        }
                        if (xpressQuant.getHeavy2lightRatio() != null)
                        {
                            _quantStmt.setString(index++, xpressQuant.getHeavy2lightRatio());
                        }
                        else
                        {
                            _quantStmt.setNull(index++, Types.VARCHAR);
                        }
                    }
                    else
                    {
                        _quantStmt.setNull(index++, Types.VARCHAR);
                        _quantStmt.setNull(index++, Types.VARCHAR);
                    }
                    _quantStmt.setFloat(index++, quant.getLightArea());
                    _quantStmt.setFloat(index++, quant.getHeavyArea());
                    _quantStmt.setFloat(index++, quant.getDecimalRatio());
                    _quantStmt.setInt(index++, quant.getQuantId());

                    _quantStmt.executeUpdate();
                }
            }
        }
    }

    public static boolean isFractionsFile(File pepXmlFile)
    {
        return pepXmlFile.getName().toLowerCase().equals("all.pep.xml");
    }
}
