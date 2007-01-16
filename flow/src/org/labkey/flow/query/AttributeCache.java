package org.labkey.flow.query;

import org.labkey.api.data.*;
import org.labkey.flow.persist.FlowManager;
import org.labkey.api.util.Cache;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Collections;
import java.util.TreeMap;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

abstract public class AttributeCache<T>
{
    static final private Logger _log = Logger.getLogger(AttributeCache.class);

    TableInfo _table;
    ColumnInfo _attrIdColumn;
    ColumnInfo _objectIdColumn;

    public AttributeCache(ColumnInfo attrIdColumn, ColumnInfo objectIdColumn)
    {
        _table = attrIdColumn.getParentTable();
        _attrIdColumn = attrIdColumn;
        _objectIdColumn = objectIdColumn;
        assert _table == _objectIdColumn.getParentTable();
    }

    public Map<T, Integer> getAttrValues(ColumnInfo colDataId)
    {
        TableInfo table = colDataId.getParentTable();
        SQLFragment sql = new SQLFragment("SELECT DISTINCT flow.Attribute.RowId, flow.Attribute.Name\nFROM ");
        sql.append(table.getFromSQL("Data"));
        sql.append("\nINNER JOIN flow.Object ON flow.Object.DataId = ");
        sql.append(colDataId.getValueSql("Data"));
        sql.append("\nINNER JOIN ");
        sql.append(_table.getFromSQL("property"));
        sql.append(" ON flow.Object.RowId = ");
        sql.append(_objectIdColumn.getValueSql("property"));
        sql.append("\nINNER JOIN flow.Attribute ON flow.Attribute.RowId = ");
        sql.append(_attrIdColumn.getValueSql("property"));
        String key = makeCacheKey(sql);
        Map<T, Integer> ret = (Map) DbCache.get(_table, key);
        if (ret != null)
            return ret;

        try
        {
            ResultSet rs = Table.executeQuery(FlowManager.get().getSchema(), sql);
            ret = new TreeMap();
            while (rs.next())
            {
                ret.put(keyFromString(rs.getString(2)), rs.getInt(1));
            }
            ret = Collections.unmodifiableMap(ret);
            DbCache.put(_table, key, ret, Cache.HOUR);
            rs.close();
            return ret;
        }
        catch (SQLException e)
        {
            _log.error("exception", e);
            return Collections.EMPTY_MAP;
        }

    }

    abstract protected T keyFromString(String str);

    private String makeCacheKey(SQLFragment sql)
    {
        StringBuilder ret = new StringBuilder();
        ret.append(getClass().getName());
        ret.append("|||");
        ret.append(_table.getName());
        ret.append("|||");
        ret.append(sql.toString());
        ret.append("|||");
        ret.append(sql.getParams().toString());
        return ret.toString();
    }

    static public class KeywordCache extends AttributeCache<String>
    {
        private KeywordCache(TableInfo keywordTable)
        {
            super(keywordTable.getColumn("KeywordId"), keywordTable.getColumn("ObjectId"));
        }

        protected String keyFromString(String str)
        {
            return str;
        }
    }

    static public class StatisticCache extends AttributeCache<StatisticSpec>
    {
        private StatisticCache(TableInfo statsTable)
        {
            super(statsTable.getColumn("StatisticId"), statsTable.getColumn("ObjectId"));
        }

        protected StatisticSpec keyFromString(String str)
        {
            return new StatisticSpec(str);
        }
    }

    static public class GraphCache extends AttributeCache<GraphSpec>
    {
        private GraphCache(TableInfo graphsTable)
        {
            super(graphsTable.getColumn("GraphId"), graphsTable.getColumn("ObjectId"));
        }

        protected GraphSpec keyFromString(String str)
        {
            return new GraphSpec(str);
        }
    }

    static public final KeywordCache KEYWORDS = new KeywordCache(FlowManager.get().getTinfoKeyword());
    static public final StatisticCache STATS = new StatisticCache(FlowManager.get().getTinfoStatistic());
    static public final GraphCache GRAPHS = new GraphCache(FlowManager.get().getTinfoGraph());
}
