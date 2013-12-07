/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.ms1.maintenance.PurgeTask;
import org.labkey.ms1.model.DataFile;
import org.labkey.ms1.model.Feature;
import org.labkey.ms1.model.MinMaxScanInfo;
import org.labkey.ms1.model.Scan;
import org.labkey.ms1.model.Software;
import org.labkey.ms1.model.SoftwareParam;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MS1Manager
{
    private static MS1Manager _instance = new MS1Manager();

    public static final String SCHEMA_NAME = "ms1";
    public static final String TABLE_SCANS = "Scans";
    public static final String TABLE_CALIBRATION_PARAMS = "Calibrations";
    public static final String TABLE_PEAK_FAMILIES = "PeakFamilies";
    public static final String TABLE_PEAKS_TO_FAMILIES = "PeaksToFamilies";
    public static final String TABLE_PEAKS = "Peaks";
    public static final String TABLE_FEATURES = "Features";
    public static final String TABLE_FILES = "Files";
    public static final String TABLE_SOFTWARE = "Software";
    public static final String TABLE_SOFTWARE_PARAMS = "SoftwareParams";

    //constants for the file type bitmask
    public static final int FILETYPE_FEATURES = 1;
    public static final int FILETYPE_PEAKS = 2;

    private Thread _purgeThread = null;
    private static final Logger _log = Logger.getLogger(MS1Manager.class);

    private MS1Manager()
    {
        // prevent external construction with a private default constructor
    }

    public static MS1Manager get()
    {
        return _instance;
    }

    /**
     * Starts a manual purge of deleted data files on a background thread.
     * If the purge process is already running, this results in a NOOP.
     */
    public void startManualPurge()
    {
        if(null == _purgeThread || _purgeThread.getState() == Thread.State.TERMINATED)
        {
            _purgeThread = new Thread(new PurgeTask(), "MS1 Purge Task");
            _purgeThread.start();
        }
    }

    public String getSchemaName()
    {
        return SCHEMA_NAME;
    }
    public DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }

    public SchemaTableInfo getTable(String tablename)
    {
        return getSchema().getTable(tablename);
    }

    public Integer getRunIdFromFeature(int featureId)
    {
        StringBuilder sql = new StringBuilder("SELECT RunId FROM exp.Data AS d INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (d.RowId=f.ExpDataFileId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FEATURES));
        sql.append(" AS fe ON (f.FileId=fe.FileId) WHERE fe.FeatureId=?");

        return new SqlSelector(getSchema(), sql, featureId).getObject(Integer.class);
    }

    public enum PeakAvailability {Available, PartiallyAvailable, NotAvailable}

    public PeakAvailability isPeakDataAvailable(int runId)
    {
        String sql = "Select FileId, Imported FROM ms1.Files AS f INNER JOIN exp.Data AS d on (f.ExpDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=? AND f.Deleted=?";
        Map<String, Object>[] values = new SqlSelector(getSchema(), sql, runId, FILETYPE_PEAKS, false).getMapArray();

        if (0 == values.length || null == values[0].get("FileId"))
            return PeakAvailability.NotAvailable;
        else if(Boolean.FALSE.equals(values[0].get("Imported")))
            return PeakAvailability.PartiallyAvailable;
        else
            return PeakAvailability.Available;
    }

    public Integer getFileIdForRun(int runId, int fileType)
    {
        StringBuilder sql = new StringBuilder("SELECT FileId FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f INNER JOIN exp.Data AS d ON (f.ExpDataFileId=d.RowId)");
        sql.append(" WHERE d.RunId=? AND f.Type=? AND f.Imported=? AND f.Deleted=?");

        return new SqlSelector(getSchema(), sql, runId, fileType, true, false).getObject(Integer.class);
    }

    public DataFile getDataFile(int fileId)
    {
        return new TableSelector(getTable(TABLE_FILES)).getObject(fileId, DataFile.class);
    }

    public Feature getFeature(int featureId)
    {
        return new TableSelector(getTable(TABLE_FEATURES)).getObject(featureId, Feature.class);
    }

    public Scan getScan(int scanId)
    {
        return new TableSelector(getTable(TABLE_SCANS)).getObject(scanId, Scan.class);
    }

    public Software[] getSoftware(int fileId)
    {
        SimpleFilter fltr = new SimpleFilter(FieldKey.fromParts("FileId"), fileId);
        return new TableSelector(getTable(TABLE_SOFTWARE), fltr, null).getArray(Software.class);
    }

    public SoftwareParam[] getSoftwareParams(int softwareId)
    {
        SimpleFilter fltr = new SimpleFilter(FieldKey.fromParts("SoftwareId"), softwareId);
        return new TableSelector(getTable(TABLE_SOFTWARE_PARAMS), fltr, null).getArray(SoftwareParam.class);
    }

    public Table.TableResultSet getPeakData(int runId, int scan, double mzLow, double mzHigh)
    {
        StringBuilder sql = new StringBuilder("SELECT s.ScanId, s.Scan, s.RetentionTime, s.ObservationDuration, p.PeakId, p.MZ, p.Intensity, p.Area, p.Error, p.Frequency, p.Phase, p.Decay FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" AS p INNER JOIN ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" AS s ON (s.ScanId=p.ScanId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (s.FileId=f.FileId) INNER JOIN exp.Data AS d ON (f.expDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=");
        sql.append(FILETYPE_PEAKS);
        sql.append(" AND s.Scan=? AND (p.MZ BETWEEN ? AND ?) AND f.Imported=? AND f.Deleted=?");

        return new SqlSelector(getSchema(), sql, runId, scan, mzLow, mzHigh, true, false).getResultSet();
    }

    public Table.TableResultSet getPeakData(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast)
    {
        return getPeakData(runId, mzLow, mzHigh, scanFirst, scanLast, null);
    }

    public Table.TableResultSet getPeakData(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast, String orderBy)
    {
        StringBuilder sql = new StringBuilder("SELECT s.ScanId, s.Scan, s.RetentionTime, s.ObservationDuration, p.PeakId, p.MZ, p.Intensity, p.Area, p.Error, p.Frequency, p.Phase, p.Decay FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" AS p INNER JOIN ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" AS s ON (s.ScanId=p.ScanId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (s.FileId=f.FileId) INNER JOIN exp.Data AS d ON (f.expDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=");
        sql.append(FILETYPE_PEAKS);
        sql.append(" AND (p.MZ BETWEEN ? AND ?)");
        sql.append(" AND (s.Scan BETWEEN ? AND ?) AND f.Imported=? AND f.Deleted=?");
        if(null != orderBy)
            sql.append(" ORDER BY ").append(orderBy);

        return new SqlSelector(getSchema(), sql, runId, mzLow, mzHigh, scanFirst, scanLast, true, false).getResultSet();
    }

    public Integer[] getPrevNextScan(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast, int scanCur)
    {
        StringBuilder sql = new StringBuilder(" FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" AS p INNER JOIN ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" AS s ON (s.ScanId=p.ScanId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (s.FileId=f.FileId) INNER JOIN exp.Data AS d ON (f.expDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=");
        sql.append(FILETYPE_PEAKS);
        sql.append(" AND (p.MZ BETWEEN ? AND ?)");
        sql.append(" AND (s.Scan BETWEEN ? AND ?) AND f.Imported=? AND f.Deleted=?");

        //find the max of those less than cur
        String sqlPrev = "SELECT MAX(s.Scan)" + sql.toString() + " AND s.Scan < ?";
        Integer prevScan = new SqlSelector(getSchema(), sqlPrev, runId, mzLow, mzHigh, scanFirst, scanLast, true, false, scanCur).getObject(Integer.class);

        //find the min of those greater than cur
        String sqlNext = "SELECT MIN(s.Scan)" + sql.toString() + " AND s.Scan > ?";
        Integer nextScan = new SqlSelector(getSchema(), sqlNext, runId, mzLow, mzHigh, scanFirst, scanLast, true, false, scanCur).getObject(Integer.class);

        return new Integer[]{prevScan, nextScan};
    }

    public MinMaxScanInfo getMinMaxScanRT(int runId, int scanFirst, int scanLast)
    {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(MIN(s.Scan),0) AS MinScan, COALESCE(MAX(s.Scan),0) AS MaxScan");
        sql.append(", COALESCE(MIN(s.RetentionTime),0) AS MinRetentionTime, COALESCE(MAX(s.RetentionTime),0) AS MaxRetentionTime FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" AS p INNER JOIN ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" AS s ON (s.ScanId=p.ScanId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (s.FileId=f.FileId) INNER JOIN exp.Data AS d ON (f.expDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=");
        sql.append(FILETYPE_PEAKS);
        sql.append(" AND (s.Scan BETWEEN ? AND ?) AND f.Imported=? AND f.Deleted=? AND s.Scan IS NOT NULL and s.RetentionTime IS NOT NULL");

        MinMaxScanInfo[] result = new SqlSelector(getSchema(), sql, runId, scanFirst, scanLast, true, false).getArray(MinMaxScanInfo.class);
        return 0 == result.length ? null : result[0];
    }

    public Collection<String> getContainerSummary(Container container)
    {
        ArrayList<String> items = new ArrayList<>();

        String sql = "SELECT COUNT(*) AS NumRuns \n" +
                "FROM ms1.Files as f inner join exp.data as d on (f.ExpDataFileId=d.RowId)\n" +
                "where type=? and deleted=? and d.Container=?";
        Integer count = new SqlSelector(getSchema(), sql, FILETYPE_FEATURES, false, container).getObject(Integer.class);
        if (count > 0)
            items.add(count + (count > 1 ? " MS1 Runs" : " MS1 Run"));
        return items;
    }

    public void deleteFeaturesData(ExpData expData) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE ms1.Files SET Deleted=? WHERE ExpDataFileId=? AND Type=?",
                true,expData.getRowId(),FILETYPE_FEATURES);
    }

    public void purgeFeaturesData(int fileId)
    {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_FEATURES));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        sql.append(";");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE_PARAMS));
        sql.append(" WHERE SoftwareId IN (");
        sql.append(genSoftwareListSQL(fileId));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        sql.append(";");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        sql.append(";");

        new SqlExecutor(getSchema()).execute(sql.toString());
    }

    public void moveFileData(int oldExpDataFileID, int newExpDataFileID) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE " + SCHEMA_NAME + "." + TABLE_FILES + " SET ExpDataFileID=? WHERE ExpDataFileID=?", newExpDataFileID, oldExpDataFileID);
    }

    public void deletePeakData(int expDataFileId) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE ms1.Files SET Deleted=? WHERE ExpDataFileId=? AND Type=?",
                true, expDataFileId, FILETYPE_PEAKS);
    }

    public void purgePeakData(int fileId) throws SQLException
    {
        DbSchema schema = getSchema();
        DbScope scope = schema.getScope();

        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS_TO_FAMILIES));
        sql.append(" WHERE PeakFamilyId IN (");
        sql.append(genPeakFamilyListSQL(fileId));
        sql.append("); ");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_PEAK_FAMILIES));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(fileId));
        sql.append(")");

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            //execute this much
            _log.info("Purging peak families for file " + String.valueOf(fileId) + "...");
            new SqlExecutor(getSchema()).execute(sql.toString());
            transaction.commit();
            _log.info("Finished purging peak families for file " + String.valueOf(fileId) + ".");
        }

        //now delete the peaks themselves
        sql = new StringBuilder("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(fileId));
        sql.append(")");

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            _log.info("Purging peaks for file " + String.valueOf(fileId) + "...");
            new SqlExecutor(getSchema()).execute(sql.toString());
            transaction.commit();
            _log.info("Finished purging peaks for file " + String.valueOf(fileId) + ".");
        }

        //now the rest of it
        sql = new StringBuilder("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_CALIBRATION_PARAMS));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(fileId));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        sql.append(";");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE_PARAMS));
        sql.append(" WHERE SoftwareId IN (");
        sql.append(genSoftwareListSQL(fileId));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        sql.append(";");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" WHERE FileId=");
        sql.append(String.valueOf(fileId));
        sql.append(";");

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            _log.info("Purging scans and related file data for file " + String.valueOf(fileId) + "...");
            new SqlExecutor(getSchema()).execute(sql.toString());
            transaction.commit();
            _log.info("Finished purging scans and related file data for file " + String.valueOf(fileId) + ".");
        }
    } //deletePeakData

    protected void purgePeaks(int fileId) throws SQLException
    {
        //NOTE: This is not ideal, but seems to be necessary for PostgreSQL.
        //On SQL Server, a simple delete statement with a nested sub-select will delete
        //several hundreds of thousands of rows quickly and without issue, but on PostgreSQL,
        //the same query will literally take *hours* to run. Splitting it up into
        //a query per scan reduced this from several hours to a couple of minutes on my dev machine.
        //Clearly PostgreSQL is doing something truly awful here, but until they fix it on their
        //side, we'll have to delete peaks on a per-scan basis.
        _log.info("Purging peaks for file " + String.valueOf(fileId) + "...");
        DbSchema schema = getSchema();
        DbScope scope = schema.getScope();
        ResultSet rs = null;
        try
        {
            rs = new SqlSelector(getSchema(), "SELECT ScanId FROM ms1.Scans WHERE FileId=" + String.valueOf(fileId),
                    null).getResultSet(false);

            int scanId = 0;
            long numScans = 0;
            scope.ensureTransaction();

            while(rs.next())
            {
                scanId = rs.getInt(1);
                if(!rs.wasNull())
                    purgePeaksForScan(scanId);
                ++numScans;

                //commit after every 10 scans
                if(numScans % 10 == 0)
                {
                    scope.commitTransaction();
                    scope.ensureTransaction();
                }
            }

            //final commit if necessary
            scope.commitTransaction();

            _log.info("Finished purging peaks for file " + String.valueOf(fileId) + ".");
        }
        finally
        {
            ResultSetUtil.close(rs);
            scope.closeConnection();
        }
    }

    protected void purgePeaksForScan(int scanId) throws SQLException
    {
        _log.info("Purging peaks for scan " + String.valueOf(scanId) + "...");
        String sql = "DELETE FROM ms1.Peaks WHERE ScanId=" + String.valueOf(scanId);
        Table.execute(getSchema(), sql);
        _log.info("Finished purging peaks for scan " + String.valueOf(scanId) + ".");
    }

    protected String genPeakFamilyListSQL(int fileId)
    {
        StringBuilder sql = new StringBuilder("SELECT PeakFamilyId FROM ");
        sql.append(getSQLTableName(TABLE_PEAK_FAMILIES));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(fileId));
        sql.append(")");
        return sql.toString();

    }

    protected String genPeakListSQL(int fileId)
    {
        StringBuilder sql = new StringBuilder("SELECT PeakId FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(fileId));
        sql.append(")");
        return sql.toString();
    }

    protected String genScanListSQL(int fileId)
    {
        StringBuilder sql = new StringBuilder("SELECT ScanId FROM ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        return sql.toString();
    }

    protected String genSoftwareListSQL(int fileId)
    {
        StringBuilder sql = new StringBuilder("SELECT SoftwareId FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE));
        sql.append(" WHERE FileId=");
        sql.append(fileId);
        return sql.toString();
    }

    public int getDeletedFileCount()
    {
        return new SqlSelector(getSchema(), "SELECT COUNT(FileId) FROM ms1.Files WHERE Deleted = ?", true).getObject(Integer.class);
    }

    /**
     * Returns the next file id to purge from the ms1 schema
     * @return The next file id, or null if there are no more to purge
     */
    public Integer getNextPurgeFile()
    {
        return new SqlSelector(getSchema(), "SELECT MIN(FileId) FROM ms1.Files WHERE Deleted = ?", true).getObject(Integer.class);
    }

    /**
     * Purges a the data for a given file id
     * @param fileId The id of the file to purge
     * @throws SQLException thrown if something goes wrong
     */
    public void purgeFile(int fileId) throws SQLException
    {
        Integer fileType = new SqlSelector(getSchema(), "SELECT Type FROM ms1.Files WHERE FileId = ?", fileId).getObject(Integer.class);
        if(null == fileType)
            return;
        if(fileType == FILETYPE_FEATURES)
            purgeFeaturesData(fileId);
        else if(fileType == FILETYPE_PEAKS)
            purgePeakData(fileId);
    }

    public void deleteFailedImports(int expDataFileId, int fileType) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE ms1.Files SET Deleted=? WHERE ExpDataFileId=? AND Type=? AND Imported=?",
                true,expDataFileId,fileType,false);
    }

    /**
     * Returns the fully-qualified table name (schema.table) for use in SQL statements
     * @param tableName The table name
     * @return Fully-qualified table name
     */
    public String getSQLTableName(String tableName)
    {
        return SCHEMA_NAME + "." + tableName;
    }

    /**
     * Returns true if this data file has already been imported into the experiment's container
     * @param dataFile  Data file to import
     * @param data      Experiment data object
     * @return          True if already loaded into the experiment's container, otherwise false
     */
    public boolean isAlreadyImported(File dataFile, ExpData data)
    {
        StringBuilder sql = new StringBuilder("SELECT * FROM exp.Data AS d INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f");
        sql.append(" ON (d.RowId = f.ExpDataFileId) WHERE DataFileUrl=? AND Container=? AND f.Imported=? AND f.Deleted=?");

        return new SqlSelector(getSchema(), sql, dataFile.toURI().toString(), data.getContainer(), true, false).exists();
    } //isAlreadyImported()

    /**
     * Returns a string containing all errors from a SQLException, which may contain many messages
     * @param e The SQLException object
     * @return A string containing all the error messages
     */
    public String getAllErrors(SQLException e)
    {
        StringBuilder sb = new StringBuilder(e.toString());
        while(null != (e = e.getNextException()))
        {
            sb.append("; ");
            sb.append(e.toString());
        }
        return sb.toString();
    }

    /**
     * Returns true if any of the peptide sequences passed contain any modifiers
     *
     * @param peptideSequences Array of peptide sequences
     * @return True if any contain modifiers
     */
    public boolean containsModifiers(String[] peptideSequences)
    {
        for(String seq : peptideSequences)
        {
            if(containsModifiers(seq))
                return true;
        }
        return false;
    }

    /**
     * Returns true if the passed peptide sequence contains any modifiers
     * @param peptideSequence the sequence to examine
     * @return true if peptideSequence contains modifiers
     */
    public boolean containsModifiers(String peptideSequence)
    {
        char ch = 0;
        for(int idx = 0; idx < peptideSequence.length(); ++idx)
        {
            ch = peptideSequence.charAt(idx);
            if(ch < 'A' || ch > 'Z')
                return true;
        }
        return false;
    }

} //class MS1Manager