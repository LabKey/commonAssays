package org.labkey.flow.persist;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.*;
import org.labkey.api.exp.Data;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.api.util.LimitedCacheMap;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowManager
{
    static private FlowManager instance = new FlowManager();
    private static final String SCHEMA_NAME = "flow";
    private final Map<String, Integer> _attridCacheMap = new LimitedCacheMap(1000, 10000);

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
                return i;
            }
            catch (SQLException e)
            {
                throw UnexpectedException.wrap(e);
            }
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
        AttrObject newObject = new AttrObject();
        newObject.setDataId(data.getRowId());
        newObject.setTypeId(type.getTypeId());
        if (uri != null)
        {
            newObject.setUri(uri.toString());
        }
        return Table.insert(null, getTinfoObject(), newObject);
    }

    public void deleteData(List<Data> datas) throws SQLException
    {
        if (datas.size() == 0)
            return;
        StringBuilder sqlGetOIDs = new StringBuilder("SELECT flow.Object.RowId FROM flow.Object WHERE flow.Object.DataId IN (");
        String comma = "";
        for (Data data : datas)
        {
            sqlGetOIDs.append(comma);
            comma = ",";
            sqlGetOIDs.append(data.getRowId());
        }
        sqlGetOIDs.append(")");
        Integer[] objectIds = Table.executeArray(getSchema(), sqlGetOIDs.toString(), null, Integer.class);
        if (objectIds.length == 0)
            return;
        String sqlOIDs = "(" + StringUtils.join(objectIds, ",") + ")";

        DbScope scope = getSchema().getScope();
        boolean fTrans = false;
        try
        {
            if (!scope.isTransactionActive())
            {
                scope.beginTransaction();
                fTrans = true;
            }
            Table.execute(getSchema(), "DELETE FROM flow.Statistic WHERE ObjectId IN " + sqlOIDs, null);
            Table.execute(getSchema(), "DELETE FROM flow.Keyword WHERE ObjectId IN " + sqlOIDs, null);
            Table.execute(getSchema(), "DELETE FROM flow.Graph WHERE ObjectId IN " + sqlOIDs, null);
            Table.execute(getSchema(), "DELETE FROM flow.Script WHERE ObjectId IN " + sqlOIDs, null);
            Table.execute(getSchema(), "DELETE FROM flow.Object WHERE RowId IN " + sqlOIDs, null);
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
        }
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
        int keywordId = getAttributeId(keyword);
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
}
