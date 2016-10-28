/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector.ForEachBlock;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.labkey.flow.data.AttributeType.keyword;

public class FlowManager
{
    private static final FlowManager instance = new FlowManager();
    private static final Logger _log = Logger.getLogger(FlowManager.class);
    private static final String SCHEMA_NAME = "flow";

    static public FlowManager get()
    {
        return instance;
    }

    public String getSchemaName()
    {
        return SCHEMA_NAME;
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }

    public TableInfo getTinfoStatisticAttr()
    {
        return getSchema().getTable("StatisticAttr");
    }

    public TableInfo getTinfoGraphAttr()
    {
        return getSchema().getTable("GraphAttr");
    }

    public TableInfo getTinfoKeywordAttr()
    {
        return getSchema().getTable("KeywordAttr");
    }

    public TableInfo getTinfoObject()
    {
        return getSchema().getTable("Object");
    }

    public TableInfo getTinfoKeyword()
    {
        return getSchema().getTable("Keyword");
    }

    public TableInfo getTinfoStatistic()
    {
        return getSchema().getTable("Statistic");
    }

    public TableInfo getTinfoGraph()
    {
        return getSchema().getTable("Graph");
    }

    public TableInfo getTinfoScript()
    {
        return getSchema().getTable("Script");
    }

    private TableInfo attributeTable(AttributeType type)
    {
        return type.getAttributeTable();
    }

    private TableInfo valueTable(AttributeType type)
    {
        return type.getValueTable();
    }

    /** The column name of attribute id column on the value table. */
    private String valueTableAttrIdColumn(AttributeType type)
    {
        return type.getValueTableAttributeIdColumn();
    }

    /** The column name of original attribute id column on the value table. */
    private String valueTableOriginalAttrIdColumn(AttributeType type)
    {
        return type.getValueTableOriginalAttributeIdColumn();
    }

    /**
     * Get the row id of an attribute name.
     * DOES NOT CACHE.
     *
     * @param container The container.
     * @param type The attribute type.
     * @param attr The attribute name.
     * @return The row id of the attribute or 0 if not found.
     */
    private int getAttributeRowId(Container container, AttributeType type, String attr)
    {
        return getAttributeRowId(container.getId(), type, attr);
    }

    private int getAttributeRowId(String containerId, AttributeType type, String attr)
    {
        FlowEntry entry = getAttributeEntry(containerId, type, attr);
        if (entry != null)
            return entry._rowId;

        return 0;
    }

    /**
     * Get the canonical id of an attribute name.
     * DOES NOT CACHE.
     *
     * @param container The container.
     * @param type The attribute type.
     * @param attr The attribute name.
     * @return The row id of the attribute or 0 if not found.
    private int getAttributeId(Container container, AttributeType type, String attr)
    {
        FlowEntry entry = getAttributeEntry(container.getId(), type, attr);
        if (entry != null)
            return entry._aliasId;

        return 0;
    }
     */

    /**
     * Get the FlowEntry for the attribute name.
     * DOES NOT CACHE.
     *
     * @param containerId The container.
     * @param type The attribute type.
     * @param attr The attribute name.
     * @return The FlowEntry of the attribute or null if not found.
     */
    public FlowEntry getAttributeEntry(String containerId, AttributeType type, String attr)
    {
        //_log.info("getAttributeEntry(" + containerId + ", " + type + ", " + attr + ")");
        try (ResultSet rs = new SqlSelector(getSchema(), "SELECT RowId, Id FROM " + attributeTable(type) + " WHERE Container = ? AND Name = ?", containerId, attr).getResultSet())
        {
            // we're not caching misses because this is an unlimited cachemap
            if (!rs.next())
                return null;

            Integer rowId = rs.getInt("RowId");
            Integer aliasId = rs.getInt("Id");
            FlowEntry a = new FlowEntry(type, rowId, containerId, attr, aliasId);
            return a;
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    /*
    private FlowEntry[] getAttributeEntry(AttributeType type, Integer[] rowIds)
    {
        FlowEntry[] ret = new FlowEntry[rowIds.length];
        boolean hasNulls = false;
        for (int i = 0; i < rowIds.length; i ++)
        {
            Integer rowId = rowIds[i];
            if (rowId != null)
            {
                ret[i] = getAttributeEntry(type, rowIds[i]);
            }
            if (ret[i] == null)
            {
                _log.error("Request for attribute " + rowId + " returned null.", new Exception());
                hasNulls = true;
            }
        }
        if (!hasNulls)
            return ret;
        ArrayList<FlowEntry> lstRet = new ArrayList<>();
        for (FlowEntry entry : ret)
        {
            if (entry != null)
            {
                lstRet.add(entry);
            }
        }
        return lstRet.toArray(new FlowEntry[lstRet.size()]);
    }
    */


    /**
     * Get an entry by type and rowId.
     * DOES NOT CACHE
     */
    @Nullable
    public FlowEntry getAttributeEntry(@NotNull AttributeType type, int rowId)
    {
        //_log.info("getAttributeEntry(" + type + ", " + rowId + ")");
        Map<String, Object> row = new SqlSelector(getSchema(), "SELECT Container, Name, Id FROM " + attributeTable(type) + " WHERE RowId = ?", rowId).getMap();
        if (row == null)
        {
            return null;
        }
        String name = (String)row.get("Name");
        String containerId = (String)row.get("Container");
        Integer aliasId = (Integer)row.get("Id");
        return new FlowEntry(type, rowId, containerId, name, aliasId);
    }

    /**
     * Get an ordered list of all names in the container.
     * DOES NOT CACHE
    @NotNull
    public List<String> getAttributeNames(@NotNull String containerId, @NotNull AttributeType type)
    {
        TableInfo table = attributeTable(type);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("Container"), containerId);
        Sort sort = new Sort("Name");
        TableSelector selector = new TableSelector(table, Collections.singleton("Name"), filter, sort);
        return selector.getArrayList(String.class);
    }
     */

    /** Get all attributes in the container. */
    public Collection<FlowEntry> getAttributeEntries(@NotNull String containerId, @NotNull final AttributeType type)
    {
        //_log.info("getAttributeEntries(" + containerId + ", " + type + ")");
        TableInfo table = attributeTable(type);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("Container"), containerId);
        TableSelector selector = new TableSelector(table, filter, null);

        final List<FlowEntry> entries = new ArrayList<>();
        selector.forEachMap(row -> {
            Integer rowId = (Integer)row.get("RowId");
            String name = (String)row.get("Name");
            String containerId1 = (String)row.get("Container");
            Integer aliasId = (Integer)row.get("Id");
            FlowEntry entry = new FlowEntry(type, rowId, containerId1, name, aliasId);

            entries.add(entry);
        });

        return Collections.unmodifiableList(entries);
    }

    /** Equality based on attribute type and rowid. */
    public static class FlowEntry implements Comparable<FlowEntry>
    {
        public final AttributeType _type;
        public final Integer _rowId;
        public final String _containerId;
        public final String _name;
        public final Integer _aliasId;

        public FlowEntry(@NotNull AttributeType type, @NotNull Integer rowId, @NotNull String containerId, @NotNull String name, @NotNull Integer aliasId)
        {
            _type = type;
            _rowId = rowId;
            _containerId = containerId;
            _name = name;
            _aliasId = aliasId;
        }

        public boolean isAlias()
        {
            return !_rowId.equals(_aliasId);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FlowEntry flowEntry = (FlowEntry) o;

            if (!_rowId.equals(flowEntry._rowId)) return false;
            if (_type != flowEntry._type) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _type.hashCode();
            result = 31 * result + _rowId.hashCode();
            return result;
        }

        @Override
        public int compareTo(FlowEntry o)
        {
            return _name.compareTo(o._name);
        }
    }

    private int ensureAttributeName(Container container, AttributeType type, String attr, int aliasId)
    {
        return ensureAttributeName(container.getId(), type, attr, aliasId);
    }

    /**
     * Ensure the attribute exists.  If the aliasId >= 0, the aliasId points at the RowId of the preferred name for the attribute.
     * DOES NOT CACHE.  CALLERS SHOULD CLEAR CACHE APPROPRIATELY.
     *
     * @param containerId Container
     * @param type attribute type
     * @param attr attribute name
     * @param aliasId RowId of aliased attribute or -1 to set alias to itself.
     * @return The RowId of the rewly inserted or existing attribute.
     * @throws SQLException
     */
    private int ensureAttributeName(String containerId, AttributeType type, String attr, int aliasId)
    {
        //_log.info("ensureAttributeName(" + containerId + ", " + type + ", " + attr + ", " + aliasId + ")");
        DbSchema schema = getSchema();
        if (schema.getScope().isTransactionActive())
        {
            throw new IllegalStateException("ensureAttributeId cannot be called within a transaction");
        }

        int ret = getAttributeRowId(containerId, type, attr);
        if (ret != 0)
            return ret;

        // Validate the name
        if (attr == null || attr.length() == 0)
            throw new IllegalArgumentException("Name must not be null");

        // Parse the name before storing
        Object attribute = type.createAttribute(attr);

        Map<String, Object> map = new HashMap<>();
        map.put("Container", containerId);
        map.put("Name", attr);
        map.put("Id", aliasId);

        TableInfo table = attributeTable(type);
        map = Table.insert(null, table, map);

        // Set Id to RowId if we aren't inserting an alias
        if (aliasId <= 0)
        {
            map.put("Id", map.get("RowId"));
            Table.update(null, table, map, map.get("RowId"));
        }

        return getAttributeRowId(containerId, type, attr);
    }

    private int ensureAttributeName(Container container, AttributeType type, String name)
    {
        return ensureAttributeName(container, type, name, -1);
    }


    public int ensureStatisticName(Container c, String name, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, AttributeType.statistic, name, Collections.emptyList(), uncache);
    }


    public int ensureKeywordName(Container c, String name, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, keyword, name, Collections.emptyList(), uncache);
    }


    public int ensureGraphName(Container c, String name, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, AttributeType.graph, name, Collections.emptyList(), uncache);
    }


    public int ensureStatisticNameAndAliases(Container c, String name, Iterable<? extends Object> aliases, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, AttributeType.statistic, name, aliases, uncache);
    }


    public int ensureKeywordNameAndAliases(Container c, String name, Iterable<? extends Object> aliases, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, keyword, name, aliases, uncache);
    }


    public int ensureGraphNameAndAliases(Container c, String name, Iterable<? extends Object> aliases, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, AttributeType.graph, name, aliases, uncache);
    }


    private int ensureAttributeNameAndAliases(Container c, AttributeType type, String name, Iterable<? extends Object> aliases, boolean uncache)
    {
        //_log.info("ensureAlias(" + c + ", " + type + ", " + name + ", aliases)");
        try
        {
            List<String> names = new ArrayList<>();
            names.add(name);
            for (Object alias : aliases)
                names.add(alias.toString());

            // Check for an existing alias in the list of new attribute names.
            Integer aliasId = null;
            for (String s : names)
            {
                FlowEntry entry = getAttributeEntry(c.getId(), type, s);
                if (entry != null)
                {
                    aliasId = entry._aliasId;
                    break;
                }
            }

            // If no existing primary attribute was found, insert attr as the preferred attribute name.
            if (aliasId == null)
                aliasId = ensureAttributeName(c, type, name);
            else
                ensureAttributeName(c, type, name, aliasId);

            for (Object alias : aliases)
                ensureAttributeName(c, type, alias.toString(), aliasId);

            return aliasId;
        }
        finally
        {
            if (uncache)
                AttributeCache.uncacheAllAfterCommit(c);
        }
    }

    public void ensureAlias(@NotNull AttributeType type, int rowId, @NotNull String aliasName, boolean uncache)
    {
        //_log.info("ensureAlias(" + type + ", " + rowId + ", " + aliasName + ")");
        try (DbScope.Transaction tx = getSchema().getScope().ensureTransaction())
        {
            FlowEntry entry = getAttributeEntry(type, rowId);
            if (entry == null)
                throw new IllegalArgumentException("Attribute not found");

            if (entry.isAlias())
                throw new IllegalArgumentException("Can't create alias of an alias");

            // Find existing attribute for the provided alias name
            FlowEntry existing = getAttributeEntry(entry._containerId, type, aliasName);
            if (existing != null)
            {
                if (entry.equals(existing))
                    return;

                // If this existing entry doesn't have any aliases, we can make this existing entry an alias of the entry.
                if (existing.isAlias())
                    throw new IllegalArgumentException("The " + type.name() + " attribute '" + aliasName + "' is already an alias of '" + getAttributeEntry(type, existing._aliasId)._name + "'");

                if (!getAliases(type, existing._rowId).isEmpty())
                    throw new IllegalArgumentException("The " + type.name() + " attribute '" + aliasName + "' has aliases and can't be made an alias of '" + entry._name + "'");

                // update usages of the existing attribute to point at the new parent, keeping the original attribute id the same
                updateAttributeValuesPreferredId(existing._containerId, type, existing._rowId, entry._rowId);

                // parent the existing entry to the other attribute
                Container c = ContainerManager.getForId(existing._containerId);
                updateAttribute(c, type, existing._rowId, existing._name, rowId, uncache);
            }
            else
            {
                int newEntryId = 0;
                try
                {
                    newEntryId = ensureAttributeName(entry._containerId, type, aliasName, entry._rowId);
                }
                finally
                {
                    if (uncache)
                    {
                        // uncache parent
                        AttributeCache.uncache(entry);

                        if (newEntryId > 0)
                        {
                            // uncache new alias
                            FlowEntry newEntry = getAttributeEntry(type, newEntryId);
                            AttributeCache.uncache(newEntry);
                        }
                    }
                }
            }

            tx.commit();
        }
    }

    public void updateAttribute(@NotNull Container container, @NotNull AttributeType type, int rowId, @NotNull String name, int aliasId, boolean uncache)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            throw new IllegalArgumentException("Attribute not found");

        // Validate the name
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Name must not be null");

        // Parse the name before storing
        Object attribute = type.createAttribute(name);

        Map<String, Object> map = new HashMap<>();
        map.put("Container", container.getId());
        map.put("Name", name);
        map.put("Id", aliasId);

        try
        {
            TableInfo table = attributeTable(type);
            Table.update(null, table, map, rowId);
        }
        finally
        {
            if (uncache)
            {
                AttributeCache cache = AttributeCache.forType(type);

                // old name
                cache.uncache(container, entry._rowId, entry._name);

                // new name
                cache.uncache(container, entry._rowId, name);

                if (rowId != entry._aliasId || rowId != aliasId)
                {
                    // old alias id
                    FlowEntry aliasedEntry = getAttributeEntry(type, entry._aliasId);
                    cache.uncache(aliasedEntry);

                    // new alias id
                    aliasedEntry = getAttributeEntry(type, aliasId);
                    cache.uncache(aliasedEntry);
                }
            }
        }
    }

    // Update any attribute usages of the current rowId to the new rowId, keeping the original id the same
    private int updateAttributeValuesPreferredId(@NotNull String containerId, @NotNull AttributeType type, int currentRowId, int newRowId)
    {
        TableInfo attrTable = attributeTable(type);
        TableInfo valueTable = valueTable(type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(type);
        String valueTableOriginalAttrIdColumn = valueTableOriginalAttrIdColumn(type);

        // Check that the existing entry and the entry being aliased aren't both present on a single object
        // to avoid violating the primary key constraint. For flow.keyword the pk_keyword covers (objectid, keywordid)
        SQLFragment check = new SQLFragment()
                .append("SELECT *\n")
                .append("FROM (\n")
                .append("  SELECT ObjectId, COUNT(*) AS usages\n")
                .append("  FROM ").append(valueTable, "v").append("\n")
                .append("  INNER JOIN flow.object obj ON v.objectId = obj.rowId\n")
                .append("  WHERE obj.container = ?\n").add(containerId)
                .append("    AND v.").append(valueTableAttrIdColumn).append(" IN (").append(currentRowId).append(", ").append(newRowId).append(")").append("\n")
                .append("  GROUP BY v.objectId\n")
                .append(") X\n")
                .append("WHERE X.usages > 1");
        long objectsWithBothAttrsCount = new SqlSelector(getSchema(), check).getRowCount();
        if (objectsWithBothAttrsCount > 0)
            throw new IllegalArgumentException("There are objects that have both attributes: " + objectsWithBothAttrsCount);

        SQLFragment sql = new SQLFragment()
                .append("UPDATE ").append(valueTable)
                .append(" SET ").append(valueTableAttrIdColumn)
                .append(" = ").append(newRowId)
                .append(" WHERE ").append(valueTableAttrIdColumn).append(" = ").append(currentRowId);

        return new SqlExecutor(getSchema()).execute(sql);
    }


    public boolean isAlias(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return false;

        return entry.isAlias();
    }

    /** Return the preferred name for the rowId or null if rowId is not an alias id. */
    public FlowEntry getAliased(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null || !entry.isAlias())
            return null;

        return getAttributeEntry(type, entry._aliasId);
    }

    /** Return the preferred/primary name for the rowId or null if rowId is not an alias id. */
    public FlowEntry getAliased(FlowEntry entry)
    {
        if (entry == null || !entry.isAlias())
            return null;

        return getAttributeEntry(entry._type, entry._aliasId);
    }

    /** Get aliases for the preferred/primary attribute rowId or empty collection if rowId is not a preferred attribute. */
    public Collection<FlowEntry> getAliases(final AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return Collections.emptyList();

        return getAliases(entry);
    }

    /** Get aliases for the preferred/primary attribute rowId or empty collection if rowId is not a preferred attribute. */
    public Collection<FlowEntry> getAliases(final FlowEntry entry)
    {
        //_log.info("getAliases");
        // Get the attributes that have an id equal to the entry and exclude the entry itself.
        TableInfo table = attributeTable(entry._type);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(table.getColumn("Container"), entry._containerId);
        filter.addCondition(table.getColumn("Id"), entry._rowId);
        filter.addCondition(table.getColumn("RowId"), entry._rowId, CompareType.NEQ);
        TableSelector selector = new TableSelector(table, filter, null);

        final List<FlowEntry> aliases = new ArrayList<>();
        selector.forEachMap(new ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Integer rowId = (Integer)row.get("RowId");
                String name = (String)row.get("Name");
                String containerId = (String)row.get("Container");
                Integer aliasId = (Integer)row.get("Id");
                FlowEntry alias = new FlowEntry(entry._type, rowId, containerId, name, aliasId);

                aliases.add(alias);
            }
        });

        return aliases;
    }

    public Collection<Integer> getAliasIds(final FlowEntry entry)
    {
        //_log.info("getAliasIds");
        // Get the attributes that have an id equal to the entry and exclude the entry itself.
        TableInfo table = attributeTable(entry._type);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(table.getColumn("Container"), entry._containerId);
        filter.addCondition(table.getColumn("Id"), entry._rowId);
        filter.addCondition(table.getColumn("RowId"), entry._rowId, CompareType.NEQ);
        TableSelector selector = new TableSelector(table, Collections.singleton("RowId"), filter, null);

        return selector.getArrayList(Integer.class);
    }

    public Map<FlowEntry, Collection<FlowEntry>> getAliases(Container c, final AttributeType type)
    {
        TableInfo table = attributeTable(type);
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        Sort sort = new Sort("Name");
        TableSelector selector = new TableSelector(table, filter, sort);

        final Map<FlowEntry, Collection<FlowEntry>> aliasMap = new LinkedHashMap<>();
        selector.forEachMap(new ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Integer rowId = (Integer)row.get("RowId");
                String name = (String)row.get("Name");
                String containerId = (String)row.get("Container");
                Integer aliasId = (Integer)row.get("Id");
                FlowEntry entry = new FlowEntry(type, rowId, containerId, name, aliasId);

                FlowEntry preferredEntry;
                if (entry.isAlias())
                    preferredEntry = getAttributeEntry(type, entry._aliasId);
                else
                    preferredEntry = entry;

                Collection<FlowEntry> aliases = aliasMap.get(preferredEntry);
                if (aliases == null)
                    aliasMap.put(preferredEntry, aliases = new ArrayList<>());

                if (entry.isAlias())
                    aliases.add(entry);
            }
        });

        return Collections.unmodifiableMap(aliasMap);
    }

    /**
     * Get all unused primary attributes (usages of aliaes count to the primary attirubte's usages, but aren't included in the result set.)
     */
    @NotNull
    public Collection<FlowEntry> getUnused(@NotNull Container c, @NotNull final AttributeType type)
    {
        TableInfo attrTable = attributeTable(type);
        TableInfo valueTable = valueTable(type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(type);

        SQLFragment sql = new SQLFragment()
                .append("-- Outermost query: get all rowids not in use\n")
                .append("SELECT attr3.rowid, attr3.container, attr3.name, attr3.id\n")
                .append("FROM ").append(attrTable, "attr3").append("\n")
                .append("WHERE attr3.container = ?\n")
                .append("AND attr3.rowid NOT IN (\n")
                .append("    -- Second query: all rowids in use; maps used ids back to alias or primary rowid\n")
                .append("    SELECT attr2.rowid\n")
                .append("    FROM ").append(attrTable, "attr2").append("\n")
                .append("    WHERE attr2.id IN (\n")
                .append("        -- First query: all ids in use\n")
                .append("        SELECT attr.id\n")
                .append("        FROM ").append(attrTable, "attr").append("\n")
                .append("        WHERE attr.container = ?\n")
                .append("        AND attr.rowid IN (SELECT val.").append(valueTableAttrIdColumn).append(" FROM ").append(valueTable, "val").append(")\n")
                .append("  )\n")
                .append(")\n");

        sql.add(c.getId());
        sql.add(c.getId());

        SqlSelector selector = new SqlSelector(getSchema(), sql);

        final List<FlowEntry> unused = new ArrayList<>();
        selector.forEachMap(new ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Integer rowId = (Integer)row.get("RowId");
                String name = (String)row.get("Name");
                String containerId = (String)row.get("Container");
                Integer aliasId = (Integer)row.get("Id");
                FlowEntry alias = new FlowEntry(type, rowId, containerId, name, aliasId);

                unused.add(alias);
            }
        });

        return Collections.unmodifiableList(unused);
    }

    public void deleteUnused(@NotNull Container c)
    {
        try (DbScope.Transaction tx = getSchema().getScope().ensureTransaction())
        {
            deleteUnused(c, AttributeType.keyword);
            deleteUnused(c, AttributeType.statistic);
            deleteUnused(c, AttributeType.graph);

            tx.commit();
        }
    }

    private int deleteUnused(@NotNull Container c, AttributeType type)
    {
        assert getSchema().getScope().isTransactionActive();

        TableInfo attrTable = attributeTable(type);
        TableInfo valueTable = valueTable(type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(type);

        SQLFragment sql = new SQLFragment()
                .append("-- Outermost query: get all rowids not in use\n")
                .append("DELETE\n")
                .append("FROM ").append(attrTable).append("\n")
                .append("WHERE container = ?\n")
                .append("AND rowid NOT IN (\n")
                .append("    -- Second query: all rowids in use; maps used ids back to alias or primary rowid\n")
                .append("    SELECT attr2.rowid\n")
                .append("    FROM ").append(attrTable, "attr2").append("\n")
                .append("    WHERE attr2.id IN (\n")
                .append("        -- First query: all ids in use\n")
                .append("        SELECT attr.id\n")
                .append("        FROM ").append(attrTable, "attr").append("\n")
                .append("        WHERE attr.container = ?\n")
                .append("        AND attr.rowid IN (SELECT val.").append(valueTableAttrIdColumn).append(" FROM ").append(valueTable, "val").append(")\n")
                .append("  )\n")
                .append(")\n");

        sql.add(c.getId());
        sql.add(c.getId());

        SqlExecutor executor = new SqlExecutor(getSchema());
        return executor.execute(sql);
    }

    /**
     * Get a usage count for an attribute and its aliases.
     */
    public Map<Integer, Number> getUsageCount(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return Collections.emptyMap();

        TableInfo attrTable = attributeTable(type);
        TableInfo valueTable = valueTable(type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(type);
        String valueTableOriginalAttrIdColumn = valueTableOriginalAttrIdColumn(type);

        SQLFragment sql = new SQLFragment()
                .append("SELECT val.").append(valueTableOriginalAttrIdColumn).append(" AS OriginalAttrId, COUNT(fo.rowid) AS ObjectCount\n")
                .append("FROM ")
                .append(valueTable, "val").append(", ")
                .append(getTinfoObject(), "fo").append("\n")
                .append("WHERE fo.rowid = val.objectid\n")
                .append("  AND val.").append(valueTableAttrIdColumn).append(" = ").append(entry._rowId).append("\n")
                .append("GROUP BY val.").append(valueTableOriginalAttrIdColumn).append("\n");

        SqlSelector selector = new SqlSelector(getSchema(), sql);
        return selector.getValueMap();
    }

    /**
     * Get usages for an attribute, excluding its aliases.
     */
    public Collection<FlowDataObject> getUsages(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return Collections.emptyList();

        TableInfo attrTable = attributeTable(type);
        TableInfo valueTable = valueTable(type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(type);
        String valueTableOriginalAttrIdColumn = valueTableOriginalAttrIdColumn(type);

        SQLFragment sql = new SQLFragment()
                .append("SELECT fo.rowid, fo.dataid,")
                .append(" val.").append(valueTableAttrIdColumn).append(" AS AttrId,")
                .append(" val.").append(valueTableOriginalAttrIdColumn).append(" AS OriginalAttrId\n")
                .append("FROM ")
                .append(valueTable, "val").append(", ")
                .append(getTinfoObject(), "fo").append("\n")
                .append("WHERE fo.rowid = val.objectid\n")
                .append("  AND val.").append(valueTableOriginalAttrIdColumn).append(" = ").append(rowId).append("\n");

        final List<FlowDataObject> usages = new ArrayList<>();
        SqlSelector selector = new SqlSelector(getSchema(), sql);
        selector.forEachMap(new ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Integer dataId = (Integer)row.get("DataId");

                FlowDataObject fdo = FlowDataObject.fromRowId(dataId);
                usages.add(fdo);
            }
        });

        return Collections.unmodifiableList(usages);
    }

    /**
     * Get usages for an attribute and its aliases.
     */
    public Map<Integer, Collection<FlowDataObject>> getAllUsages(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return Collections.emptyMap();

        TableInfo attrTable = attributeTable(type);
        TableInfo valueTable = valueTable(type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(type);
        String valueTableOriginalAttrIdColumn = valueTableOriginalAttrIdColumn(type);

        SQLFragment sql = new SQLFragment()
                .append("SELECT fo.rowid, fo.dataid,")
                .append(" val.").append(valueTableAttrIdColumn).append(" AS AttrId,")
                .append(" val.").append(valueTableOriginalAttrIdColumn).append(" AS OriginalAttrId\n")
                .append("FROM ")
                .append(valueTable, "val").append(", ")
                .append(getTinfoObject(), "fo").append("\n")
                .append("WHERE fo.rowid = val.objectid\n")
                .append("  AND val.").append(valueTableAttrIdColumn).append(" = ").append(rowId).append("\n");

        final Map<Integer, Collection<FlowDataObject>> usages = new HashMap<>();
        SqlSelector selector = new SqlSelector(getSchema(), sql);
        selector.forEachMap(new ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Integer attributeRowId = (Integer)row.get("OriginalAttrId");
                Integer dataId = (Integer)row.get("DataId");

                Collection<FlowDataObject> datas = usages.get(attributeRowId);
                if (datas == null)
                    usages.put(attributeRowId, datas = new ArrayList<>());

                FlowDataObject fdo = FlowDataObject.fromRowId(dataId);
                datas.add(fdo);
            }
        });

        return Collections.unmodifiableMap(usages);
    }


    public List<AttrObject> getAttrObjects(Collection<ExpData> datas)
    {
        if (datas.isEmpty())
            return Collections.emptyList();
        SQLFragment sql = new SQLFragment ("SELECT * FROM " + getTinfoObject().toString() + " WHERE DataId IN (");
        String comma = "";
        for (ExpData data : datas)
        {
            sql.append(comma).append(data.getRowId());
            comma = ",";
        }
        sql.append(")");
        AttrObject[] array = new SqlSelector(getSchema(), sql).getArray(AttrObject.class);
        return Arrays.asList(array);
    }

    public AttrObject getAttrObject(ExpData data)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("DataId"), data.getRowId());

        return new TableSelector(getTinfoObject(), filter, null).getObject(AttrObject.class);
    }

    public AttrObject getAttrObjectFromRowId(int rowid)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RowId"), rowid);

        return new TableSelector(getTinfoObject(), filter, null).getObject(AttrObject.class);
    }

    public Collection<AttrObject> getAttrObjectsFromURI(Container c, URI uri)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        filter.addCondition(FieldKey.fromParts("URI"), uri.toString());

        return new TableSelector(getTinfoObject(), filter, null).getCollection(AttrObject.class);
    }


    public AtomicLong flowObjectModificationCount = new AtomicLong();


    public void flowObjectModified()
    {
        flowObjectModificationCount.incrementAndGet();
    }

    
    public AttrObject createAttrObject(ExpData data, ObjectType type, URI uri) throws SQLException
    {
        if (FlowDataHandler.instance.getPriority(ExperimentService.get().getExpData(data.getRowId())) != Handler.Priority.HIGH)
        {
            // Need to make sure the right ExperimentDataHandler is associated with this data file, otherwise, you
            // won't be able to delete it because of the foreign key constraint from the flow.object table.
            throw new IllegalStateException("FlowDataHandler must be associated with data file");
        }
        AttrObject newObject = new AttrObject();
        newObject.setContainer(data.getContainer());
        newObject.setDataId(data.getRowId());
        newObject.setTypeId(type.getTypeId());
        if (uri != null)
        {
            newObject.setUri(uri.toString());
        }
        flowObjectModified();
        return Table.insert(null, getTinfoObject(), newObject);
    }


    int MAX_BATCH = 1000;

    private String join(Integer[] oids, int from, int to)
    {
        Iterator i = new ArrayIterator(oids, from, to);
        return StringUtils.join(i, ',');
    }

    private void deleteAttributes(Integer[] oids)
    {
        if (oids.length == 0)
            return;

        SqlExecutor executor = new SqlExecutor(getSchema());

        for (int from = 0, to; from < oids.length; from = to)
        {
            to = from + MAX_BATCH;
            if (to > oids.length)
                to = oids.length;

            String list = join(oids, from, to);
            // XXX: delete no longer referenced statattr afterwards?
            executor.execute("DELETE FROM flow.Statistic WHERE ObjectId IN (" + list + ")");
            executor.execute("DELETE FROM flow.Keyword WHERE ObjectId IN (" + list + ")");
            executor.execute("DELETE FROM flow.Graph WHERE ObjectId IN (" + list + ")");
            executor.execute("DELETE FROM flow.Script WHERE ObjectId IN (" + list + ")");
        }
    }


    private void deleteAttributes(SQLFragment sqlObjectIds)
    {
        DbScope scope = getSchema().getScope();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            Integer[] objids = new SqlSelector(getSchema(), sqlObjectIds).getArray(Integer.class);
            deleteAttributes(objids);
            transaction.commit();
        }
    }

    public void deleteAttributes(ExpData data)
    {
        AttrObject obj = getAttrObject(data);
        if (obj == null)
            return;
        deleteAttributes(new Integer[] {obj.getRowId()});
    }


    private void deleteObjectIds(Integer[] oids, Set<Container> containers)
    {
        DbScope scope = getSchema().getScope();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            deleteAttributes(oids);
            SQLFragment sqlf = new SQLFragment("DELETE FROM flow.Object WHERE RowId IN (" );
            sqlf.append(StringUtils.join(oids,','));
            sqlf.append(")");
            new SqlExecutor(getSchema()).execute(sqlf);
            transaction.commit();
        }
        finally
        {
            for (Container container : containers)
            {
                AttributeCache.uncacheAllAfterCommit(container);
            }
            flowObjectModified();
        }
    }
    

    private void deleteObjectIds(SQLFragment sqlOIDs, Set<Container> containers)
    {
        DbScope scope = getSchema().getScope();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            deleteAttributes(sqlOIDs);
            new SqlExecutor(getSchema()).execute("DELETE FROM flow.Object WHERE RowId IN (" + sqlOIDs.getSQL() + ")", sqlOIDs.getParamsArray());
            transaction.commit();
        }
        finally
        {
            for (Container container : containers)
            {
                AttributeCache.uncacheAllAfterCommit(container);
            }
            flowObjectModified();
        }
    }

    public void deleteData(List<ExpData> datas)
    {
        if (datas.size() == 0)
            return;
        StringBuilder sqlGetOIDs = new StringBuilder("SELECT flow.Object.RowId FROM flow.Object WHERE flow.Object.DataId IN (");
        String comma = "";
        Set<Container> containers = new HashSet<>();
        for (ExpData data : datas)
        {
            sqlGetOIDs.append(comma);
            comma = ", ";
            sqlGetOIDs.append(data.getRowId());
            containers.add(data.getContainer());
        }
        sqlGetOIDs.append(")");
        Integer[] objectIds = new SqlSelector(getSchema(), sqlGetOIDs).getArray(Integer.class);
        if (objectIds.length == 0)
            return;
        deleteObjectIds(objectIds, containers);
    }

    static private String sqlSelectKeyword = "SELECT flow.keyword.value FROM flow.object" +
                                            "\nINNER JOIN flow.keyword on flow.object.rowid = flow.keyword.objectid" +
                                            "\nINNER JOIN flow.KeywordAttr ON flow.KeywordAttr.id = flow.keyword.keywordid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.KeywordAttr.name = ?";
    public String getKeyword(ExpData data, String keyword)
    {
        SqlSelector selector = new SqlSelector(getSchema(), sqlSelectKeyword, data.getRowId(), keyword);
        return selector.getObject(String.class);
    }

    // Select a set of keywords and values.  The keyword name IN clause will be appended.
    static private String sqlSelectKeywords = "SELECT flow.keywordAttr.name, flow.keyword.value FROM flow.object" +
                                            "\nINNER JOIN flow.keyword on flow.object.rowid = flow.keyword.objectid" +
                                            "\nINNER JOIN flow.KeywordAttr ON flow.KeywordAttr.id = flow.keyword.keywordid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.KeywordAttr.name ";
    public Map<String, String> getKeywords(ExpData data, String... keywords)
    {
        SQLFragment sql = new SQLFragment(sqlSelectKeywords, data.getRowId());
        getSchema().getSqlDialect().appendInClauseSql(sql, Arrays.asList(keywords));
        SqlSelector selector = new SqlSelector(getSchema(), sql);

        return selector.fillValueMap(new TreeMap<String, String>());
    }

    static private String sqlDeleteKeyword = "DELETE FROM flow.keyword WHERE ObjectId = ? AND KeywordId = ?";
    static private String sqlInsertKeyword = "INSERT INTO flow.keyword (ObjectId, KeywordId, OriginalKeywordId, Value) VALUES (?, ?, ?, ?)";

    // UNDONE: add audit log entries for keyword updates
    public void setKeyword(Container c, ExpData data, String keyword, String value)
    {
        value = StringUtils.trimToNull(value);
        String oldValue = getKeyword(data, keyword);
        if (Objects.equals(oldValue, value))
        {
            return;
        }
        AttrObject obj = getAttrObject(data);
        if (obj == null)
        {
            throw new IllegalArgumentException("Object not found.");
        }

        ensureKeywordName(c, keyword, false);

        AttributeCache.Entry a = AttributeCache.KEYWORDS.byAttribute(c, keyword);
        assert a != null;
        int preferredId = a.getAliasedId() == null ? a.getRowId() : a.getAliasedId();
        int originalId = a.getRowId();

        DbSchema schema = getSchema();
        try (DbScope.Transaction transaction = schema.getScope().ensureTransaction())
        {
            int rowsDeleted = new SqlExecutor(schema).execute(sqlDeleteKeyword, obj.getRowId(), preferredId);
            if (value != null)
            {
                new SqlExecutor(schema).execute(sqlInsertKeyword, obj.getRowId(), preferredId, originalId, value);
            }
            transaction.commit();
        }
        finally
        {
            AttributeCache.uncacheAllAfterCommit(data.getContainer());
        }

    }

    static private String sqlSelectStat = "SELECT flow.statistic.value FROM flow.object" +
                                            "\nINNER JOIN flow.statistic on flow.object.rowid = flow.statistic.objectid" +
                                            "\nINNER JOIN flow.StatisticAttr ON flow.StatisticAttr.id = flow.statistic.statisticid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.StatisticAttr.name = ?";
    public Double getStatistic(ExpData data, StatisticSpec stat)
    {
        return new SqlSelector(getSchema(), sqlSelectStat, data.getRowId(), stat.toString()).getObject(Double.class);
    }

    static private String sqlSelectGraph = "SELECT flow.graph.data FROM flow.object" +
                                            "\nINNER JOIN flow.graph on flow.object.rowid = flow.graph.objectid" +
                                            "\nINNER JOIN flow.GraphAttr ON flow.GraphAttr.id = flow.graph.graphid" +
                                            "\nWHERE flow.object.dataid = ? AND flow.GraphAttr.name = ?";
    public byte[] getGraphBytes(ExpData data, GraphSpec graph)
    {
        return new SqlSelector(getSchema(), sqlSelectGraph, data.getRowId(), graph.toString()).getObject(byte[].class);
    }

    static private String sqlSelectScript = "SELECT flow.script.text from flow.object" +
                                            "\nINNER JOIN flow.script ON flow.object.rowid = flow.script.objectid" +
                                            "\nWHERE flow.object.dataid = ?";
    public String getScript(ExpData data)
    {
        return new SqlSelector(getSchema(), sqlSelectScript, data.getRowId()).getObject(String.class);
    }

    public void setScript(User user, ExpData data, String scriptText) throws SQLException
    {
        AttrObject obj = getAttrObject(data);
        if (obj == null)
        {
            obj = createAttrObject(data, ObjectType.script, null);
        }
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("ObjectId"), obj.getRowId());
        Script script = new TableSelector(getTinfoScript(), filter, null).getObject(Script.class);
        if (script == null)
        {
            script = new Script();
            script.setObjectId(obj.getRowId());
            script.setText(scriptText);
            script = Table.insert(user, getTinfoScript(), script);
        }
        else
        {
            script.setText(scriptText);
            script = Table.update(user, getTinfoScript(), script, script.getRowId());
        }
    }

    public int getObjectCount(Container container, ObjectType type)
    {
        String sqlFCSFileCount = "SELECT COUNT(flow.object.rowid) FROM flow.object\n" +
                "WHERE flow.object.container = ? AND flow.object.typeid = ?";
        return new SqlSelector(getSchema(), sqlFCSFileCount, container.getId(), type.getTypeId()).getObject(Integer.class);
    }

    // CONSIDER: move to experiment module
    public int getFlaggedCount(Container container)
    {
        String sql = "SELECT COUNT(OP.objectid) FROM exp.object OB, exp.objectproperty OP, exp.propertydescriptor PD\n" +
                "WHERE OB.container = ? AND\n" +
                "OB.objectid = OP.objectid AND\n" +
                "OP.propertyid = PD.propertyid AND\n" +
                "PD.propertyuri = '" + ExperimentProperty.COMMENT.getPropertyDescriptor().getPropertyURI() + "'";
        return new SqlSelector(getSchema(), sql, container.getId()).getObject(Integer.class);
    }

    // counts FCSFiles in Keyword runs
    public int getFCSFileCount(User user, Container container)
    {
        FlowSchema schema = new FlowSchema(user, container);

        // count(fcsfile)
        TableInfo table = schema.getTable(FlowTableType.FCSFiles);
        List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.BaseType.COUNT));
        List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));

        // filter to those wells that were imported from a Keywords run
        // ignoring 'fake' FCSFiles created while importing a FlowJo workspace.
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("Original"), true, CompareType.EQUAL);

        Map<String, List<Aggregate.Result>> agg = new TableSelector(table, columns, filter, null).getAggregates(aggregates);
        //TODO: multiple aggregates
        Aggregate.Result result = agg.get(aggregates.get(0).getColumnName()).get(0);
        if (result != null && result.getValue() instanceof Number)
            return ((Number)result.getValue()).intValue();

        return 0;
    }

    // count FCSFiles with or without samples
    public int getFCSFileSamplesCount(User user, Container container, boolean hasSamples)
    {
        FlowSchema schema = new FlowSchema(user, container);

        TableInfo table = schema.getTable(FlowTableType.FCSFiles);
        List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.BaseType.COUNT));
        List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Sample", "Name"), null, hasSamples ? CompareType.NONBLANK : CompareType.ISBLANK);

        Map<String, List<Aggregate.Result>> agg = new TableSelector(table, columns, filter, null).getAggregates(aggregates);
        //TODO: multiple aggregates
        Aggregate.Result result = agg.get(aggregates.get(0).getColumnName()).get(0);
        if (result != null && result.getValue() instanceof Number)
            return ((Number)result.getValue()).intValue();

        return 0;
    }

    // counts Keyword runs
    public int getFCSFileOnlyRunsCount(User user, Container container)
    {
        FlowSchema schema = new FlowSchema(user, container);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("FCSFileCount"), 0, CompareType.NEQ);
        filter.addCondition(FieldKey.fromParts("ProtocolStep"), "Keywords", CompareType.EQUAL);
        TableInfo table = schema.getTable(FlowTableType.Runs);
        List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.BaseType.COUNT));
        List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));
        Map<String, List<Aggregate.Result>> agg = new TableSelector(table, columns, filter, null).getAggregates(aggregates);
        Aggregate.Result result = agg.get("RowId").get(0);
        if (result != null && result.getValue() instanceof Number)
            return ((Number)result.getValue()).intValue();

        return 0;
    }

    public int getRunCount(Container container, ObjectType type)
    {
        String sqlFCSRunCount = "SELECT COUNT (exp.ExperimentRun.RowId) FROM exp.experimentrun\n" +
                "WHERE exp.ExperimentRun.RowId IN (" +
                "SELECT exp.data.runid FROM exp.data INNER JOIN flow.object ON flow.object.dataid = exp.data.rowid\n" +
                "AND exp.data.container = ?\n" +
                "AND flow.object.container = ?\n" +
                "AND flow.object.typeid = ?)";
        return new SqlSelector(getSchema(), sqlFCSRunCount, container.getId(), container.getId(), type.getTypeId()).getObject(Integer.class);
    }

    public int getFCSRunCount(Container container)
    {
        String sqlFCSRunCount = "SELECT COUNT (exp.ExperimentRun.RowId) FROM exp.experimentrun\n" +
                "WHERE exp.ExperimentRun.RowId IN (" +
                "SELECT exp.data.runid FROM exp.data INNER JOIN flow.object ON flow.object.dataid = exp.data.rowid\n" +
                "AND exp.data.container = ?\n" +
                "AND flow.object.container = ?\n" +
                "AND flow.object.typeid = ?) AND exp.ExperimentRun.FilePathRoot IS NOT NULL";
        return new SqlSelector(getSchema(), sqlFCSRunCount, container.getId(), container.getId(), ObjectType.fcsKeywords.getTypeId()).getObject(Integer.class);
    }

    public void deleteContainer(Container container)
    {
        SQLFragment sqlOIDs = new SQLFragment("SELECT flow.object.rowid FROM flow.object INNER JOIN exp.data ON flow.object.dataid = exp.data.rowid AND exp.data.container = ?", container.getId());
        deleteObjectIds(sqlOIDs, Collections.singleton(container));
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTinfoKeywordAttr() + " WHERE container=?", container);
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTinfoStatisticAttr() + " WHERE container=?", container);
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTinfoGraphAttr() + " WHERE container=?", container);
    }


    /**
     * this is a bit of a hack
     * script job and WorkspaceJob.createExperimentRun() do not update these new fields
     */
    public void updateFlowObjectCols(Container c)
    {
        DbSchema s = getSchema();
        TableInfo o = getTinfoObject();
        try (DbScope.Transaction transaction = s.getScope().ensureTransaction())
        {
            if (o.getColumn("container") != null)
            {
                new SqlExecutor(s).execute(
                        "UPDATE flow.object "+
                        "SET container = ? " +
                        "WHERE container IS NULL AND dataid IN (select rowid from exp.data WHERE exp.data.container = ?)", c.getId(), c.getId());
            }

            if (o.getColumn("compid") != null)
            {
                // Update FCSAnalysis and FCSFile rows to point to their inputs.
                // The 'fake' workspace FCSFiles may have original FCSFile as inputs.
                new SqlExecutor(s).execute(
                        "UPDATE flow.object SET "+
                        "compid = COALESCE(compid,"+
                        "    (SELECT MIN(DI.dataid) "+
                        "    FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid "+
                        "    WHERE D.rowid = flow.object.dataid AND INPUT.typeid=4)), " +
                        "fcsid = COALESCE(fcsid,"+
                        "    (SELECT MIN(DI.dataid) "+
                        "    FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid "+
                        "    WHERE D.rowid = flow.object.dataid AND INPUT.typeid=1)), " +
                        "scriptid = COALESCE(scriptid,"+
                        "    (SELECT MIN(DI.dataid) "+
                        "    FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid "+
                        "    WHERE D.rowid = flow.object.dataid AND INPUT.typeid IN (5,7))) " +
                        "WHERE dataid IN (select rowid from exp.data where exp.data.container = ?) AND typeid IN (1,3) AND (compid IS NULL OR fcsid IS NULL OR scriptid IS NULL)", c.getId());
            }
            transaction.commit();
        }
        finally
        {
            flowObjectModified();
        }
    }

}
