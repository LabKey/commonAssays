/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpObject;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.Tuple3;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.data.AttributeType;
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

    private final HashMap<NameCacheKey, FlowEntry> _attrIdCacheMap = new HashMap<NameCacheKey, FlowEntry>(1000);
    private final HashMap<IdCacheKey, FlowEntry> _attrNameCacheMap = new HashMap<IdCacheKey, FlowEntry>(1000);

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

    private TableInfo attributeTable(AttributeType type)
    {
        switch (type)
        {
            case keyword:   return getTinfoKeywordAttr();
            case statistic: return getTinfoStatisticAttr();
            case graph:     return getTinfoGraphAttr();
            default:        throw new RuntimeException();
        }
    }

    public int getAttributeId(Container container, AttributeType type, String attr)
    {
        synchronized (_attrIdCacheMap)
        {
            quickFillCache();

            NameCacheKey key = new NameCacheKey(container.getId(), type, attr);
            FlowEntry a = _attrIdCacheMap.get(key);
            if (a != null)
                return a._rowId.intValue();

            ResultSet rs = null;
            try
            {
                rs = Table.executeQuery(getSchema(), "SELECT RowId, Id FROM " + attributeTable(type) + " WHERE Container = ? AND Name = ?", new Object[] {container, attr});
                // we're not caching misses because this is an unlimited cachemap
                if (!rs.next())
                    return 0;

                Integer i = rs.getInt("RowId");
                Integer aliasId = rs.getInt("Id");
                a = new FlowEntry(type, i, container, attr, aliasId);
                _attrIdCacheMap.put(key, a);
                _attrNameCacheMap.put(new IdCacheKey(type, i), a);
                return i.intValue();
            }
            catch (SQLException e)
            {
                throw UnexpectedException.wrap(e);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }
        }
    }

    public FlowEntry[] getAttributeEntry(AttributeType type, Integer[] ids)
    {
        FlowEntry[] ret = new FlowEntry[ids.length];
        boolean hasNulls = false;
        for (int i = 0; i < ids.length; i ++)
        {
            Integer id = ids[i];
            if (id != null)
            {
                ret[i] = getAttributeEntry(type, ids[i]);
            }
            if (ret[i] == null)
            {
                _log.error("Request for attribute " + id + " returned null.", new Exception());
                hasNulls = true;
            }
        }
        if (!hasNulls)
            return ret;
        ArrayList<FlowEntry> lstRet = new ArrayList<FlowEntry>();
        for (FlowEntry entry : ret)
        {
            if (entry != null)
            {
                lstRet.add(entry);
            }
        }
        return lstRet.toArray(new FlowEntry[lstRet.size()]);
    }


    public FlowEntry getAttributeEntry(AttributeType type, int id)
    {
        synchronized(_attrIdCacheMap)
        {
            quickFillCache();

            IdCacheKey key = new IdCacheKey(type, id);
            FlowEntry entry = _attrNameCacheMap.get(key);
            if (entry == null)
            {
                try
                {
                    Map<String, Object> row = Table.executeSingleton(getSchema(), "SELECT Container, Name, Id FROM " + attributeTable(type) + " WHERE RowId = ?", new Object[] { id }, Map.class);
                    if (row == null)
                    {
                        return null;
                    }
                    String name = (String)row.get("Name");
                    Container container = ContainerManager.getForId((String)row.get("Container"));
                    Integer aliasId = (Integer)row.get("Id");
                    entry = new FlowEntry(type, id, container, name, aliasId);

                    _attrNameCacheMap.put(key, entry);
                    _attrIdCacheMap.put(new NameCacheKey(container.getId(), type, name), entry);
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }

            return entry;
        }
    }

    private final class NameCacheKey extends Tuple3<String, AttributeType, String>
    {
        public NameCacheKey(String containerId, AttributeType type, String attrName)
        {
            super(containerId, type, attrName);
        }
    }

    private final class IdCacheKey extends Pair<AttributeType, Integer>
    {
        public IdCacheKey(AttributeType type, Integer rowId)
        {
            super(type, rowId);
        }
    }

    public static class FlowEntry
    {
        public final AttributeType _type;
        public final Integer _rowId;
        public final Container _container;
        public final String _name;
        public final Integer _aliasId;

        public FlowEntry(@NotNull AttributeType type, @NotNull Integer rowId, @NotNull Container container, @NotNull String name, @NotNull Integer aliasId)
        {
            _type = type;
            _rowId = rowId;
            _container = container;
            _name = name;
            _aliasId = aliasId;
        }

        public boolean isAlias()
        {
            return !_rowId.equals(_aliasId);
        }
    }

    private void quickFillCache(AttributeType type)
    {
        try
        {
            Map<String, Object>[] rows = Table.selectMaps(attributeTable(type), new HashSet<String>(Arrays.asList("RowId", "Container", "Name", "Id")), null, null);
            for (Map<String, Object> row : rows)
            {
                Integer rowid = (Integer)row.get("RowId");
                String containerId = (String)row.get("Container");
                Container container = ContainerManager.getForId(containerId);
                String name = (String)row.get("Name");
                Integer aliasId = (Integer)row.get("Id");
                FlowEntry entry = new FlowEntry(type, rowid, container, name, aliasId);
                _attrNameCacheMap.put(new IdCacheKey(type, rowid), entry);
                _attrIdCacheMap.put(new NameCacheKey(containerId, type, name), entry);
            }
        }
        catch (SQLException e)
        {
            _log.error("Unexpected error", e);
            // fall through;
        }
    }

    private void quickFillCache()
    {
        if (_attrNameCacheMap.isEmpty())
        {
            quickFillCache(AttributeType.keyword);
            quickFillCache(AttributeType.statistic);
            quickFillCache(AttributeType.graph);
        }
    }


    /**
     * Ensure the attribute exists.  If the id >= 0, the id points at the RowId of the preferred name for the attribute.
     *
     * @param container Container
     * @param type attribute type
     * @param attr attribute name
     * @param id RowId of aliased attribute.
     * @return The RowId of the rewly inserted or existing attribute.
     * @throws SQLException
     */
    private int ensureAttributeId(Container container, AttributeType type, String attr, int id) throws SQLException
    {
        DbSchema schema = getSchema();
        if (schema.getScope().isTransactionActive())
        {
            throw new IllegalStateException("ensureAttributeId cannot be called within a transaction");
        }
        synchronized(_attrIdCacheMap)
        {
            int ret = getAttributeId(container, type, attr);
            if (ret != 0)
                return ret;
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("Container", container.getId());
            map.put("Name", attr);
            map.put("Id", id);

            TableInfo table = attributeTable(type);
            map = Table.insert(null, table, map);

            // Set Id to RowId if we aren't inserting an alias
            if (id <= 0)
            {
                map.put("Id", map.get("RowId"));
                Table.update(null, table, map, map.get("RowId"));
            }
            _attrIdCacheMap.remove(new NameCacheKey(container.getId(), type, attr));
            return getAttributeId(container, type, attr);
        }
    }

    private int ensureAttributeId(Container container, AttributeType type, String attr) throws SQLException
    {
        return ensureAttributeId(container, type, attr, -1);
    }


    public int ensureStatisticId(Container c, String attr) throws SQLException
    {
        return ensureAttributeId(c, AttributeType.statistic, attr);
    }


    public int ensureKeywordId(Container c, String attr) throws SQLException
    {
        return ensureAttributeId(c, AttributeType.keyword, attr);
    }


    public int ensureGraphId(Container c, String attr) throws SQLException
    {
        return ensureAttributeId(c, AttributeType.graph, attr);
    }

    public void ensureStatisticAliases(Container c, String attr, Iterable<? extends Object> aliases) throws SQLException
    {
        ensureAttributeAliases(c, AttributeType.statistic, attr, aliases);
    }


    public void ensureKeywordAliases(Container c, String attr, Iterable<? extends Object> aliases) throws SQLException
    {
        ensureAttributeAliases(c, AttributeType.keyword, attr, aliases);
    }


    public void ensureGraphAliases(Container c, String attr, Iterable<? extends Object> aliases) throws SQLException
    {
        ensureAttributeAliases(c, AttributeType.graph, attr, aliases);
    }

    private void ensureAttributeAliases(Container c, AttributeType type, String attr, Iterable<? extends Object> aliases)
            throws SQLException
    {
        int id = ensureAttributeId(c, type, attr);
        for (Object alias : aliases)
            ensureAttributeId(c, type, alias.toString(), id);
    }

    public boolean isAlias(AttributeType type, int attrId)
    {
        FlowEntry entry = getAttributeEntry(type, attrId);
        if (entry == null)
            return false;

        return entry.isAlias();
    }

    /** Return the preferred name for the attrId or null if attrId is not an alias id. */
    public FlowEntry getAliased(AttributeType type, int attrId)
    {
        FlowEntry entry = getAttributeEntry(type, attrId);
        if (entry == null || !entry.isAlias())
            return null;

        return getAttributeEntry(type, entry._aliasId);
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
            // XXX: delete no longer referenced statattr afterwards?
            Table.execute(getSchema(), "DELETE FROM flow.Statistic WHERE ObjectId IN (" + list + ")");
            Table.execute(getSchema(), "DELETE FROM flow.Keyword WHERE ObjectId IN (" + list + ")");
            Table.execute(getSchema(), "DELETE FROM flow.Graph WHERE ObjectId IN (" + list + ")");
            Table.execute(getSchema(), "DELETE FROM flow.Script WHERE ObjectId IN (" + list + ")");
        }
    }


    private void deleteAttributes(SQLFragment sqlObjectIds) throws SQLException
    {
        DbScope scope = getSchema().getScope();
        try
        {
            scope.ensureTransaction();

            Integer[] objids = Table.executeArray(getSchema(), sqlObjectIds, Integer.class);
            deleteAttributes(objids);
            scope.commitTransaction();
        }
        finally
        {
            scope.closeConnection();
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
        try
        {
            scope.ensureTransaction();

            deleteAttributes(oids);
            SQLFragment sqlf = new SQLFragment("DELETE FROM flow.Object WHERE RowId IN (" );
            sqlf.append(StringUtils.join(oids,','));
            sqlf.append(")");
            Table.execute(getSchema(), sqlf);
            scope.commitTransaction();
        }
        finally
        {
            getSchema().getScope().closeConnection();

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
        try
        {
            scope.ensureTransaction();

            deleteAttributes(sqlOIDs);
            Table.execute(getSchema(), "DELETE FROM flow.Object WHERE RowId IN (" + sqlOIDs.getSQL() + ")", sqlOIDs.getParamsArray());
            scope.commitTransaction();
        }
        finally
        {
            getSchema().getScope().closeConnection();

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
                                            "\nINNER JOIN flow.KeywordAttr ON flow.KeywordAttr.rowid = flow.keyword.keywordid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.KeywordAttr.name = ?";
    public String getKeyword(ExpData data, String keyword) throws SQLException
    {
        return Table.executeSingleton(getSchema(), sqlSelectKeyword, new Object[] { data.getRowId(), keyword }, String.class);
    }

    static private String sqlDeleteKeyword = "DELETE FROM flow.keyword WHERE ObjectId = ? AND KeywordId = ?";
    static private String sqlInsertKeyword = "INSERT INTO flow.keyword (ObjectId, KeywordId, Value) VALUES (?, ?, ?)";
    public void setKeyword(Container c, ExpData data, String keyword, String value) throws SQLException
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
        int keywordId = ensureKeywordId(c, keyword);
        DbSchema schema = getSchema();
        try
        {
            schema.getScope().ensureTransaction();

            Table.execute(schema, sqlDeleteKeyword, obj.getRowId(), keywordId);
            if (value != null)
            {
                Table.execute(schema, sqlInsertKeyword, obj.getRowId(), keywordId, value);
            }
            schema.getScope().commitTransaction();
        }
        finally
        {
            schema.getScope().closeConnection();
            AttributeCache.invalidateCache(data.getContainer());
        }

    }

    static private String sqlSelectStat = "SELECT flow.statistic.value FROM flow.object" +
                                            "\nINNER JOIN flow.statistic on flow.object.rowid = flow.statistic.objectid" +
                                            "\nINNER JOIN flow.StatisticAttr ON flow.StatisticAttr.rowid = flow.statistic.statisticid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.StatisticAttr.name = ?";
    public Double getStatistic(ExpData data, StatisticSpec stat) throws SQLException
    {
        return Table.executeSingleton(getSchema(), sqlSelectStat, new Object[] { data.getRowId(), stat.toString() }, Double.class);
    }

    static private String sqlSelectGraph = "SELECT flow.graph.data FROM flow.object" +
                                            "\nINNER JOIN flow.graph on flow.object.rowid = flow.graph.objectid" +
                                            "\nINNER JOIN flow.GraphAttr ON flow.GraphAttr.rowid = flow.graph.graphid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.GraphAttr.name = ?";
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
            Map<String, Aggregate.Result> agg = Table.selectAggregatesForDisplay(table, aggregates, columns, null, filter, false);
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

            Map<String, Aggregate.Result> agg = Table.selectAggregatesForDisplay(table, aggregates, columns, null, filter, false);
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
            Map<String, Aggregate.Result> agg = Table.selectAggregatesForDisplay(table, aggregates, columns, null, filter, false);
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
            Table.execute(getSchema(), "DELETE FROM " + getTinfoKeywordAttr() + " WHERE container=?", container);
            Table.execute(getSchema(), "DELETE FROM " + getTinfoStatisticAttr() + " WHERE container=?", container);
            Table.execute(getSchema(), "DELETE FROM " + getTinfoGraphAttr() + " WHERE container=?", container);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    /**
     * this is a bit of a hack
     * script job and WorkspaceJob.createExperimentRun() do not update these new fields
     */
    public void updateFlowObjectCols(Container c)
    {
        DbSchema s = getSchema();
        TableInfo o = getTinfoObject();
        try
        {
            s.getScope().ensureTransaction();

            if (o.getColumn("container") != null)
            {
                Table.execute(s,
                        "UPDATE flow.object "+
                        "SET container = ? " +
                        "WHERE container IS NULL AND dataid IN (select rowid from exp.data WHERE exp.data.container = ?)", c.getId(), c.getId());
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
                        "WHERE dataid IN (select rowid from exp.data where exp.data.container = ?) AND typeid=3 AND (compid IS NULL OR fcsid IS NULL OR scriptid IS NULL)", c.getId());
            }
            s.getScope().commitTransaction();
        }
        catch (SQLException sqlx)
        {
            throw new RuntimeSQLException(sqlx);
        }
        finally
        {
            flowObjectModified();
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
                Table.execute(FlowManager.get().getSchema(), "VACUUM exp.data; VACUUM flow.object; VACUUM flow.keyword; VACUUM flow.statistic;");
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
                Table.execute(FlowManager.get().getSchema(), "ANALYZE exp.data; ANALYZE flow.object; ANALYZE flow.keyword; ANALYZE flow.statistic;");
            }
            catch (SQLException x)
            {
                _log.error("unexpected error", x);
            }
        }
    }
}
