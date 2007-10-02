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
    public static final String TABLE_PEAKS_FILES = "PeaksFiles";
    public static final String TABLE_SCANS = "Scans";
    public static final String TABLE_CALIBRATION_PARAMS = "CalibrationParams";
    public static final String TABLE_PEAK_FAMILIES = "PeakFamilies";
    public static final String TABLE_PEAKS_TO_FAMILIES = "PeaksToFamilies";
    public static final String TABLE_PEAKS = "Peaks";
    public static final String TABLE_FEATURES = "Features";
    public static final String TABLE_FEATURES_FILES = "FeaturesFiles";

    //this maps the class of our DbBean derivatives to their proper table names
    //used in the save() method below
    private static final HashMap<Class,String> _tablemap = new HashMap<Class,String>();
    static
    {
        _tablemap.put(PeaksFile.class,TABLE_PEAKS_FILES);
        _tablemap.put(Scan.class,TABLE_SCANS);
        _tablemap.put(CalibrationParam.class,TABLE_CALIBRATION_PARAMS);
        _tablemap.put(PeakFamily.class,TABLE_PEAK_FAMILIES);
        _tablemap.put(Peak.class,TABLE_PEAKS);
    }

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
     * Saves a DbBean-derived class to the database. This will use the
     * dirty and new flags to determine if the bean needs to be saved
     * and if so, if it should be inserted or updated. The function
     * returns a reference to the object returned from the Table layer
     * (which is typically the same instance as the object passed in)
     * @param dbbean    bean to save
     * @param user      user saving the bean
     * @return          reference to bean returned from the Table layer
     * @throws SQLException
     */
    public <K extends DbBean> K save(K dbbean, User user) throws SQLException
    {
        if(dbbean.isDirty())
        {
            if(dbbean.isNew())
                dbbean = Table.insert(user, getTable(_tablemap.get(dbbean.getClass())), dbbean);
            else
                dbbean = Table.update(user, getTable(_tablemap.get(dbbean.getClass())), dbbean, dbbean.getRowID(), null);

            //in either case, this object is clean and no longer new
            dbbean.setDirty(false);
            dbbean.setNew(false);
        }

        return dbbean;
    } //save()

    public void addPeakToFamily(PeakFamily pfam, Peak pk, User user) throws SQLException
    {
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        map.put("PeakID", pk.getPeakID());
        map.put("PeakFamilyID", pfam.getPeakFamilyID());
        Table.insert(user, getSchema().getTable(TABLE_PEAKS_TO_FAMILIES), map);
    }

    public void deletePeakData(int expDataFileID, User user) throws SQLException
    {
        StringBuilder sql = new StringBuilder("DELETE FROM ms1.PeaksToFamilies WHERE PeakID IN (SELECT PeakID FROM ms1.Peaks WHERE PeaksFileID IN (SELECT PeaksFileID FROM ms1.PeaksFiles WHERE ExpDataFileID=" + expDataFileID + "))");
        sql.append("; DELETE FROM ms1.PeakFamilies WHERE PeaksFileID IN (SELECT PeaksFileID FROM ms1.PeaksFiles WHERE ExpDataFileID=" + expDataFileID + ")");
        sql.append("; DELETE FROM ms1.Peaks WHERE PeaksFileID IN (SELECT PeaksFileID FROM ms1.PeaksFiles WHERE ExpDataFileID=" + expDataFileID + ")");
        sql.append("; DELETE FROM ms1.CalibrationParams WHERE PeaksFileID IN (SELECT PeaksFileID FROM ms1.PeaksFiles WHERE ExpDataFileID=" + expDataFileID + ")");
        sql.append("; DELETE FROM ms1.Scans WHERE PeaksFileID IN (SELECT PeaksFileID FROM ms1.PeaksFiles WHERE ExpDataFileID=" + expDataFileID + ")");
        sql.append("; DELETE FROM ms1.PeaksFiles WHERE ExpDataFileID=" + expDataFileID);
        
        Table.execute(getSchema(), sql.toString(), null);
    } //deletePeakData

    public void movePeakData(int oldExpDataFileID, int newExpDataFileID, User user) throws SQLException
    {
        Integer[] ids = {newExpDataFileID, oldExpDataFileID};
        Table.execute(getSchema(), "UPDATE ms1.PeaksFiles SET ExpDataFileID=? WHERE ExpDataFileID=?", ids);
    } //movePeakData()

    /**
     * Returns true if this data file has already been imported into the experiment's container
     * @param dataFile  Data file to import
     * @param data      Experiment data object
     * @return          True if already loaded into the experiment's container, otherwise false
     * @throws SQLException
     */
    protected boolean isAlreadyImported(File dataFile, ExpData data) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS existing FROM exp.Data INNER JOIN ");
        if(dataFile.getPath().endsWith(".peaks.xml"))
            sql.append("ms1.PeaksFiles as t");
        else if(dataFile.getPath().endsWith(".features.tsv"))
            sql.append("ms1.FeaturesFiles as t");
        else
            assert false : "isAlreadyImported() only works with .peaks.xml and .features.tsv files!";

        sql.append(" ON (exp.Data.RowId = t.ExpDataFileID) WHERE DataFileUrl='file:/");
        sql.append(dataFile.getAbsolutePath().replace("\\", "/"));
        sql.append("' AND Container='");
        sql.append(data.getContainer().getId());
        sql.append("'");

        Integer count = new Integer(0);
        count = Table.executeSingleton(getSchema(), sql.toString(), null, Integer.class);
        return (count.intValue() != 0);
    } //isAlreadyImported()

} //class MS1Manager