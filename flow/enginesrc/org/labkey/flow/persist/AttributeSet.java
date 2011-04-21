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

import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.flowdata.xml.*;
import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.labkey.api.util.URIUtil;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.io.*;
import java.net.URI;

import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.analysis.model.CompensationMatrix;

public class AttributeSet implements Serializable
{

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

    public void setKeywords(Map<String, String> keywords)
    {
        _keywords = new TreeMap();
        _keywords.putAll(keywords);
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
                if (StringUtils.isEmpty(entry.getKey()) || StringUtils.isEmpty(entry.getValue()))
                    continue;
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

    public void save(File file, DataBaseType dbt) throws Exception
    {
        dbt.setDataFileUrl(file.toURI().toString());
        OutputStream os = new FileOutputStream(file);
        save(os);
        os.close();
    }

    public void save(OutputStream os) throws Exception
    {
        String str = toXML().toString();
        os.write(str.getBytes("UTF-8")); 
    }

    public Map<String, String> getKeywords()
    {
        if (_keywords == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_keywords);
    }

    public Set<String> getKeywordNames()
    {
        if (_keywords == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(_keywords.keySet());
    }

    public Map<StatisticSpec, Double> getStatistics()
    {
        if (_statistics == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_statistics);
    }

    public Set<StatisticSpec> getStatisticNames()
    {
        if (_statistics == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(_statistics.keySet());
    }

    public Map<GraphSpec, byte[]> getGraphs()
    {
        if (_graphs == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_graphs);
    }

    public Set<GraphSpec> getGraphNames()
    {
        if (_graphs == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(_graphs.keySet());
    }

    public ObjectType getType()
    {
        return _type;
    }

    public URI getURI()
    {
        return _uri;
    }

    public void setURI(URI uri)
    {
        _uri = uri;
    }
    
    public void relativizeURI(URI uriPipelineRoot)
    {
        if (uriPipelineRoot == null || _uri == null || !_uri.isAbsolute())
            return;
        _uri = URIUtil.relativize(uriPipelineRoot, _uri);
    }
}
