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
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.DataSet;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.nab.query.NabProtocolSchema;

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

    public static boolean useNewNab = false;

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
        // First get rows of ExperimentRun matching the container
        Filter containerFilter = new SimpleFilter(FieldKey.fromString("Container"), container.getEntityId());
        TableSelector expSelector = new TableSelector(ExperimentService.get().getTinfoExperimentRun().getColumn("RowId"), containerFilter, null);
        List<Integer> runIds = expSelector.getArrayList(Integer.class);

        // Now get rows of NAbSpecimen that match those runIds
        Filter runIdFilter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RunId"), runIds));
        TableInfo nabTableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        TableSelector nabSelector = new TableSelector(nabTableInfo.getColumn("RowId"), runIdFilter, null);
        List<Integer> nabSpecimenIds = nabSelector.getArrayList(Integer.class);

        // Now delete all rows in CutoffValue table that match those nabSpecimenIds
        Filter specimenIdFilter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("NAbSpecimenId"), nabSpecimenIds));
        Table.delete(getSchema().getTable(NabProtocolSchema.CUTOFF_VALUE_TABLE_NAME), specimenIdFilter);

        // Finally, delete the rows in NASpecimen
        Table.delete(nabTableInfo, runIdFilter);

        PlateService.get().deleteAllPlateData(container);
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
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause("ObjectId", allObjectIds));

        Map<Integer, ExpProtocol> readableObjectIds = new HashMap<Integer, ExpProtocol>();

        // For each readable study data table, find any NAb runs that match the requested objectIds, and add them to the run list:
        for (Map.Entry<TableInfo, ExpProtocol> entry : dataTables.entrySet())
        {
            TableInfo dataTable = entry.getKey();
            ExpProtocol protocol = entry.getValue();
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
        TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        Filter filter = new SimpleFilter(FieldKey.fromString("RowId"), rowId);
        List<NabSpecimen> nabSpecimens = new TableSelector(tableInfo, Table.ALL_COLUMNS, filter, null).getArrayList(NabSpecimen.class);
        if (!nabSpecimens.isEmpty())
            return nabSpecimens.get(0);
        return null;
    }

    @Nullable
    public NabSpecimen getNabSpecimen(String dataRowLsid, Container container)
    {
        // dataRowLsid is the objectUri column
        TableInfo tableInfo = getSchema().getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Container"), container.getEntityId());
        filter.addCondition(FieldKey.fromString("ObjectUri"), dataRowLsid);
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
}
