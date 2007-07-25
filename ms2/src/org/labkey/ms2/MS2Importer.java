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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.exp.Data;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.util.CsvSet;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.common.tools.Rounder;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: arauch
 * Date: Aug 18, 2005
 * Time: 1:56:35 AM
 */
public abstract class MS2Importer
{
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    private static final String UPLOAD_STARTED = "Loading... (refresh to check status)";
    private static final String UPLOAD_SUCCEEDED = "";

    protected User _user;
    protected Container _container;
    protected String _description, _fileName, _path;
    protected Connection _conn = null;
    protected PreparedStatement _stmt = null;
    protected PreparedStatement _stmtWithReselect = null;
    protected PreparedStatement _prophetStmt = null;
    protected PreparedStatement _quantStmt = null;
    protected int _runId, _fractionId;
    protected long _startTime;

    // Use passed in logger for upload status, information, and file format problems.  This should
    // end up in the pipeline log.
    protected Logger _log = null;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static Logger _systemLog = Logger.getLogger(MS2Importer.class);
    protected final XarContext _context;

    public MS2Importer(XarContext context, User user, Container c, String description, String fullFileName, Logger log)
    {
        _context = context;
        _user = user;
        _container = c;

        String pathFile = fullFileName.replace('\\', '/');
        int index = pathFile.lastIndexOf('/');

        _path = (-1 == index ? "" : pathFile.substring(0, index));
        _fileName = (-1 == index ? pathFile : pathFile.substring(index + 1));

        if (null != description)
            _description = description;
        else
        {
            int extension = _fileName.lastIndexOf(".");

            if (-1 != extension)
                _description = _fileName.substring(0, extension);
        }

        _log = (null == log ? _systemLog : log);
    }


    private static final Object _schemaLock = new Object();

    public static class RunInfo implements Serializable
    {
        private final int _runId;
        private final boolean _alreadyLoaded;

        private RunInfo(int runId, boolean alreadyLoaded)
        {
            _runId = runId;

            _alreadyLoaded = alreadyLoaded;
        }

        public int getRunId()
        {
            return _runId;
        }

        public boolean isAlreadyLoaded()
        {
            return _alreadyLoaded;
        }
    }

    protected RunInfo prepareRun(boolean restart) throws SQLException
    {
        try
        {
            boolean alreadyLoaded = false;
            MS2Manager.getSchema().getScope().beginTransaction();
            synchronized (_schemaLock)
            {
                // Don't upload if we've already loaded this file (undeleted run exists matching this file name)
                _runId = getRun();
                if (_runId != -1)
                {
                    if (!restart)
                    {
                        alreadyLoaded = true;
                    }
                    else
                    {
                        _log.info("Restarting upload from \"" + _fileName + "\"");
                    }
                }
                else
                {
                    _log.info("Starting upload from \"" + _fileName + "\"");
                    _runId = createRun();
                }
            }

            MS2Manager.getSchema().getScope().commitTransaction();
            return new RunInfo(_runId, alreadyLoaded);
        }
        finally
        {
            MS2Manager.getSchema().getScope().closeConnection();
        }
    }

    protected int upload(RunInfo info) throws SQLException, IOException, XMLStreamException
    {
        _runId = info.getRunId();

        MS2Run run = MS2Manager.getRun(_runId);

        // Skip if run was already fully imported
        if (info.isAlreadyLoaded() && run != null && run.getStatusId() == MS2Importer.STATUS_SUCCESS)
        {
            _log.info(_fileName + " has already been loaded so it does not need to be reloaded");
            return info.getRunId();
        }

        if (_startTime == 0)
        {
            _startTime = System.currentTimeMillis();
        }

        try
        {
            _log.info("Clearing out existing MS2 data for " + _fileName);
            updateRunStatus(UPLOAD_STARTED);
            MS2Manager.clearRun(_runId);
            _log.info("Finished clearing out existing MS2 data for " + _fileName);

            uploadRun();
            updatePeptideColumns();
            updateCounts();
        }
        catch (FileNotFoundException fnfe)
        {
            logError("MS2 data upload failed due to a missing file.", fnfe);
            updateRunStatus("Upload failed (see pipeline log)", STATUS_FAILED);
            throw fnfe;
        }
        catch (SQLException e)
        {
            logError("MS2 data upload failed", e);
            updateRunStatus("Upload failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (IOException e)
        {
            logError("MS2 data upload failed", e);
            updateRunStatus("Upload failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (XMLStreamException e)
        {
            logError("MS2 data upload failed", e);
            updateRunStatus("Upload failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (RuntimeException e)
        {
            logError("MS2 data upload failed", e);
            updateRunStatus("Upload failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        finally
        {
            close();
        }

        updateRunStatus(UPLOAD_SUCCEEDED, STATUS_SUCCESS);

        MS2Manager.computeBasicMS2Stats();       // Update runs/peptides statistics
        logElapsedTime(_startTime, "upload \"" + _fileName + "\"");
        return info.getRunId();
    }


    abstract public void uploadRun() throws IOException, SQLException, XMLStreamException;

    abstract protected String getType();

    protected void close()
    {
        try
        {
            if (null != _stmt)
                _stmt.close();
        }
        catch (SQLException e)
        {
            logError("Error closing simple prepared statement", e);
        }

        try
        {
            if (null != _stmtWithReselect)
                _stmtWithReselect.close();
        }
        catch (SQLException e)
        {
            logError("Error closing reselect prepared statement", e);
        }

        try
        {
            if (null != _quantStmt)
                _quantStmt.close();
        }
        catch (SQLException e)
        {
            logError("Error closing quantitation prepared statement", e);
        }

        try
        {
            if (null != _prophetStmt)
                _prophetStmt.close();
        }
        catch (SQLException e)
        {
            logError("Error closing PeptideProphet data prepared statement", e);
        }

        try
        {
            if (null != _conn)
                MS2Manager.getSchema().getScope().releaseConnection(_conn);
        }
        catch (SQLException e)
        {
            logError("Error releasing connection", e);
        }
    }


    protected int getRun() throws SQLException
    {
        SimpleFilter filter = new SimpleFilter("Path", _path);
        filter.addCondition("FileName", _fileName);
        filter.addCondition("Container", _container.getId());
        filter.addCondition("Deleted", Boolean.FALSE);
        ResultSet rs = Table.select(MS2Manager.getTableInfoRuns(), new CsvSet("Run"), filter, null);

        int runId = -1;

        if (rs.next())
            runId = rs.getInt("Run");

        rs.close();
        return runId;
    }


    protected int createRun() throws SQLException
    {
        HashMap<String, Object> runMap = new HashMap<String, Object>();

        runMap.put("Description", _description);
        runMap.put("Container", _container.getId());
        runMap.put("Path", _path);
        runMap.put("FileName", _fileName);
        runMap.put("Status", UPLOAD_STARTED);
        runMap.put("Type", getType());    // TODO: Change how we handle type: For pepXML, this is null at this point... okay for Comet

        Map returnMap = Table.insert(_user, MS2Manager.getTableInfoRuns(), runMap);
        return (Integer)returnMap.get("Run");
    }


    protected static int createFraction(User user, Container c, int runId, String path, File mzXmlFile) throws SQLException, IOException
    {
        HashMap<String, Object> fractionMap = new HashMap<String, Object>();
        fractionMap.put("Run", runId);

        // Old Comet runs won't have an mzXmlFile
        if (null != mzXmlFile)
        {
            fractionMap.put("MzXMLURL", mzXmlFile.getCanonicalFile().toURI().toString());
            String mzXmlFileName  = mzXmlFile.getName();
            int extensionIndex = mzXmlFileName.lastIndexOf('.');

            String pepXMLFileName;
            if (extensionIndex != -1)
            {
                pepXMLFileName = mzXmlFileName.substring(0, extensionIndex) + ".pep.xml";
            }
            else
            {
                pepXMLFileName = mzXmlFileName + ".pep.xml";
            }
            File pepXMLFile = new File(path, pepXMLFileName);

            Data pepXMLData = ExperimentService.get().getDataByURL(pepXMLFile, c);
            if (pepXMLData != null)
            {
                fractionMap.put("PepXmlDataLSID", pepXMLData.getLSID());
            }
        }

        Map returnMap = Table.insert(user, MS2Manager.getTableInfoFractions(), fractionMap);
        return (Integer) returnMap.get("Fraction");
    }


    // When we first create a fraction record we don't know what the spectrum file name or type is,
    // so update it after loading the spectra.
    protected void updateFractionSpectrumFileName(File spectrumFile) throws SQLException
    {
        if (null != spectrumFile)
        {
            HashMap<String, Object> fractionMap = new HashMap<String, Object>();

            Map<String, Object> existingFraction = Table.selectObject(MS2Manager.getTableInfoFractions(), _fractionId, Map.class);
            if (existingFraction != null && existingFraction.get("mzxmlurl") == null)
            {
                fractionMap.put("mzxmlurl", spectrumFile.toURI().toString());
            }
            fractionMap.put("FileName", spectrumFile.getName());
            Table.update(_user, MS2Manager.getTableInfoFractions(), fractionMap, _fractionId, null);
        }
    }


    private static String _updateSeqIdSql;

    static
    {
        StringBuilder sql = new StringBuilder();

        /*
            UPDATE ms2.PeptidesData
	            SET SeqId = fs.SeqId
	            FROM prot.FastaSequences fs
	            WHERE Fraction IN (SELECT Fraction FROM ms2.Fractions WHERE Run = ?) AND
		            ms2.PeptidesData.Protein = fs.LookupString AND fs.FastaId = ?
         */

        sql.append("UPDATE ");
        sql.append(MS2Manager.getTableInfoPeptidesData());
        sql.append(" SET SeqId = fs.SeqId\nFROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs WHERE Fraction = ? AND ");
        sql.append(MS2Manager.getTableInfoPeptidesData());
        sql.append(".Protein = fs.LookupString AND fs.FastaId = ?");

        _updateSeqIdSql = sql.toString();
    }

    private static String _updateSequencePositionSql;

    static
    {
        String concat = MS2Manager.getSqlDialect().getConcatenationOperator();
        String searchString = "CASE WHEN (PrevAA = ' ' OR PrevAA = '-') THEN '' ELSE CAST(PrevAA AS VARCHAR) END " +
                                concat + " TrimmedPeptide " + concat +
                             " CASE WHEN (NextAA = ' ' OR NextAA = '-') THEN '' ELSE CAST(NextAA AS VARCHAR) END ";
        String positionText = MS2Manager.getSqlDialect().getStringIndexOfFunction(searchString, "ProtSequence");

        _updateSequencePositionSql = "UPDATE " + MS2Manager.getTableInfoPeptidesData() + " SET SequencePosition = COALESCE(\n" +
                "       CASE WHEN PrevAA = ' ' OR PrevAA = '-' THEN\n" +
                "           CASE WHEN NextAA = ' ' OR NextAA = '-' THEN\n" +
                "               CASE WHEN " + positionText + " = 1 AND " + MS2Manager.getSqlDialect().getClobLengthFunction() + "( ProtSequence ) = " + MS2Manager.getSqlDialect().getVarcharLengthFunction() + "( TrimmedPeptide ) THEN 1 END\n" +
                "           ELSE\n" +
                "               CASE WHEN " + positionText + " = 1 THEN 1 END\n" +
                "           END\n" +
                "       ELSE\n" +
                "           CASE WHEN NextAA = ' ' OR NextAA = '-' THEN\n" +
                "               CASE WHEN " + positionText + " = " + MS2Manager.getSqlDialect().getClobLengthFunction() + "( ProtSequence ) - " + MS2Manager.getSqlDialect().getVarcharLengthFunction() + "( TrimmedPeptide ) THEN " + positionText + " + 1 END\n" +
                "           ELSE\n" +
                "               CASE WHEN " + positionText + " > 0 THEN " + positionText + " + 1 END\n" +
                "           END\n" +
                "       END\n" +
                ", 0)\n" +
                "FROM " + ProteinManager.getTableInfoSequences() + " ps\n" +
                "WHERE " + MS2Manager.getTableInfoPeptidesData() + ".SeqId = ps.SeqId AND " + MS2Manager.getTableInfoPeptidesData() + ".Fraction = ?";
    }


    protected void updatePeptideColumns() throws SQLException
    {
        updateRunStatus("Updating peptide columns");
        MS2Run run = MS2Manager.getRun(_runId);
        MS2Fraction[] fractions = run.getFractions();
        int fractionCount = fractions.length;

        long start = System.currentTimeMillis();
        int i = 0;

        for (MS2Fraction fraction : fractions)
        {
            Table.execute(MS2Manager.getSchema(), _updateSeqIdSql, new Object[]{fraction.getFraction(), run.getFastaId()});

            if (fractionCount > 1)
                _log.info("Updating SeqId column: fraction " + (++i) + " out of " + fractionCount);
        }

        logElapsedTime(start, "update SeqId column");

        start = System.currentTimeMillis();
        i = 0;

        for (MS2Fraction fraction : fractions)
        {
            Table.execute(MS2Manager.getSchema(), _updateSequencePositionSql, new Object[]{fraction.getFraction()});

            if (fractionCount > 1)
                _log.info("Updating SequencePosition column: fraction " + (++i) + " out of " + fractionCount);
        }

        logElapsedTime(start, "update SequencePosition column");
    }


    private static String _updateCountsSql = "UPDATE " + MS2Manager.getTableInfoRuns() +
            " SET PeptideCount = (SELECT COUNT(*) AS PepCount FROM " + MS2Manager.getTableInfoPeptides() + " pep WHERE pep.run = " + MS2Manager.getTableInfoRuns() + ".run), " +
                " NegativeHitCount = (SELECT COUNT(*) AS NegHitCount FROM " + MS2Manager.getTableInfoPeptides() + " pep WHERE pep.run = " + MS2Manager.getTableInfoRuns() + ".run AND pep.Protein LIKE ?), " +
                " SpectrumCount = (SELECT COUNT(*) AS SpecCount FROM " + MS2Manager.getTableInfoSpectra() + " spec WHERE spec.run = " + MS2Manager.getTableInfoRuns() + ".run)" +
            " WHERE Run = ?";

    private void updateCounts() throws SQLException
    {
        String negativeHitLike = MS2Manager.getNegativeHitPrefix(_container) + "%";

        Table.execute(MS2Manager.getSchema(), _updateCountsSql, new Object[]{negativeHitLike, _runId});
    }


    protected String getTableColumnNames()
    {
        return "Fraction, Scan, RetentionTime, Charge, IonPercent, Mass, DeltaMass, PeptideProphet, PeptideProphetErrorRate, Peptide, PrevAA, TrimmedPeptide, NextAA, ProteinHits, Protein";
    }


    // TODO: Prepare statement after determining whether we have XPress data or not; reselect RowId only if we do
    protected void prepareStatement() throws SQLException
    {
        _conn = MS2Manager.getSchema().getScope().getConnection();
        String columnNames = getTableColumnNames();
        int columnCount = StringUtils.countMatches(columnNames, ",") + 1;
        String insertSql = "INSERT INTO " + MS2Manager.getTableInfoPeptidesData() + " (" + columnNames + ") VALUES (" + StringUtils.repeat("?, ", columnCount - 1) + "?)";
        String insertWithReselectSql = MS2Manager.getSqlDialect().appendSelectAutoIncrement(insertSql, MS2Manager.getTableInfoPeptidesData(), "RowId");

        _systemLog.debug(insertSql);
        _stmt = _conn.prepareStatement(insertSql);

        _systemLog.debug(insertWithReselectSql);
        _stmtWithReselect = _conn.prepareStatement(insertWithReselectSql);

        _prophetStmt = _conn.prepareStatement("INSERT INTO " + MS2Manager.getTableInfoPeptideProphetData() + " (PeptideId,ProphetFVal,ProphetDeltaMass,ProphetNumTrypticTerm,ProphetNumMissedCleav) VALUES (?,?,?,?,?)");

        _quantStmt = _conn.prepareStatement("INSERT INTO " + MS2Manager.getTableInfoQuantitation() + "(PeptideId, LightFirstScan, LightLastScan, LightMass, HeavyFirstScan, HeavyLastScan, HeavyMass, Ratio, Heavy2LightRatio, LightArea, HeavyArea, DecimalRatio, QuantId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }


    protected void updateRunStatus(String status)
    {
        // Default statusId = running
        updateRunStatus(status, STATUS_RUNNING);
    }


    protected void updateRunStatus(String status, int statusId)
    {
        updateRunStatus(_runId, status, statusId);
    }


    protected static void updateRunStatus(int run, String status)
    {
        // Default statusId = running
        updateRunStatus(run, status, STATUS_RUNNING);
    }


    protected static void updateRunStatus(int run, String status, int statusId)
    {
        try
        {
            Table.execute(MS2Manager.getSchema(), "UPDATE " + MS2Manager.getTableInfoRuns() + " SET Status = ?, StatusId = ? WHERE Run = ?",
                    new Object[]{status, statusId, run});
        }
        catch (SQLException e)
        {
            _systemLog.error(e);
        }
    }


    // Returns the index of the nth occurrence of search string within s, starting from the end
    public static int nthLastIndexOf(String s, String search, int n)
    {
        int index = s.length() + 1;

        for (int i = 0; i < n; i++)
        {
            index = s.lastIndexOf(search, index - 1);
            if (-1 == index) break;
        }

        return index;
    }


    protected void logError(String message)
    {
        _systemLog.error(message);
        _log.error(message);
    }


    protected void logError(String message, Exception e)
    {
        _systemLog.error(message, e);
        _log.error(message, e);
    }


    protected void logElapsedTime(long startTime, String action)
    {
        logElapsedTime(_log, startTime, action);
    }


    public static void logElapsedTime(Logger log, long startTime, String action)
    {
        double seconds = (double) (System.currentTimeMillis() - startTime) / 1000;
        double minutes = seconds / 60;

        log.info(Rounder.round(seconds, 2) + " seconds " + ((minutes > 1) ? ("(" + Rounder.round(seconds / 60, 2) + " minutes) ") : "") + "to " + action);
    }


    // Calculates progress of an MS2 import.  PepXML file progress is based on offset within the peptide portion of the file;
    // progress through each fraction's spectra based on actual number of scans loaded vs. expected.
    protected class MS2Progress
    {
        // Default assumption is equal weighting between scans and spectra... change this value if (for example) loading
        // a spectrum takes significantly longer than loading a peptide.  Value is time to load a peptide as a fraction
        // of total time for loading both a peptide and a spectrum.  Always a fraction of 1.0.
        private static final float DEFAULT_PEPTIDE_WEIGHTING = 0.5f;

        private float _peptideWeighting = DEFAULT_PEPTIDE_WEIGHTING;

        private boolean _peptideMode = true;

        private long _peptideFileSize;
        private long _peptideFileInitialOffset;
        private long _peptideFileCurrentOffset;

        private long _spectrumEstimate;
        private long _spectrumCount;

        private float _peptidePercent;
        private float _spectrumPercent;
        private float _previousSpectrumPercent;

        private Integer _previous = null;

        protected void setMs2FileInfo(long size, long initialOffset)
        {
            _peptideFileSize = size;
            _peptideFileInitialOffset = initialOffset;
            _peptideFileCurrentOffset = initialOffset;
            updateIfChanged();
        }


        protected void setCurrentMs2FileOffset(long offset)
        {
            _peptideFileCurrentOffset = offset;
            updateIfChanged();
        }


        // If we're not loading spectra then use the peptide file progress only (weight the spectrum progress at 0.0).
        // This should work on multiple fraction runs, even those where some fractions have spectra loaded and some
        // don't, since progress in the peptide file is always the master.  Call this method before each fraction is
        // loaded to reset the weights appropriately.
        protected void setLoadSpectra(boolean loadSpectra)
        {
            _peptideWeighting = loadSpectra ? DEFAULT_PEPTIDE_WEIGHTING : 1.0F;
        }


        private float getPeptideWeighting()
        {
            return _peptideWeighting;
        }


        private float getSpectrumWeighting()
        {
            return 1.0F - _peptideWeighting;  // Weightings always total 1.0
        }


        protected void setPeptideMode()
        {
            _peptideMode = true;
            updateIfChanged();
            _previousSpectrumPercent = _peptidePercent;
        }


        protected void setSpectrumMode(long spectrumEstimate)
        {
            _peptideMode = false;
            _spectrumEstimate = spectrumEstimate;
            _spectrumCount = 0;
            updateIfChanged();
        }


        protected void addSpectrum()
        {
            _spectrumCount++;
            updateIfChanged();
        }


        private void updateIfChanged()
        {
            Integer percent = getPercentCompleteIfChanged();

            if (null != percent)
            {
                _log.info("Loading MS/MS results is " + percent + "% complete");
                updateRunStatus("Loading is " + percent + "% complete");
            }
        }


        private Integer getPercentCompleteIfChanged()
        {
            Integer current = getPercentComplete();

            if (current == _previous)
                return null;

            _previous = current;
            return current;
        }


        private int getPercentComplete()
        {
            if (_peptideMode)
                _peptidePercent = ((float) _peptideFileCurrentOffset - _peptideFileInitialOffset)/(_peptideFileSize - _peptideFileInitialOffset);
            else
                _spectrumPercent = _previousSpectrumPercent + (_peptidePercent - _previousSpectrumPercent) * (0 != _spectrumEstimate ? ((float)_spectrumCount / _spectrumEstimate) : 0);

            return Math.round((getPeptideWeighting() * _peptidePercent + getSpectrumWeighting() * _spectrumPercent) * 100);
        }
    }
}
