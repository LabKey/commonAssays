/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.labkey.api.util.URIUtil;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.flowdata.xml.Aliases;
import org.labkey.flow.flowdata.xml.FlowData;
import org.labkey.flow.flowdata.xml.FlowdataDocument;
import org.labkey.flow.flowdata.xml.Graph;
import org.labkey.flow.flowdata.xml.Keyword;
import org.labkey.flow.flowdata.xml.Statistic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class AttributeSet implements Serializable
{

    ObjectType _type;
    URI _uri;
    SortedMap<String, String> _keywords;
    SortedMap<StatisticSpec, Double> _statistics;
    SortedMap<GraphSpec, byte[]> _graphs;

    SortedMap<String, Set<String>> _keywordAliases;
    SortedMap<StatisticSpec, Set<StatisticSpec>> _statisticAliases;
    SortedMap<GraphSpec, Set<GraphSpec>> _graphAliases;

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
            _keywords = new TreeMap<>();
            _keywordAliases = new TreeMap<>();
            for (Keyword keyword : keywords.getKeywordArray())
            {
                String name = keyword.getName();
                _keywords.put(name, keyword.getValue());
                if (keyword.isSetAliases())
                {
                    Set<String> aliases = new LinkedHashSet<>();
                    _keywordAliases.put(name, aliases);
                    for (String alias : keyword.getAliases().getAliasArray())
                        aliases.add(alias);
                }
            }
        }

        FlowData.Statistics statistics = data.getStatistics();
        if (statistics != null)
        {
            _statistics = new TreeMap<>();
            _statisticAliases = new TreeMap<>();
            for (Statistic statistic : statistics.getStatisticArray())
            {
                StatisticSpec spec = new StatisticSpec(statistic.getName());
                _statistics.put(spec, statistic.getValue());
                if (statistic.isSetAliases())
                {
                    Set<StatisticSpec> aliases = new LinkedHashSet<>();
                    _statisticAliases.put(spec, aliases);
                    for (String alias : statistic.getAliases().getAliasArray())
                        aliases.add(new StatisticSpec(alias));
                }
            }
        }

        FlowData.Graphs graphs = data.getGraphs();
        if (graphs != null)
        {
            _graphs = new TreeMap<>();
            _graphAliases = new TreeMap<>();
            for (Graph graph : graphs.getGraphArray())
            {
                GraphSpec spec = new GraphSpec(graph.getName());
                _graphs.put(spec, graph.getData());
                if (graph.isSetAliases())
                {
                    Set<GraphSpec> aliases = new LinkedHashSet<>();
                    _graphAliases.put(spec, aliases);
                    for (String alias : graph.getAliases().getAliasArray())
                        aliases.add(new GraphSpec(alias));
                }
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
                if (StringUtils.isEmpty(entry.getKey()) || StringUtils.isEmpty(entry.getValue()))
                    continue;
                Keyword keyword = keywords.addNewKeyword();
                keyword.setName(entry.getKey());
                keyword.setValue(entry.getValue());

                if (_keywordAliases != null && _keywordAliases.containsKey(entry.getKey()))
                {
                    Aliases aliases = keyword.addNewAliases();
                    addAliases(aliases, getKeywordAliases(entry.getKey()));
                }
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

                if (_statisticAliases != null && _statisticAliases.containsKey(entry.getKey()))
                {
                    Aliases aliases = statistic.addNewAliases();
                    addAliases(aliases, getStatisticAliases(entry.getKey()));
                }
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

                if (_graphAliases != null && _graphAliases.containsKey(entry.getKey()))
                {
                    Aliases aliases = graph.addNewAliases();
                    addAliases(aliases, getGraphAliases(entry.getKey()));
                }
            }
        }
        return ret;
    }

    private <T> void addAliases(Aliases aliasesXb, Iterable<T> aliases)
    {
        if (aliases == null)
            return;

        for (T alias : aliases)
            aliasesXb.addAlias(alias.toString());
    }

    public void setKeyword(String keyword, String value)
    {
        if (_keywords == null)
            _keywords = new TreeMap();
        if (StringUtils.isEmpty(value))
        {
            _keywords.remove(keyword);
        }
        else
        {
            _keywords.put(keyword, value);
        }
    }

    public void setKeywords(Map<String, String> keywords)
    {
        _keywords = new TreeMap();
        _keywords.putAll(keywords);
    }

    public void addKeywordAlias(String spec, String alias)
    {
        if (_keywordAliases == null)
            _keywordAliases = new TreeMap<>();

        Set<String> aliases = _keywordAliases.get(spec);
        if (aliases == null)
            _keywordAliases.put(spec, aliases = new LinkedHashSet<>());
        aliases.add(alias);
    }

    public void setKeywordAliases(Map<String, Set<String>> keywordAliases)
    {
        _keywordAliases = new TreeMap<>();

        for (String keyword : keywordAliases.keySet())
        {
            Set<String> aliases = keywordAliases.get(keyword);
            _keywordAliases.put(keyword, new LinkedHashSet<>(aliases));
        }
    }

    public Iterable<String> getKeywordAliases(String spec)
    {
        if (_keywordAliases == null)
            return Collections.emptyList();

        Set<String> aliases = _keywordAliases.get(spec);
        if (aliases == null || aliases.size() == 0)
            return Collections.emptyList();

        return Collections.unmodifiableCollection(aliases);
    }

    public void setStatistic(StatisticSpec stat, double value)
    {
        if (_statistics == null)
            _statistics = new TreeMap();
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            _statistics.remove(stat);
        }
        else
        {
            _statistics.put(stat, value);
        }
    }

    public void setStatistics(Map<StatisticSpec, Double> statistics)
    {
        _statistics = new TreeMap<>();
        _statistics.putAll(statistics);
    }

    public void addStatisticAlias(StatisticSpec spec, StatisticSpec alias)
    {
        if (_statisticAliases == null)
            _statisticAliases = new TreeMap<>();

        Set<StatisticSpec> aliases = _statisticAliases.get(spec);
        if (aliases == null)
            _statisticAliases.put(spec, aliases = new LinkedHashSet<>());
        aliases.add(alias);
    }

    public void setStatisticAliases(Map<StatisticSpec, Set<StatisticSpec>> statisticAliases)
    {
        _statisticAliases = new TreeMap<>();

        for (StatisticSpec statisticSpec : statisticAliases.keySet())
        {
            Set<StatisticSpec> aliases = statisticAliases.get(statisticSpec);
            _statisticAliases.put(statisticSpec, new LinkedHashSet<>(aliases));
        }
    }

    public Iterable<StatisticSpec> getStatisticAliases(StatisticSpec spec)
    {
        if (_statisticAliases == null)
            return Collections.emptyList();

        Set<StatisticSpec> aliases = _statisticAliases.get(spec);
        if (aliases == null || aliases.size() == 0)
            return Collections.emptyList();

        //ArrayList<String> strings = new ArrayList<String>(aliases.size());
        //for (StatisticSpec alias : aliases)
        //    strings.add(alias.toString());
        return Collections.unmodifiableCollection(aliases);
    }

    public void setGraph(GraphSpec graph, byte[] data)
    {
        if (_graphs == null)
            _graphs = new TreeMap();
        _graphs.put(graph, data);
    }

    public void setGraphs(Map<GraphSpec, byte[]> graphs)
    {
        _graphs = new TreeMap<>();
        _graphs.putAll(graphs);
    }

    public void addGraphAlias(GraphSpec spec, GraphSpec alias)
    {
        if (_graphAliases == null)
            _graphAliases = new TreeMap<>();

        Set<GraphSpec> aliases = _graphAliases.get(spec);
        if (aliases == null)
            _graphAliases.put(spec, aliases = new LinkedHashSet<>());
        aliases.add(alias);
    }

    public void setGraphAliases(Map<GraphSpec, Set<GraphSpec>> graphAliases)
    {
        _graphAliases = new TreeMap<>();

        for (GraphSpec graphSpec : graphAliases.keySet())
        {
            Set<GraphSpec> aliases = graphAliases.get(graphSpec);
            _graphAliases.put(graphSpec, new LinkedHashSet<>(aliases));
        }
    }

    public Iterable<GraphSpec> getGraphAliases(GraphSpec spec)
    {
        if (_graphAliases == null)
            return Collections.emptyList();

        Set<GraphSpec> aliases = _graphAliases.get(spec);
        if (aliases == null || aliases.size() == 0)
            return Collections.emptyList();

        //ArrayList<String> strings = new ArrayList<String>(aliases.size());
        //for (GraphSpec alias : aliases)
        //    strings.add(alias.toString());
        return Collections.unmodifiableCollection(aliases);
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

    /** Get keywords and values.  Does not include keyword aliases. */
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

    public Map<String, Set<String>> getKeywordAliases()
    {
        if (_keywordAliases == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_keywordAliases);
    }

    /** Get statistic specs and values.  Does not include statistic aliases. */
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

    public Map<StatisticSpec, Set<StatisticSpec>> getStatisticAliases()
    {
        if (_statisticAliases == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_statisticAliases);
    }

    /** Get graph specs and values.  Does not include graph aliases. */
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

    public Map<GraphSpec, Set<GraphSpec>> getGraphAliases()
    {
        if (_graphAliases == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_graphAliases);
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
