package org.labkey.flow.persist;

import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.query.AttributeCache;

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
     * Called outside of any transaction, ensures that the necessary entries have been added to the flow.attribute
     * table.  That way, we never have to deal with transactions being rolled back and having to remove attribute
     * names from the cache, or two threads each trying to insert the same attribute name.
     * @throws SQLException
     */
    public static void prepareForSave(AttributeSet attrs, Container c) throws SQLException
    {
        ensureKeywordIds(c, attrs.getKeywordNames());
        ensureStatisticIds(c, attrs.getStatisticNames());
        ensureGraphIds(c, attrs.getGraphNames());
    }

    private static void ensureKeywordIds(Container c, Collection<? extends Object> ids) throws SQLException
    {
        for (Object id : ids)
        {
            FlowManager.get().ensureKeywordId(c, id.toString());
        }
    }

    private static void ensureStatisticIds(Container c, Collection<? extends Object> ids) throws SQLException
    {
        for (Object id : ids)
        {
            FlowManager.get().ensureStatisticId(c, id.toString());
        }
    }

    private static void ensureGraphIds(Container c, Collection<? extends Object> ids) throws SQLException
    {
        for (Object id : ids)
        {
            FlowManager.get().ensureGraphId(c, id.toString());
        }
    }

    public static void save(AttributeSet attrs, User user, ExpData data) throws SQLException
    {
        prepareForSave(attrs, data.getContainer());
        doSave(attrs, user, data);
    }

    public static void doSave(AttributeSet attrs, User user, ExpData data) throws SQLException
    {
        FlowManager mgr = FlowManager.get();
        boolean fTransaction = false;
        try
        {
            if (!mgr.getSchema().getScope().isTransactionActive())
            {
                mgr.getSchema().getScope().beginTransaction();
                fTransaction = true;
            }
            AttrObject obj = mgr.createAttrObject(data, attrs.getType(), attrs.getURI());
            Map<String, String> keywords = attrs.getKeywords();
            if (!keywords.isEmpty())
            {
                String sql = "INSERT INTO " + mgr.getTinfoKeyword() + " (ObjectId, KeywordId, Value) VALUES (?,?,?)";
                List<List<?>> paramsList = new ArrayList<List<?>>();
                for (Map.Entry<String, String> entry : keywords.entrySet())
                {
                    paramsList.add(Arrays.asList(obj.getRowId(), mgr.getAttributeId(entry.getKey()), entry.getValue()));
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }

            Map<StatisticSpec, Double> statistics = attrs.getStatistics();
            if (!statistics.isEmpty())
            {
                String sql = "INSERT INTO " + mgr.getTinfoStatistic() + " (ObjectId, StatisticId, Value) VALUES (?,?,?)";
                List<List<?>> paramsList = new ArrayList<List<?>>();
                for (Map.Entry<StatisticSpec, Double> entry : statistics.entrySet())
                {
                    paramsList.add(Arrays.<Object>asList(obj.getRowId(), mgr.getAttributeId(entry.getKey().toString()), entry.getValue()));
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }

            Map<GraphSpec, byte[]> graphs = attrs.getGraphs();
            if (!graphs.isEmpty())
            {
                String sql = "INSERT INTO " + mgr.getTinfoGraph() + " (ObjectId, GraphId, Data) VALUES (?, ?, ?)";
                List<List<?>> paramsList = new ArrayList<List<?>>();
                for (Map.Entry<GraphSpec, byte[]> entry : graphs.entrySet())
                {
                    paramsList.add(Arrays.asList(obj.getRowId(), mgr.getAttributeId(entry.getKey().toString()), entry.getValue()));
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }
            if (fTransaction)
            {
                mgr.getSchema().getScope().commitTransaction();
                fTransaction = false;
            }
        }
        finally
        {
            if (fTransaction)
                mgr.getSchema().getScope().rollbackTransaction();
            AttributeCache.invalidateCache(data.getContainer());
        }

    }

    private static void loadFromDb(AttributeSet attrs, AttrObject obj, boolean includeGraphBytes) throws SQLException
    {
        FlowManager mgr = FlowManager.get();
        Object[] params = new Object[] { obj.getRowId() };

        String sqlKeywords = "SELECT flow.attribute.name, flow.keyword.value FROM flow.keyword INNER JOIN flow.attribute on flow.keyword.keywordid = flow.attribute.rowid WHERE flow.keyword.objectId = ?";
        ResultSet rsKeywords = Table.executeQuery(mgr.getSchema(), sqlKeywords, params);
        Map<String, String> keywords = new TreeMap();
        while (rsKeywords.next())
        {
            keywords.put(rsKeywords.getString(1), rsKeywords.getString(2));
        }
        rsKeywords.close();
        attrs.setKeywords(keywords);

        String sqlStatistics = "SELECT flow.attribute.name, flow.statistic.value FROM flow.statistic INNER JOIN flow.attribute on flow.statistic.statisticid = flow.attribute.rowid WHERE flow.statistic.objectId = ?";
        ResultSet rsStatistics = Table.executeQuery(mgr.getSchema(), sqlStatistics, params);
        while (rsStatistics.next())
        {
            attrs.setStatistic(new StatisticSpec(rsStatistics.getString(1)), rsStatistics.getDouble(2));
        }
        rsStatistics.close();


        ResultSet rsGraphs = null;
        try
        {
            if (!includeGraphBytes)
            {
                String sqlGraphs = "SELECT flow.attribute.name FROM flow.graph INNER JOIN flow.attribute on flow.graph.graphid = flow.attribute.rowid WHERE flow.graph.objectid = ?";
                rsGraphs = Table.executeQuery(mgr.getSchema(), sqlGraphs, params);
            }
            else
            {
                String sqlGraphs = "SELECT flow.attribute.name, flow.graph.data FROM flow.graph INNER JOIN flow.attribute ON flow.graph.graphid = flow.attribute.rowid WHERE flow.graph.objectid = ?";
                rsGraphs = Table.executeQuery(mgr.getSchema(), sqlGraphs, params);
            }
            while (rsGraphs.next())
            {
                if (!includeGraphBytes)
                {
                    attrs.setGraph(new GraphSpec(rsGraphs.getString(1)), null);
                }
                else
                {
                    attrs.setGraph(new GraphSpec(rsGraphs.getString(1)), rsGraphs.getBytes(2));
                }
            }
        }
        finally
        {
            if (rsGraphs != null) try { rsGraphs.close(); } catch (SQLException e) {}
        }
    }

}
