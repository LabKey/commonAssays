/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

package org.labkey.flow.persist;

import org.apache.commons.collections15.iterators.ArrayIterator;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpObject;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.security.User;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.query.AttributeCache;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FlowManager
{
    private static final FlowManager instance = new FlowManager();
    private static final Logger _log = Logger.getLogger(FlowManager.class);
    private static final String SCHEMA_NAME = "flow";

    private final HashMap<String, Integer> _attridCacheMap = new HashMap<String, Integer>(1000);
    private final HashMap<Integer, String> _attrNameCacheMap = new HashMap<Integer, String>(1000);

    static public FlowManager get()
    {
        return instance;
    }

    public String getSchemaName()
    {
        return SCHEMA_NAME;
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }

    public TableInfo getTinfoStatisticAttr()
    {
        return getSchema().getTable("StatisticAttr");
    }

    public TableInfo getTinfoGraphAttr()
    {
        return getSchema().getTable("GraphAttr");
    }

    public TableInfo getTinfoKeywordAttr()
    {
        return getSchema().getTable("KeywordAttr");
    }

    public TableInfo getTinfoAttribute()
    {
        return getSchema().getTable("Attribute");
    }

    public TableInfo getTinfoObject()
    {
        return getSchema().getTable("Object");
    }

    public TableInfo getTinfoKeyword()
    {
        return getSchema().getTable("Keyword");
    }

    public TableInfo getTinfoStatistic()
    {
        return getSchema().getTable("Statistic");
    }

    public TableInfo getTinfoGraph()
    {
        return getSchema().getTable("Graph");
    }

    public TableInfo getTinfoScript()
    {
        return getSchema().getTable("Script");
    }

    public int getAttributeId(String attr)
    {
        synchronized (_attridCacheMap)
        {
            quickFillCache();
            
            Integer ret = _attridCacheMap.get(attr);
            if (ret != null)
                return ret.intValue();

            try
            {
                Integer i = Table.executeSingleton(getSchema(), "SELECT RowId FROM flow.Attribute WHERE Name = ?", new Object[] {attr }, Integer.class);
                // we're not caching misses because this is an unlimited cachemap
                if (i==null)
                    return 0;
                _attridCacheMap.put(attr, i);
                _attrNameCacheMap.put(i, attr);
                return i.intValue();
            }
            catch (SQLException e)
            {
                throw UnexpectedException.wrap(e);
            }
        }
    }

    public Map.Entry<Integer, String>[] getAttributeNames(Integer[] ids)
    {
        Map.Entry<Integer, String>[] ret = new Map.Entry[ids.length];
        boolean hasNulls = false;
        for (int i = 0; i < ids.length; i ++)
        {
            Integer id = ids[i];
            if (id != null)
            {
                ret[i] = getAttributeName(ids[i]);
            }
            if (ret[i] == null)
            {
                _log.error("Request for attribute " + id + " returned null.", new Exception());
                hasNulls = true;
            }
        }
        if (!hasNulls)
            return ret;
        ArrayList<Map.Entry<Integer, String>> lstRet = new ArrayList<Map.Entry<Integer, String>>();
        for (Map.Entry<Integer, String> entry : ret)
        {
            if (entry != null)
            {
                lstRet.add(entry);
            }
        }
        return lstRet.toArray(new Map.Entry[lstRet.size()]);
    }


    public Map.Entry<Integer, String> getAttributeName(int id)
    {
        synchronized(_attridCacheMap)
        {
            quickFillCache();

            String name = _attrNameCacheMap.get(id);
            if (name == null)
            {
                try
                {
                    name = Table.executeSingleton(getSchema(), "SELECT Name FROM flow.Attribute WHERE RowId = ?", new Object[] { id }, String.class);
                    if (name == null)
                    {
                        return null;
                    }
                    _attrNameCacheMap.put(id, name);
                    _attridCacheMap.put(name, id);
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }

            return new FlowEntry(id, name);
        }
    }

    private static class FlowEntry implements Map.Entry<Integer, String>
    {
        private final Integer _key;
        private final String _value;

        private FlowEntry(Integer key, String value)
        {
            _key = key;
            _value = value;
        }

        @Override
        public Integer getKey()
        {
            return _key;
        }

        @Override
        public String getValue()
        {
            return _value;
        }

        @Override
        public String setValue(String value)
        {
            throw new IllegalStateException("Can't set value on a FlowEntry");
        }
    }

    private void quickFillCache()
    {
        if (_attrNameCacheMap.isEmpty())
        {
            ResultSet rs = null;
            try
            {
                rs = Table.executeQuery(getSchema(), "SELECT RowId, Name FROM flow.Attribute", null, 0, false);
                while (rs.next())
                {
                    int rowid = rs.getInt(1);
                    String name = rs.getString(2);
                    _attrNameCacheMap.put(rowid, name);
                    _attridCacheMap.put(name, rowid);
                }
            }
            catch (SQLException e)
            {
                _log.error("Unexpected error", e);
                // fall through;
            }
            finally
            {
                ResultSetUtil.close(rs);
            }
        }
    }


    private int ensureAttributeId(String attr) throws SQLException
    {
        DbSchema schema = getSchema();
        if (schema.getScope().isTransactionActive())
        {
            throw new IllegalStateException("ensureAttributeId cannot be called within a transaction");
        }
        synchronized(_attridCacheMap)
        {
            int ret = getAttributeId(attr);
            if (ret != 0)
                return ret;
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("Name", attr);
            Table.insert(null, getTinfoAttribute(), map);
            _attridCacheMap.remove(attr);
            return getAttributeId(attr);
        }
    }


    public int ensureStatisticId(Container c, String attr) throws SQLException
    {
        int id = ensureAttributeId(attr);
        TableInfo statkey = getTinfoStatisticAttr();
        Table.execute(getSchema(), "INSERT INTO " + statkey + " (container, id) " +
                "SELECT ? AS container, ? as id " +
                "WHERE NOT EXISTS (SELECT id FROM " + statkey + " WHERE container=? and id=?)",
                new Object[] {c.getId(), id, c.getId(), id});
        return id;
    }


    public int ensureKeywordId(Container c, String attr) throws SQLException
    {
        int id = ensureAttributeId(attr);
        TableInfo statkey = getTinfoKeywordAttr();
        Table.execute(getSchema(), "INSERT INTO " + statkey + " (container, id) " +
                "SELECT ? AS container, ? as id " +
                "WHERE NOT EXISTS (SELECT id FROM " + statkey + " WHERE container=? and id=?)",
                new Object[] {c.getId(), id, c.getId(), id});
        return id;
    }


    public int ensureGraphId(Container c, String attr) throws SQLException
    {
        int id = ensureAttributeId(attr);
        TableInfo statkey = getTinfoGraphAttr();
        Table.execute(getSchema(), "INSERT INTO " + statkey + " (container, id) " +
                "SELECT ? AS container, ? as id " +
                "WHERE NOT EXISTS (SELECT id FROM " + statkey + " WHERE container=? and id=?)",
                new Object[] {c.getId(), id, c.getId(), id});
        return id;
    }


    public List<AttrObject> getAttrObjects(Collection<ExpData> datas)
    {
        try
        {
            if (datas.isEmpty())
                return Collections.emptyList();
            SQLFragment sql = new SQLFragment ("SELECT * FROM " + getTinfoObject().toString() + " WHERE DataId IN (");
            String comma = "";
            for (ExpData data : datas)
            {
                sql.append(comma).append(data.getRowId());
                comma = ",";
            }
            sql.append(")");
            AttrObject[] array = Table.executeQuery(getSchema(), sql.getSQL(), sql.getParamsArray(), AttrObject.class);
            return Arrays.asList(array);
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public AttrObject getAttrObject(ExpData data)
    {
        try
        {
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition("DataId", data.getRowId());
            AttrObject[] array = Table.select(getTinfoObject(), Table.ALL_COLUMNS, filter, null, AttrObject.class);
            if (array.length == 0)
                return null;
            return array[0];
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public AttrObject getAttrObjectFromRowId(int rowid)
    {
        try
        {
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition("RowId", rowid);
            return Table.selectObject(getTinfoObject(), Table.ALL_COLUMNS, filter, null, AttrObject.class);
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }


    public AtomicLong flowObjectModificationCount = new AtomicLong();


    public void flowObjectModified()
    {
        flowObjectModificationCount.incrementAndGet();
    }

    
    public AttrObject createAttrObject(ExpData data, ObjectType type, URI uri) throws SQLException
    {
        if (FlowDataHandler.instance.getPriority(ExperimentService.get().getExpData(data.getRowId())) != Handler.Priority.HIGH)
        {
            // Need to make sure the right ExperimentDataHandler is associated with this data file, otherwise, you
            // won't be able to delete it because of the foreign key constraint from the flow.object table.
            throw new IllegalStateException("FlowDataHandler must be associated with data file");
        }
        AttrObject newObject = new AttrObject();
        newObject.setContainer(data.getContainer());
        newObject.setDataId(data.getRowId());
        newObject.setTypeId(type.getTypeId());
        if (uri != null)
        {
            newObject.setUri(uri.toString());
        }
        flowObjectModified();
        return Table.insert(null, getTinfoObject(), newObject);
    }


    int MAX_BATCH = 1000;

    private String join(Integer[] oids, int from, int to)
    {
        Iterator i = new ArrayIterator(oids, from, to);
        return StringUtils.join(i, ',');
    }

    private void deleteAttributes(Integer[] oids) throws SQLException
    {
        if (oids.length == 0)
            return;
        for (int from = 0, to; from < oids.length; from = to)
        {
            to = from + MAX_BATCH;
            if (to > oids.length)
                to = oids.length;

            String list = join(oids, from, to);
            Table.execute(getSchema(), "DELETE FROM flow.Statistic WHERE ObjectId IN (" + list + ")", null);
            Table.execute(getSchema(), "DELETE FROM flow.Keyword WHERE ObjectId IN (" + list + ")", null);
            Table.execute(getSchema(), "DELETE FROM flow.Graph WHERE ObjectId IN (" + list + ")", null);
            Table.execute(getSchema(), "DELETE FROM flow.Script WHERE ObjectId IN (" + list + ")", null);
        }
    }


    private void deleteAttributes(SQLFragment sqlObjectIds) throws SQLException
    {
        boolean transaction = false;
        DbScope scope = getSchema().getScope();
        try
        {
            if (!scope.isTransactionActive())
            {
                scope.beginTransaction();
                transaction = true;
            }
            Integer[] objids = Table.executeArray(getSchema(), sqlObjectIds, Integer.class);
            deleteAttributes(objids);
            /* This can be very slow with postgres! so we'll try selecting the objectids
            Table.execute(getSchema(), "DELETE FROM flow.Statistic WHERE ObjectId IN (" + sqlObjectIds.getSQL() + ")", sqlObjectIds.getParamsArray());
            Table.execute(getSchema(), "DELETE FROM flow.Keyword WHERE ObjectId IN (" + sqlObjectIds.getSQL() + ")", sqlObjectIds.getParamsArray());
            Table.execute(getSchema(), "DELETE FROM flow.Graph WHERE ObjectId IN (" + sqlObjectIds.getSQL() + ")", sqlObjectIds.getParamsArray());
            Table.execute(getSchema(), "DELETE FROM flow.Script WHERE ObjectId IN (" + sqlObjectIds.getSQL() + ")", sqlObjectIds.getParamsArray());
            */
            if (transaction)
            {
                scope.commitTransaction();
                transaction = false;
            }
        }
        finally
        {
            if (transaction)
            {
                scope.rollbackTransaction();
            }
        }
    }

    public void deleteAttributes(ExpData data) throws SQLException
    {
        AttrObject obj = getAttrObject(data);
        if (obj == null)
            return;
        deleteAttributes(new Integer[] {obj.getRowId()});
    }


    private void deleteObjectIds(Integer[] oids, Set<Container> containers) throws SQLException
    {
        DbScope scope = getSchema().getScope();
        boolean fTrans = false;
        try
        {
            if (!scope.isTransactionActive())
            {
                scope.beginTransaction();
                fTrans = true;
            }
            deleteAttributes(oids);
            SQLFragment sqlf = new SQLFragment("DELETE FROM flow.Object WHERE RowId IN (" );
            sqlf.append(StringUtils.join(oids,','));
            sqlf.append(")");
            Table.execute(getSchema(), sqlf);
            if (fTrans)
            {
                scope.commitTransaction();
                fTrans = false;
            }
        }
        finally
        {
            if (fTrans)
            {
                getSchema().getScope().rollbackTransaction();
            }
            for (Container container : containers)
            {
                AttributeCache.invalidateCache(container);
            }
            flowObjectModified();
        }
    }
    

    private void deleteObjectIds(SQLFragment sqlOIDs, Set<Container> containers) throws SQLException
    {
        DbScope scope = getSchema().getScope();
        boolean fTrans = false;
        try
        {
            if (!scope.isTransactionActive())
            {
                scope.beginTransaction();
                fTrans = true;
            }
            deleteAttributes(sqlOIDs);
            Table.execute(getSchema(), "DELETE FROM flow.Object WHERE RowId IN (" + sqlOIDs.getSQL() + ")", sqlOIDs.getParamsArray());
            if (fTrans)
            {
                scope.commitTransaction();
                fTrans = false;
            }
        }
        finally
        {
            if (fTrans)
            {
                getSchema().getScope().rollbackTransaction();
            }
            for (Container container : containers)
            {
                AttributeCache.invalidateCache(container);
            }
            flowObjectModified();
        }
    }

    public void deleteData(List<ExpData> datas) throws SQLException
    {
        if (datas.size() == 0)
            return;
        StringBuilder sqlGetOIDs = new StringBuilder("SELECT flow.Object.RowId FROM flow.Object WHERE flow.Object.DataId IN (");
        String comma = "";
        Set<Container> containers = new HashSet<Container>();
        for (ExpData data : datas)
        {
            sqlGetOIDs.append(comma);
            comma = ",";
            sqlGetOIDs.append(data.getRowId());
            containers.add(data.getContainer());
        }
        sqlGetOIDs.append(")");
        Integer[] objectIds = Table.executeArray(getSchema(), sqlGetOIDs.toString(), null, Integer.class);
        if (objectIds.length == 0)
            return;
        deleteObjectIds(objectIds, containers);
    }

    static private String sqlSelectKeyword = "SELECT flow.keyword.value FROM flow.object" +
                                            "\nINNER JOIN flow.keyword on flow.object.rowid = flow.keyword.objectid" +
                                            "\nINNER JOIN flow.attribute ON flow.attribute.rowid = flow.keyword.keywordid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.attribute.name = ?";
    public String getKeyword(ExpData data, String keyword) throws SQLException
    {
        return Table.executeSingleton(getSchema(), sqlSelectKeyword, new Object[] { data.getRowId(), keyword }, String.class);
    }

    static private String sqlDeleteKeyword = "DELETE FROM flow.keyword WHERE ObjectId = ? AND KeywordId = ?";
    static private String sqlInsertKeyword = "INSERT INTO flow.keyword (ObjectId, KeywordId, Value) VALUES (?, ?, ?)";
    public void setKeyword(User user, ExpData data, String keyword, String value) throws SQLException
    {
        String oldValue = getKeyword(data, keyword);
        if (ObjectUtils.equals(oldValue, value))
        {
            return;
        }
        AttrObject obj = getAttrObject(data);
        if (obj == null)
        {
            throw new IllegalArgumentException("Object not found.");
        }
        int keywordId = ensureAttributeId(keyword);
        DbSchema schema = getSchema();
        boolean fTrans = false;
        try
        {
            if (!schema.getScope().isTransactionActive())
            {
                schema.getScope().beginTransaction();
                fTrans = true;
            }
            Table.execute(schema, sqlDeleteKeyword, new Object[] { obj.getRowId(), keywordId });
            if (value != null)
            {
                Table.execute(schema, sqlInsertKeyword, new Object[] { obj.getRowId(), keywordId, value} );
            }
            if (fTrans)
            {
                schema.getScope().commitTransaction();
                fTrans = false;
            }
        }
        finally
        {
            if (fTrans)
            {
                schema.getScope().commitTransaction();
            }
            AttributeCache.invalidateCache(data.getContainer());
        }

    }

    static private String sqlSelectStat = "SELECT flow.statistic.value FROM flow.object" +
                                            "\nINNER JOIN flow.statistic on flow.object.rowid = flow.statistic.objectid" +
                                            "\nINNER JOIN flow.attribute ON flow.attribute.rowid = flow.statistic.statisticid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.attribute.name = ?";
    public Double getStatistic(ExpData data, StatisticSpec stat) throws SQLException
    {
        return Table.executeSingleton(getSchema(), sqlSelectStat, new Object[] { data.getRowId(), stat.toString() }, Double.class);
    }

    static private String sqlSelectGraph = "SELECT flow.graph.data FROM flow.object" +
                                            "\nINNER JOIN flow.graph on flow.object.rowid = flow.graph.objectid" +
                                            "\nINNER JOIN flow.attribute ON flow.attribute.rowid = flow.graph.graphid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.attribute.name = ?";
    public byte[] getGraphBytes(ExpData data, GraphSpec graph) throws SQLException
    {
        return Table.executeSingleton(getSchema(), sqlSelectGraph, new Object[] { data.getRowId(), graph.toString() }, byte[].class);
    }

    static private String sqlSelectScript = "SELECT flow.script.text from flow.object" +
                                            "\nINNER JOIN flow.script ON flow.object.rowid = flow.script.objectid" +
                                            "\nWHERE flow.object.dataid = ?";
    public String getScript(ExpData data) throws SQLException
    {
        return Table.executeSingleton(getSchema(), sqlSelectScript, new Object[] { data.getRowId() }, String.class);
    }

    public void setScript(User user, ExpData data, String scriptText) throws SQLException
    {
        AttrObject obj = getAttrObject(data);
        if (obj == null)
        {
            obj = createAttrObject(data, ObjectType.script, null);
        }
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("ObjectId", obj.getRowId());
        Script script = Table.selectObject(getTinfoScript(), Table.ALL_COLUMNS, filter, null, Script.class);
        if (script == null)
        {
            script = new Script();
            script.setObjectId(obj.getRowId());
            script.setText(scriptText);
            script = Table.insert(user, getTinfoScript(), script);
        }
        else
        {
            script.setText(scriptText);
            script = Table.update(user, getTinfoScript(), script, script.getRowId());
        }
    }

    public int getObjectCount(Container container, ObjectType type)
    {
        try
        {
            String sqlFCSFileCount = "SELECT COUNT(flow.object.rowid) FROM flow.object\n" +
                    "WHERE flow.object.container = ? AND flow.object.typeid = ?";
            return Table.executeSingleton(getSchema(), sqlFCSFileCount, new Object[] { container.getId(), type.getTypeId() }, Integer.class);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }

    // CONSIDER: move to experiment module
    public int getFlaggedCount(Container container)
    {
        try
        {
            ExpObject o;
            String sql = "SELECT COUNT(OP.objectid) FROM exp.object OB, exp.objectproperty OP, exp.propertydescriptor PD\n" +
                    "WHERE OB.container = ? AND\n" +
                    "OB.objectid = OP.objectid AND\n" +
                    "OP.propertyid = PD.propertyid AND\n" +
                    "PD.propertyuri = '" + ExperimentProperty.COMMENT.getPropertyDescriptor().getPropertyURI() + "'";
            return Table.executeSingleton(getSchema(), sql, new Object[] { container.getId() }, Integer.class);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }

    // counts FCSFiles in Keyword runs
    public int getFCSFileCount(User user, Container container)
    {
        FlowSchema schema = new FlowSchema(user, container);


        try
        {
            // count(fcsfile)
            TableInfo table = schema.getTable(FlowTableType.FCSFiles);
            List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.Type.COUNT));
            List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));
            SimpleFilter filter = null;

/*        // sum of run.count where protocol = 'Keyword'
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition("ProtocolStep", "Keywords", CompareType.EQUAL);
            TableInfo table = schema.getTable(FlowTableType.Runs);
            List<Aggregate> aggregates = Collections.singletonList(new Aggregate("FCSFileCount", Aggregate.Type.SUM));
            List<ColumnInfo> columns = Collections.singletonList(table.getColumn("FCSFileCount"));
*/
            Map<String, Aggregate.Result> agg = Table.selectAggregatesForDisplay(table, aggregates, columns, filter, false);
            Aggregate.Result result = agg.get(aggregates.get(0).getColumnName());
            if (result != null)
                return ((Long)result.getValue()).intValue();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return 0;
    }

    // count FCSFiles with or without samples
    public int getFCSFileSamplesCount(User user, Container container, boolean hasSamples)
    {
        FlowSchema schema = new FlowSchema(user, container);

        try
        {
            TableInfo table = schema.getTable(FlowTableType.FCSFiles);
            List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.Type.COUNT));
            List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));
            SimpleFilter filter = new SimpleFilter("Sample/Name", null, hasSamples ? CompareType.NONBLANK : CompareType.ISBLANK);

            Map<String, Aggregate.Result> agg = Table.selectAggregatesForDisplay(table, aggregates, columns, filter, false);
            Aggregate.Result result = agg.get(aggregates.get(0).getColumnName());
            if (result != null)
                return ((Long)result.getValue()).intValue();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return 0;
    }

    // counts Keyword runs
    public int getFCSFileOnlyRunsCount(User user, Container container)
    {
        FlowSchema schema = new FlowSchema(user, container);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("FCSFileCount", 0, CompareType.NEQ);
        filter.addCondition("ProtocolStep", "Keywords", CompareType.EQUAL);
        TableInfo table = schema.getTable(FlowTableType.Runs);
        try
        {
            List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.Type.COUNT));
            List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));
            Map<String, Aggregate.Result> agg = Table.selectAggregatesForDisplay(table, aggregates, columns, filter, false);
            Aggregate.Result result = agg.get("RowId");
            if (result != null)
                return ((Long)result.getValue()).intValue();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return 0;
    }

    public int getRunCount(Container container, ObjectType type)
    {
        try
        {
            String sqlFCSRunCount = "SELECT COUNT (exp.ExperimentRun.RowId) FROM exp.experimentrun\n" +
                    "WHERE exp.ExperimentRun.RowId IN (" +
                    "SELECT exp.data.runid FROM exp.data INNER JOIN flow.object ON flow.object.dataid = exp.data.rowid\n" +
                    "AND exp.data.container = ?\n" +
                    "AND flow.object.container = ?\n" +
                    "AND flow.object.typeid = ?)";
            return Table.executeSingleton(getSchema(), sqlFCSRunCount, new Object[] { container.getId(), container.getId(), type.getTypeId() }, Integer.class);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }

    public int getFCSRunCount(Container container)
    {
        try
        {
            String sqlFCSRunCount = "SELECT COUNT (exp.ExperimentRun.RowId) FROM exp.experimentrun\n" +
                    "WHERE exp.ExperimentRun.RowId IN (" +
                    "SELECT exp.data.runid FROM exp.data INNER JOIN flow.object ON flow.object.dataid = exp.data.rowid\n" +
                    "AND exp.data.container = ?\n" +
                    "AND flow.object.container = ?\n" +
                    "AND flow.object.typeid = ?) AND exp.ExperimentRun.FilePathRoot IS NOT NULL";
            return Table.executeSingleton(getSchema(), sqlFCSRunCount, new Object[] { container.getId(), container.getId(), ObjectType.fcsKeywords.getTypeId() }, Integer.class);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }

    public void deleteContainer(Container container)
    {
        try
        {
            SQLFragment sqlOIDs = new SQLFragment("SELECT flow.object.rowid FROM flow.object INNER JOIN exp.data ON flow.object.dataid = exp.data.rowid AND exp.data.container = ?", container.getId());
            deleteObjectIds(sqlOIDs, Collections.singleton(container));
            Table.execute(getSchema(), "DELETE FROM " + getTinfoStatisticAttr() + " WHERE container=?", new Object[] {container});
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    /**
     * this is a bit of a hack
     * script job and FlowJoWorkspace.createExperimentRun() do not update these new fields
     */
    public void updateFlowObjectCols(Container c)
    {
        DbSchema s = getSchema();
        TableInfo o = getTinfoObject();
        boolean beginTrans = !s.getScope().isTransactionActive();

        try
        {
            if (beginTrans)
                s.getScope().beginTransaction();

            if (o.getColumn("container") != null)
            {
                Table.execute(s,
                        "UPDATE flow.object "+
                        "SET container = ? " +
                        "WHERE container IS NULL AND dataid IN (select rowid from exp.data WHERE exp.data.container = ?)", new Object[] {c.getId(), c.getId()});
            }

            if (o.getColumn("compid") != null)
            {
                Table.execute(s,
                        "UPDATE flow.object SET "+
                        "compid = COALESCE(compid,"+
                        "    (SELECT MIN(DI.dataid) "+
                        "    FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid "+
                        "    WHERE D.rowid = flow.object.dataid AND INPUT.typeid=4)), " +
                        "fcsid = COALESCE(fcsid,"+
                        "    (SELECT MIN(DI.dataid) "+
                        "    FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid "+
                        "    WHERE D.rowid = flow.object.dataid AND INPUT.typeid=1)), " +
                        "scriptid = COALESCE(scriptid,"+
                        "    (SELECT MIN(DI.dataid) "+
                        "    FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid "+
                        "    WHERE D.rowid = flow.object.dataid AND INPUT.typeid IN (5,7))) " +
                        "WHERE dataid IN (select rowid from exp.data where exp.data.container = ?) AND typeid=3 AND (compid IS NULL OR fcsid IS NULL OR scriptid IS NULL)", new Object[] {c.getId()});
            }
            if (beginTrans)
                s.getScope().commitTransaction();
        }
        catch (SQLException sqlx)
        {
            throw new RuntimeSQLException(sqlx);
        }
        finally
        {
            flowObjectModified();
            if (beginTrans)
                s.getScope().closeConnection();
        }
    }


    // postgres 8.2 workaround
    private static Boolean _postgreSQL82 = null;

    private static boolean isPostgresSQL82()
    {
        if (null == _postgreSQL82)
        {
            _postgreSQL82 = false;
            DbSchema db = FlowManager.get().getSchema();

            if (db.getSqlDialect().isPostgreSQL())
            {
                try
                {
                    if (null != Table.executeSingleton(db, "SELECT version() WHERE version() like '% 8.2%'", null, String.class))
                        _postgreSQL82 = true;
                }
                catch (SQLException x)
                {
                    throw new RuntimeSQLException(x);
                }
            }
        }

        return _postgreSQL82.booleanValue();
    }

    static public void vacuum()
    {
        if (isPostgresSQL82())
        {
            try
            {
                Table.execute(FlowManager.get().getSchema(), "VACUUM exp.data; VACUUM flow.object; VACUUM flow.keyword; VACUUM flow.statistic;", null);
            }
            catch (SQLException x)
            {
                _log.error("unexpected error", x);
            }
        }
    }

    static public void analyze()
    {
        if (isPostgresSQL82())
        {
            try
            {
                Table.execute(FlowManager.get().getSchema(), "ANALYZE exp.data; ANALYZE flow.object; ANALYZE flow.keyword; ANALYZE flow.statistic;", null);
            }
            catch (SQLException x)
            {
                _log.error("unexpected error", x);
            }
        }
    }
}
