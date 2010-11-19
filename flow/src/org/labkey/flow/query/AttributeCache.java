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

package org.labkey.flow.query;

import org.apache.log4j.Logger;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.*;
import org.labkey.api.util.Filter;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.persist.FlowManager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

abstract public class AttributeCache<T>
{
    private static final Logger _log = Logger.getLogger(AttributeCache.class);
    private static final Cache<CacheKey, Map.Entry<Integer, String>[]> _cache = CacheManager.getCache(200, CacheManager.DAY, "Flow AttributeCache");
    private static long _transactionCount;
    private static Container _lastContainerInvalidated;

    private static class CacheKey
    {
        final public Container _container;
        final public String _sql;
        final public Object[] _params;

        public CacheKey(Container container, String sql, Object[] params)
        {
            _container = container;
            _sql = sql;
            _params = params;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CacheKey cacheKey = (CacheKey) o;
            if (_container != null ? !_container.equals(cacheKey._container) : cacheKey._container != null)
                return false;
            if (!Arrays.equals(_params, cacheKey._params))
                return false;
            if (!_sql.equals(cacheKey._sql))
                return false;
            return true;
        }

        public int hashCode()
        {
            int result;
            result = (_container != null ? _container.hashCode() : 0);
            result = 31 * result + _sql.hashCode();
            result = 31 * result + Arrays.hashCode(_params);
            return result;
        }
    }

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

    static private long getTransactionCount()
    {
        synchronized(_cache)
        {
            return _transactionCount;
        }
    }

    static public void invalidateCache(final Container container)
    {
        synchronized(_cache)
        {
            if (_lastContainerInvalidated != null && _lastContainerInvalidated.equals(container))
            {
                return;
            }
            if (container == null)
            {
                _cache.clear();
            }
            else
            {
                _cache.removeUsingFilter(new Filter<CacheKey>(){
                    @Override
                    public boolean accept(CacheKey key)
                    {
                        return key._container == null || key._container.equals(container);
                    }
                });
            }
            _transactionCount ++;
            _lastContainerInvalidated = container;
        }
    }

    private static void storeInCache(long transactionCount, CacheKey key, Map.Entry<Integer, String>[] value)
    {
        synchronized(_cache)
        {
            if (getTransactionCount() != transactionCount)
            {
                return;
            }
            _cache.put(key, value);
            if (key._container == null || key._container.equals(_lastContainerInvalidated))
            {
                _lastContainerInvalidated = null;
            }
        }
    }

    private Map.Entry<Integer, String>[] getFromCache(CacheKey key)
    {
        synchronized(_cache)
        {
            return _cache.get(key);
        }
    }

    public Map<T, Integer> getAttrValues(Container container, ColumnInfo colDataId)
    {
        SQLFragment sql = new SQLFragment();

//        if (1==0)
//        {
//            TableInfo table = colDataId.getParentTable();
//            sql.append("SELECT DISTINCT ");
//            sql.append(_attrIdColumn.getValueSql("property"));
//            sql.append(" AS attrId\nFROM ");
//            sql.append(table.getFromSQL("Data"));
//            sql.append("\nINNER JOIN flow.Object ON flow.Object.DataId = ");
//            sql.append(colDataId.getValueSql("Data"));
//            sql.append("\nINNER JOIN ");
//            sql.append(_table.getFromSQL("property"));
//            sql.append(" ON flow.Object.RowId = ");
//            sql.append(_objectIdColumn.getValueSql("property"));
//        }
//        else
//        {
//            sql.append("SELECT rowid\nFROM flow.attribute\nWHERE rowid IN (SELECT " + _attrIdColumn.getValueSql("property")  + " FROM flow.Object INNER JOIN ").append(_table.getFromSQL("property"))
//                .append(" ON flow.Object.RowId = ").append(_objectIdColumn.getValueSql("property"));
//            sql.append("\n WHERE flow.Object.container=?)");
//            sql.add(container.getId());
//        }



        if ("statisticid".equalsIgnoreCase(_attrIdColumn.getName()))
        {
            sql.append("SELECT id AS attrId FROM " + FlowManager.get().getTinfoStatisticAttr() + " WHERE container=?");
            sql.add(container.getId());
        }
        else if ("graphid".equalsIgnoreCase(_attrIdColumn.getName()))
        {
            sql.append("SELECT id AS attrId FROM " + FlowManager.get().getTinfoGraphAttr() + " WHERE container=?");
            sql.add(container.getId());
        }
        else if ("keywordid".equalsIgnoreCase(_attrIdColumn.getName()))
        {
            sql.append("SELECT id AS attrId FROM " + FlowManager.get().getTinfoKeywordAttr() + " WHERE container=?");
            sql.add(container.getId());
        }
        else
        {
            assert false;
            sql.append("SELECT DISTINCT ");
            sql.append(_attrIdColumn.getValueSql("property"));
            sql.append(" AS attrId\nFROM ");
            sql.append("flow.Object INNER JOIN ");
            sql.append(_table.getFromSQL("property"));
            sql.append(") property ON flow.Object.RowId = ");
            sql.append(_objectIdColumn.getValueSql("property"));
            sql.append(" WHERE flow.Object.container=?");
            sql.add(container.getId());
        }

//        SQLFragment sql = new SQLFragment();
//        sql.append("SELECT rowid\nFROM flow.attribute");

        CacheKey key = new CacheKey(container, sql.getSQL(), sql.getParams().toArray());
        Map.Entry<Integer, String>[] entries = getFromCache(key);
        if (entries != null)
        {
            return mapFromEntries(entries);
        }
        try
        {
            long transactionCount = getTransactionCount();
            Integer[] ids = Table.executeArray(FlowManager.get().getSchema(), sql, Integer.class);
            entries = FlowManager.get().getAttributeNames(ids);
            storeInCache(transactionCount, key, entries);
            return mapFromEntries(entries);
        }
        catch (SQLException e)
        {
            _log.error("exception", e);
            return Collections.emptyMap();
        }
    }

    private Map<T, Integer> mapFromEntries(Map.Entry<Integer, String>[] entries)
    {
        TreeMap<T, Integer> ret = new TreeMap<T, Integer>();
        for (Map.Entry<Integer, String> entry : entries)
        {
            ret.put(keyFromString(entry.getValue()), entry.getKey());
        }
        return ret;
    }

    abstract protected T keyFromString(String str);

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
