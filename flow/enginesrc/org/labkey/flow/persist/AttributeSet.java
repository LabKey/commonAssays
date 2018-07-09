/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveTreeMap;
import org.labkey.api.util.StringUtilsLabKey;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class AttributeSet implements Serializable
{
    private static final Logger LOG = Logger.getLogger(AttributeSet.class);

    ObjectType _type;
    URI _uri;

    SortedMap<String, String> _keywords;
    SortedMap<String, Set<String>> _keywordAliases;
    // mapping from case-insensitive keyword name to canonical casing (the first added)
    CaseInsensitiveTreeMap<String> _keywordNames;

    SortedMap<StatisticSpec, Double> _statistics;
    SortedMap<StatisticSpec, Set<StatisticSpec>> _statisticAliases;
    // mapping from case-insensitive statistic name to canonical casing (the first added)
    CaseInsensitiveTreeMap<StatisticSpec> _statisticNames;

    SortedMap<GraphSpec, byte[]> _graphs;
    SortedMap<GraphSpec, Set<GraphSpec>> _graphAliases;
    // mapping from case-insensitive graph name to canonical casing (the first added)
    CaseInsensitiveTreeMap<GraphSpec> _graphNames;


    public AttributeSet(ObjectType type, URI uri)
    {
        _type = type;
        _uri = uri;
    }

    public AttributeSet(FlowData data, URI uri)
    {
        this(ObjectType.valueOf(data.getType()), uri);
        FlowData.Keywords keywords = data.getKeywords();
        if (keywords != null)
        {
            initKeywordMaps();
            _keywordAliases = new CaseInsensitiveTreeMap<>();
            for (Keyword keyword : keywords.getKeywordArray())
            {
                String name = keyword.getName();
                setKeyword(name, keyword.getValue());
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
            initStatisticMaps();
            _statisticAliases = new TreeMap<>(StatisticSpec.CASE_INSENSITIVE_ORDER);
            for (Statistic statistic : statistics.getStatisticArray())
            {
                StatisticSpec spec = new StatisticSpec(statistic.getName());
                setStatistic(spec, statistic.getValue());
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
            initGraphMaps();
            _graphAliases = new TreeMap<>(GraphSpec.CASE_INSENSITIVE_ORDER);
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
        for (String keyword : data.getKeywordNames())
        {
            setKeyword(keyword, data.getKeyword(keyword));
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

    private void initKeywordMaps()
    {
        _keywords = new CaseInsensitiveTreeMap<>();
        _keywordNames = new CaseInsensitiveTreeMap<>();
    }

    public void setKeyword(@NotNull String keyword, @Nullable String value)
    {
        if (_keywords == null)
            initKeywordMaps();

        if (StringUtils.isBlank(value))
            value = null;

        String existing = _keywordNames.put(keyword, keyword);
        if (existing != null && !existing.equals(keyword))
            LOG.warn("Duplicate keyword '" + existing + "' and '" + keyword + "'");

        _keywords.put(keyword, value);
    }

    public void setKeywords(Map<String, String> keywords)
    {
        initKeywordMaps();
        for (Map.Entry<String, String> entry : keywords.entrySet())
            setKeyword(entry.getKey(), entry.getValue());
    }

    public void addKeywordAlias(String spec, String alias)
    {
        if (_keywordAliases == null)
            _keywordAliases = new CaseInsensitiveTreeMap<>();

        Set<String> aliases = _keywordAliases.computeIfAbsent(spec, k -> new LinkedHashSet<>());
        aliases.add(alias);
    }

    public Collection<String> getKeywordAliases(String spec)
    {
        if (_keywordAliases == null)
            return Collections.emptyList();

        Set<String> aliases = _keywordAliases.get(spec);
        if (aliases == null || aliases.size() == 0)
            return Collections.emptyList();

        return Collections.unmodifiableCollection(aliases);
    }

    private void initStatisticMaps()
    {
        _statistics = new TreeMap<>(StatisticSpec.CASE_INSENSITIVE_ORDER);
        _statisticNames = new CaseInsensitiveTreeMap<>();
    }

    public void setStatistic(StatisticSpec stat, double value)
    {
        if (_statistics == null)
            initStatisticMaps();

        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            _statistics.remove(stat);
            _statisticNames.remove(stat.toString());
        }
        else
        {
            StatisticSpec existing = _statisticNames.put(stat.toString(), stat);
            if (existing != null && !existing.toString().equals(stat.toString()))
                LOG.warn("Duplicate statistic '" + existing + "' and '" + stat.toString() + "'");
            _statistics.put(stat, value);
        }
    }

    public void addStatisticAlias(StatisticSpec spec, StatisticSpec alias)
    {
        if (_statisticAliases == null)
            _statisticAliases = new TreeMap<>(StatisticSpec.CASE_INSENSITIVE_ORDER);

        Set<StatisticSpec> aliases = _statisticAliases.computeIfAbsent(spec, k -> new LinkedHashSet<>());
        aliases.add(alias);
    }

    public Collection<StatisticSpec> getStatisticAliases(StatisticSpec spec)
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

    private void initGraphMaps()
    {
        _graphs = new TreeMap<>(GraphSpec.CASE_INSENSITIVE_ORDER);
        _graphNames = new CaseInsensitiveTreeMap<>();
    }

    public void setGraph(GraphSpec graph, byte[] data)
    {
        if (_graphs == null)
            initGraphMaps();

        GraphSpec existing = _graphNames.put(graph.toString(), graph);
        if (existing != null && !existing.toString().equals(graph.toString()))
            LOG.warn("Duplicate graph '" + existing + "' and '" + graph.toString() + "'");

        _graphs.put(graph, data);
    }

    public void setGraphs(Map<GraphSpec, byte[]> graphs)
    {
        initGraphMaps();
        for (Map.Entry<GraphSpec, byte[]> entry : graphs.entrySet())
            setGraph(entry.getKey(), entry.getValue());
    }

    public void addGraphAlias(GraphSpec spec, GraphSpec alias)
    {
        if (_graphAliases == null)
            _graphAliases = new TreeMap<>(GraphSpec.CASE_INSENSITIVE_ORDER);

        Set<GraphSpec> aliases = _graphAliases.computeIfAbsent(spec, k -> new LinkedHashSet<>());
        aliases.add(alias);
    }

    public Collection<GraphSpec> getGraphAliases(GraphSpec spec)
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
        os.write(str.getBytes(StringUtilsLabKey.DEFAULT_CHARSET));
    }

    /** Get keywords and values.  Does not include keyword aliases. */
    public Map<String, String> getKeywords()
    {
        if (_keywords == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(_keywords);
    }

    /** Get set of keyword names including any keyword names that have been aliased, but not explicitly added with a value. */
    public Set<String> getKeywordNames()
    {
        Set<String> names = _getKeywordNames();
        if (names.isEmpty())
            return Collections.emptySet();

        return Collections.unmodifiableSet(names);
    }

    private Set<String> _getKeywordNames()
    {
        if (_keywords == null && _keywordAliases == null)
            return Collections.emptySet();

        Set<String> keywordNames = new TreeSet<>();
        if (_keywords != null)
            keywordNames.addAll(_keywords.keySet());
        if (_keywordAliases != null)
            keywordNames.addAll(_keywordAliases.keySet());

        return keywordNames;
    }

    /** Get the set of primary names that are not aliases. */
    public Set<String> getKeywordNamesWithoutAliases()
    {
        return getNamesWithoutAliases(_getKeywordNames(), _keywordAliases);
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

    /** Get set of stat names including any stat names that have been aliased, but not explicitly added with a value. */
    public Set<StatisticSpec> getStatisticNames()
    {
        Set<StatisticSpec> names = _getStatisticNames();
        if (names.isEmpty())
            return Collections.emptySet();

        return Collections.unmodifiableSet(names);
    }

    public Set<StatisticSpec> _getStatisticNames()
    {
        if (_statistics == null && _statisticAliases == null)
            return Collections.emptySet();

        Set<StatisticSpec> statisticNames = new TreeSet<>(StatisticSpec.CASE_INSENSITIVE_ORDER);
        if (_statistics != null)
            statisticNames.addAll(_statistics.keySet());
        if (_statisticAliases != null)
            statisticNames.addAll(_statisticAliases.keySet());

        return statisticNames;
    }

    /** Get the set of primary names that are not aliases. */
    public Set<StatisticSpec> getStatisticNamesWithoutAliases()
    {
        return getNamesWithoutAliases(_getStatisticNames(), _statisticAliases);
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

    /** Get set of graph names including any graph names that have been aliased, but not explicitly added with a value. */
    public Set<GraphSpec> getGraphNames()
    {
        Set<GraphSpec> names = _getGraphNames();
        if (names.isEmpty())
            return Collections.emptySet();

        return Collections.unmodifiableSet(names);
    }

    private Set<GraphSpec> _getGraphNames()
    {
        if (_graphs == null && _graphAliases == null)
            return Collections.emptySet();

        Set<GraphSpec> graphNames = new TreeSet<>(GraphSpec.CASE_INSENSITIVE_ORDER);
        if (_graphs != null)
            graphNames.addAll(_graphs.keySet());
        if (_graphAliases != null)
            graphNames.addAll(_graphAliases.keySet());

        return graphNames;
    }

    /** Get the set of primary names that are not aliases. */
    public Set<GraphSpec> getGraphNamesWithoutAliases()
    {
        return getNamesWithoutAliases(_getGraphNames(), _graphAliases);
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

    private <T> Set<T> getNamesWithoutAliases(Set<T> allNamesIncludingAliases, Map<T, Set<T>> aliases)
    {
        if (allNamesIncludingAliases == null || allNamesIncludingAliases.isEmpty())
            return Collections.emptySet();

        if (aliases != null && !aliases.isEmpty())
        {
            for (Set<T> set : aliases.values())
                allNamesIncludingAliases.removeAll(set);
        }

        return Collections.unmodifiableSet(allNamesIncludingAliases);
    }


}
