package org.labkey.ms1;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
import org.labkey.common.tools.TabLoader;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.*;

/**
 * This data handler loads msInspect feature files, which use a tsv format.
 * It also handles deleting and moving that data when the experiment run
 * is deleted or moved.
 * User: daves
 * Date: Sept, 2007
 */
public class MSInspectFeaturesDataHandler extends AbstractExperimentDataHandler
{
    /**
     * This class maps a source column in the features tsv file with its
     * target database column and its jdbc data type. This is used within
     * the MSInspectFeaturesDataHandler class, and enables us to handle
     * feature files with missing or additional (but well-known) columns.
     * User: daves
     */
    protected static class ColumnBinding
    {
        String sourceColumn;    //name of the source column
        String targetColumn;    //name of the target column
        int jdbcType;           //jdbc data type of target column
        boolean isRequired;     //true if this column is required

        public ColumnBinding(String sourceColumn, String targetColumn, int jdbcType, boolean isRequired)
        {
            this.sourceColumn = sourceColumn;
            this.targetColumn = targetColumn;
            this.jdbcType = jdbcType;
            this.isRequired = isRequired;
        } //c-tor

        @Override
        public String toString()
        {
            return sourceColumn + "->" + targetColumn + " (type: " + jdbcType + ")" + (isRequired ? " Required" : "");
        }
    } //class Binding

    /**
     * Helper class for storing a map of ColumnBinding objects, keyed on the source column name
     */
    protected static class ColumnBindingHashMap extends HashMap<String,ColumnBinding>
    {
        public ColumnBinding put(ColumnBinding binding)
        {
            return put(binding.sourceColumn, binding);
        }
    } //class ColumBindingHashMap

    //Constants and Static Data Members
    protected static final String TABLE_FEATURES = "ms1.Features";              //features db table name
    protected static final String TABLE_FEATURES_FILES = "ms1.FeaturesFiles";   //FeaturesFiles db table name
    private static final int CHUNK_SIZE = 1000;                                 //number of insert statements in a batch

    //Master map of all possible column bindings.
    //The code below will select the appropriate bindings after the
    //tsv has been loaded based on the column descriptors.
    //To handle a new column in the features file, add a new put statement here.
    //The format is:
    // _bindingMap.put(new ColumnBinding(<tsv column name>, <db column name>, <jdbc type>));
    protected static ColumnBindingHashMap _bindingMap = new ColumnBindingHashMap();
    static
    {
        _bindingMap.put(new ColumnBinding("scan", "Scan", java.sql.Types.INTEGER, true));
        _bindingMap.put(new ColumnBinding("time", "Time", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("mz", "MZ", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("accurateMZ", "AccurateMZ", java.sql.Types.BIT, false));
        _bindingMap.put(new ColumnBinding("mass", "Mass", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("intensity", "Intensity", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("charge", "Charge", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("chargeStates", "ChargeStates", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("kl", "KL", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("background", "Background", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("median", "Median", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("peaks", "Peaks", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("scanFirst", "ScanFirst", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanLast", "ScanLast", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanCount", "ScanCount", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("totalIntensity", "TotalIntensity", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("description", "Description", java.sql.Types.VARCHAR, false));

        //columns added by Ceaders-Sinai to their post-processed features files
        _bindingMap.put(new ColumnBinding("MS2scan", "MS2Scan", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("probability", "MS2ConnectivityProbability", java.sql.Types.REAL, false));
    } //static init for _bindingMap

    /**
     * The experiment loader calls this to load the data file.
     * @param data The experiment data file
     * @param dataFile The data file to load
     * @param info Background info
     * @param log Log to write to
     * @param context The XarContext
     * @throws ExperimentException
     */
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if(null == data || null == dataFile || null == info || null == log || null == context)
            return;

        //NOTE: I'm using the highly-efficient technique of prepared statements and batch execution here,
        //but that also means I'm not using the Table layer and benefiting from its functionality.
        // This may need to change in the future.
        Connection cn = null;
        PreparedStatement pstmt = null;

        //get the ms1 schema and scope
        DbSchema schema = DbSchema.get("ms1");
        DbScope scope = schema.getScope();

        try
        {
            //if this file has already been imported before, just return
            if(MS1Manager.get().isAlreadyImported(dataFile, data))
            {
                log.info("Already imported features file " + dataFile.getPath().replace("\\","/") + " for this experiment into this container.");
                return;
            }

            //begin a transaction
            scope.beginTransaction();
            cn = schema.getScope().getConnection();

            //insert the feature files row
            int idFeaturesFile = insertFeaturesFile(info.getUser(), schema, data);

            //open the tsv file using TabLoader for automatic parsing
            TabLoader tsvloader = new TabLoader(dataFile);
            TabLoader.TabLoaderIterator iter = tsvloader.iterator();
            TabLoader.ColumnDescriptor[] coldescrs = tsvloader.getColumns();

            //select the appropriate bindings for this tsv file
            ArrayList<ColumnBinding> bindings = selectBindings(coldescrs);

            //build the approrpriate insert sql for the features table
            //and prepare it
            pstmt = cn.prepareStatement(genInsertSQL(bindings));

            Map row = null;
            int numRows = 0;

            //iterate over the rows
            while(iter.hasNext())
            {
                //get a row
                row = (Map)iter.next();
                ++numRows;

                //set parameter values
                pstmt.clearParameters();
                pstmt.setInt(1, idFeaturesFile); //jdbc params are 1-based!

                for(int idx = 0; idx < bindings.size(); ++idx)
                    setParam(pstmt, idx + 2, numRows, bindings.get(idx), row);

                //add a batch
                pstmt.addBatch();

                //execute if we've reached our chunk limit
                if((numRows % CHUNK_SIZE) == 0)
                {
                    pstmt.executeBatch();
                    log.info("Uploaded " + CHUNK_SIZE + " feature rows to the database.");
                }
            } //while reading rows

            //execute any remaining in the batch
            if(numRows % CHUNK_SIZE != 0)
                pstmt.executeBatch();

            //commit the transaction
            scope.commitTransaction();

            log.info("Finished loading " + numRows + " features.");
        }
        catch(IOException ex)
        {
            scope.rollbackTransaction();
            throw new ExperimentException(ex);
        }
        catch(SQLException ex)
        {
            scope.rollbackTransaction();
            throw new ExperimentException(ex);
        }
        finally
        {
            //final cleanup
            try{if(null != pstmt) pstmt.close();}catch(SQLException ignore){}
            try{if(null != cn) scope.releaseConnection(cn);}catch(SQLException ignore){}
        } //finally

    } //importFile()

    protected int insertFeaturesFile(User user, DbSchema schema, ExpData data) throws SQLException
    {
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("FeaturesFileID",null);
        map.put("ExpDataFileID",new Integer(data.getRowId()));
        map.put("MzXmlURL", getMzXmlFilePath(data));

        map = Table.insert(user, schema.getTable("FeaturesFiles"), map);
        return (Integer)(map.get("FeaturesFileID"));
    } //insertFeaturesFile()

    /**
     * Returns the master mzXML file path for the data file
     * @param data  Experiment data object
     * @return      Path to the mzXML File
     */
    protected String getMzXmlFilePath(ExpData data)
    {
        File dataFile = data.getDataFile();
        String dataFileName = dataFile.getName().substring(0, dataFile.getName().length() - 4);
        File mzxmlFile = new File(dataFile.getParentFile().getParentFile().getParentFile(), dataFileName + ".mzXML");
        return mzxmlFile.getPath().replace('\\', '/');
    } //getMzXmlFilePath()

    /**
     * Selects the appropriate column bindings based on the passed column descriptors
     * @param coldescrs The set of column descriptors for the tsv file
     * @return          The appropriate set of column bindings
     */
    protected ArrayList<ColumnBinding> selectBindings(TabLoader.ColumnDescriptor[] coldescrs)
    {
        ArrayList<ColumnBinding> ret = new ArrayList<ColumnBinding>(coldescrs.length);
        ColumnBinding binding = null;
        for(int idx = 0; idx < coldescrs.length; ++idx)
        {
            binding = _bindingMap.get(coldescrs[idx].name);
            if(null != binding)
                ret.add(binding);
        }
        return ret;
    } //selectBindings()

    /**
     * Generates the insert SQL statement for the Features table, with the correct column
     * names and number of parameter markers.
     * @param bindings  The column Bindings
     * @return          A properly constructed SQL INSERT statement for the given column bindings
     */
    protected String genInsertSQL(ArrayList<ColumnBinding> bindings)
    {
        StringBuilder sbCols = new StringBuilder("INSERT INTO ");
        sbCols.append(TABLE_FEATURES);
        sbCols.append(" (FeaturesFileID");

        StringBuilder sbParams = new StringBuilder("(?");

        for(ColumnBinding binding : bindings)
        {
            //if binding is null, we don't know how to import this
            //column, so don't include it in the sql
            if(null != binding)
            {
                sbCols.append(",").append(binding.targetColumn);
                sbParams.append(",?");
            }
        } //for each binding

        //close both with an end paren
        sbCols.append(")");
        sbParams.append(")");
        
        //return the complete SQL
        return sbCols.toString() + " VALUES " + sbParams.toString();
    } //genInsertSQL()

    /**
     * Sets the JDBC parameter with the row/column value from the tsv, performing the appropriate type casting
     * @param pstmt         The prepared statement
     * @param paramIndex    The parameter index to set
     * @param rowNum        The row number (used for error messages)
     * @param binding       The appropriate column bindings for this paramter
     * @param row           The row Map from the TabLoader
     * @throws ExperimentException
     * @throws SQLException
     */
    protected void setParam(PreparedStatement pstmt, int paramIndex, int rowNum, ColumnBinding binding, Map row) throws ExperimentException, SQLException
    {
        if(null == binding)
            return;

        Object val = row.get(binding.sourceColumn);

        try
        {
            //check for null and required
            if(null == val)
            {
                if(binding.isRequired)
                    throw new ExperimentException("The value in the required column '" + binding.sourceColumn +
                                                    "' at row " + rowNum + " was empty.");
                else
                    pstmt.setNull(paramIndex, binding.jdbcType);
            }
            else
            {
                //the TabLoader uses String, Integer, Double, Boolean and Date types only
                if(val instanceof String)
                    pstmt.setString(paramIndex, (String)val);
                else if(val instanceof Integer)
                    pstmt.setInt(paramIndex, (Integer)val); //relying on java 5.0 auto-unboxing
                else if(val instanceof Double)
                    pstmt.setDouble(paramIndex, (Double)val);
                else if(val instanceof Boolean)
                    pstmt.setBoolean(paramIndex, (Boolean)val);
                else if(val instanceof Date)
                    pstmt.setDate(paramIndex, new java.sql.Date(((Date)val).getTime()));
                else
                    assert(false) : "Unsupported column value data type in column " + binding.sourceColumn + ", row " + rowNum;
            } //not null
        }
        catch(SQLException e)
        {
            throw new ExperimentException("Problem setting the value for column " + binding.sourceColumn + 
                                            " in row " + rowNum + " to the value " + val + ": " + e.toString());
        }
    } //setParam()

    /**
     * Returns the content URL for files imported through this class
     * @param request
     * @param container
     * @param data
     * @return
     * @throws ExperimentException
     */
    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        ViewURLHelper url = new ViewURLHelper(request, "ms1", "showFeaturesFile.view", container);
        url.addParameter("dataRowId", Integer.toString(data.getRowId()));
        return url;
    }

    /**
     * Deletes data rows imported by this class when the experiment run is deleted
     * @param data
     * @param container
     * @param user
     * @throws ExperimentException
     */
    public void deleteData(Data data, Container container, User user) throws ExperimentException
    {
        // Delete the database records for this features file
        if(null == data || null == user)
                return;

        //Although it's not terribly obvious, the caller will have already begun a transaction
        //and the DbScope code will generate an exception if you call beginTrans() more than once
        //so don't use a transaction here because it's already transacted in the caller.
        try
        {
            DbSchema schema = DbSchema.get("ms1");
            Table.execute(schema, "delete from " + TABLE_FEATURES + " where FeaturesFileID in (select FeaturesFileID from "
                                + TABLE_FEATURES_FILES + " where ExpDataFileID="
                                + data.getRowId() + ")", null);
            Table.execute(schema, "delete from " + TABLE_FEATURES_FILES + " where ExpDataFileID=" + data.getRowId(), null);
        }
        catch(SQLException ex)
        {
            throw new ExperimentException(ex);
        }
    } //deleteData()

    /**
     * Moves the container for the given data file uploaded through this class
     * @param newData
     * @param container
     * @param targetContainer
     * @param oldRunLSID
     * @param newRunLSID
     * @param user
     * @param oldDataRowID
     * @throws ExperimentException
     */
    public void runMoved(Data newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        if(null == newData || null == user)
                return;

        //update the database records to reflect the new data file row id
        try
        {
            DbSchema schema = DbSchema.get("ms1");
            Table.execute(schema, "update " + TABLE_FEATURES_FILES + " set ExpDataFileID=" + newData.getRowId() + " where ExpDataFileID=" + oldDataRowID, null);
        }
        catch(SQLException ex)
        {
            throw new ExperimentException(ex);
        }
    } //runMoved()

    /**
     * Returns the priority if the passed data file is one this class knows how to import, otherwise null.
     * @param data  The data file to import
     * @return      Priority if this file can import it, otherwise null.
     */
    public Priority getPriority(Data data)
    {
        //we handle only *.rtfeatures.tvt files
        String fileUrl = data.getDataFileUrl();
        if(null != fileUrl && (fileUrl.endsWith(".rtfeatures.tsv") || fileUrl.endsWith(".features.tsv")))
            return Priority.MEDIUM;
        else
            return null;
    }
} //class MSInspectFeaturesDataHandler
