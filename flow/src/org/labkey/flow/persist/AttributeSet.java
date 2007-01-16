package org.labkey.flow.persist;

import org.labkey.flow.flowdata.xml.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.labkey.api.security.User;
import org.labkey.api.data.Table;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.XMLUtil;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlCursor;
import org.w3c.dom.Node;

import java.util.*;
import java.io.*;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.net.URI;
import java.net.URISyntaxException;

import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

public class AttributeSet
{
    static public AttributeSet fromData(ExpData data)
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
            ret.loadFromDb(obj);
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

    ObjectType _type;
    URI _uri;
    SortedMap<String, String> _keywords;
    SortedMap<StatisticSpec, Double> _statistics;
    SortedMap<GraphSpec, byte[]> _graphs;

    public AttributeSet(ObjectType type, URI uri)
    {
        _type = type;
        _uri = uri;
    }

    public AttributeSet(AttrObject obj)
    {
        _type = ObjectType.fromTypeId(obj.getTypeId());
        try
        {
            _uri = new URI(obj.getUri());
        }
        catch (URISyntaxException use)
        {
            throw UnexpectedException.wrap(use);
        }

    }

    public AttributeSet(FlowData data, URI uri) throws Exception
    {
        this(ObjectType.valueOf(data.getType()), uri);
        FlowData.Keywords keywords = data.getKeywords();
        if (keywords != null)
        {
            _keywords = new TreeMap();
            for (Keyword keyword : keywords.getKeywordArray())
            {
                _keywords.put(keyword.getName(), keyword.getValue());
            }
        }
        FlowData.Statistics statistics = data.getStatistics();
        if (statistics != null)
        {
            _statistics = new TreeMap();
            for (Statistic statistic : statistics.getStatisticArray())
            {
                _statistics.put(new StatisticSpec(statistic.getName()), statistic.getValue());
            }
        }
        FlowData.Graphs graphs = data.getGraphs();
        if (graphs != null)
        {
            _graphs = new TreeMap();
            for (Graph graph : graphs.getGraphArray())
            {
                _graphs.put(new GraphSpec(graph.getName()), graph.getData());
            }
        }
    }

    public AttributeSet(CompensationMatrix matrix)
    {
        this(ObjectType.compensationMatrix, null);
        String[] channelNames = matrix.getChannelNames();
        for (int iChannel = 0; iChannel < channelNames.length; iChannel ++)
        {
            String strChannel = channelNames[iChannel];
            for (int iChannelValue = 0; iChannelValue < channelNames.length; iChannelValue ++)
            {
                String strChannelValue = channelNames[iChannelValue];
                StatisticSpec spec = new StatisticSpec(null, StatisticSpec.STAT.Spill, strChannel + ":" + strChannelValue);
                setStatistic(spec, matrix.getRow(iChannel)[iChannelValue]);
            }
        }

    }

    public AttributeSet(FCSKeywordData data)
    {
        this(ObjectType.fcsKeywords, data.getURI());
        _keywords = new TreeMap();
        for (String keyword : data.getKeywordNames())
        {
            _keywords.put(keyword, data.getKeyword(keyword));
        }
    }

    public FlowdataDocument toXML()
    {
        FlowdataDocument ret = FlowdataDocument.Factory.newInstance();
        FlowData root = ret.addNewFlowdata();
        if (_uri != null)
            root.setUri(_uri.toString());
        root.setType(_type.toString());
        if (_keywords != null)
        {
            FlowData.Keywords keywords = root.addNewKeywords();
            for (Map.Entry<String, String> entry : _keywords.entrySet())
            {
                Keyword keyword = keywords.addNewKeyword();
                keyword.setName(entry.getKey());
                keyword.setValue(entry.getValue());
            }
        }
        if (_statistics != null)
        {
            FlowData.Statistics statistics = root.addNewStatistics();
            for (Map.Entry<StatisticSpec, Double> entry : _statistics.entrySet())
            {
                Statistic statistic = statistics.addNewStatistic();
                statistic.setName(entry.getKey().toString());
                statistic.setValue(entry.getValue());
            }
        }
        if (_graphs != null)
        {
            FlowData.Graphs graphs = root.addNewGraphs();
            for (Map.Entry<GraphSpec, byte[]> entry : _graphs.entrySet())
            {
                Graph graph = graphs.addNewGraph();
                graph.setName(entry.getKey().toString());
                graph.setData(entry.getValue());
            }
        }
        return ret;
    }

    public void setStatistic(StatisticSpec stat, double value)
    {
        if (_statistics == null)
        {
            _statistics = new TreeMap();
        }
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            _statistics.remove(stat);
        }
        else
        {
            _statistics.put(stat, value);
        }
    }

    public void setGraph(GraphSpec graph, byte[] data)
    {
        if (_graphs == null)
        {
            _graphs = new TreeMap();
        }
        _graphs.put(graph, data);
    }

    public void prepareForSave() throws SQLException
    {
        if (_keywords != null)
        {
            ensureIds(_keywords.keySet());
        }
        if (_statistics != null)
        {
            ensureIds(_statistics.keySet());
        }
        if (_graphs != null)
        {
            ensureIds(_graphs.keySet());
        }
    }

    public void ensureIds(Collection<? extends Object> ids) throws SQLException
    {
        for (Object id : ids)
        {
            FlowManager.get().ensureAttributeId(id.toString());
        }
    }

    public void doSave(User user, ExpData data) throws SQLException
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
            AttrObject obj = mgr.createAttrObject(data, _type, _uri);
            if (_keywords != null)
            {
                String sql = "INSERT INTO " + mgr.getTinfoKeyword() + " (ObjectId, KeywordId, Value) VALUES (?,?,?)";
                List<Object[]> paramsList = new ArrayList();
                for (Map.Entry<String, String> entry : _keywords.entrySet())
                {
                    paramsList.add(new Object[] { obj.getRowId(), mgr.getAttributeId(entry.getKey()), entry.getValue()});
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }
            if (_statistics != null)
            {
                String sql = "INSERT INTO " + mgr.getTinfoStatistic() + " (ObjectId, StatisticId, Value) VALUES (?,?,?)";
                List<Object[]> paramsList = new ArrayList();
                for (Map.Entry<StatisticSpec, Double> entry : _statistics.entrySet())
                {
                    paramsList.add(new Object[] { obj.getRowId(), mgr.getAttributeId(entry.getKey().toString()), entry.getValue() });
                }
                Table.batchExecute(mgr.getSchema(), sql, paramsList);
            }
            if (_graphs != null)
            {
                String sql = "INSERT INTO " + mgr.getTinfoGraph() + " (ObjectId, GraphId, Data) VALUES (?, ?, ?)";
                List<Object[]> paramsList = new ArrayList();
                for (Map.Entry<GraphSpec, byte[]> entry : _graphs.entrySet())
                {
                    paramsList.add(new Object[] { obj.getRowId(), mgr.getAttributeId(entry.getKey().toString()), entry.getValue()});
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
        }

    }

    public void save(User user, ExpData data) throws SQLException
    {
        prepareForSave();
        doSave(user, data);
    }
    public void save(File file, DataBaseType dbt) throws Exception
    {
        dbt.setDataFileUrl(file.toURI().toString());
        OutputStream os = new FileOutputStream(file);
        save(os);
        os.close();
    }

    public void save(OutputStream os) throws Exception
    {
        String str = XMLUtil.toString(toXML());
        os.write(str.getBytes("UTF-8")); 
    }

    private void loadFromDb(AttrObject obj) throws SQLException
    {
        FlowManager mgr = FlowManager.get();
        Object[] params = new Object[] { obj.getRowId() };
        String sqlKeywords = "SELECT flow.attribute.name, flow.keyword.value FROM flow.keyword INNER JOIN flow.attribute on flow.keyword.keywordid = flow.attribute.rowid WHERE flow.keyword.objectId = ?";
        ResultSet rsKeywords = Table.executeQuery(mgr.getSchema(), sqlKeywords, params);
        _keywords = new TreeMap();
        while (rsKeywords.next())
        {
            _keywords.put(rsKeywords.getString(1), rsKeywords.getString(2));
        }
        rsKeywords.close();
        String sqlStatistics = "SELECT flow.attribute.name, flow.statistic.value FROM flow.statistic INNER JOIN flow.attribute on flow.statistic.statisticid = flow.attribute.rowid WHERE flow.statistic.objectId = ?";
        _statistics = new TreeMap();
        ResultSet rsStatistics = Table.executeQuery(mgr.getSchema(), sqlStatistics, params);
        while (rsStatistics.next())
        {
            _statistics.put(new StatisticSpec(rsStatistics.getString(1)), rsStatistics.getDouble(2));
        }
        rsStatistics.close();
        String sqlGraphs = "SELECT flow.attribute.name FROM flow.graph INNER JOIN flow.attribute on flow.graph.graphid = flow.attribute.rowid WHERE flow.graph.objectid = ?";
        ResultSet rsGraphs = null;
        try
        {
            rsGraphs = Table.executeQuery(mgr.getSchema(), sqlGraphs, params);
            _graphs = new TreeMap();
            while (rsGraphs.next())
            {
                _graphs.put(new GraphSpec(rsGraphs.getString(1)), null);
            }
        }
        finally
        {
            if (rsGraphs != null) try { rsGraphs.close(); } catch (SQLException e) {}
        }
    }

    public Map<String, String> getKeywords()
    {
        return Collections.unmodifiableMap(_keywords);
    }
    public Map<StatisticSpec, Double> getStatistics()
    {
        return Collections.unmodifiableMap(_statistics);
    }
    public Set<GraphSpec> getGraphNames()
    {
        return Collections.unmodifiableSet(_graphs.keySet());
    }

    public URI getURI()
    {
        return _uri;
    }

    public void relativizeURI(URI uriPipelineRoot)
    {
        if (uriPipelineRoot == null || _uri == null || !_uri.isAbsolute())
            return;
        _uri = URIUtil.relativize(uriPipelineRoot, _uri);
    }
}
