/*
 * Copyright (c) 2009 LabKey Corporation
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
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.security.User;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.query.ValidationException;

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

    public static ViabilityResult[] getResults(ExpData data, Container container) throws SQLException
    {
        ViabilityResult[] result = Table.select(ViabilitySchema.getTableInfoResults(),
                Table.ALL_COLUMNS,
                new SimpleFilter(),
                null,
                ViabilityResult.class);
        return result;
    }

    /**
     * Get a viability result row.
     * @param resultRowId The row id of the result to get.
     * @return The ViabilityArrayResult for the row.
     */
    public static ViabilityResult getResult(Container c, int resultRowId) throws SQLException
    {
        ViabilityResult result = Table.selectObject(ViabilitySchema.getTableInfoResults(), resultRowId, ViabilityResult.class);
        if (result == null)
            return null;
        // lazily fetch specimens and properties
//        String[] specimens = getSpecimens(resultRowId);
//        result.setSpecimenIDs(Arrays.asList(specimens));
//        result.setProperties(getProperties(c, result.getObjectID()));
        return result;
    }

    static String[] getSpecimens(int resultRowId) throws SQLException
    {
        String[] specimens = Table.executeArray(
                ViabilitySchema.getTableInfoResultSpecimens(),
                "SpecimenID",
                new SimpleFilter("ResultID", resultRowId),
                new Sort("Index"),
                String.class);
        return specimens;
    }

    static Map<PropertyDescriptor, Object> getProperties(int objectID) throws SQLException
    {
        assert objectID > 0;
        OntologyObject obj = OntologyManager.getOntologyObject(objectID);
        assert obj != null;

        Map<String, Object> oprops = OntologyManager.getProperties(obj.getContainer(), obj.getObjectURI());
        Map<PropertyDescriptor, Object> properties = new HashMap<PropertyDescriptor, Object>();
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
    public static void saveResult(User user, Container c, ViabilityResult result) throws SQLException, ValidationException
    {
        assert user != null && c != null : "user or container is null";
        assert result.getDataID() > 0 : "DataID is not set";
        assert result.getPoolID() != null : "PoolID is not set";
        assert result.getTotalCells() > 0 : "TotalCells is not set";
        assert result.getViableCells() > 0 : "ViableCells is not set";
//        assert result.getSpecimenIDs() != null && result.getSpecimenIDs().size() > 0;

        if (result.getRowID() == 0)
        {
            String lsid = new Lsid(ViabilityAssayProvider.RESULT_LSID_PREFIX, result.getDataID() + "-" + result.getPoolID()).toString();
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

    private static void insertSpecimens(User user, ViabilityResult result) throws SQLException
    {
//        assert result.getSpecimenIDs() != null && result.getSpecimenIDs().size() > 0;
        List<String> specimens = result.getSpecimenIDs();
        for (int index = 0; index < specimens.size(); index++)
        {
            String specimenID = specimens.get(index);
            if (specimenID == null || specimenID.length() == 0)
                continue;
            
            Map<String, Object> resultSpecimen = new HashMap<String, Object>();
            resultSpecimen.put("ResultID", result.getRowID());
            resultSpecimen.put("SpecimenID", specimens.get(index));
            resultSpecimen.put("Index", index);

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

        List<ObjectProperty> oprops = new ArrayList<ObjectProperty>(properties.size());
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
    public static void deleteResult(Container c, int resultRowID, int resultObjectID) throws SQLException
    {
        deleteSpecimens(resultRowID);
        Table.delete(ViabilitySchema.getTableInfoResults(), resultRowID);

        OntologyObject obj = OntologyManager.getOntologyObject(resultObjectID);
        OntologyManager.deleteOntologyObject(obj.getObjectURI(), c, true);
    }

    private static void deleteSpecimens(int resultRowId) throws SQLException
    {
        Table.delete(ViabilitySchema.getTableInfoResultSpecimens(), new SimpleFilter("ResultID", resultRowId));
    }

    /** Delete the properties for objectID, but not the object itself. */
    private static void deleteProperties(Container c, int objectID) throws SQLException
    {
        OntologyManager.deleteProperties(objectID, c);
    }

    /**
     * Get the ExpData for the viability result row or null.
     * @param resultRowId The row id of the result.
     * @return The ExpData of the viabilty result row or null.
     */
    /*package*/ static ExpData getResultExpData(int resultRowId)
    {
        Integer dataId = Table.executeSingleton(ViabilitySchema.getTableInfoResults(), "DataID", new SimpleFilter("RowID", resultRowId), null, Integer.class);
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
    // XXX: optimize
    public static void deleteAll(List<ExpData> datas, Container c)
    {
        try
        {
            List<Integer> dataIDs = new ArrayList<Integer>(datas.size());
            for (ExpData data : datas)
                dataIDs.add(data.getRowId());

            Map<String, Object>[] rows =
                    Table.selectMaps(ViabilitySchema.getTableInfoResults(),
                    new HashSet<String>(Arrays.asList("RowID", "ObjectID")),
                    new SimpleFilter("DataID", dataIDs, CompareType.IN), null);

            Integer[] objectIDs = new Integer[rows.length];

            for (int i = 0; i < rows.length; i++)
            {
                Map<String, Object> row = rows[i];
                Integer resultID = (Integer)row.get("RowID");
                objectIDs[i] = (Integer)row.get("ObjectID");

                deleteSpecimens(resultID.intValue());
                Table.delete(ViabilitySchema.getTableInfoResults(), resultID);
            }

            OntologyManager.deleteOntologyObjects(objectIDs, c, true);
        }
        catch (SQLException ex)
        {
            throw new RuntimeSQLException(ex);
        }
    }

    public static class TestCase extends junit.framework.TestCase
    {
        private ExpData _data;
        private PropertyDescriptor _propertyA;
        private PropertyDescriptor _propertyB;

        public TestCase() { super(); }
        public TestCase(String name) { super(name); }
        public static Test suite() { return new TestSuite(TestCase.class); }

        @Override
        protected void setUp() throws Exception
        {
            cleanup();

            Container c = JunitUtil.getTestContainer();
            TestContext context = TestContext.get();
            User user = context.getUser();

            _data = ExperimentService.get().createData(c, ViabilityTsvDataHandler.DATA_TYPE, "viability-exp-data");
            _data.save(user);

            _propertyA = new PropertyDescriptor("viability-juni-propertyA", PropertyType.STRING.getTypeUri(), "propertyA", c);
            OntologyManager.insertPropertyDescriptor(_propertyA);

            _propertyB = new PropertyDescriptor("viability-juni-propertyB", PropertyType.BOOLEAN.getTypeUri(), "propertyB", c);
            OntologyManager.insertPropertyDescriptor(_propertyB);
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

            Map<String, Object>[] rows = Table.selectMaps(
                    ViabilitySchema.getTableInfoResults(),
                    new HashSet<String>(Arrays.asList("RowID", "ObjectID")),
                    new SimpleFilter("PoolID", "xxx-", CompareType.STARTS_WITH), null);
            for (Map<String, Object> row : rows)
            {
                int resultId = (Integer)row.get("RowID");
                int objectId = (Integer)row.get("ObjectID");
                ViabilityManager.deleteResult(c, resultId, objectId);
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

            int resultId;
            int objectId;
            String objectURI;

            // INSERT
            {
                ViabilityResult result = new ViabilityResult();
                result.setDataID(_data.getRowId());
                result.setPoolID("xxx-12345-67890");
                result.setTotalCells(10000);
                result.setViableCells(9000);
                assertEquals(0.9, result.getViability());
                result.setSpecimenIDs(Arrays.asList("111", "222", "333"));

                Map<PropertyDescriptor, Object> properties = new HashMap<PropertyDescriptor, Object>();
                properties.put(_propertyA, "hello property");
                properties.put(_propertyB, true);
                result.setProperties(properties);

                ViabilityManager.saveResult(user, c, result);
                resultId = result.getRowID();
            }

            // verify
            {
                ExpData d = ViabilityManager.getResultExpData(resultId);
                assertEquals(_data.getRowId(), d.getRowId());
                assertEquals(_data.getName(), d.getName());

                ViabilityResult result = ViabilityManager.getResult(c, resultId);
                assertEquals(resultId, result.getRowID());
                assertEquals(_data.getRowId(), result.getDataID());
                assertEquals("xxx-12345-67890", result.getPoolID());
                assertEquals(10000, result.getTotalCells());
                assertEquals(9000, result.getViableCells());
                assertEquals(0.9, result.getViability());

                objectId = result.getObjectID();
                assertTrue(objectId > 0);
                OntologyObject obj = OntologyManager.getOntologyObject(objectId);
                objectURI = obj.getObjectURI();

                List<String> specimenIDs = result.getSpecimenIDs();
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
                List<String> specimens = result.getSpecimenIDs();
                specimens = new ArrayList<String>(specimens);
                specimens.remove("222");
                specimens.add("444");
                specimens.add("555");
                result.setSpecimenIDs(specimens);
                result.getProperties().put(_propertyA, "goodbye property");
                result.getProperties().remove(_propertyB);
                ViabilityManager.saveResult(user, c, result);
            }

            // verify
            {
                ViabilityResult result = ViabilityManager.getResult(c, resultId);
                List<String> specimenIDs = result.getSpecimenIDs();
                assertEquals("111", specimenIDs.get(0));
                assertEquals("333", specimenIDs.get(1));
                assertEquals("444", specimenIDs.get(2));
                assertEquals("555", specimenIDs.get(3));

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
            }
        }


    }
}