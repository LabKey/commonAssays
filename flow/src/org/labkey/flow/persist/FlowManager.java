package org.labkey.flow.persist;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.util.*;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.query.AttributeCache;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class FlowManager
{
    static private FlowManager instance = new FlowManager();
    static private final Logger _log = Logger.getLogger(FlowManager.class);
    private static final String SCHEMA_NAME = "flow";
    private final Map<String, Integer> _attridCacheMap = new LimitedCacheMap(1000, 10000);
    class AttrNameCacheMap extends LimitedCacheMap<Integer, String>
    {
        public AttrNameCacheMap(int initialSize, int maxSize)
        {
            super(initialSize, maxSize);
        }
        public Entry<Integer, String> findEntry(Object key)
        {
            return super.findEntry(key);
        }
    }
    private final AttrNameCacheMap _attrNameCacheMap = new AttrNameCacheMap(1000, 10000);

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

    public SqlDialect getDialect()
    {
        return getSchema().getSqlDialect();
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
            Integer ret = _attridCacheMap.get(attr);
            if (ret != null)
                return ret;

            try
            {
                Integer i = Table.executeSingleton(getSchema(), "SELECT RowId FROM flow.Attribute WHERE Name = ?", new Object[] {attr }, Integer.class);
                _attridCacheMap.put(attr, i);
                if (i == null)
                    return 0;
                _attrNameCacheMap.put(i, attr);
                return i;
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
        ArrayList<Map.Entry<Integer, String>> lstRet = new ArrayList();
        for (Map.Entry<Integer, String> entry : ret)
        {
            if (entry != null)
            {
                lstRet.add(entry);
            }
        }
        return lstRet.toArray(new Map.Entry[0]);
    }

    public Map.Entry<Integer, String> getAttributeName(int id)
    {
        synchronized(_attridCacheMap)
        {
            Map.Entry<Integer, String>ret = _attrNameCacheMap.findEntry(id);
            if (ret == null)
            {
                try
                {
                    String name = Table.executeSingleton(getSchema(), "SELECT Name FROM flow.Attribute WHERE RowId = ?", new Object[] { id }, String.class);
                    if (name == null)
                    {
                        return null;
                    }
                    _attrNameCacheMap.put(id, name);
                    _attridCacheMap.put(name, id);
                    return _attrNameCacheMap.findEntry(id);
                }
                catch (SQLException e)
                {
                    _log.error("Error retrieving attribute " + id, e);
                    return null;
                }
            }
            return ret;
        }
    }

    public int ensureAttributeId(String attr) throws SQLException
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
            Map<String, Object> map = new HashMap();
            map.put("Name", attr);
            Table.insert(null, getTinfoAttribute(), map);
            _attridCacheMap.remove(attr);
            return getAttributeId(attr);
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

    public AttrObject createAttrObject(ExpData data, ObjectType type, URI uri) throws SQLException
    {
        if (FlowDataHandler.instance.getPriority(ExperimentService.get().getExpData(data.getRowId())) != Handler.Priority.HIGH)
        {
            // Need to make sure the right ExperimentDataHandler is associated with this data file, otherwise, you
            // won't be able to delete it because of the foreign key constraint from the flow.object table.
            throw new IllegalStateException("FlowDataHandler must be associated with data file");
        }
        AttrObject newObject = new AttrObject();
        newObject.setDataId(data.getRowId());
        newObject.setTypeId(type.getTypeId());
        if (uri != null)
        {
            newObject.setUri(uri.toString());
        }
        return Table.insert(null, getTinfoObject(), newObject);
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
            Table.execute(getSchema(), "DELETE FROM flow.Statistic WHERE ObjectId IN " + sqlObjectIds, sqlObjectIds.getParamsArray());
            Table.execute(getSchema(), "DELETE FROM flow.Keyword WHERE ObjectId IN " + sqlObjectIds, sqlObjectIds.getParamsArray());
            Table.execute(getSchema(), "DELETE FROM flow.Graph WHERE ObjectId IN " + sqlObjectIds, sqlObjectIds.getParamsArray());
            Table.execute(getSchema(), "DELETE FROM flow.Script WHERE ObjectId IN " + sqlObjectIds, sqlObjectIds.getParamsArray());
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
        deleteAttributes(new SQLFragment("(" + obj.getRowId() + ")"));
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
            Table.execute(getSchema(), "DELETE FROM flow.Object WHERE RowId IN " + sqlOIDs, sqlOIDs.getParamsArray());
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
        }

    }

    public void deleteData(List<ExpData> datas) throws SQLException
    {
        if (datas.size() == 0)
            return;
        StringBuilder sqlGetOIDs = new StringBuilder("SELECT flow.Object.RowId FROM flow.Object WHERE flow.Object.DataId IN (");
        String comma = "";
        Set<Container> containers = new HashSet();
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
        SQLFragment sqlOIDs = new SQLFragment("(");
        sqlOIDs.append(StringUtils.join(objectIds, ","));
        sqlOIDs.append(")");
        deleteObjectIds(sqlOIDs, containers);
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
            script = Table.update(user, getTinfoScript(), script, script.getRowId(), null);
        }
    }

    public int getObjectCount(Container container, ObjectType type) throws SQLException
    {
        String sqlFCSFileCount = "SELECT COUNT(flow.object.rowid) FROM flow.object\n" +
                "INNER JOIN exp.data ON flow.object.dataid = exp.data.rowid\n" +
                "WHERE exp.data.container = ? AND " +
                "flow.object.typeid = ?";
        return Table.executeSingleton(getSchema(), sqlFCSFileCount, new Object[] { container.getId(), type.getTypeId() }, Integer.class);
    }

    public int getRunCount(Container container, ObjectType type) throws SQLException
    {
        String sqlFCSRunCount = "SELECT COUNT (exp.ExperimentRun.RowId) FROM exp.experimentrun\n" +
                "WHERE exp.ExperimentRun.RowId IN (" +
                "SELECT exp.data.runid FROM exp.data INNER JOIN flow.object ON flow.object.dataid = exp.data.rowid\n" +
                "AND exp.data.container = ?\n" +
                "AND flow.object.typeid = ?)";
        return Table.executeSingleton(getSchema(), sqlFCSRunCount, new Object[] { container.getId(), type.getTypeId() }, Integer.class);
    }

    public int getFCSRunCount(Container container) throws SQLException
    {
        String sqlFCSRunCount = "SELECT COUNT (exp.ExperimentRun.RowId) FROM exp.experimentrun\n" +
                "WHERE exp.ExperimentRun.RowId IN (" +
                "SELECT exp.data.runid FROM exp.data INNER JOIN flow.object ON flow.object.dataid = exp.data.rowid\n" +
                "AND exp.data.container = ?\n" +
                "AND flow.object.typeid = ?) AND exp.ExperimentRun.FilePathRoot IS NOT NULL";
        return Table.executeSingleton(getSchema(), sqlFCSRunCount, new Object[] { container.getId(), ObjectType.fcsKeywords.getTypeId() }, Integer.class);
    }

    public void deleteContainer(Container container) throws SQLException
    {
        SQLFragment sqlOIDs = new SQLFragment("(SELECT flow.object.rowid FROM flow.object INNER JOIN exp.data ON flow.object.dataid = exp.data.rowid AND exp.data.container = ?)", container.getId());
        deleteObjectIds(sqlOIDs, Collections.singleton(container));
    }


    public MultiMap searchFCSFiles(Collection<String> containerIds, Search.SearchTermParser parser)
    {
        FCSFileSearch search = new FCSFileSearch(containerIds, parser);
        return search.search();
    }


    public static class FCSFileSearch implements Search.Searchable
    {
        Collection<String> containerIds;
        Search.SearchTermParser parser;
        
        public FCSFileSearch(Collection<String> containerIds, Search.SearchTermParser parser)
        {
            this.containerIds = containerIds;
            this.parser = parser;
        }

        public MultiMap search(Collection<String> containerIds, Search.SearchTermParser parser)
        {
            this.containerIds = containerIds;
            this.parser = parser;
            return search();
        }


        protected MultiMap search()
        {
            DbSchema s = DbSchema.get("flow");
            String fromClause = "flow.attribute A inner join flow.keyword K on A.rowid=K.keywordid inner join flow.object O on K.objectid = O.rowid inner join exp.data D on O.dataid = D.rowid";
            SQLFragment fragment = Search.getSQLFragment("container, uri, dataid", "D.container, O.uri, O.dataid, A.name, K.value", fromClause, "D.Container", null, containerIds, parser, s.getSqlDialect(),  "A.name", "K.value");

            MultiMap map = new MultiValueMap();
            ResultSet rs = null;

            ActionURL url = new ActionURL("flow-well", "showWell", "");
            StringBuilder link = new StringBuilder(200);

            try
            {
                rs = Table.executeQuery(s, fragment);

                while(rs.next())
                {
                    String containerId = rs.getString(1);
                    String uri = rs.getString(2);
                    String wellId = String.valueOf(rs.getInt(3));
                    
                    String path;
                    try
                    {
                        path = new File(new URI(uri)).getPath();
                    }
                    catch (URISyntaxException x)
                    {
                        continue;
                    }
                    Container c = ContainerManager.getForId(containerId);
                    url.setExtraPath(c.getPath());
                    url.replaceParameter("wellId", wellId);
                    link.append("<a href=\"");
                    link.append(url.getEncodedLocalURIString());
                    link.append("\">");
                    link.append(PageFlowUtil.filter(path));
                    link.append("</a>");
                    map.put(containerId, link.toString());
                    link.setLength(0);
                }
            }
            catch(SQLException e)
            {
                ExceptionUtil.logExceptionToMothership(HttpView.currentRequest(), e);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }

            return map;
        }

        public String getSearchResultName()
        {
            return "FCS File";
        }
    }
}
