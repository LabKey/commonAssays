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

package org.labkey.nab;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.Filter;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.study.DataSet;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.nab.query.NabProtocolSchema;
import org.labkey.nab.query.NabRunDataTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brittp
 * Date: Oct 26, 2006
 * Time: 4:13:59 PM
 */
public class NabManager extends AbstractNabManager
{
    private static final Logger _log = Logger.getLogger(NabManager.class);
    private static final NabManager _instance = new NabManager();

    public static boolean useNewNab = true;

    private NabManager()
    {
    }

    public static NabManager get()
    {
        return _instance;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(NabProtocolSchema.NAB_DBSCHEMA_NAME);
    }

    public void deleteContainerData(Container container) throws SQLException
    {
        // Remove rows from NAbSpecimen and CutoffValue tables
        SimpleFilter runIdFilter = makeNabSpecimenContainerClause(container);

        // Delete all rows in CutoffValue table that match those nabSpecimenIds
        Filter specimenIdFilter = makeCuttoffValueSpecimenClause(runIdFilter);
        Table.delete(getSchema().getTable(NabProtocolSchema.CUTOFF_VALUE_TABLE_NAME), specimenIdFilter);

        // Delete the rows in NASpecimen hat match those runIdFilter
        TableInfo nabTableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        Table.delete(nabTableInfo, runIdFilter);

        PlateService.get().deleteAllPlateData(container);
    }

    public void deleteRunData(List<ExpData> datas) throws SQLException
    {
        // Get dataIds that match the ObjectUri and make filter on NabSpecimen
        List<Integer> dataIDs = new ArrayList<Integer>(datas.size());
        for (ExpData data : datas)
            dataIDs.add(data.getRowId());
        SimpleFilter dataIdFilter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("DataId"), dataIDs));

        // Now delete all rows in CutoffValue table that match those nabSpecimenIds
        Filter specimenIdFilter = makeCuttoffValueSpecimenClause(dataIdFilter);
        Table.delete(NabManager.getSchema().getTable(NabProtocolSchema.CUTOFF_VALUE_TABLE_NAME), specimenIdFilter);

        // Finally, delete the rows in NASpecimen
        TableInfo nabTableInfo = NabManager.getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        Table.delete(nabTableInfo, dataIdFilter);
    }

    public ExpRun getNAbRunByObjectId(int objectId)
    {
        if (!useNewNab)
        {
            OntologyObject dataRow = OntologyManager.getOntologyObject(objectId);
            if (dataRow != null)
            {
                OntologyObject dataRowParent = OntologyManager.getOntologyObject(dataRow.getOwnerObjectId().intValue());
                if (dataRowParent != null)
                {
                    ExpData data = ExperimentService.get().getExpData(dataRowParent.getObjectURI());
                    if (data != null)
                        return data.getRun();
                }
            }
        }
        else
        {   // objectId is really a nabSpecimenId
            TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
            Filter filter = new SimpleFilter(new SimpleFilter(FieldKey.fromString("RowId"), objectId));
            List<Integer> runIds = new TableSelector(tableInfo.getColumn("RunId"), filter, null).getArrayList(Integer.class);
            if (!runIds.isEmpty())
            {
                ExpRun run = ExperimentService.get().getExpRun(runIds.get(0));
                if (null != run)
                    return run;
            }
        }
        return null;
    }

    public Map<Integer, ExpProtocol> getReadableStudyObjectIds(Container studyContainer, User user, int[] objectIds)
    {
        if (objectIds == null || objectIds.length == 0)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a non-empty list of object ids.");

        Study study = StudyService.get().getStudy(studyContainer);
        if (study == null)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a valid study folder.");

        List<? extends DataSet> dataSets = study.getDataSets();
        if (dataSets == null || dataSets.isEmpty())
            return Collections.emptyMap();

        // Gather a list of readable study dataset TableInfos associated with NAb protocols (these are created when NAb data
        // is copied to a study).  We use an ArrayList, rather than a set or other dup-removing structure, because there
        // can only be one dataset/tableinfo per protocol.
        Map<TableInfo, ExpProtocol> dataTables = new HashMap<TableInfo, ExpProtocol>();
        for (DataSet dataset : dataSets)
        {
            if (dataset.isAssayData() && dataset.canRead(user))
            {
                ExpProtocol protocol = dataset.getAssayProtocol();
                if (protocol != null && AssayService.get().getProvider(protocol) instanceof NabAssayProvider)
                    dataTables.put(dataset.getTableInfo(user), protocol);
            }
        }

        Collection<Integer> allObjectIds = new HashSet<Integer>();
        for (int objectId : objectIds)
            allObjectIds.add(objectId);
        SimpleFilter filter = (!useNewNab) ? new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("ObjectId"), allObjectIds)) :
                new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RowId"), allObjectIds));

        Map<Integer, ExpProtocol> readableObjectIds = new HashMap<Integer, ExpProtocol>();

        // For each readable study data table, find any NAb runs that match the requested objectIds, and add them to the run list:
        for (Map.Entry<TableInfo, ExpProtocol> entry : dataTables.entrySet())
        {
            TableInfo dataTable = entry.getKey();
            ExpProtocol protocol = entry.getValue();
            if (!useNewNab)
            {
                ResultSet rs = null;
                try
                {
                    rs = Table.select(dataTable, Collections.singleton("ObjectId"), filter, null);

                    while (rs.next())
                    {
                        int objectId = rs.getInt("ObjectId");
                        readableObjectIds.put(objectId, protocol);
                    }
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
                finally
                {
                    if (rs != null)
                        try { rs.close(); } catch (SQLException e) { }
                }
            }
            else
            {
                List<Integer> rowIds = new TableSelector(dataTable.getColumn("RowId"), filter, null).getArrayList(Integer.class);
                if (rowIds.size() > 0)
                    readableObjectIds.put(rowIds.get(0), protocol);
            }
        }
        return readableObjectIds;
    }

    public int insertNabSpecimenRow(User user, Map<String, Object> fields) throws SQLException
    {
        TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        Map<String, Object> newFields = Table.insert(user, tableInfo, fields);
        return (Integer)newFields.get("RowId");
    }

    public void insertCutoffValueRow(User user, Map<String, Object> fields) throws SQLException
    {
        TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.CUTOFF_VALUE_TABLE_NAME);
        Map<String, Object> newFields = Table.insert(user, tableInfo, fields);
    }

    @Nullable
    public NabSpecimen getNabSpecimen(int rowId)
    {
        Filter filter = new SimpleFilter(FieldKey.fromString("RowId"), rowId);
        return getNabSpecimen(filter);
    }

    @Nullable
    public NabSpecimen getNabSpecimen(String dataRowLsid, Container container)
    {
        // dataRowLsid is the objectUri column
        SimpleFilter filter = makeNabSpecimenContainerClause(container);
        filter.addCondition(FieldKey.fromString("ObjectUri"), dataRowLsid);
        return getNabSpecimen(filter);
    }

    @Nullable
    private NabSpecimen getNabSpecimen(Filter filter)
    {
        TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        List<NabSpecimen> nabSpecimens = new TableSelector(tableInfo, Table.ALL_COLUMNS, filter, null).getArrayList(NabSpecimen.class);
        if (!nabSpecimens.isEmpty())
            return nabSpecimens.get(0);
        return null;
    }

    public List<NabSpecimen> getNabSpecimens(List<Integer> rowIds)
    {
        TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        Filter filter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RowId"), rowIds));
        List<NabSpecimen> nabSpecimens = new TableSelector(tableInfo, Table.ALL_COLUMNS, filter, null).getArrayList(NabSpecimen.class);
        return nabSpecimens;
    }

    private SimpleFilter makeNabSpecimenContainerClause(Container container)
    {
        String str = "RunId IN (SELECT RowId FROM " + ExperimentService.get().getTinfoExperimentRun().getSelectName() + " WHERE Container = '" + container.getEntityId() + "')";
        return new SimpleFilter(new SimpleFilter.SQLClause(str, new Object[]{}));
    }

    private SimpleFilter makeCuttoffValueSpecimenClause(SimpleFilter nabSpecimenFilter)
    {
        TableInfo table = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        String str = "NAbSpecimenId IN (SELECT RowId FROM " + table.getSelectName() + " " +
                nabSpecimenFilter.getWhereSQL(getSchema().getSqlDialect()) + ")";

        List<Object> paramVals = nabSpecimenFilter.getWhereParams(table);
        Object[] params = new Object[paramVals.size()];
        for (int i = 0; i < paramVals.size(); i += 1)
            params[i] = paramVals.get(i);

        return new SimpleFilter(new SimpleFilter.SQLClause(str, params));
    }

    public void getDataPropertiesFromNabRunData(NabRunDataTable nabRunDataTable, String dataRowLsid, Container container,
                        List<PropertyDescriptor> propertyDescriptors, Map<PropertyDescriptor, Object> dataProperties)
    {
        // dataRowLsid is the objectUri column
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("ObjectUri"), dataRowLsid);
        Map<PropertyDescriptor, FieldKey> fieldKeys = new HashMap<PropertyDescriptor, FieldKey>();
        for (PropertyDescriptor pd : propertyDescriptors)
        {
            PropDescCategory pdCat = getPropDescCategory(pd.getName());
            FieldKey fieldKey = pdCat.getFieldKey();
            if (null != fieldKey)
                fieldKeys.put(pd, fieldKey);
        }

        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(nabRunDataTable, fieldKeys.values());
        Table.TableResultSet resultSet = null;
        try
        {
            resultSet = new TableSelector(nabRunDataTable, columns.values(), filter, null).getResultSet();

            // We're expecting only 1 row, but there could 0 in some cases
            if (resultSet.getSize() > 0)
            {
                resultSet.next();
                Map<String, Object> rowMap = resultSet.getRowMap();
                for (PropertyDescriptor pd : propertyDescriptors)
                {
                    ColumnInfo column = columns.get(fieldKeys.get(pd));
                    if (null != column)
                    {
                        String columnAlias = column.getAlias();
                        if (null != columnAlias)
                            dataProperties.put(pd, rowMap.get(columnAlias));
                    }
                }
            }
            resultSet.close();
        }
        catch (SQLException e)
        {
            try
            {
                if (null != resultSet)
                    resultSet.close();
            }
            catch (SQLException ex)
            {
            }
            throw new RuntimeSQLException(e);
        }

    }

    // Class for parsing a Data Property Descriptor name and categorizing it
    public static class PropDescCategory
    {
        private String _origName = null;
        private String _type = null;         // ic_4pl, ic_5pl, ic_poly, point, null
        private boolean _oor = false;
        private String _rangeOrNum = null;   // inrange, number, null
        private Integer _cutoffValue = null; // value, null

        public PropDescCategory(String name)
        {
            _origName = name;
        }

        public String getOrigName()
        {
            return _origName;
        }

        public void setOrigName(String origName)
        {
            _origName = origName;
        }

        public String getType()
        {
            return _type;
        }

        public void setType(String type)
        {
            _type = type;
        }

        public boolean isOor()
        {
            return _oor;
        }

        public void setOor(boolean oor)
        {
            _oor = oor;
        }

        public String getRangeOrNum()
        {
            return _rangeOrNum;
        }

        public void setRangeOrNum(String rangeOrNum)
        {
            _rangeOrNum = rangeOrNum;
        }

        public Integer getCutoffValue()
        {
            return _cutoffValue;
        }

        public void setCutoffValue(Integer cutoffValue)
        {
            _cutoffValue = cutoffValue;
        }

        public String getColumnName()
        {
            String colName = _type;
            if (_oor) colName += "OORIndicator";
            return colName;
        }

        public String getCalculatedColumnName()
        {
            String columnName = null;
            if (null == getRangeOrNum())
            {
                columnName = getColumnName();
            }
            else if (getRangeOrNum().equalsIgnoreCase("inrange"))
            {
                columnName = getColumnName() + "InRange";
            }
            else if (getRangeOrNum().equalsIgnoreCase("number"))
            {
                columnName = getColumnName() + "Number";
            }
            return columnName;
        }

        @Nullable
        public String getCutoffValueColumnName()
        {
            if (null != _cutoffValue)
                return "Cutoff" + _cutoffValue;
            return null;
        }

        public FieldKey getFieldKey()
        {
            String cutoffColumnName = getCutoffValueColumnName();
            String columnName = getCalculatedColumnName();
            if (null != cutoffColumnName)
                return FieldKey.fromParts(cutoffColumnName, columnName);
            return FieldKey.fromString(columnName);
        }
    }

    public static PropDescCategory getPropDescCategory(String name)
    {
        PropDescCategory pdCat = new PropDescCategory(name);
        if (name.contains("InRange"))
            pdCat.setRangeOrNum("inrange");
        else if (name.contains("Number"))
            pdCat.setRangeOrNum("number");

        if (name.startsWith("Point") && name.contains("IC"))
        {
            pdCat.setType("Point");
        }
        else if (name.startsWith("Curve") && name.contains("IC"))
        {
            if (name.contains("4pl"))
                pdCat.setType("IC_4pl");
            else if (name.contains("5pl"))
                pdCat.setType("IC_5pl");
            else if (name.contains("poly"))
                pdCat.setType("IC_Poly");
            else
                pdCat.setType("IC");
        }
        else if (name.contains("AUC"))
        {
            String typePrefix = name.toLowerCase().contains("positive") ?  "PositiveAuc" : "Auc";

            if (name.contains("4pl"))
                pdCat.setType(typePrefix + NabDataHandler.PL4_SUFFIX);
            else if (name.contains("5pl"))
                pdCat.setType(typePrefix + NabDataHandler.PL5_SUFFIX);
            else if (name.contains("poly"))
                pdCat.setType(typePrefix + NabDataHandler.POLY_SUFFIX);
            else
                pdCat.setType(typePrefix);
        }
        else if (name.equalsIgnoreCase("specimen lsid"))
            pdCat.setType("SpecimenLsid");
        else if (name.equalsIgnoreCase("wellgroup name"))
            pdCat.setType("WellgroupName");
        else if (name.equalsIgnoreCase("fit error"))
            pdCat.setType("FitRrror");

        pdCat.setOor(name.contains("OORIndicator"));
        pdCat.setCutoffValue(cutoffValueFromName(name));
        return pdCat;
    }

    @Nullable
    private static Integer cutoffValueFromName(String name)
    {
        int icIndex = name.indexOf("IC");
        if (icIndex >= 0 && icIndex + 4 <= name.length())
            return Integer.valueOf(name.substring(icIndex + 2, icIndex + 4));
        return null;
    }


}
