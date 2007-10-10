package org.labkey.ms1;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.Table;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.security.User;
import org.labkey.api.exp.api.ExpData;

import java.sql.SQLException;
import java.util.HashMap;
import java.io.File;

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

    /**
     * Returns the corresponding ScanId for a given Experiment runId and scan number.
     * @param runId The experiment run id
     * @param scan  The scan number
     * @return The corresponding ScanId from the ms1.Scans table, or null if no match is found
     * @throws SQLException Thrown if there is a database exception
     */
    public Integer getScanIdFromRunScan(int runId, int scan) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT ScanId FROM ").append(getSQLTableName(TABLE_SCANS));
        sql.append(" AS s INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" AS f ON (s.FileId=f.FileId) INNER JOIN exp.Data as d ON (f.ExpDataFileId=d.RowId)");
        sql.append(" WHERE d.RunId=").append(runId);
        sql.append(" AND s.Scan=").append(scan);
        
        return Table.executeSingleton(getSchema(), sql.toString(), null, Integer.class);
    }

    public void deleteFeaturesData(ExpData expData, User user) throws SQLException
    {
        int idExpData = expData.getRowId();

        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_FEATURES));
        sql.append(" WHERE FileId IN (");
        sql.append(genFileListSQL(idExpData));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE_PARAMS));
        sql.append(" WHERE SoftwareId IN (");
        sql.append(genSoftwareListSQL(idExpData));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE));
        sql.append(" WHERE FileId IN (");
        sql.append(genFileListSQL(idExpData));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" WHERE ExpDataFileId=");
        sql.append(String.valueOf(idExpData));

        Table.execute(getSchema(), sql.toString(), null);
    }

    public void moveFileData(int oldExpDataFileID, int newExpDataFileID, User user) throws SQLException
    {
        Integer[] ids = {newExpDataFileID, oldExpDataFileID};
        Table.execute(getSchema(), "UPDATE " + SCHEMA_NAME + "." + TABLE_FILES + " SET ExpDataFileID=? WHERE ExpDataFileID=?", ids);
    }

    public void deletePeakData(int expDataFileID, User user) throws SQLException
    {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS_TO_FAMILIES));
        sql.append(" WHERE PeakId IN (");
        sql.append(genPeakListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_PEAK_FAMILIES));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_CALIBRATION_PARAMS));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" WHERE FileId IN (");
        sql.append(genFileListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE_PARAMS));
        sql.append(" WHERE SoftwareId IN (");
        sql.append(genSoftwareListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE));
        sql.append(" WHERE FileId IN (");
        sql.append(genFileListSQL(expDataFileID));
        sql.append(");");

        sql.append("DELETE FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" WHERE ExpDataFileID=");
        sql.append(String.valueOf(expDataFileID));
        sql.append(";");

        Table.execute(getSchema(), sql.toString(), null);
    } //deletePeakData

    protected String genPeakListSQL(int expDataFileID)
    {
        StringBuilder sql = new StringBuilder("SELECT PeakId FROM ");
        sql.append(getSQLTableName(TABLE_PEAKS));
        sql.append(" WHERE ScanId IN (");
        sql.append(genScanListSQL(expDataFileID));
        sql.append(")");
        return sql.toString();
    }

    protected String genScanListSQL(int expDataFileID)
    {
        StringBuilder sql = new StringBuilder("SELECT ScanId FROM ");
        sql.append(getSQLTableName(TABLE_SCANS));
        sql.append(" WHERE FileId IN (");
        sql.append(genFileListSQL(expDataFileID));
        sql.append(")");
        return sql.toString();
    }

    protected String genFileListSQL(int expDataFileID)
    {
        StringBuilder sql = new StringBuilder("SELECT FileId FROM ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" WHERE ExpDataFileId=");
        sql.append(String.valueOf(expDataFileID));
        return sql.toString();
    }

    protected String genSoftwareListSQL(int expDataFileID)
    {
        StringBuilder sql = new StringBuilder("SELECT SoftwareId FROM ");
        sql.append(getSQLTableName(TABLE_SOFTWARE));
        sql.append(" WHERE FileId IN (");
        sql.append(genFileListSQL(expDataFileID));
        sql.append(")");
        return sql.toString();
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
    protected boolean isAlreadyImported(File dataFile, ExpData data) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS existing FROM exp.Data as d INNER JOIN ");
        sql.append(getSQLTableName(TABLE_FILES));
        sql.append(" as f");
        sql.append(" ON (d.RowId = f.ExpDataFileId) WHERE DataFileUrl=? AND Container=?");

        Integer count = Table.executeSingleton(getSchema(), sql.toString(),
                                                new Object[]{dataFile.toURI().toString(), data.getContainer().getId()},
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

} //class MS1Manager