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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingStringKeyCache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.util.MemTracker;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.persist.FlowManager.FlowEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cache of attribute names and aliases within a container.
 */
abstract public class AttributeCache<A extends Comparable<A>, E extends AttributeCache.Entry<A, E>>
{
    private static final Logger LOG = Logger.getLogger(AttributeCache.class);

    // container id -> list of names (sorted)
    private CacheLoader BY_CONTAINER_LOADER = new CacheLoader<String, List<E>>()
    {
        @Override
        public List<E> load(String containerId, @Nullable Object argument)
        {
            //LOG.info("+Loading " + _type + " by containerId: " + containerId);
            Collection<FlowEntry> entries = FlowManager.get().getAttributeEntries(containerId, _type);
            ArrayList<E> list = new ArrayList<>(entries.size());
            for (FlowEntry entry : entries)
            {
                // 'Load' by entry which will insert cache entries by rowid and by container+name
                list.add(byEntry(entry));
            }

            Collections.sort(list);

            //LOG.info("-Loaded " + _type + " by containerId: " + containerId);
            return Collections.unmodifiableList(list);
        }
    };

    // container id + name -> AttributeEntry
    private CacheLoader BY_CONTAINER_NAME_LOADER = new CacheLoader<String, E>()
    {
        @Override
        public E load(String key, @Nullable Object argument)
        {
            //LOG.info("+Loading " + _type + " by name: " + key);
            int colon = key.indexOf(":");
            String containerId = key.substring(0, colon);
            String name = key.substring(colon + 1);

            // Load from the cache by rowid
            FlowEntry flowEntry = FlowManager.get().getAttributeEntry(containerId, _type, name);
            if (flowEntry == null)
                return null;

            E entry = byRowId(flowEntry._rowId);
            //LOG.info("-Loaded " + _type + " by name: " + key);
            return entry;
        }
    };

    // rowid -> Entry
    private CacheLoader BY_ROWID_LOADER = new CacheLoader<String, E>()
    {
        @Override
        public E load(String key, @Nullable Object argument)
        {
            //LOG.info("+Loading " + _type + " by rowid: " + key);
            if (!key.startsWith("rowid:"))
                return null;

            String rowIdStr = key.substring("rowid:".length());
            int rowId = Integer.parseInt(rowIdStr);

            // Load from the database
            E entry = createEntry(FlowManager.get().getAttributeEntry(_type, rowId));
            //LOG.info("-Loaded " + _type + " by rowid: " + key);
            return entry;
        }
    };


    public static abstract class Entry<Q extends Comparable<Q>, Z extends Entry<Q, Z>> implements Comparable<Entry<Q, Z>>
    {
        private final AttributeType _type;
        private final int _rowId;
        private final String _containerId;
        private final String _name;
        private final Q _attribute;
        private final Integer _aliasedId;
        private final Collection<Integer> _aliasIds;

        protected Entry(@NotNull String containerId, @NotNull AttributeType type, int rowId, @NotNull String name, @NotNull Q attribute, @Nullable Integer aliasedId, @NotNull Collection<Integer> aliasIds)
        {
            _containerId = containerId;
            _type = type;
            _rowId = rowId;
            _name = name;
            _attribute = attribute;
            _aliasedId = aliasedId;
            _aliasIds = aliasIds;
            MemTracker.getInstance().put(this);
        }

        public AttributeType getType()
        {
            return _type;
        }

        public int getRowId()
        {
            return _rowId;
        }

        public String getContainerId()
        {
            return _containerId;
        }

        public Container getContainer()
        {
            return ContainerManager.getForId(_containerId);
        }

        public String getName()
        {
            return _name;
        }

        public Q getAttribute()
        {
            return _attribute;
        }

        @Override
        public int compareTo(@NotNull Entry<Q, Z> other)
        {
            return getAttribute().compareTo(other.getAttribute());
        }

        /** Get the rowid of the aliased attribute or null if this is the preferred attribute. */
        public Integer getAliasedId()
        {
            return _aliasedId;
        }

        /** Get the aliased entry or null if this is the preferred attribute. */
        public Z getAliasedEntry()
        {
            if (_aliasedId == null)
                return null;

            return (Z)AttributeCache.forType(_type).byRowId(_aliasedId);
        }

        /** Get the list of aliases for this attribute. */
        public Collection<Integer> getAliasIds()
        {
            return _aliasIds;
        }

        /** Get the list of aliases for this attribute. */
        public Collection<Z> getAliases()
        {
            if (_aliasIds.isEmpty())
                return Collections.emptyList();

            AttributeCache cache = AttributeCache.forType(_type);
            ArrayList<Z> entries = new ArrayList<>(_aliasIds.size());
            for (Integer id : _aliasIds)
            {
                Z entry = (Z)cache.byRowId(id);
                if (entry != null)
                    entries.add(entry);
            }

            return Collections.unmodifiableList(entries);
        }

        /** Get a list of usages of this attribute, excluding usages of this attribute's aliases. */
        public Collection<FlowDataObject> getUsages()
        {
            return FlowManager.get().getUsages(_type, _rowId);
        }

        /** Get a list of usages of this attribute, including usages of this attribute's aliases. */
        public Map<Z, Collection<FlowDataObject>> getAllUsages()
        {
            Map<Integer, Collection<FlowDataObject>> usagesMap = FlowManager.get().getAllUsages(_type, _rowId);
            Map<Z, Collection<FlowDataObject>> ret = new HashMap<>();

            // Include usages of this attribute
            Collection<FlowDataObject> thisUsages = usagesMap.get(getRowId());
            if (thisUsages == null)
                thisUsages = Collections.emptyList();
            ret.put((Z)this, thisUsages);

            // Include usages of all attribute aliases
            for (Z alias : getAliases())
            {
                Collection<FlowDataObject> usages = usagesMap.get(alias.getRowId());
                if (usages == null)
                    usages = Collections.emptyList();
                ret.put(alias, usages);
            }

            return ret;
        }
    }

    public static class KeywordEntry extends Entry<String, KeywordEntry>
    {
        protected KeywordEntry(@NotNull String containerId, int rowId, @NotNull String name, @Nullable Integer aliased, @NotNull Collection<Integer> aliases)
        {
            super(containerId, AttributeType.keyword, rowId, name, name, aliased, aliases);
        }

        @Override
        public String getAttribute()
        {
            return super.getAttribute();
        }

        @Override
        public KeywordEntry getAliasedEntry()
        {
            return super.getAliasedEntry();
        }
    }

    public static class StatisticEntry extends Entry<StatisticSpec, StatisticEntry>
    {
        protected StatisticEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull StatisticSpec spec, @Nullable Integer aliased, @NotNull Collection<Integer> aliases)
        {
            super(containerId, AttributeType.statistic, rowId, name, spec, aliased, aliases);
        }

        @Override
        public StatisticSpec getAttribute()
        {
            return super.getAttribute();
        }

        @Override
        public StatisticEntry getAliasedEntry()
        {
            return super.getAliasedEntry();
        }
    }

    public static class GraphEntry extends Entry<GraphSpec, GraphEntry>
    {
        protected GraphEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull GraphSpec spec, @Nullable Integer aliased, @NotNull Collection<Integer> aliases)
        {
            super(containerId, AttributeType.graph, rowId, name, spec, aliased, aliases);
        }

        @Override
        public GraphSpec getAttribute()
        {
            return super.getAttribute();
        }

        @Override
        public GraphEntry getAliasedEntry()
        {
            return super.getAliasedEntry();
        }
    }

    private final BlockingStringKeyCache<Object> _cache;
    private final AttributeType _type;

    public AttributeCache(AttributeType type)
    {
        _type = type;
        _cache = CacheManager.getBlockingStringKeyCache(CacheManager.UNLIMITED, CacheManager.DAY, "Flow " + _type + " cache", null);
    }

    @Nullable
    private E createEntry(@Nullable FlowEntry entry)
    {
        if (entry == null)
            return null;

        assert entry._type == this._type;

        Integer aliasId = entry.isAlias() ? entry._aliasId : null;

        Collection<Integer> aliases = Collections.unmodifiableCollection(FlowManager.get().getAliasIds(entry));

        A attribute = _createAttribute(entry._name);

        return _createEntry(entry._containerId, entry._rowId, entry._name, attribute, aliasId, aliases);
    }

    protected abstract E _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull A attribute, @Nullable Integer aliased, @NotNull Collection<Integer> aliases);

    protected abstract A _createAttribute(@NotNull String name);

    protected AttributeType type()
    {
        return _type;
    }


    private static class UncacheTask implements Runnable
    {
        private String _prefix;

        UncacheTask(String prefix)
        {
            _prefix = prefix;
        }

        @Override
        public void run()
        {
            _uncacheAll(_prefix);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UncacheTask that = (UncacheTask) o;
            return Objects.equals(_prefix, that._prefix);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(_prefix);
        }
    }

    public static void uncacheAllAfterCommit(Container c)
    {
        FlowManager mgr = FlowManager.get();
        DbScope.Transaction t = mgr.getSchema().getScope().getCurrentTransaction();
        if (t != null)
        {
            t.addCommitTask(new AttributeCache.UncacheTask(c.getId()), DbScope.CommitTaskOption.POSTCOMMIT);
        }
        else
        {
            _uncacheAll(c.getId());
        }
    }

    private static void _uncacheAll(String prefix)
    {
        //LOG.info("+Uncache all: " + (prefix == null ? "entire world" : "container='" + prefix + "'"));
        KEYWORDS.uncache(prefix);
        STATS.uncache(prefix);
        GRAPHS.uncache(prefix);
        FCSAnalyzer.get().clearFCSCache(null);
        //LOG.info("-Uncache all: " + (prefix == null ? "entire world" : "container='" + prefix + "'"));
    }

    protected void uncache(String prefix)
    {
        if (prefix == null)
        {
            _cache.clear();
        }
        else
        {
            // clears both the name list and the entries scoped by container
            _cache.removeUsingPrefix(prefix);
            // clears all entries by rowid
            _cache.removeUsingPrefix("rowid:");
        }
    }

    public static void uncache(FlowEntry entry)
    {
        if (entry == null)
            return;

        AttributeCache cache = AttributeCache.forType(entry._type);
        Container c = ContainerManager.getForId(entry._containerId);
        cache.uncache(c, entry._rowId, entry._name);
    }

    public void uncache(@NotNull Container c, int rowId, String name)
    {
        //LOG.info("+Uncache single: type=" + _type + ", container='" + c.getName() + "', rowid=" + rowId + ", name='" + name + "'");
        _cache.remove(createKey(c));
        _cache.remove(createKey(c, name));
        _cache.remove(createKey(rowId));
        //LOG.info("-Uncache single: type=" + _type + ", container='" + c.getName() + "', rowid=" + rowId + ", name='" + name + "'");
    }

    /**
     * Get all AttributeEntries of the type in the Container.
     */
    @NotNull
    public Collection<E> byContainer(Container c)
    {
        //noinspection unchecked
        return (List<E>)_cache.get(createKey(c), null, BY_CONTAINER_LOADER);
    }

    @NotNull
    private String createKey(Container c)
    {
        return c.getId();
    }

    /**
     * Get an AttributeEntry by name.
     */
    @Nullable
    public E byName(Container c, String name)
    {
        //noinspection unchecked
        return (E)_cache.get(createKey(c, name), null, BY_CONTAINER_NAME_LOADER);
    }

    /**
     * Get an AttributeEntry by attribute.
     */
    @Nullable
    public E byAttribute(Container c, A attr)
    {
        return byName(c, attr.toString());
    }

    /**
     * Get the preferred AttributeEntry by attribute.
     */
    @Nullable
    public E preferred(Container c, A attr)
    {
        E e = byAttribute(c, attr);
        if (e == null)
            return null;

        E aliased = e.getAliasedEntry();
        return aliased == null ? e : aliased;
    }

    @NotNull
    private String createKey(Container c, String name)
    {
        return createKey(c.getId(), name);
    }

    @NotNull
    private String createKey(String containerId, String name)
    {
        return containerId + ":" + name;
    }

    @Nullable
    private E byEntry(@Nullable FlowEntry flowEntry)
    {
        if (flowEntry == null)
            return null;

        // Add the entry directly by rowid and by container+name
        E entry = createEntry(flowEntry);
        _cache.put(createKey(flowEntry._rowId), entry);
        _cache.put(createKey(flowEntry._containerId, flowEntry._name), entry);
        return entry;
    }

    /**
     * Get an AttributeEntry by rowid.
     */
    @Nullable
    public E byRowId(int rowId)
    {
        //noinspection unchecked
        return (E)_cache.get(createKey(rowId), null, BY_ROWID_LOADER);
    }

    private String createKey(int rowId)
    {
        return "rowid:" + String.valueOf(rowId);
    }

    public static class KeywordCache extends AttributeCache<String, KeywordEntry>
    {
        private KeywordCache()
        {
            super(AttributeType.keyword);
        }

        @Override
        protected String _createAttribute(@NotNull String name)
        {
            return name;
    }

        @Override
        protected KeywordEntry _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull String attribute, @Nullable Integer aliased, @NotNull Collection<Integer> aliases)
        {
            return new KeywordEntry(containerId, rowId, name, aliased, aliases);
        }
    }

    public static class StatisticCache extends AttributeCache<StatisticSpec, StatisticEntry>
    {
        public StatisticCache()
        {
            super(AttributeType.statistic);
        }

        @Override
        protected StatisticSpec _createAttribute(@NotNull String name)
        {
            return new StatisticSpec(name);
        }

        @Override
        protected StatisticEntry _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull StatisticSpec attribute, @Nullable Integer aliased, @NotNull Collection<Integer> aliases)
        {
            return new StatisticEntry(containerId, rowId, name, attribute, aliased, aliases);
        }
    }

    public static class GraphCache extends AttributeCache<GraphSpec, GraphEntry>
    {
        private GraphCache()
        {
            super(AttributeType.graph);
        }

        @Override
        protected GraphSpec _createAttribute(@NotNull String name)
        {
            return new GraphSpec(name);
        }

        @Override
        protected GraphEntry _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull GraphSpec attribute, @Nullable Integer aliased, @NotNull Collection<Integer> aliases)
        {
            return new GraphEntry(containerId, rowId, name, attribute, aliased, aliases);
        }
    }

    static public final KeywordCache KEYWORDS = new KeywordCache();
    static public final StatisticCache STATS = new StatisticCache();
    static public final GraphCache GRAPHS = new GraphCache();

    public static AttributeCache forType(AttributeType type)
    {
        switch (type)
        {
            case keyword:   return KEYWORDS;
            case statistic: return STATS;
            case graph:     return GRAPHS;
            default:
                throw new IllegalArgumentException();
        }
    }
}
