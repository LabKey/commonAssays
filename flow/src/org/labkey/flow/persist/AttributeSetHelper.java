/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * User: kevink
 * Date: Apr 15, 2011
 *
 * Static helper methods for reading and saving AttributeSet to/from the database.
 */
public class AttributeSetHelper
{
    private static final Logger LOG = Logger.getLogger(AttributeSetHelper.class);


    public static AttributeSet fromData(ExpData data)
    {
        return fromData(data, false);
    }

    public static AttributeSet fromData(ExpData data, boolean includeGraphBytes)
    {
        AttrObject obj = FlowManager.get().getAttrObject(data);
        if (obj == null)
            return null;
        try
        {
            URI uri = null;
            if (obj.getUri() != null)
            {
                uri = new URI(obj.getUri());
            }
            AttributeSet ret = new AttributeSet(ObjectType.fromTypeId(obj.getTypeId()), uri);
            loadFromDb(ret, obj, includeGraphBytes);
            return ret;
        }
        catch (URISyntaxException use)
        {
            throw UnexpectedException.wrap(use);
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }


    /**
     * Called outside of any transaction, ensures that the necessary entries have been added to the flow.*Attr
     * tables.  That way, we never have to deal with transactions being rolled back and having to remove attribute
     * names from the cache, or two threads each trying to insert the same attribute name.
     * @throws SQLException
     */
    public static void prepareForSave(AttributeSet attrs, Container c, boolean clearCache)
    {
        //LOG.info("+prepareForSave");
        ensureKeywordNames(attrs, c, attrs.getKeywordNames());
        ensureStatisticNames(attrs, c, attrs.getStatisticNames());
        ensureGraphNames(attrs, c, attrs.getGraphNames());
        if (clearCache)
            AttributeCache.uncacheAllAfterCommit(c);
        //LOG.info("-prepareForSave");
    }

    private static void ensureKeywordNames(AttributeSet attrs, Container c, Collection<String> specs)
    {
        for (String spec : specs)
        {
            FlowManager.get().ensureKeywordNameAndAliases(c, spec, attrs.getKeywordAliases(spec), false);
        }
    }

    private static void ensureStatisticNames(AttributeSet attrs, Container c, Collection<StatisticSpec> specs)
    {
        for (StatisticSpec spec : specs)
        {
            String s = spec.toString();
            FlowManager.get().ensureStatisticNameAndAliases(c, s, attrs.getStatisticAliases(spec), false);
        }
    }

    private static void ensureGraphNames(AttributeSet attrs, Container c, Collection<GraphSpec> specs)
    {
        for (GraphSpec spec : specs)
        {
            String s = spec.toString();
            FlowManager.get().ensureGraphNameAndAliases(c, s, attrs.getGraphAliases(spec), false);
        }
    }

    public static void save(AttributeSet attrs, User user, ExpData data) throws SQLException
    {
        prepareForSave(attrs, data.getContainer(), true); // TODO: is clearing the cache here correct?
        doSave(attrs, user, data);
    }

    // NOTE: uncaches on transaction post-commit
    public static void doSave(AttributeSet attrs, User user, ExpData data) throws SQLException
    {
        //LOG.info("+doSave");
        FlowManager mgr = FlowManager.get();
        try (DbScope.Transaction transaction = mgr.getSchema().getScope().ensureTransaction())
        {
            Container c = data.getContainer();
            
            AttrObject obj = mgr.createAttrObject(data, attrs.getType(), attrs.getURI());
            Map<String, String> keywords = attrs.getKeywords();
            if (!keywords.isEmpty())
            {
                String sql = "INSERT INTO " + mgr.getTinfoKeyword() + " (ObjectId, KeywordId, OriginalKeywordId, Value) VALUES (?,?,?,?)";
                List<List<?>> paramsList = new ArrayList<>();
                for (Map.Entry<String, String> entry : keywords.entrySet())
                {
                    AttributeCache.Entry a = AttributeCache.KEYWORDS.byAttribute(c, entry.getKey());
                    int preferredId = a.getAliasedId() == null ? a.getRowId() : a.getAliasedId();
                    int originalId = a.getRowId();
                    paramsList.add(Arrays.asList(obj.getRowId(), preferredId, originalId, entry.getValue()));
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }

            Map<StatisticSpec, Double> statistics = attrs.getStatistics();
            if (!statistics.isEmpty())
            {
                String sql = "INSERT INTO " + mgr.getTinfoStatistic() + " (ObjectId, StatisticId, OriginalStatisticId, Value) VALUES (?,?,?,?)";
                List<List<?>> paramsList = new ArrayList<>();
                for (Map.Entry<StatisticSpec, Double> entry : statistics.entrySet())
                {
                    AttributeCache.Entry a = AttributeCache.STATS.byAttribute(c, entry.getKey());
                    int preferredId = a.getAliasedId() == null ? a.getRowId() : a.getAliasedId();
                    int originalId = a.getRowId();
                    paramsList.add(Arrays.<Object>asList(obj.getRowId(), preferredId, originalId, entry.getValue()));
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }

            Map<GraphSpec, byte[]> graphs = attrs.getGraphs();
            if (!graphs.isEmpty())
            {
                String sql = "INSERT INTO " + mgr.getTinfoGraph() + " (ObjectId, GraphId, OriginalGraphId, Data) VALUES (?, ?, ?, ?)";
                List<List<?>> paramsList = new ArrayList<>();
                for (Map.Entry<GraphSpec, byte[]> entry : graphs.entrySet())
                {
                    AttributeCache.Entry a = AttributeCache.GRAPHS.byAttribute(c, entry.getKey());
                    int preferredId = a.getAliasedId() == null ? a.getRowId() : a.getAliasedId();
                    int originalId = a.getRowId();
                    paramsList.add(Arrays.asList(obj.getRowId(), preferredId, originalId, entry.getValue()));
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }
            transaction.commit();
        }
        finally
        {
            AttributeCache.uncacheAllAfterCommit(data.getContainer());
            //LOG.info("-doSave");
        }

    }

    private static void loadFromDb(final AttributeSet attrs, AttrObject obj, final boolean includeGraphBytes) throws SQLException
    {
        FlowManager mgr = FlowManager.get();
        int rowId = obj.getRowId();

        String sqlKeywords = "SELECT flow.KeywordAttr.name, flow.keyword.KeywordId, flow.keyword.OriginalKeywordId, flow.keyword.value " +
                "FROM flow.keyword " +
                "INNER JOIN flow.KeywordAttr ON flow.keyword.OriginalKeywordId = flow.KeywordAttr.rowid " +
                "WHERE flow.keyword.objectId = ?";

        final List<Integer> keywordIDs = new ArrayList<>();
        final Map<String, String> keywords = new TreeMap<>();

        new SqlSelector(mgr.getSchema(), sqlKeywords, rowId).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                String name = rs.getString(1);
                int preferredId = rs.getInt(2);
                int originalId = rs.getInt(3);
                String value = rs.getString(4);

                keywordIDs.add(preferredId);
                keywords.put(name, value);
            }
        });

        attrs.setKeywords(keywords);

        if (keywordIDs.size() > 0)
        {
            String sqlKeywordAliaes = "SELECT A.name AS PreferredName, B.Name AS AliasName\n" +
                    "FROM flow.KeywordAttr A\n" +
                    "INNER JOIN flow.KeywordAttr B\n" +
                    "ON A.RowId = B.Id\n" +
                    "AND B.RowId != B.Id\n" +
                    "AND A.RowId IN (" + StringUtils.join(keywordIDs, ", ") + ")";

            new SqlSelector(mgr.getSchema(), sqlKeywordAliaes).forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    attrs.addKeywordAlias(rs.getString(1), rs.getString(2));
                }
            });
        }

        String sqlStatistics = "SELECT flow.StatisticAttr.name, flow.statistic.StatisticId, flow.statistic.OriginalStatisticId, flow.statistic.value " +
                "FROM flow.statistic " +
                "INNER JOIN flow.StatisticAttr ON flow.statistic.OriginalStatisticId = flow.StatisticAttr.rowid " +
                "WHERE flow.statistic.objectId = ?";

        final List<Integer> statisticIDs = new ArrayList<>();

        new SqlSelector(mgr.getSchema(), sqlStatistics, rowId).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                String name = rs.getString(1);
                int preferredId = rs.getInt(2);
                int originalId = rs.getInt(3);
                Double value = rs.getDouble(4);

                statisticIDs.add(preferredId);
                attrs.setStatistic(new StatisticSpec(name), value);
            }
        });

        if (statisticIDs.size() > 0)
        {
            String sqlStatisticAliaes = "SELECT A.name AS PreferredName, B.Name AS AliasName\n" +
                    "FROM flow.StatisticAttr A\n" +
                    "INNER JOIN flow.StatisticAttr B\n" +
                    "ON A.RowId = B.Id\n" +
                    "AND B.RowId != B.Id\n" +
                    "AND A.RowId IN (" + StringUtils.join(statisticIDs, ", ") + ")";

            new SqlSelector(mgr.getSchema(), sqlStatisticAliaes).forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    attrs.addStatisticAlias(new StatisticSpec(rs.getString(1)), new StatisticSpec(rs.getString(2)));
                }
            });
        }

        String sqlGraphs;

        if (!includeGraphBytes)
        {
            sqlGraphs = "SELECT flow.GraphAttr.name, flow.graph.GraphId, flow.graph.OriginalGraphId " +
                    "FROM flow.graph " +
                    "INNER JOIN flow.GraphAttr ON flow.graph.OriginalGraphId = flow.GraphAttr.rowid " +
                    "WHERE flow.graph.objectid = ?";
        }
        else
        {
            sqlGraphs = "SELECT flow.GraphAttr.name, flow.graph.GraphId, flow.graph.OriginalGraphId, flow.graph.data " +
                    "FROM flow.graph " +
                    "INNER JOIN flow.GraphAttr ON flow.graph.OriginalGraphId = flow.GraphAttr.rowid " +
                    "WHERE flow.graph.objectid = ?";
        }

        final List<Integer> graphIDs = new ArrayList<>();

        new SqlSelector(mgr.getSchema(), sqlGraphs, rowId).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                String name = rs.getString(1);
                int preferredId = rs.getInt(2);
                int originalId = rs.getInt(3);

                graphIDs.add(preferredId);
                if (!includeGraphBytes)
                {
                    attrs.setGraph(new GraphSpec(name), null);
                }
                else
                {
                    byte[] value = rs.getBytes(4);
                    attrs.setGraph(new GraphSpec(name), value);
                }

            }
        });

        if (graphIDs.size() > 0)
        {
            String sqlGraphAliaes = "SELECT A.name AS PreferredName, B.Name AS AliasName\n" +
                    "FROM flow.GraphAttr A\n" +
                    "INNER JOIN flow.GraphAttr B\n" +
                    "ON A.RowId = B.Id\n" +
                    "AND B.RowId != B.Id\n" +
                    "AND A.RowId IN (" + StringUtils.join(graphIDs, ", ") + ")";

            new SqlSelector(mgr.getSchema(), sqlGraphAliaes).forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    attrs.addGraphAlias(new GraphSpec(rs.getString(1)), new GraphSpec(rs.getString(2)));
                }
            });
        }
    }
}
