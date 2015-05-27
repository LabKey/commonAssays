/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.elispot;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.ConcurrentCaseInsensitiveSortedMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Filter;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.StorageProvisioner;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by davebradlee on 3/19/15.
 */
public class ElispotManager
{
    private static final Logger _log = Logger.getLogger(ElispotManager.class);
    private static final ElispotManager _instance = new ElispotManager();

    public static final String ELISPOT_RUNDATA_TABLE_NAME = "rundata";

    private ElispotManager()
    {
    }

    public static ElispotManager get()
    {
        return _instance;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(ElispotProtocolSchema.ELISPOT_DBSCHEMA_NAME, DbSchemaType.Module);
    }

    public static TableInfo getTableInfoElispotRunData()
    {
        return getSchema().getTable(ELISPOT_RUNDATA_TABLE_NAME);
    }

    @NotNull
    public static TableInfo getTableInfoElispotAntigen(ExpProtocol protocol)
    {
        Domain domain = AbstractAssayProvider.getDomainByPrefix(protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        if (null != domain)
            return StorageProvisioner.createTableInfo(domain);
        throw new IllegalStateException("Domain not found for protocol: " + protocol.getName());
    }

    public int insertRunDataRow(User user, Map<String, Object> fields)
    {
        Map<String, Object> results = Table.insert(user, getTableInfoElispotRunData(), fields);
        return (Integer)results.get("RowId");
    }

    @Nullable
    public RunDataRow getRunDataRow(int rowId)
    {
        Filter filter = new SimpleFilter(FieldKey.fromString("RowId"), rowId);
        return new TableSelector(getTableInfoElispotRunData()).getObject(filter, RunDataRow.class);
    }

    @NotNull
    public List<RunDataRow> getRunDataRows(String dataRowLsid, Container container)
    {
        // dataRowLsid is the objectUri column
        SimpleFilter filter = makeRunDataContainerClause(container);
        filter.addCondition(FieldKey.fromString("ObjectUri"), dataRowLsid);
        return getRunDataRows(filter);
    }

    @Nullable
    @Deprecated
    public RunDataRow getRunDataRow(String dataRowLsid, Container container)
    {
        // dataRowLsid is the objectUri column
        SimpleFilter filter = makeRunDataContainerClause(container);
        filter.addCondition(FieldKey.fromString("ObjectUri"), dataRowLsid);
        return getRunDataRow(filter);
    }

    @Nullable
    @Deprecated
    private RunDataRow getRunDataRow(Filter filter)
    {
        List<RunDataRow> runDataRows = new TableSelector(getTableInfoElispotRunData(), filter, null).getArrayList(RunDataRow.class);
        if (!runDataRows.isEmpty())
            return runDataRows.get(0);
        return null;
    }

    @NotNull
    private List<RunDataRow> getRunDataRows(Filter filter)
    {
        return new TableSelector(getTableInfoElispotRunData(), filter, null).getArrayList(RunDataRow.class);
    }

    public void updateRunDataRow(User user, Map<String, Object> fields)
    {
        Table.update(user, getTableInfoElispotRunData(), fields, fields.get("RowId"));
    }

    public String insertAntigenRow(User user, Map<String, Object> fields, ExpProtocol protocol)
    {
        Map<String, Object> results = Table.insert(user, getTableInfoElispotAntigen(protocol), fields);
        return results.get("AntigenLsid").toString();
    }

    public void updateAntigenRow(User user, Map<String, Object> fields, ExpProtocol protocol)
    {
        String antigenLsid = fields.get("AntigenLsid").toString();
        fields.remove("AntigenLsid");
        Table.update(user, getTableInfoElispotAntigen(protocol), fields, antigenLsid);
    }

    public Map<String, Set<Integer>> getAntigenHeadings(Container container, TableInfo antigenRunTable)
    {
        final Map<String, Set<Integer>> antigenHeadingMap = new ConcurrentCaseInsensitiveSortedMap<>();
        Set<String> columns = new HashSet<>();
        columns.add("AntigenHeading");
        columns.add("RunId");
        new TableSelector(antigenRunTable, columns, makeRunDataContainerClause(container), null).
                forEachMap(new Selector.ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> object) throws SQLException
            {
                String antigenHeading = (String)object.get("AntigenHeading");
                if (null != antigenHeading)
                {
                    if (!antigenHeadingMap.containsKey(antigenHeading))
                        antigenHeadingMap.put(antigenHeading, new HashSet<Integer>());
                    antigenHeadingMap.get(antigenHeading).add((Integer)object.get("RunId"));
                }
            }
        });
        return antigenHeadingMap;
    }

    public Map<String, Object> getAntigenRow(String antigenLsid, ExpProtocol protocol)
    {
        return new TableSelector(getTableInfoElispotAntigen(protocol), new SimpleFilter(FieldKey.fromString("AntigenLsid"), antigenLsid), null).getMap();
    }

    protected SimpleFilter makeRunDataContainerClause(Container container)
    {
        String str = "RunId IN (SELECT RowId FROM " + ExperimentService.get().getTinfoExperimentRun().getSelectName() + " WHERE Container = '" + container.getEntityId() + "')";
        return new SimpleFilter(new SimpleFilter.SQLClause(str, new Object[]{}, FieldKey.fromString("RunId")));
    }

    public void deleteRunData(List<ExpData> datas)
    {
        DbScope scope = getSchema().getScope();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            // Delete all rows based on the runId
            Set<Integer> runIds = new HashSet<>();
            for (ExpData data : datas)
            {
                runIds.add(data.getRunId());
            }

            // Since runIds may be from different folders, delete from each separately
            for (Integer runId : runIds)
            {
                if (null != runId)
                {
                    ExpRun run = ExperimentService.get().getExpRun(runId);
                    if (null != run)
                    {
                        ExpProtocol protocol = run.getProtocol();
                        if (null != protocol)
                            Table.delete(getTableInfoElispotAntigen(protocol), new SimpleFilter(FieldKey.fromString("RunId"), runId));
                    }
                }
            }

            SimpleFilter dataIdFilter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RunId"), runIds));
            Table.delete(getTableInfoElispotRunData(), dataIdFilter);

            transaction.commit();
        }
    }
}
