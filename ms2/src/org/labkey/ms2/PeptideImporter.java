/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.exp.XarContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.ms2.protein.FastaDbLoader;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.reader.AbstractQuantAnalysisResult;
import org.labkey.ms2.reader.MS2Loader;
import org.labkey.ms2.reader.PeptideProphetHandler;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.RelativeQuantAnalysisSummary;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by susanh on 10/25/15.
 */
public abstract class PeptideImporter extends MS2Importer
{
    protected String _gzFileName = null;
    private Collection<FieldKey> _scoreColumnNames;
    protected List<RelativeQuantAnalysisSummary> _quantSummaries;
    private boolean _scoringAnalysis;
    protected MS2Run _run = null;

    /**
     * ProteomeDiscoverer does not write out pepXML file with full fraction information, even for fraction searches
     * Thus, we end up importing data as if it was all from a single fraction, which can cause errors when multiple fractions
     * have IDs on the same scan numbers. Thus, as a hack, assign a "queryNumber" (ala Mascot) to make each ID unique.
     */
    private int _proteomeDiscovererOffset = 0;

    public PeptideImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(context, user, c, description, fullFileName, log);
    }

    public String getType()
    {
        return _run != null ? _run.getType() : null;
    }

    public void writePeptideProphetSummary(int runId, PeptideProphetSummary summary)
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
    public void writeQuantSummaries(int runId, List<RelativeQuantAnalysisSummary> quantSummaries)
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

    public void writeRunInfo(MS2Loader.PeptideFraction fraction, MS2Progress progress) throws SQLException, IOException
    {
        _run = MS2Run.getRunFromTypeString(fraction.getSearchEngine(), fraction.getSearchEngineVersion());
        _scoreColumnNames = _run.getPepXmlScoreColumnNames();

        Map<String, Object> m = new HashMap<>();
        m.put("Type", _run.getType());
        m.put("SearchEngine", fraction.getSearchEngine());
        m.put("MassSpecType", fraction.getMassSpecType());
        m.put("SearchEnzyme", fraction.getSearchEnzyme());
        m.put("MascotFile", fraction.getMascotFile());
        m.put("DistillerRawFile", fraction.getDistillerRawFile());

        List<String> dbPaths = new ArrayList<>();

        // If path to fasta is relative, prepend current dir
        for (String dbPath : fraction.getDatabaseLocalPaths())
        {
            if (! isAbsolute(dbPath))
                dbPath = _path + "/" + dbPath;
            dbPaths.add(dbPath);
        }

        try
        {
            // Clear any stale values so we can re-insert
            new SqlExecutor(MS2Manager.getSchema()).execute(new SQLFragment("DELETE FROM " + MS2Manager.getTableInfoFastaRunMapping() + " WHERE Run = ?", _runId));

            for (String dbPath : dbPaths)
            {
                updateRunStatus("Importing FASTA file");
                progress.getCumulativeTimer().setCurrentTask(Tasks.ImportFASTA, dbPath);
                int fastaId = FastaDbLoader.loadAnnotations(_path, dbPath, FastaDbLoader.UNKNOWN_ORGANISM, true, _log, _context);

                // Insert a row into the run/FASTA mapping table
                new SqlExecutor(MS2Manager.getSchema()).execute(new SQLFragment("INSERT INTO " + MS2Manager.getTableInfoFastaRunMapping() + " (Run, FastaId) VALUES (?, ?)", _runId, fastaId));

                _scoringAnalysis |= new SqlSelector(ProteinManager.getSchema(),
                        "SELECT ScoringAnalysis " +
                                "FROM " + ProteinManager.getTableInfoFastaFiles() + " " +
                                "WHERE FastaId = ?", fastaId).getObject(Boolean.class);
            }
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


    protected String getTableColumnNames()
    {
        StringBuilder columnNames = new StringBuilder();

        for (int i = 0; i < _scoreColumnNames.size(); i++)
        {
            columnNames.append(", Score");
            columnNames.append(i + 1);
        }
        columnNames.append(", QueryNumber");
        columnNames.append(", HitRank");
        columnNames.append(", Decoy");

        return super.getTableColumnNames() + columnNames.toString();
    }


    protected void setPeptideParameters(PreparedStatement stmt, MS2Loader.Peptide peptide, PeptideProphetSummary peptideProphetSummary) throws SQLException
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
        stmt.setFloat(n++, peptide.getIonPercent().floatValue());

        // Convert calculated neutral mass into calculated MH+
        // Store as double so mass + deltaMass returns high mass accuracy precursor
        stmt.setDouble(n++, peptide.getCalculatedNeutralMass() + MS2Peptide.pMass);
        stmt.setFloat(n++, peptide.getDeltaMass().floatValue());

        PeptideProphetHandler.PeptideProphetResult pp = peptide.getPeptideProphetResult();
        if (pp == null)
            stmt.setNull(n++, Types.FLOAT);
        else
            stmt.setFloat(n++, pp.getProbability());

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

        // ProteomeDiscoverer exports lack a previous and next AA value
        boolean likelyProteomeDiscovererImport = peptide.getPrevAA() == null && peptide.getNextAA() == null;

        stmt.setString(n++, peptide.getPeptide());
        stmt.setString(n++, peptide.getPrevAA() == null ? "?" : peptide.getPrevAA());
        stmt.setString(n++, peptide.getTrimmedPeptide());
        stmt.setString(n++, peptide.getNextAA() == null ? "?" : peptide.getNextAA());
        stmt.setInt(n++, peptide.getProteinHits());
        stmt.setString(n++, peptide.getProtein());

        Map<String, String> scores = peptide.getScores();
        _run.adjustScores(scores);

        for (FieldKey scoreColumnName : _scoreColumnNames)
        {
            String value = scores.get(scoreColumnName.toString());  // Get value from the scores parsed from XML
            setAsFloat(stmt, n++, value);
        }
        if (null != peptide.getQueryNumber())
            stmt.setInt(n++, peptide.getQueryNumber());
        else
        {
            if (likelyProteomeDiscovererImport)
            {
                stmt.setInt(n++, _proteomeDiscovererOffset++);
            }
            else
            {
                stmt.setNull(n++, Types.INTEGER);
            }
        }

        stmt.setInt(n++, peptide.getHitRank());
        stmt.setBoolean(n, peptide.getDecoy());
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

    public void write(MS2Loader.Peptide peptide, PeptideProphetSummary peptideProphetSummary) throws SQLException
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

}
