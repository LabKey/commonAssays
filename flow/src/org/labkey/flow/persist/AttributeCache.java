/*
 * Copyright (c) 2006-2018 LabKey Corporation
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

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingStringKeyCache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.collections.CaseInsensitiveHashMap;
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
import java.util.Map;
import java.util.Objects;

/**
 * Cache of attribute names and aliases within a container.
 */
abstract public class AttributeCache<A extends Comparable<A>, E extends AttributeCache.Entry<A, E>>
{
    private static final Logger LOG = Logger.getLogger(AttributeCache.class);

    // container id -> list of names (sorted)
    private CacheLoader<String, Attributes<A, E>> BY_CONTAINER_LOADER = new CacheLoader<String, Attributes<A, E>>()
    {
        @Override
        public Attributes<A, E> load(String containerId, @Nullable Object argument)
        {
            LOG.debug("Loading " + _type + " by containerId: " + containerId);
            Collection<FlowEntry> entries = FlowManager.get().getAttributeEntries(containerId, _type);
            ArrayList<E> list = new ArrayList<>(entries.size());
            for (FlowEntry entry : entries)
            {
                list.add(createEntry(entry));
            }

            Collections.sort(list);

            Attributes<A, E> attributes = new Attributes<>(containerId, list);

            //LOG.debug("Loaded " + _type + " by containerId: " + containerId);
            return attributes;
        }
    };


    private static class Attributes<Q extends Comparable<Q>, Z extends Entry<Q, Z>>
    {
        private final String _containerId;
        private final Collection<Z> _entries;
        private final Map<String, Z> _byName;
        private final Map<Integer, Z> _byRowId;
        private final MultiValuedMap<Integer, Integer> _aliases;

        private Attributes(String containerId, Collection<Z> all)
        {
            _containerId = containerId;
            _entries = all;

            Map<String, Z> byName = new CaseInsensitiveHashMap<>();
            Map<Integer, Z> byRowId = new HashMap<>();
            MultiValuedMap<Integer, Integer> aliases = new ArrayListValuedHashMap<>();
            for (Z entry : all)
            {
                byRowId.put(entry.getRowId(), entry);

                Z existing = byName.putIfAbsent(entry.getName(), entry);
                if (existing != null)
                    LOG.warn("Duplicate entry '" + existing.getName() + "' (id=" + existing.getRowId() + ", aliasId=" + existing.getAliasedId() + ") and '" + entry.getName() + "' (id=" + entry.getRowId() + ", aliasId=" + entry.getAliasedId() + ")");

                if (entry.getAliasedId() != null)
                    aliases.put(entry.getAliasedId(), entry.getRowId());
            }
            _byName = Collections.unmodifiableMap(byName);
            _byRowId = Collections.unmodifiableMap(byRowId);
            _aliases = aliases;
        }
    }

    public static abstract class Entry<Q extends Comparable<Q>, Z extends Entry<Q, Z>> implements Comparable<Entry<Q, Z>>
    {
        private final AttributeType _type;
        private final int _rowId;
        private final String _containerId;
        private final String _name;
        private final Q _attribute;
        private final Integer _aliasedId;

        protected Entry(@NotNull String containerId, @NotNull AttributeType type, int rowId, @NotNull String name, @NotNull Q attribute, @Nullable Integer aliasedId)
        {
            _containerId = containerId;
            _type = type;
            _rowId = rowId;
            _name = name;
            _attribute = attribute;
            _aliasedId = aliasedId;
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

            //noinspection unchecked
            AttributeCache<Q, Z> cache = (AttributeCache<Q, Z>) AttributeCache.forType(_type);
            return cache.byRowId(_containerId, _aliasedId);
        }

        /** Get the list of aliases for this attribute. */
        public Collection<Z> getAliases()
        {
            //noinspection unchecked
            AttributeCache<Q, Z> cache = (AttributeCache<Q, Z>) AttributeCache.forType(_type);
            Attributes<Q, Z> attributes = cache._cache.get(_containerId);

            Collection<Integer> aliasIds = attributes._aliases.get(_rowId);
            if (aliasIds.isEmpty())
                return Collections.emptyList();

            ArrayList<Z> entries = new ArrayList<>(aliasIds.size());
            for (Integer aliasId : aliasIds)
            {
                Z entry = cache.byRowId(_containerId, aliasId);
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
        protected KeywordEntry(@NotNull String containerId, int rowId, @NotNull String name, @Nullable Integer aliased)
        {
            super(containerId, AttributeType.keyword, rowId, name, name, aliased);
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
        protected StatisticEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull StatisticSpec spec, @Nullable Integer aliased)
        {
            super(containerId, AttributeType.statistic, rowId, name, spec, aliased);
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
        protected GraphEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull GraphSpec spec, @Nullable Integer aliased)
        {
            super(containerId, AttributeType.graph, rowId, name, spec, aliased);
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

    private final BlockingStringKeyCache<Attributes<A, E>> _cache;
    private final AttributeType _type;

    public AttributeCache(AttributeType type)
    {
        _type = type;
        _cache = CacheManager.getBlockingStringKeyCache(CacheManager.UNLIMITED, CacheManager.DAY, "Flow " + _type + " cache", BY_CONTAINER_LOADER);
    }

    @Nullable
    private E createEntry(@Nullable FlowEntry entry)
    {
        if (entry == null)
            return null;

        assert entry._type == _type;

        Integer aliasId = entry.isAlias() ? entry._aliasId : null;

        A attribute = _createAttribute(entry._name);

        return _createEntry(entry._containerId, entry._rowId, entry._name, attribute, aliasId);
    }

    protected abstract E _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull A attribute, @Nullable Integer aliased);

    protected abstract A _createAttribute(@NotNull String name);

    protected AttributeType type()
    {
        return _type;
    }


    private static class UncacheTask implements Runnable
    {
        private Container _c;
        private @Nullable AttributeCache _cache;

        UncacheTask(Container c, @Nullable AttributeCache cache)
        {
            _c = c;
            _cache = cache;
        }

        @Override
        public void run()
        {
            if (_cache == null)
                _uncacheAllNow(_c);
            else
                _cache.uncacheNow(_c);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UncacheTask that = (UncacheTask) o;
            return Objects.equals(_c, that._c) && _cache == that._cache;
        }

        @Override
        public int hashCode()
        {
            return _c.hashCode();
        }
    }

    /** Uncache all caches after commit. */
    public static void uncacheAllAfterCommit(@NotNull Container c)
    {
        FlowManager mgr = FlowManager.get();
        DbScope.Transaction t = mgr.getSchema().getScope().getCurrentTransaction();
        if (t != null)
        {
            t.addCommitTask(new AttributeCache.UncacheTask(c, null), DbScope.CommitTaskOption.POSTCOMMIT);
        }
        else
        {
            _uncacheAllNow(c);
        }
    }

    /** Uncache all caches immediately. */
    private static void _uncacheAllNow(@Nullable Container c)
    {
        KEYWORDS.uncacheNow(c);
        STATS.uncacheNow(c);
        GRAPHS.uncacheNow(c);
        FCSAnalyzer.get().clearFCSCache(null);
    }

    /** Uncache this cache after commit. */
    public void uncacheAfterCommit(@NotNull Container c)
    {
        FlowManager mgr = FlowManager.get();
        DbScope.Transaction t = mgr.getSchema().getScope().getCurrentTransaction();
        if (t != null)
        {
            t.addCommitTask(new AttributeCache.UncacheTask(c, this), DbScope.CommitTaskOption.POSTCOMMIT);
        }
        else
        {
            _uncacheNow(c);
        }
    }

    /** Uncache this cache immediately. */
    public void uncacheNow(@Nullable Container c)
    {
        _uncacheNow(c);
    }

    private void _uncacheNow(@Nullable Container c)
    {
        LOG.debug("Uncache " + _type.name() + ": " + (c == null ? "entire world" : "container='" + c.getName() + "', id='" + c.getId() + "'"));
        if (c == null)
        {
            _cache.clear();
        }
        else
        {
            _cache.remove(c.getId());
        }
    }

    /**
     * Get all AttributeEntries of the type in the Container.
     */
    @NotNull
    public Collection<E> byContainer(@NotNull Container c)
    {
        Attributes<A, E> attributes = _cache.get(c.getId());
        if (attributes == null)
            return Collections.emptyList();

        return attributes._entries;
    }

    /**
     * Get an AttributeEntry by name.
     */
    @Nullable
    public E byName(@NotNull Container c, @NotNull String name)
    {
        Attributes<A, E> attributes = _cache.get(c.getId());
        if (attributes == null)
            return null;

        return attributes._byName.get(name);
    }

    /**
     * Get an AttributeEntry by attribute.
     */
    @Nullable
    public E byAttribute(@NotNull Container c, @NotNull A attr)
    {
        return byName(c, attr.toString());
    }

    /**
     * Get the preferred AttributeEntry by attribute.
     */
    @Nullable
    public E preferred(@NotNull Container c, @NotNull A attr)
    {
        E e = byAttribute(c, attr);
        if (e == null)
            return null;

        E aliased = e.getAliasedEntry();
        return aliased == null ? e : aliased;
    }


    /**
     * Get an AttributeEntry by rowid.
     */
    @Nullable
    public E byRowId(@NotNull Container container, int rowId)
    {
        return byRowId(container.getId(), rowId);
    }

    private E byRowId(@NotNull String containerId, int rowId)
    {
        Attributes<A, E> attributes = _cache.get(containerId);
        if (attributes == null)
            return null;

        return attributes._byRowId.get(rowId);
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
        protected KeywordEntry _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull String attribute, @Nullable Integer aliased)
        {
            return new KeywordEntry(containerId, rowId, name, aliased);
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
        protected StatisticEntry _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull StatisticSpec attribute, @Nullable Integer aliased)
        {
            return new StatisticEntry(containerId, rowId, name, attribute, aliased);
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
        protected GraphEntry _createEntry(@NotNull String containerId, int rowId, @NotNull String name, @NotNull GraphSpec attribute, @Nullable Integer aliased)
        {
            return new GraphEntry(containerId, rowId, name, attribute, aliased);
        }
    }

    static public final KeywordCache KEYWORDS = new KeywordCache();
    static public final StatisticCache STATS = new StatisticCache();
    static public final GraphCache GRAPHS = new GraphCache();

    public static AttributeCache forType(AttributeType type)
    {
        return type.getCache();
    }
}
