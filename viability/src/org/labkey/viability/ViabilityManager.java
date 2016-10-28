/*
 * Copyright (c) 2009-2016 LabKey Corporation
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

package org.labkey.viability;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.TempTableTracker;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.CPUTimer;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.TestContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ViabilityManager
{
    private static final Logger LOG = org.apache.log4j.Logger.getLogger(ViabilityManager.class);
    private static final ViabilityManager _instance = new ViabilityManager();

    private ViabilityManager()
    {
        // prevent external construction with a private default constructor
    }

    public static ViabilityManager get()
    {
        return _instance;
    }

    public ViabilityAssayProvider getProvider()
    {
        return (ViabilityAssayProvider) AssayService.get().getProvider(ViabilityAssayProvider.NAME);
    }

    public static ViabilityResult[] getResults(ExpData data, Container container)
    {
        return new TableSelector(ViabilitySchema.getTableInfoResults(), SimpleFilter.createContainerFilter(container).addCondition(FieldKey.fromParts("DataID"), data.getRowId()), null).getArray(ViabilityResult.class);
    }

    public static ViabilityResult[] getResults(@Nullable ExpRun run, @Nullable Container container)
    {
        if (run == null && container == null)
            throw new IllegalArgumentException();

        SimpleFilter filter = new SimpleFilter();
        if (container != null)
            filter.addCondition(FieldKey.fromParts("Container"), container);
        if (run != null)
            filter.addCondition(FieldKey.fromParts("runid"), run.getRowId());

        return new TableSelector(ViabilitySchema.getTableInfoResults(), filter, null).getArray(ViabilityResult.class);
    }

    /**
     * Get a viability result row.
     * @param resultRowId The row id of the result to get.
     * @return The ViabilityArrayResult for the row.
     */
    public static ViabilityResult getResult(Container c, int resultRowId) throws SQLException
    {
        ViabilityResult result = new TableSelector(ViabilitySchema.getTableInfoResults()).getObject(resultRowId, ViabilityResult.class);
        if (result == null)
            return null;
        // lazily fetch specimens and properties
//        String[] specimens = getSpecimens(resultRowId);
//        result.setSpecimenIDs(Arrays.asList(specimens));
//        result.setProperties(getProperties(c, result.getObjectID()));
        return result;
    }

    static String[] getSpecimens(int resultRowId)
    {
        return new TableSelector(
                ViabilitySchema.getTableInfoResultSpecimens(),
                PageFlowUtil.set("SpecimenID"),
                new SimpleFilter(FieldKey.fromParts("ResultID"), resultRowId),
                new Sort("SpecimenIndex")).getArray(String.class);
    }

    static Map<PropertyDescriptor, Object> getProperties(int objectID) throws SQLException
    {
        assert objectID > 0;
        OntologyObject obj = OntologyManager.getOntologyObject(objectID);
        assert obj != null;

        Map<String, Object> oprops = OntologyManager.getProperties(obj.getContainer(), obj.getObjectURI());
        Map<PropertyDescriptor, Object> properties = new HashMap<>();
        for (Map.Entry<String, Object> entry : oprops.entrySet())
        {
            String propertyURI = entry.getKey();
            PropertyDescriptor pd = OntologyManager.getPropertyDescriptor(propertyURI, obj.getContainer());
            assert pd != null;
            properties.put(pd, entry.getValue());
        }
        return properties;
    }

    /**
     * Insert or update a ViabilityResult row.
     * @param user
     * @param result
     * @throws SQLException
     */
    public static void saveResult(User user, Container c, ViabilityResult result, int rowIndex) throws SQLException, ValidationException
    {
        assert user != null && c != null : "user or container is null";
        assert result.getDataID() > 0 : "DataID is not set";
        assert result.getPoolID() != null : "PoolID is not set";
        assert result.getRunID() > 0 : "RunID is not set";

        if (result.getRowID() == 0)
        {
            String lsid = new Lsid(ViabilityAssayProvider.RESULT_LSID_PREFIX, result.getDataID() + "-" + result.getPoolID() + "-" + rowIndex).toString();
            Integer id = OntologyManager.ensureObject(c, lsid);

            result.setObjectID(id.intValue());
            ViabilityResult inserted = Table.insert(user, ViabilitySchema.getTableInfoResults(), result);
            result.setRowID(inserted.getRowID());
        }
        else
        {
            assert result.getObjectID() > 0;
            deleteSpecimens(result.getRowID());
            deleteProperties(c, result.getObjectID());
            Table.update(user, ViabilitySchema.getTableInfoResults(), result, result.getRowID());
        }

        insertSpecimens(user, result);
        insertProperties(c, result);
    }

    private static void insertSpecimens(User user, ViabilityResult result)
    {
        insertSpecimens(user, result.getRowID(), result.getSpecimenIDList());
    }

    private static void insertSpecimens(User user, int resultId, List<String> specimens)
    {
        if (specimens == null || specimens.size() == 0)
            return;

        for (int index = 0; index < specimens.size(); index++)
        {
            String specimenID = specimens.get(index);
            if (specimenID == null || specimenID.length() == 0)
                continue;
            
            Map<String, Object> resultSpecimen = new HashMap<>();
            resultSpecimen.put("ResultID", resultId);
            resultSpecimen.put("SpecimenID", specimens.get(index));
            resultSpecimen.put("SpecimenIndex", index);

            Table.insert(user, ViabilitySchema.getTableInfoResultSpecimens(), resultSpecimen);
        }
    }

    private static void insertProperties(Container c, ViabilityResult result) throws SQLException, ValidationException
    {
        Map<PropertyDescriptor, Object> properties = result.getProperties();
        if (properties == null || properties.size() == 0)
            return;

        OntologyObject obj = OntologyManager.getOntologyObject(result.getObjectID());
        assert obj != null;

        List<ObjectProperty> oprops = new ArrayList<>(properties.size());
        for (Map.Entry<PropertyDescriptor, Object> prop : properties.entrySet())
        {
            Object value = prop.getValue();
            if (value == null)
                continue;

            PropertyDescriptor pd = prop.getKey();
            assert pd != null && pd.getPropertyURI() != null;
            String propertyURI = pd.getPropertyURI();
            oprops.add(new ObjectProperty(obj.getObjectURI(), c, propertyURI, value));
        }

        OntologyManager.insertProperties(c, obj.getObjectURI(), oprops.toArray(new ObjectProperty[oprops.size()]));
    }


    // need an an object to track the temptable (don't want to use String, too confusing)
    static class SpecimenAggregateTempTableToken
    {
        SpecimenAggregateTempTableToken(String name)
        {
            this.name = name;
        }
        String name;
    }

    static AtomicInteger tempTableCounter = new AtomicInteger();

    // TODO: call when: specimens imported, editable specimens inserted/updated/deleted, target study edited (for editable assay)
    // DONE: call when: run inserted, LetvinController.CreateVialsAction
    public static void updateSpecimenAggregates(User user, Container c, @NotNull AssayProvider provider, @NotNull ExpProtocol protocol, @Nullable ExpRun run)
    {
        if (!(provider instanceof ViabilityAssayProvider))
            throw new IllegalArgumentException("Viability assay provider required");

        ViabilityAssaySchema schema = new ViabilityAssaySchema(user, c, (ViabilityAssayProvider)provider, protocol, null);

        DbScope scope = schema.getDbSchema().getScope();

        SQLFragment specimenAggregates = specimenAggregates(schema, run);
        if (null == specimenAggregates)
            return;

        // Create temp table with specimen aggregate results
        String shortName = "ViabilitySpecimenAgg_" + protocol.getRowId() + "_" + tempTableCounter.incrementAndGet();
        String tempTableName = schema.getSqlDialect().getGlobalTempTablePrefix() + shortName;
        SpecimenAggregateTempTableToken tok = new SpecimenAggregateTempTableToken(tempTableName);
        TempTableTracker tracker = TempTableTracker.track(shortName, tok);

        CPUTimer t = new CPUTimer("viability.updateSpecimenAggregates");
        try (DbScope.Transaction tx = scope.ensureTransaction())
        {
            // Working with a temp table, so use the same connection for all updates
            Connection connection = scope.getConnection();
            SqlExecutor executor = new SqlExecutor(scope, connection);

            SQLFragment insertSQL = new SQLFragment();
            insertSQL.append("SELECT X.* INTO ").append(tempTableName).append(" FROM (\n");
            insertSQL.append(specimenAggregates).append("\n");
            insertSQL.append(") X");

            t.start();
            executor.execute(insertSQL);
            t.stop();

            if (LOG.isDebugEnabled())
            {
                long count = executor.executeWithResults(new SQLFragment("SELECT COUNT(*) FROM " + tempTableName), new BaseSelector.ResultSetHandler<Long>()
                {
                    @Override
                    public Long handle(ResultSet rs, Connection conn) throws SQLException
                    {
                        rs.next();
                        return rs.getLong(1);
                    }
                });
                LOG.debug(String.format("viability specimens: create temp table: rows=%d, duration=%d", count, t.getTotalMilliseconds()));
            }

            // CONSIDER: add indices on the temp table


// NULL-ing out the values doesn't seem necessary
//            //
//            // NULL out the aggregate values for rows that no longer have any specimen matches
//            //
//
//            SQLFragment nullFrag = new SQLFragment();
//            nullFrag.append("UPDATE ").append(ViabilitySchema.getTableInfoResults()).append(" SET\n");
//            nullFrag.append("  SpecimenAggregatesUpdated = {fn now()},\n");
//            nullFrag.append("  SpecimenCount = specimen_agg.SpecimenCount,\n");
//            nullFrag.append("  SpecimenMatchCount = NULL,\n");
//            nullFrag.append("  OriginalCells = NULL,\n");
//            nullFrag.append("  SpecimenMatches = NULL\n");
//            nullFrag.append("WHERE\n");
//            nullFrag.append("  ProtocolId = ").append(protocol.getRowId()).append(" AND\n");
//            if (run != null)
//            {
//                nullFrag.append("  RunId = ").append(run.getRowId()).append(" AND\n");
//            }
//            // Only update rows that previously held specimen match values and no longer do
//            nullFrag.append("  SpecimenMatches IS NOT NULL AND\n");
//            nullFrag.append("  RowId NOT IN (SELECT ResultId FROM ").append(tempTableName).append(" specimen_agg)\n");
//
//            t.start();
//            int rows = executor.execute(nullFrag);
//            t.stop();
//            LOG.debug(String.format("viability specimens: null aggregates: rows=%d, duration=%d", rows, t.getTotalMilliseconds()));


            //
            // update the aggregate values for rows that have specimen matches, skipping rows whose values aren't changed.
            //

            SQLFragment updateFrag = new SQLFragment();
            updateFrag.append("UPDATE ").append(ViabilitySchema.getTableInfoResults()).append(" SET\n");
            updateFrag.append("  SpecimenAggregatesUpdated = agg.SpecimenAggregatesUpdated,\n");
            updateFrag.append("  SpecimenCount = agg.SpecimenCount,\n");
            updateFrag.append("  SpecimenMatchCount = agg.SpecimenMatchCount,\n");
            updateFrag.append("  OriginalCells = agg.OriginalCells,\n");
            updateFrag.append("  SpecimenIDs = CASE WHEN agg.SpecimenCount = 0 THEN NULL ELSE agg.SpecimenIDs END,\n");
            updateFrag.append("  SpecimenMatches = CASE WHEN agg.SpecimenMatchCount = 0 THEN NULL ELSE agg.SpecimenMatches END\n");
            updateFrag.append("FROM ").append(tempTableName).append(" agg\n");
            // The EXISTS clause excludes rows that are the same in both tables and handles NULLs correctly
            updateFrag.append("WHERE\n");
            updateFrag.append("  ProtocolId = ").append(protocol.getRowId()).append(" AND\n");
            updateFrag.append("  RowId = agg.ResultId AND\n");
            updateFrag.append("  EXISTS (\n");
            updateFrag.append("    SELECT results.RowId, results.SpecimenCount, results.SpecimenMatchCount, results.OriginalCells, results.SpecimenMatches\n");
            updateFrag.append("    EXCEPT\n");
            updateFrag.append("    SELECT agg.ResultId, agg.SpecimenCount, agg.SpecimenMatchCount, agg.OriginalCells, agg.SpecimenMatches\n");
            updateFrag.append("  )\n");

            t.start();
            int rows = executor.execute(updateFrag);
            t.stop();
            LOG.info(String.format("viability specimens: update aggregates: rows=%d, duration=%d", rows, t.getTotalMilliseconds()));

            tx.commit();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            // Clean up the temp table
            if (tracker != null)
                tracker.delete();
        }
    }

    @Nullable
    private static SQLFragment specimenAggregates(ViabilityAssaySchema schema, ExpRun run)
    {
        ViabilityAssaySchema.ResultSpecimensTable rs = schema.createResultSpecimensTable();
        rs.setContainerFilter(ContainerFilter.EVERYTHING);

        List<FieldKey> fields = new ArrayList<>();
        FieldKey resultId = FieldKey.fromParts("ResultID");
        FieldKey specimenId = FieldKey.fromParts("SpecimenID");
        FieldKey volume = FieldKey.fromParts("SpecimenID", "Volume");
        FieldKey globalUniqueId = FieldKey.fromParts("SpecimenID", "GlobalUniqueId");
        fields.add(resultId);
        fields.add(specimenId);
        fields.add(volume);
        fields.add(globalUniqueId);

        // Use the copied TargetStudy as a column from viability.results instead of the TargetStudy that is on the batch or run domain.
        fields.add(FieldKey.fromParts("ResultID", AbstractAssayProvider.TARGET_STUDY_PROPERTY_NAME));

        Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(rs, fields);

        if (!columnMap.containsKey(resultId) || !columnMap.containsKey(volume) || !columnMap.containsKey(specimenId) || !columnMap.containsKey(globalUniqueId))
            return null;

        SimpleFilter filter = new SimpleFilter();
        if (run != null)
        {
            // Only update results in the run
            filter.addCondition(FieldKey.fromParts("ResultID", "Run"), run.getRowId());
        }
        SQLFragment sub = QueryService.get().getSelectSQL(rs, columnMap.values(), filter, null, Table.ALL_ROWS, Table.NO_OFFSET, false);

        SQLFragment groupFrag = new SQLFragment();
        groupFrag.append("SELECT\n");
        groupFrag.append("  {fn now()} AS SpecimenAggregatesUpdated,\n");
        groupFrag.append("  " + columnMap.get(resultId).getAlias() + " AS ResultID,\n");
        groupFrag.append("  SUM(" + columnMap.get(volume).getAlias() + ") AS OriginalCells,\n");
        groupFrag.append("  COUNT(" + columnMap.get(specimenId).getAlias() + ") AS SpecimenCount,\n");
        groupFrag.append("  COUNT(" + columnMap.get(globalUniqueId).getAlias() + ") AS SpecimenMatchCount,\n");

        if (schema.getDbSchema().getSqlDialect().supportsGroupConcat())
        {
            // New viability.resultspecimens are added by the LetvinController.CreateVialsAction.importSpecimens() method
            // We need to re-generate the group-concat of the specimen IDs.
            SQLFragment specimenID = new SQLFragment(columnMap.get(specimenId).getAlias());
            SQLFragment specimenIDs = schema.getDbSchema().getSqlDialect().getGroupConcat(specimenID, true, true);
            groupFrag.append("  ").append(specimenIDs).append(" AS SpecimenIDs,\n");

            SQLFragment guid = new SQLFragment(columnMap.get(globalUniqueId).getAlias());
            SQLFragment specimenMatches = schema.getDbSchema().getSqlDialect().getGroupConcat(guid, true, true);
            groupFrag.append("  ").append(specimenMatches).append(" AS SpecimenMatches\n");
        }
        else
        {
            groupFrag.append("  NULL AS SpecimenIDs,\n");
            groupFrag.append("  NULL AS SpecimenMatches\n");
        }

        groupFrag.append("FROM (\n");
        groupFrag.append(sub);
        groupFrag.append(") y \nGROUP BY " + columnMap.get(resultId).getAlias());

        // Debug the specimen aggregate queries
        if (LOG.isDebugEnabled())
        {
            DbScope scope = schema.getDbSchema().getScope();

            LOG.debug(String.format("viability specimens: all for container=%s, protocol=%d, run=%d", schema.getContainer().getPath(), schema.getProtocol().getRowId(), run == null ? 0 : run.getRowId()));
            ResultSet allResultSet = new SqlSelector(scope, sub).getResultSet();
            ResultSetUtil.logData(allResultSet, LOG);

            LOG.debug(String.format("viability specimens: aggregates for container=%s, protocol=%d, run=%d", schema.getContainer().getPath(), schema.getProtocol().getRowId(), run == null ? 0 : run.getRowId()));
            ResultSet groupResultSet = new SqlSelector(scope, groupFrag).getResultSet();
            ResultSetUtil.logData(groupResultSet, LOG);
        }

        return groupFrag;
    }

    /**
     * Delete a ViabilityResult row.
     */
    public static void deleteResult(Container c, ViabilityResult result) throws SQLException
    {
        assert result.getRowID() > 0;
        assert result.getObjectID() > 0;
        deleteResult(c, result.getRowID(), result.getObjectID());
    }

    /**
     * Delete a ViabilityResult row by rowid.
     */
    public static void deleteResult(Container c, int resultRowID, int resultObjectID)
    {
        deleteSpecimens(resultRowID);
        Table.delete(ViabilitySchema.getTableInfoResults(), resultRowID);

        OntologyObject obj = OntologyManager.getOntologyObject(resultObjectID);
        OntologyManager.deleteOntologyObject(obj.getObjectURI(), c, true);
    }

    private static void deleteSpecimens(int resultRowId)
    {
        Table.delete(ViabilitySchema.getTableInfoResultSpecimens(), new SimpleFilter(FieldKey.fromParts("ResultID"), resultRowId));
    }

    private static void deleteSpecimens(Collection<Integer> resultRowIds)
    {
        Table.delete(ViabilitySchema.getTableInfoResultSpecimens(), new SimpleFilter(FieldKey.fromParts("ResultID"), resultRowIds, CompareType.IN));
    }

    /** Delete the properties for objectID, but not the object itself. */
    private static void deleteProperties(Container c, int objectID)
    {
        OntologyManager.deleteProperties(c, objectID);
    }

    /**
     * Get the ExpData for the viability result row or null.
     * @param resultRowId The row id of the result.
     * @return The ExpData of the viabilty result row or null.
     */
    /*package*/ static ExpData getResultExpData(int resultRowId)
    {
        Integer dataId = new TableSelector(ViabilitySchema.getTableInfoResults(), Collections.singleton("DataID"), new SimpleFilter(FieldKey.fromParts("RowID"), resultRowId), null).getObject(Integer.class);
        if (dataId != null)
            return ExperimentService.get().getExpData(dataId.intValue());
        return null;
    }

    /** Delete all viability results that reference the ExpData. */
    public static void deleteAll(ExpData data, Container c)
    {
        deleteAll(Arrays.asList(data), c);
    }

    /** Delete all viability results that reference the ExpData. */
    public static void deleteAll(List<ExpData> datas, Container c)
    {
        DbScope scope = ViabilitySchema.getSchema().getScope();
        try (DbScope.Transaction tx = scope.ensureTransaction())
        {
            List<Integer> dataIDs = new ArrayList<>(datas.size());
            for (ExpData data : datas)
                dataIDs.add(data.getRowId());

            TableSelector ts = new TableSelector(ViabilitySchema.getTableInfoResults(),
                    new HashSet<String>(Arrays.asList("RowID", "ObjectID")),
                    new SimpleFilter(FieldKey.fromParts("DataID"), dataIDs, CompareType.IN), null);

            ts.forEachMapBatch((rows) -> {

                List<Integer> resultIDs = new ArrayList<>(rows.size());
                int[] objectIDs = new int[rows.size()];

                int i = 0;
                for (Map<String, Object> row : rows)
                {
                    resultIDs.add(((Integer) row.get("RowID")).intValue());
                    objectIDs[i] = ((Integer) row.get("ObjectID")).intValue();
                    i++;
                }

                deleteSpecimens(resultIDs);

                Table.delete(ViabilitySchema.getTableInfoResults(), new SimpleFilter(FieldKey.fromParts("RowId"), resultIDs, CompareType.IN));

                OntologyManager.deleteOntologyObjects(c, true, objectIDs);

            }, 1000);

            tx.commit();
        }
    }

    public static class TestCase extends Assert
    {
        private static final double DELTA = 1E-8;

        private ExpProtocol _protocol;
        private ExpRun _run;
        private ExpData _data;
        private PropertyDescriptor _propertyA;
        private PropertyDescriptor _propertyB;

        @Before
        public void setUp() throws Exception
        {
            JunitUtil.deleteTestContainer();
            cleanup();

            Container c = JunitUtil.getTestContainer();
            TestContext context = TestContext.get();
            User user = context.getUser();

            _protocol = ExperimentService.get().createExpProtocol(c, ExpProtocol.ApplicationType.ExperimentRun, "viability-exp-protocol");
            _protocol.save(user);

            _run = ExperimentService.get().createExperimentRun(c, "viability-exp-run");
            _run.setProtocol(_protocol);
            _run.save(user);

            _data = ExperimentService.get().createData(c, ViabilityTsvDataHandler.DATA_TYPE, "viability-exp-data");
            _data.setRun(_run);
            _data.save(user);

            _propertyA = new PropertyDescriptor("viability-juni-propertyA", PropertyType.STRING, "propertyA", c);
            OntologyManager.insertPropertyDescriptor(_propertyA);

            _propertyB = new PropertyDescriptor("viability-juni-propertyB", PropertyType.BOOLEAN, "propertyB", c);
            OntologyManager.insertPropertyDescriptor(_propertyB);
        }

        @After
        public void tearDown() throws Exception
        {
            cleanup();
        }

        private void cleanup() throws Exception
        {
            Container c = JunitUtil.getTestContainer();
            TestContext context = TestContext.get();

            Map<String, Object>[] rows = new TableSelector(
                    ViabilitySchema.getTableInfoResults(),
                    new HashSet<String>(Arrays.asList("RowID", "ObjectID")),
                    new SimpleFilter(FieldKey.fromParts("PoolID"), "xxx-", CompareType.STARTS_WITH), null).getMapArray();

            for (Map<String, Object> row : rows)
            {
                int resultId = (Integer)row.get("RowID");
                int objectId = (Integer)row.get("ObjectID");
                ViabilityManager.deleteResult(c, resultId, objectId);
            }

            ExperimentService.get().deleteAllExpObjInContainer(c, context.getUser());
            OntologyManager.deleteAllObjects(c, context.getUser());
        }

        @Test
        public void testViability() throws Exception
        {
            Container c = JunitUtil.getTestContainer();
            TestContext context = TestContext.get();
            User user = context.getUser();
            assertTrue("login before running this test", null != user);
            assertFalse("login before running this test", user.isGuest());

            int resultId;
            int objectId;
            String objectURI;

            // INSERT
            {
                ViabilityResult result = new ViabilityResult();
                result.setRunID(_run.getRowId());
                result.setDataID(_data.getRowId());
                result.setContainer(c.getId());
                result.setProtocolID(_protocol.getRowId());
                result.setPoolID("xxx-12345-67890");
                result.setTotalCells(10000);
                result.setViableCells(9000);
                assertEquals(0.9, result.getViability(), DELTA);
                result.setSpecimenIDList(Arrays.asList("222", "111", "333"));

                Map<PropertyDescriptor, Object> properties = new HashMap<>();
                properties.put(_propertyA, "hello property");
                properties.put(_propertyB, true);
                result.setProperties(properties);

                ViabilityManager.saveResult(user, c, result, 0);
                resultId = result.getRowID();

                ViabilityResult[] results = ViabilityManager.getResults(_data, c);
                assertEquals(1, results.length);
                assertEquals(resultId, results[0].getRowID());
            }

            // verify
            {
                ExpData d = ViabilityManager.getResultExpData(resultId);
                assertEquals(_data.getRowId(), d.getRowId());
                assertEquals(_data.getName(), d.getName());
                assertEquals(_run.getRowId(), d.getRunId().intValue());

                ViabilityResult[] results = ViabilityManager.getResults(_run, c);
                assertEquals(1, results.length);
                assertEquals(resultId, results[0].getRowID());

                ViabilityResult result = ViabilityManager.getResult(c, resultId);
                assertEquals(resultId, result.getRowID());
                assertEquals(_data.getRowId(), result.getDataID());
                assertEquals(_run.getRowId(), result.getRunID());
                assertEquals("xxx-12345-67890", result.getPoolID());
                assertEquals(10000, result.getTotalCells());
                assertEquals(9000, result.getViableCells());
                assertEquals(0.9, result.getViability(), DELTA);

                objectId = result.getObjectID();
                assertTrue(objectId > 0);
                OntologyObject obj = OntologyManager.getOntologyObject(objectId);
                objectURI = obj.getObjectURI();

                List<String> specimenIDs = result.getSpecimenIDList();
                assertEquals("111", specimenIDs.get(0));
                assertEquals("222", specimenIDs.get(1));
                assertEquals("333", specimenIDs.get(2));

                Map<PropertyDescriptor, Object> properties = result.getProperties();
                assertEquals(2, properties.size());
                assertEquals("hello property", properties.get(_propertyA));
                assertEquals(Boolean.TRUE, properties.get(_propertyB));
            }

            // UPDATE
            {
                ViabilityResult result = ViabilityManager.getResult(c, resultId);
                List<String> specimens = result.getSpecimenIDList();
                specimens = new ArrayList<>(specimens);
                specimens.remove("222");
                specimens.add("444");
                specimens.add("000");
                result.setSpecimenIDList(specimens);
                result.getProperties().put(_propertyA, "goodbye property");
                result.getProperties().remove(_propertyB);
                ViabilityManager.saveResult(user, c, result, 0);
            }

            // verify
            {
                ViabilityResult result = ViabilityManager.getResult(c, resultId);
                List<String> specimenIDs = result.getSpecimenIDList();
                assertEquals("000", specimenIDs.get(0));
                assertEquals("111", specimenIDs.get(1));
                assertEquals("333", specimenIDs.get(2));
                assertEquals("444", specimenIDs.get(3));

                Map<PropertyDescriptor, Object> properties = result.getProperties();
                assertEquals(1, properties.size());
                assertEquals("goodbye property", properties.get(_propertyA));
            }

            // DELETE
            {
                ViabilityManager.deleteResult(c, resultId, objectId);
            }

            // verify
            {
                Map<String, Object> properties = OntologyManager.getProperties(c, objectURI);
                assertTrue(properties.size() == 0);

                String[] specimens = ViabilityManager.getSpecimens(resultId);
                assertTrue(specimens.length == 0);

                ViabilityResult result = ViabilityManager.getResult(c, resultId);
                assertNull(result);

                ViabilityResult[] results = ViabilityManager.getResults(_run, c);
                assertEquals(0, results.length);
            }
        }
    }
}
