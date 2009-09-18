/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.labkey.api.study.assay.AssayService;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.security.User;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.JunitUtil;

import java.util.*;
import java.sql.SQLException;

import junit.framework.TestSuite;
import junit.framework.Test;

public class ViabilityManager
{
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

    /**
     * Get a viability result row.
     * @param resultRowId The row id of the result to get.
     * @return The ViabilityArrayResult for the row.
     */
    public static ViabilityResult getResult(int resultRowId) throws SQLException
    {
        ViabilityResult result = Table.selectObject(ViabilitySchema.getTableInfoResults(), resultRowId, ViabilityResult.class);
        if (result == null)
            return null;
        String[] specimens = getSpecimens(resultRowId);
        result.setSpecimenIDs(Arrays.asList(specimens));
        return result;
    }

    private static String[] getSpecimens(int resultRowId) throws SQLException
    {
        String[] specimens = Table.executeArray(
                ViabilitySchema.getTableInfoResultSpecimens(),
                "SpecimenID",
                new SimpleFilter("ResultID", resultRowId),
                new Sort("Index"),
                String.class);
        return specimens;
    }

    /**
     * Insert or update a ViabilityResult row.
     * @param user
     * @param result
     * @throws SQLException
     */
    public static void saveResult(User user, ViabilityResult result) throws SQLException
    {
        assert result.getDataID() > 0;
        assert result.getObjectID() > 0;
        assert result.getPoolID() != null;
        assert result.getTotalCells() > 0;
        assert result.getViableCells() > 0;
        assert result.getSpecimenIDs() != null && result.getSpecimenIDs().size() > 0;

        if (result.getRowID() == 0)
        {
            Table.insert(user, ViabilitySchema.getTableInfoResults(), result);
        }
        else
        {
            deleteSpecimens(result.getRowID());
            Table.update(user, ViabilitySchema.getTableInfoResults(), result, result.getRowID());
        }

        insertSpecimens(user, result);
//        insertProperties();
    }

    private static void insertSpecimens(User user, ViabilityResult result) throws SQLException
    {
        List<String> specimens = result.getSpecimenIDs();
        for (int index = 0; index < specimens.size(); index++)
        {
            Map<String, Object> resultSpecimen = new HashMap<String, Object>();
            resultSpecimen.put("ResultID", result.getRowID());
            resultSpecimen.put("SpecimenID", specimens.get(index));
            resultSpecimen.put("Index", index);

            Table.insert(user, ViabilitySchema.getTableInfoResultSpecimens(), resultSpecimen);
        }
    }

    /**
     * Delete a ViabilityResult row.
     * @param result
     * @throws SQLException
     */
    public static void deleteResult(ViabilityResult result) throws SQLException
    {
        assert result.getRowID() > 0;
        deleteResult(result.getRowID());
    }

    /**
     * Delete a ViabilityResult row by rowid.
     * @param resultRowId
     * @throws SQLException
     */
    public static void deleteResult(int resultRowId) throws SQLException
    {
        deleteSpecimens(resultRowId);
        Table.delete(ViabilitySchema.getTableInfoResults(), resultRowId);
    }

    private static void deleteSpecimens(int resultRowId) throws SQLException
    {
        Table.delete(ViabilitySchema.getTableInfoResultSpecimens(), new SimpleFilter("ResultID", resultRowId));
    }

    /**
     * Get the ExpData for the viability result row or null.
     * @param resultRowId The row id of the result.
     * @return The ExpData of the viabilty result row or null.
     */
    /*package*/ static ExpData getResultExpData(int resultRowId)
    {
        // executeSingleton
        Integer dataId = Table.executeSingleton(ViabilitySchema.getTableInfoResults(), "DataID", new SimpleFilter("RowID", resultRowId), null, Integer.class);
        if (dataId != null)
            return ExperimentService.get().getExpData(dataId.intValue());
        return null;
    }

    public static class TestCase extends junit.framework.TestCase
    {
        public TestCase() { super(); }
        public TestCase(String name) { super(name); }
        public static Test suite() { return new TestSuite(TestCase.class); }

        @Override
        protected void setUp() throws Exception
        {
            cleanup();
        }

        @Override
        protected void tearDown() throws Exception
        {
            cleanup();
        }

        private void cleanup() throws Exception
        {
            Container c = JunitUtil.getTestContainer();
            TestContext context = TestContext.get();

            //noinspection unchecked
            Integer[] resultIDs = Table.executeArray(
                    ViabilitySchema.getTableInfoResults(),
                    ViabilitySchema.getTableInfoResults().getColumn("RowID"),
                    new SimpleFilter("PoolID", "xxx-", CompareType.STARTS_WITH), null,
                    Integer.class);
            if (resultIDs.length > 0)
            {
                SimpleFilter filter = new SimpleFilter();
                filter.addInClause("ResultID", Arrays.asList(resultIDs));
                Table.delete(ViabilitySchema.getTableInfoResultSpecimens(), filter);
                Table.delete(ViabilitySchema.getTableInfoResults(), resultIDs);
            }

            ExperimentService.get().deleteAllExpObjInContainer(c, context.getUser());
            OntologyManager.deleteAllObjects(c);
        }

        public void testViability()
            throws Exception
        {
            Container c = JunitUtil.getTestContainer();
            TestContext context = TestContext.get();
            User user = context.getUser();
            assertTrue("login before running this test", null != user);
            assertFalse("login before running this test", user.isGuest());

            ExpData data = ExperimentService.get().createData(c, ViabilityAssayDataHandler.DATA_TYPE, "viability-exp-data");
            data.save(user);

            int objectId = OntologyManager.ensureObject(c, "viability-junit-object");
            int resultId;

            // INSERT
            {
                ViabilityResult result = new ViabilityResult();
                result.setDataID(data.getRowId());
                result.setObjectID(objectId);
                result.setPoolID("xxx-12345-67890");
                result.setTotalCells(10000);
                result.setViableCells(9000);
                assertEquals(0.9, result.getViability());
                result.setSpecimenIDs(Arrays.asList("111", "222", "333"));

                ViabilityManager.saveResult(user, result);
                resultId = result.getRowID();
            }

            // verify
            {
                ExpData d = ViabilityManager.getResultExpData(resultId);
                assertEquals(data.getRowId(), d.getRowId());
                assertEquals(data.getName(), d.getName());

                ViabilityResult result = ViabilityManager.getResult(resultId);
                assertEquals(resultId, result.getRowID());
                assertEquals(data.getRowId(), result.getDataID());
                assertEquals(objectId, result.getObjectID());
                assertEquals("xxx-12345-67890", result.getPoolID());
                assertEquals(10000, result.getTotalCells());
                assertEquals(9000, result.getViableCells());
                assertEquals(0.9, result.getViability());
                List<String> specimenIDs = result.getSpecimenIDs();
                assertEquals("111", specimenIDs.get(0));
                assertEquals("222", specimenIDs.get(1));
                assertEquals("333", specimenIDs.get(2));
            }

            // UPDATE
            {
                ViabilityResult result = ViabilityManager.getResult(resultId);
                List<String> specimens = result.getSpecimenIDs();
                specimens = new ArrayList<String>(specimens);
                specimens.remove("222");
                specimens.add("444");
                specimens.add("555");
                result.setSpecimenIDs(specimens);
                ViabilityManager.saveResult(user, result);
            }

            // verify
            {
                ViabilityResult result = ViabilityManager.getResult(resultId);
                List<String> specimenIDs = result.getSpecimenIDs();
                assertEquals("111", specimenIDs.get(0));
                assertEquals("333", specimenIDs.get(1));
                assertEquals("444", specimenIDs.get(2));
                assertEquals("555", specimenIDs.get(3));
            }

            // DELETE
            {
                ViabilityManager.deleteResult(resultId);
            }

            // verify
            {
                String[] specimens = ViabilityManager.getSpecimens(resultId);
                assertTrue(specimens.length == 0);
                ViabilityResult result = ViabilityManager.getResult(resultId);
                assertNull(result);
            }
        }


    }
}