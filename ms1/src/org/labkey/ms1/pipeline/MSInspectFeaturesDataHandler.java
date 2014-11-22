/*
 * Copyright (c) 2006-2014 LabKey Corporation
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

package org.labkey.ms1.pipeline;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.MS1Controller;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This data handler loads msInspect feature files, which use a tsv format.
 * It also handles deleting and moving that data when the experiment run
 * is deleted or moved.
 * @author DaveS
 * User: daves
 * Date: Sept, 2007
 */
public class MSInspectFeaturesDataHandler extends AbstractExperimentDataHandler
{
    /**
     * Old feature finding extension
     */
    public static final FileType FT_FEATURES = new FileType(".features.tsv");

    /**
     * Current feature finding extension
     */
    public static final FileType FT_PEPTIDES = new FileType(".peptides.tsv");

    /**
     * Features with matching peptides
     */
    public static final FileType FT_PEPMATCH = new FileType(".pepmatch.tsv");

    /**
     * This class maps a source column in the features tsv file with its
     * target database column and its jdbc data type. This is used within
     * the MSInspectFeaturesDataHandler class, and enables us to handle
     * feature files with missing or additional (but well-known) columns.
     * @author DaveS
     */
    protected static class ColumnBinding
    {
        public String sourceColumn;    //name of the source column
        public String targetColumn;    //name of the target column
        public int jdbcType;           //jdbc data type of target column
        public boolean isRequired;     //true if this column is required

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

        public Class getJavaClass()
        {
            switch (jdbcType)
            {
                case Types.TINYINT:
                case Types.INTEGER:
                    return Integer.class;
                case Types.REAL:
                    return Double.class;
                case Types.BOOLEAN:
                    return Boolean.class;
                case Types.CHAR:
                case Types.VARCHAR:
                    return String.class;
                default:
                    throw new IllegalArgumentException("Unexpected java.sql.Type: "+ jdbcType);
            }
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

    /**
     * Helper class for detecting conversion errors when using
     * the TabLoader class.
     */
    protected static class ConversionError
    {
        private String _columnName = "";

        public ConversionError(String columnName)
        {
            _columnName = columnName;
        }

        public String getColumnName()
        {
            return _columnName;
        }
    }

    //Constants and Static Data Members
    private static final int CHUNK_SIZE = 1000;         //number of insert statements in a batch

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
        _bindingMap.put(new ColumnBinding("accurateMZ", "AccurateMZ", java.sql.Types.BOOLEAN, false));
        _bindingMap.put(new ColumnBinding("mass", "Mass", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("intensity", "Intensity", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("charge", "Charge", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("chargeStates", "ChargeStates", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("kl", "KL", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("background", "Background", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("median", "Median", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("peaks", "Peaks", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanFirst", "ScanFirst", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanLast", "ScanLast", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanCount", "ScanCount", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("totalIntensity", "TotalIntensity", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("description", "Description", java.sql.Types.VARCHAR, false));

        //columns added by Ceaders-Sinai to their post-processed features files
        _bindingMap.put(new ColumnBinding("MS2scan", "MS2Scan", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("probability", "MS2ConnectivityProbability", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("MS2charge", "MS2Charge", java.sql.Types.TINYINT, false));
    } //static init for _bindingMap

    @Override
    public DataType getDataType()
    {
        return null;
    }

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
        if (null == data || null == dataFile || null == info || null == log || null == context)
            return;

        int numRows = 0;
        //if this file has already been imported before, just return
        if (MS1Manager.get().isAlreadyImported(dataFile, data))
        {
            log.info("Already imported features file " + dataFile.toURI() + " for this experiment into this container.");
            return;
        }

        //FIX: 8403
        //When doing MSInspect + Pepmatch,
        //the new pipeline is generating a XAR that loads both the .pepmatch.tsv and the .peptides.tsv files
        //The former is simply the latter with a few extra columns added, so we should load only .pepmatch.tsv in this case.
        //if file is .peptides.tsv, look to see if there is a corresponding .pepmatch.tsv, and if so, just return
        if (FT_PEPTIDES.isType(dataFile))
        {
            File pepmatch = new File(dataFile.getParentFile(), FT_PEPMATCH.getDefaultName(FT_PEPTIDES.getBaseName(dataFile)));
            if (pepmatch.exists())
                return;
        }

        //NOTE: I'm using the highly-efficient technique of prepared statements and batch execution here,
        //but that also means I'm not using the Table layer and benefiting from its functionality.
        // This may need to change in the future.
        Connection cn;
        PreparedStatement pstmt = null;

        //get the ms1 schema and scope
        DbSchema schema = DbSchema.get("ms1");
        DbScope scope = schema.getScope();

        //begin a transaction
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            cn = transaction.getConnection();
            long startMs = System.currentTimeMillis();

            //insert the feature files row
            int idFile = insertFeaturesFile(info.getUser(), schema, data);

            //open the tsv file using TabLoader for automatic parsing
            TabLoader tsvloader = new TabLoader(dataFile);
            Iterator<Map<String, Object>> iter = tsvloader.iterator();
            ColumnDescriptor[] coldescrs = tsvloader.getColumns();

            //set the error value for each column descriptor so that we can
            //detect conversion errors as we process the rows
            for(ColumnDescriptor coldescr : coldescrs)
                coldescr.errorValues = new ConversionError(coldescr.name);

            //insert information about the software used to produce the file
            insertSoftwareInfo(tsvloader.getComments(), idFile, info.getUser(), schema);

            //select the appropriate bindings for this tsv file
            ArrayList<ColumnBinding> bindings = selectBindings(coldescrs, log);

            //if there are no bindings, there is nothing in the file we know how
            //to import, so just return
            if(bindings.isEmpty())
            {
                log.warn("The file " + dataFile.toURI() + " did not contain any columns this system knows how to import.");
                return;
            }

            //build the approrpriate insert sql for the features table
            //and prepare it
            pstmt = cn.prepareStatement(genInsertSQL(bindings));

            Map row;

            //iterate over the rows
            while(iter.hasNext())
            {
                //get a row
                row = iter.next();
                ++numRows;

                validateRow(row, numRows);

                //set parameter values
                pstmt.clearParameters();
                pstmt.setInt(1, idFile); //jdbc params are 1-based!

                for(int idx = 0; idx < bindings.size(); ++idx)
                    setParam(pstmt, idx + 2, numRows, bindings.get(idx), row);

                //add a batch
                pstmt.addBatch();

                //execute if we've reached our chunk limit
                if((numRows % CHUNK_SIZE) == 0)
                    pstmt.executeBatch();

                if(0 == numRows % 5000)
                    log.info("Imported " + numRows + " features to the database so far...");
            } //while reading rows

            //execute any remaining in the batch
            if(numRows % CHUNK_SIZE != 0)
                pstmt.executeBatch();

            //commit the transaction
            transaction.commit();

            log.info("Finished loading " + numRows + " features in " + (System.currentTimeMillis() - startMs) + " milliseconds.");
        }
        catch(ConversionException ex)
        {
            log.error("Error while converting data in row " + (numRows + 1) + " : " + ex);
            throw new ExperimentException(ex);
        }
        catch(IOException ex)
        {
            throw new ExperimentException(ex);
        }
        catch(SQLException ex)
        {
            throw new ExperimentException(MS1Manager.get().getAllErrors(ex));
        }
        finally
        {
            //final cleanup
            try{if(null != pstmt) pstmt.close();}catch(SQLException ignore){}
        } //finally

    } //importFile()

    /**
     * Validates the row, throwing an exception if it's invalid
     * @param row The row
     * @param rowNum The row number (for error messages)
     * @throws ExperimentException Thrown if the row is invalid
     */
    protected void validateRow(Map row, int rowNum) throws ExperimentException
    {
        //if MS2Scan value is present, MS2Charge must be there as well
        //and vice-versa
        if(row.get("MS2scan") != null && row.get("MS2charge") == null)
            throw new ExperimentException("Missing MS2charge value for row " + rowNum + "! If MS2scan is specified, MS2charge must be as well." +
                    " Use the MS1PeptideParser.exe program to re-match the features to the peptides.");

        if(row.get("MS2charge") != null && row.get("MS2scan") == null)
            throw new ExperimentException("Missing MS2scan value for row " + rowNum + "! If MS2charge is specified, MS2scan must be as well." + 
                    " Use the MS1PeptideParser.exe program to re-match the features to the peptides.");

    }

    protected int insertFeaturesFile(User user, DbSchema schema, ExpData data) throws SQLException, ExperimentException
    {
        HashMap<String,Object> map = new HashMap<>();
        map.put("FileId",null);
        map.put("ExpDataFileId", data.getRowId());
        map.put("Type", MS1Manager.FILETYPE_FEATURES);
        map.put("MzXmlURL", PeaksFileDataHandler.getMzXmlFilePath(data));
        map.put("Imported", Boolean.TRUE);

        Map outMap = Table.insert(user, schema.getTable(MS1Manager.TABLE_FILES), map);
        if (null == outMap.get("FileId"))
            throw new ExperimentException("Unable to get new id for features file.");
        
        return ((Integer) (outMap.get("FileId"))).intValue();
    } //insertFeaturesFile()

    protected void insertSoftwareInfo(Map<String, String> comments, int idFile, User user, DbSchema schema) throws SQLException
    {
        HashMap<String,Object> software = new HashMap<>();
        software.put("SoftwareId", null);
        software.put("FileId", idFile);
        software.put("Name", "msInspect");
        software.put("Author", "Fred Hutchinson Cancer Research Center");

        Map<String, Object> outMap = Table.insert(user, schema.getTable(MS1Manager.TABLE_SOFTWARE), software);

        //now try to get the algorithm from the comments
        //if we can get it, add that as a named parameter
        String algorithm = comments.get("algorithm");
        if(null != algorithm && algorithm.length() > 0)
        {
            HashMap<String,Object> softwareParam = new HashMap<>();
            softwareParam.put("SoftwareId", outMap.get("SoftwareId"));
            softwareParam.put("Name", "algorithm");
            softwareParam.put("Value", algorithm);

            Table.insert(user, schema.getTable(MS1Manager.TABLE_SOFTWARE_PARAMS), softwareParam);
        }
    } //insertSoftwareInfo()

    /**
     * Selects the appropriate column bindings based on the passed column descriptors
     * @param coldescrs The set of column descriptors for the tsv file
     * @param log       Log file
     * @return          The appropriate set of column bindings
     */
    protected ArrayList<ColumnBinding> selectBindings(ColumnDescriptor[] coldescrs, Logger log)
    {
        ArrayList<ColumnBinding> ret = new ArrayList<>(coldescrs.length);
        ColumnBinding binding;
        for(ColumnDescriptor coldescr : coldescrs)
        {
            binding = _bindingMap.get(coldescr.name);
            if(null != binding)
            {
                ret.add(binding);
                coldescr.clazz = binding.getJavaClass();
                coldescr.converter = ConvertUtils.lookup(coldescr.clazz);
            }
            else
                log.warn("The msInspect Features importer does not recognize the column '" + coldescr.name + "' in this file. Its contents will be ignored.");
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
        sbCols.append(MS1Manager.get().getSQLTableName(MS1Manager.TABLE_FEATURES));
        sbCols.append(" (FileId");

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
     * @throws ExperimentException Thrown if required value is not present
     * @throws SQLException Thrown if there is a database exception
     */
    protected void setParam(PreparedStatement pstmt, int paramIndex, int rowNum, ColumnBinding binding, Map row) throws ExperimentException, SQLException
    {
        if(null == binding)
            return;

        Object val = row.get(binding.sourceColumn);

        if(val instanceof ConversionError)
            throw new ExperimentException("Error converting the value in column '" + ((ConversionError)val).getColumnName() + "' at row " + rowNum);

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
                //switch on target column jdbc type
                switch(binding.jdbcType)
                {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        pstmt.setBoolean(paramIndex, objToBoolean(val, binding, rowNum));
                        break;

                    case Types.DATE:
                        pstmt.setDate(paramIndex, objToDate(val, binding, rowNum));
                        break;

                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                        pstmt.setString(paramIndex, val.toString());
                        break;

                    case Types.INTEGER:
                    case Types.SMALLINT:
                    case Types.TINYINT:
                        pstmt.setInt(paramIndex, objToInt(val, binding, rowNum));
                        break;

                    case Types.REAL:
                    case Types.NUMERIC:
                    case Types.FLOAT:
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                        pstmt.setDouble(paramIndex, objToDouble(val, binding, rowNum));
                        break;

                    default:
                        assert false : "Unsupported JDBC type."; //if you get this, add support above
                }
            } //not null
        }
        catch(SQLException e)
        {
            throw new ExperimentException("Problem setting the value for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to the value " + val + ": " + e.toString());
        }
    } //setParam()

    protected int objToInt(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Number)
                return ((Number)val).intValue();
            else if(val instanceof String)
                return Integer.parseInt((String)val);
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to an integer as required by the database.");
        }
        catch (NumberFormatException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to an integer for the following reason: " + e);
        }
    }

    protected double objToDouble(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Number)
                return ((Number)val).doubleValue();
            else if(val instanceof String)
                return Double.parseDouble((String)val);
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to a double-precision decimal number as required by the database.");
        }
        catch (NumberFormatException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to a double-precision decimal number for the following reason: " + e);
        }
    }

    protected boolean objToBoolean(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Boolean)
                return ((Boolean)val).booleanValue();
            if(val instanceof Number)
                return ((Number)val).intValue() != 0;
            else if(val instanceof String)
            {
                BooleanFormat parser = BooleanFormat.getInstance();
                return parser.parseObject((String)val).booleanValue();
            }
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to a boolean as required by the database.");
        }
        catch (ParseException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to a boolean for the following reason: " + e);
        }
    }

    protected java.sql.Date objToDate(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Date)
                return new java.sql.Date(((Date)val).getTime());
            else if(val instanceof String)
            {
                SimpleDateFormat parser = new SimpleDateFormat();
                return new java.sql.Date(parser.parse((String)val).getTime());
            }
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to a date/time as required by the database.");
        }
        catch (ParseException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to a date/time for the following reason: " + e);
        }
    }

    /**
     * Returns the content URL for files imported through this class. This is called by the Experiment module
     * @param data          The experiment data object
     * @return              The URL the user should be redirected to
     */
    public ActionURL getContentURL(ExpData data)
    {
        ExpRun run = data.getRun();
        if (run == null)
        {
            return null;
        }
        ActionURL url = new ActionURL(MS1Controller.ShowFeaturesAction.class, data.getContainer());
        url.addParameter("runId", Integer.toString(run.getRowId()));
        return url;
    }

    /**
     * Deletes data rows imported by this class when the experiment run is deleted
     * @param data          The experiment data file being deleted
     * @param container     The container in which it lives
     * @param user          The user deleting it
     */
    public void deleteData(ExpData data, Container container, User user)
    {
        // Delete the database records for this features file
        if(null == data || null == user)
                return;

        //Although it's not terribly obvious, the caller will have already begun a transaction
        //and the DbScope code will generate an exception if you call beginTrans() more than once
        //so don't use a transaction here because it's already transacted in the caller.
        MS1Manager.get().deleteFeaturesData(data);
    } //deleteData()

    /**
     * Moves the container for the given data file uploaded through this class
     * @param newData           The new experiment data object
     * @param container         The old container
     * @param targetContainer   The the container
     * @param oldRunLSID        The old run LSID
     * @param newRunLSID        The new run LSID
     * @param user              The user moving the data
     * @param oldDataRowId      The old data file row id
     */
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowId)
    {
        if(null == newData || null == user)
                return;

        //update the database records to reflect the new data file row id
        MS1Manager.get().moveFileData(oldDataRowId, newData.getRowId());
    }

    /**
     * Returns the priority if the passed data file is one this class knows how to import, otherwise null.
     * @param data  The data file to import
     * @return      Priority if this file can import it, otherwise null.
     */
    public Priority getPriority(ExpData data)
    {
        //we handle only *.features.tsv files
        String fileUrl = data.getDataFileUrl();
        if(null != fileUrl && (FT_FEATURES.isType(fileUrl) || FT_PEPTIDES.isType(fileUrl) || FT_PEPMATCH.isType(fileUrl)))
            return Priority.MEDIUM;
        else
            return null;
    }
} //class MSInspectFeaturesDataHandler
