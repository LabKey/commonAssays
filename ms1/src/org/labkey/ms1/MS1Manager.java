/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.ms1.model.*;
import org.labkey.ms1.maintenance.PurgeTask;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

public class MS1Manager
{
    private static MS1Manager _instance;
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

    public static synchronized MS1Manager get()
    {
        if (_instance == null)
            _instance = new MS1Manager();
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

    public Integer getRunIdFromFeature(int featureId) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT RunId FROM exp.Data AS d INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (d.RowId=f.ExpDataFileId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FEATURES));
        sql.append(" AS fe ON (f.FileId=fe.FileId) WHERE fe.FeatureId=?");

        return Table.executeSingleton(getSchema(), sql.toString(), new Object[]{featureId}, Integer.class);
    }

    public enum PeakAvailability {Available, PartiallyAvailable, NotAvailable}

    public PeakAvailability isPeakDataAvailable(int runId) throws SQLException
    {
        String sql = "Select FileId, Imported FROM ms1.Files AS f INNER JOIN exp.Data AS d on (f.ExpDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=? AND f.Deleted=?";
        Map[] values = Table.executeQuery(getSchema(), sql, new Object[]{runId, FILETYPE_PEAKS, false}, java.util.Map.class);

        if(null == values || 0 == values.length || null == values[0].get("FileId"))
            return PeakAvailability.NotAvailable;
        else if(Boolean.FALSE.equals(values[0].get("Imported")))
            return PeakAvailability.PartiallyAvailable;
        else
            return PeakAvailability.Available;
    }

    public Integer getFileIdForRun(int runId, int fileType) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT FileId FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f INNER JOIN exp.Data AS d ON (f.ExpDataFileId=d.RowId)");
        sql.append(" WHERE d.RunId=? AND f.Type=? AND f.Imported=? AND f.Deleted=?");

        return Table.executeSingleton(getSchema(), sql.toString(), new Object[]{runId, fileType, true, false}, Integer.class);
    }

    public DataFile getDataFile(int fileId) throws SQLException
    {
        return Table.selectObject(getTable(TABLE_FILES), fileId, DataFile.class);
    }

    public Feature getFeature(int featureId) throws SQLException
    {
        return Table.selectObject(getTable(TABLE_FEATURES), featureId, Feature.class);
    }

    public Scan getScan(int scanId) throws SQLException
    {
        return Table.selectObject(getTable(TABLE_SCANS), scanId, Scan.class);
    }

    public Software[] getSoftware(int fileId) throws SQLException
    {
        SimpleFilter fltr = new SimpleFilter("FileId", fileId);
        return Table.select(getTable(TABLE_SOFTWARE), Table.ALL_COLUMNS, fltr, null, Software.class);
    }

    public SoftwareParam[] getSoftwareParams(int softwareId) throws SQLException
    {
        SimpleFilter fltr = new SimpleFilter("SoftwareId", softwareId);
        return Table.select(getTable(TABLE_SOFTWARE_PARAMS), Table.ALL_COLUMNS, fltr, null, SoftwareParam.class);
    }

    public Table.TableResultSet getPeakData(int runId, int scan, double mzLow, double mzHigh) throws SQLException
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

        return Table.executeQuery(getSchema(), sql.toString(), new Object[]{runId, scan, mzLow, mzHigh, true, false});
    }

    public Table.TableResultSet getPeakData(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast) throws SQLException
    {
        return getPeakData(runId, mzLow, mzHigh, scanFirst, scanLast, null);
    }

    public Table.TableResultSet getPeakData(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast, String orderBy) throws SQLException
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

        return Table.executeQuery(getSchema(), sql.toString(), new Object[]{runId, mzLow, mzHigh, scanFirst, scanLast, true, false});
    }

    public Integer[] getPrevNextScan(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast, int scanCur) throws SQLException
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
        Integer prevScan = Table.executeSingleton(getSchema(), sqlPrev, new Object[]{runId, mzLow, mzHigh, scanFirst, scanLast, true, false, scanCur}, Integer.class);

        //find the min of those greater than cur
        String sqlNext = "SELECT MIN(s.Scan)" + sql.toString() + " AND s.Scan > ?";
        Integer nextScan = Table.executeSingleton(getSchema(), sqlNext, new Object[]{runId, mzLow, mzHigh, scanFirst, scanLast, true, false, scanCur}, Integer.class);

        return new Integer[]{prevScan, nextScan};
    }

    public MinMaxScanInfo getMinMaxScanRT(int runId, int scanFirst, int scanLast) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT MIN(s.Scan) AS MinScan, MAX(s.Scan) AS MaxScan");
        sql.append(", MIN(s.RetentionTime) AS MinRetentionTime, MAX(s.RetentionTime) AS MaxRetentionTime FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" AS p INNER JOIN ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" AS s ON (s.ScanId=p.ScanId) INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (s.FileId=f.FileId) INNER JOIN exp.Data AS d ON (f.expDataFileId=d.RowId) WHERE d.RunId=? AND f.Type=");
        sql.append(FILETYPE_PEAKS);
        sql.append(" AND (s.Scan BETWEEN ? AND ?) AND f.Imported=? AND f.Deleted=?");

        MinMaxScanInfo[] result = Table.executeQuery(getSchema(), sql.toString(), new Object[]{runId, scanFirst, scanLast, true, false}, MinMaxScanInfo.class);
        return null == result || 0 == result.length ? null : result[0];
    }

    public Collection<String> getContainerSummary(Container container) throws SQLException
    {
        ArrayList<String> items = new ArrayList<String>();

        String sql = "select count(*) as NumRuns \n" +
                "from ms1.Files as f inner join exp.data as d on (f.ExpDataFileId=d.RowId)\n" +
                "where type=? and deleted=? and d.Container=?";
        Integer count = Table.executeSingleton(getSchema(), sql, new Object[]{FILETYPE_FEATURES, false, container.getId()}, Integer.class);
        if(null != count && count.intValue() > 0)
            items.add(count.intValue() + (count.intValue() > 1 ? " MS1 Runs" : " MS1 Run"));
        return items;
    }

    public void deleteFeaturesData(ExpData expData) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE ms1.Files SET Deleted=? WHERE ExpDataFileId=? AND Type=?",
                    new Object[]{true,expData.getRowId(),FILETYPE_FEATURES});
    }

    public void purgeFeaturesData(int fileId) throws SQLException
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

        Table.execute(getSchema(), sql.toString(), null);
    }

    public void moveFileData(int oldExpDataFileID, int newExpDataFileID) throws SQLException
    {
        Integer[] ids = {newExpDataFileID, oldExpDataFileID};
        Table.execute(getSchema(), "UPDATE " + SCHEMA_NAME + "." + TABLE_FILES + " SET ExpDataFileID=? WHERE ExpDataFileID=?", ids);
    }

    public void deletePeakData(int expDataFileId) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE ms1.Files SET Deleted=? WHERE ExpDataFileId=? AND Type=?", 
                        new Object[]{true, expDataFileId, FILETYPE_PEAKS});
    }

    public void purgePeakData(int fileId) throws SQLException
    {
        DbSchema schema = getSchema();
        DbScope scope = schema.getScope();

        try
        {
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

            //execute this much
            _log.info("Purging peak families for file " + String.valueOf(fileId) + "...");
            scope.beginTransaction();
            Table.execute(getSchema(), sql.toString(), null);
            scope.commitTransaction();
            _log.info("Finished purging peak families for file " + String.valueOf(fileId) + ".");

            //now delete the peaks themselves
            sql = new StringBuilder("DELETE FROM ");
            sql.append(getSQLTableName(TABLE_PEAKS));
            sql.append(" WHERE ScanId IN (");
            sql.append(genScanListSQL(fileId));
            sql.append(")");

            _log.info("Purging peaks for file " + String.valueOf(fileId) + "...");
            scope.beginTransaction();
            Table.execute(getSchema(), sql.toString(), null);
            scope.commitTransaction();
            _log.info("Finished purging peaks for file " + String.valueOf(fileId) + ".");

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

            _log.info("Purging scans and related file data for file " + String.valueOf(fileId) + "...");
            scope.beginTransaction();
            Table.execute(getSchema(), sql.toString(), null);
            scope.commitTransaction();
            _log.info("Finished purging scans and related file data for file " + String.valueOf(fileId) + ".");
        }
        catch(SQLException e)
        {
            if(scope.isTransactionActive())
                scope.rollbackTransaction();
            throw e;
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
            rs = Table.executeQuery(getSchema(), "SELECT ScanId FROM ms1.Scans WHERE FileId=" + String.valueOf(fileId),
                    null, 0, false);

            int scanId = 0;
            long numScans = 0;
            scope.beginTransaction();

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
                    scope.beginTransaction();
                }
            }

            //final commit if necessary
            if(scope.isTransactionActive())
                scope.commitTransaction();

            _log.info("Finished purging peaks for file " + String.valueOf(fileId) + ".");
        }
        catch(SQLException e)
        {
            if(scope.isTransactionActive())
                scope.rollbackTransaction();
            throw e;
        }
        finally
        {
            ResultSetUtil.close(rs);
        }
    }

    protected void purgePeaksForScan(int scanId) throws SQLException
    {
        _log.info("Purging peaks for scan " + String.valueOf(scanId) + "...");
        String sql = "DELETE FROM ms1.Peaks WHERE ScanId=" + String.valueOf(scanId);
        Table.execute(getSchema(), sql, null);
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

    public int getDeletedFileCount() throws SQLException
    {
        Integer ret = Table.executeSingleton(getSchema(), "SELECT COUNT(FileId) FROM ms1.Files WHERE Deleted=?", new Object[]{true}, Integer.class);
        if(null != ret)
            return ret.intValue();
        else
            return 0;
    }

    /**
     * Returns the next file id to purge from the ms1 schema
     * @return The next file id, or null if there are no more to purge
     * @throws SQLException thrown if there is a database error
     */
    public Integer getNextPurgeFile() throws SQLException
    {
        return Table.executeSingleton(getSchema(), "SELECT MIN(FileId) FROM ms1.Files WHERE Deleted=?", new Object[]{true}, Integer.class);
    }

    /**
     * Purges a the data for a given file id
     * @param fileId The id of the file to purge
     * @throws SQLException thrown if something goes wrong
     */
    public void purgeFile(int fileId) throws SQLException
    {
        Integer fileType = Table.executeSingleton(getSchema(), "SELECT Type FROM ms1.Files WHERE FileId=?", new Object[]{fileId}, Integer.class);
        if(null == fileType)
            return;
        if(fileType.intValue() == FILETYPE_FEATURES)
            purgeFeaturesData(fileId);
        else if(fileType.intValue() == FILETYPE_PEAKS)
            purgePeakData(fileId);
    }

    public void deleteFailedImports(int expDataFileId, int fileType) throws SQLException
    {
        Table.execute(getSchema(), "UPDATE ms1.Files SET Deleted=? WHERE ExpDataFileId=? AND Type=? AND Imported=?",
                        new Object[]{true,expDataFileId,fileType,false});
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
     * @throws SQLException Exception thrown from database layer
     */
    public boolean isAlreadyImported(File dataFile, ExpData data) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS existing FROM exp.Data as d INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" as f");
        sql.append(" ON (d.RowId = f.ExpDataFileId) WHERE DataFileUrl=? AND Container=? AND f.Imported=? AND f.Deleted=?");

        Integer count = Table.executeSingleton(getSchema(), sql.toString(),
                                                new Object[]{dataFile.toURI().toString(), data.getContainer().getId(), true, false},
                                                Integer.class);
        return (null != count && count.intValue() != 0);
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