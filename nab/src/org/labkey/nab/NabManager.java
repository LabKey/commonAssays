/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.Filter;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableResultSet;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.study.Dataset;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.nab.query.NabProtocolSchema;
import org.labkey.nab.query.NabRunDataTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Oct 26, 2006
 * Time: 4:13:59 PM
 */
public class NabManager extends AbstractNabManager
{
    private static final Logger _log = Logger.getLogger(NabManager.class);
    private static final NabManager _instance = new NabManager();

    private NabManager()
    {
    }

    public static NabManager get()
    {
        return _instance;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(NabProtocolSchema.NAB_DBSCHEMA_NAME, DbSchemaType.Module);
    }

    public void deleteRunData(List<ExpData> datas) throws SQLException
    {
        super.deleteRunData(datas);

        // Get dataIds that match the ObjectUri and make filter on NabSpecimen
        Set<Integer> protocolIds = new HashSet<>();
        for (ExpData data : datas)
        {
            ExpRun run = data.getRun();
            if (null != run)
            {
                ExpProtocol protocol = run.getProtocol();
                if (null != protocol)
                    protocolIds.add(protocol.getRowId());
            }
        }

        for (Integer protocolId : protocolIds)
            NabProtocolSchema.clearProtocolFromCutoffCache(protocolId);
    }

    public ExpRun getNAbRunByObjectId(int objectId)
    {
        // objectId is really a nabSpecimenId
        TableInfo tableInfo = getSchema().getTable(NAB_SPECIMEN_TABLE_NAME);
        Filter filter = new SimpleFilter(new SimpleFilter(FieldKey.fromString("RowId"), objectId));
        List<Integer> runIds = new TableSelector(tableInfo.getColumn("RunId"), filter, null).getArrayList(Integer.class);
        if (!runIds.isEmpty())
        {
            ExpRun run = ExperimentService.get().getExpRun(runIds.get(0));
            if (null != run)
                return run;
        }
        return null;
    }

    /**
     * Returns the readable study dataset rows that correspond to the specified object ID array
     * @return a map of row information to ExpProtocol, where the row information is a pair of row ID and LSID values.
     */
    public Map<Pair<Integer, String>, ExpProtocol> getReadableStudyObjectIds(Container studyContainer, User user, int[] objectIds)
    {
        if (objectIds == null || objectIds.length == 0)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a non-empty list of object ids.");

        Study study = StudyService.get().getStudy(studyContainer);
        if (study == null)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a valid study folder.");

        List<? extends Dataset> datasets = study.getDatasets();
        if (datasets == null || datasets.isEmpty())
            return Collections.emptyMap();

        // Gather a list of readable study dataset TableInfos associated with NAb protocols (these are created when NAb data
        // is copied to a study).  We use an ArrayList, rather than a set or other dup-removing structure, because there
        // can only be one dataset/tableinfo per protocol.
        Map<TableInfo, ExpProtocol> dataTables = new HashMap<>();
        for (Dataset dataset : datasets)
        {
            if (dataset.isAssayData() && dataset.canRead(user))
            {
                ExpProtocol protocol = dataset.getAssayProtocol();
                if (protocol != null && AssayService.get().getProvider(protocol) instanceof NabAssayProvider)
                    dataTables.put(dataset.getTableInfo(user), protocol);
            }
        }

        Collection<Integer> allObjectIds = new HashSet<>();
        for (int objectId : objectIds)
            allObjectIds.add(objectId);
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RowId"), allObjectIds));

        final Map<Pair<Integer, String>, ExpProtocol> readableObjectIds = new HashMap<>();

        // For each readable study data table, find any NAb runs that match the requested objectIds, and add them to the run list:
        for (Map.Entry<TableInfo, ExpProtocol> entry : dataTables.entrySet())
        {
            TableInfo dataTable = entry.getKey();
            final ExpProtocol protocol = entry.getValue();
            TableSelector selector = new TableSelector(dataTable, PageFlowUtil.set("RowId", "Lsid"), filter, null);
            selector.forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    readableObjectIds.put(new Pair<Integer, String>(rs.getInt("RowId"), rs.getString("Lsid")), protocol);
                }
            });
        }
        return readableObjectIds;
    }

    public void getDataPropertiesFromNabRunData(NabRunDataTable nabRunDataTable, String dataRowLsid, Container container,
                        List<PropertyDescriptor> propertyDescriptors, Map<PropertyDescriptor, Object> dataProperties)
    {
        // dataRowLsid is the objectUri column
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("ObjectUri"), dataRowLsid);
        Map<PropertyDescriptor, FieldKey> fieldKeys = new HashMap<>();
        for (PropertyDescriptor pd : propertyDescriptors)
        {
            PropDescCategory pdCat = getPropDescCategory(pd.getName());
            FieldKey fieldKey = pdCat.getFieldKey();
            if (null != fieldKey)
                fieldKeys.put(pd, fieldKey);
        }

        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(nabRunDataTable, fieldKeys.values());

        try (TableResultSet resultSet = new TableSelector(nabRunDataTable, columns.values(), filter, null).getResultSet())
        {
            // We're expecting only 1 row, but there could be 0 in some cases
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
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }
}
