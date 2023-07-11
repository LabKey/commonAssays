/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpSampleType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.exp.query.SamplesSchema;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.FlowModule;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.labkey.flow.data.AttributeType.keyword;

public class FlowManager
{
    private static final FlowManager instance = new FlowManager();
    private static final Logger _log = LogManager.getLogger(FlowManager.class);
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
     * Get the FlowEntry for the attribute name, matching exact casing.
     * DOES NOT CACHE.
     *
     * @param containerId The container.
     * @param type The attribute type.
     * @param attr The attribute name.
     * @return The FlowEntry of the attribute or null if not found.
     */
    public FlowEntry getAttributeEntry(String containerId, AttributeType type, String attr)
    {
        return getAttributeEntryCaseSensitive(containerId, type, attr);
    }

    private FlowEntry getAttributeEntryCaseSensitive(String containerId, AttributeType type, String attr)
    {
        //_log.info("getAttributeEntryCaseSensitive(" + containerId + ", " + type + ", " + attr + ")");
        SQLFragment sql = new SQLFragment("SELECT Name, RowId, Id FROM ")
                .append(attributeTable(type))
                .append(" WHERE Container = ?").add(containerId);
        if (FlowManager.get().getSchema().getSqlDialect().isSqlServer())
        {
            sql.append(" AND cast(Name AS VARBINARY(512)) = cast(? AS VARBINARY(512))").add(attr);
        }
        else
        {
            sql.append(" AND Name = ?").add(attr);
        }

        try (ResultSet rs = new SqlSelector(getSchema(), sql).getResultSet())
        {
            // we're not caching misses because this is an unlimited cachemap
            if (!rs.next())
                return null;

            String name = rs.getString("Name");
            Integer rowId = rs.getInt("RowId");
            Integer aliasId = rs.getInt("Id");
            FlowEntry a = new FlowEntry(type, rowId, containerId, name, aliasId);
            return a;
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    // Get list of entries sorted by name that match the 'attr' case-insensitively
    private List<FlowEntry> getAttributeEntryCaseInsensitive(String containerId, AttributeType type, String attr)
    {
        //_log.info("getAttributeEntryCaseInsensitive(" + containerId + ", " + type + ", " + attr + ")");
        SQLFragment sql = new SQLFragment("SELECT Name, RowId, Id FROM ")
                .append(attributeTable(type))
                .append(" WHERE Container = ?").add(containerId)
                .append(" AND lower(Name) = lower(?)").add(attr);

        return new SqlSelector(getSchema(), sql).mapStream().map(map -> {
            String name = (String)map.get("Name");
            Integer rowId = (Integer)map.get("RowId");
            Integer aliasId = (Integer)map.get("Id");
            FlowEntry a = new FlowEntry(type, rowId, containerId, name, aliasId);
            return a;
        }).sorted().collect(Collectors.toList());
    }


    /**
     * Get an entry by type and rowId.
     * DOES NOT USE CACHE
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

    /**
     * Ensure the attribute exists.  If the aliasId >= 0, the aliasId points at the RowId of the preferred name for the attribute.
     * If an existing alias with different casing is found, an exception is thrown.
     * DOES NOT CACHE.  CALLERS SHOULD CLEAR CACHE APPROPRIATELY.
     *
     * @param container Container
     * @param sampleLabel Sample name associated with this attribute -- for error message reporting only
     * @param type attribute type
     * @param attr attribute name
     * @param aliasId RowId of aliased attribute or -1 to set alias to itself.
     * @param allowCaseChangeAlias When true, allow an attribute to be registered as an alias of another attribute if it differs by casing.
     * @return The RowId of the newly inserted or existing attribute.
     */
    private int ensureAttributeName(@NotNull Container container, @Nullable String sampleLabel, @NotNull AttributeType type, @NotNull String attr, int aliasId, boolean allowCaseChangeAlias)
    {
        // Get case-sensitivity rule
        final FlowProtocol protocol = FlowProtocol.getForContainer(container);
        boolean caseSensitive = protocol != null ? type.isCaseSensitive(protocol) : true;

        final String containerId = container.getId();

        //_log.info("ensureAttributeName(" + containerId + ", " + type + ", " + attr + ", " + aliasId + ")");
        DbSchema schema = getSchema();
        if (schema.getScope().isTransactionActive())
        {
            throw new IllegalStateException("ensureAttributeName cannot be called within a transaction");
        }

        // Validate the name
        if (attr == null || attr.length() == 0)
            throw new IllegalArgumentException("Name must not be null");

        // Validate that name can be parsed as the given attribute type
        Object attribute = type.createAttribute(attr);

        // Get existing attribute entry using exact casing
        FlowEntry existing = getAttributeEntryCaseSensitive(containerId, type, attr);
        if (existing != null)
            return existing._rowId;


        // If exact casing is not found, check if there are any existing attributes with different casing
        // If a collision is found, an exception is thrown to prevent inserting a duplicate
        List<FlowEntry> others = getAttributeEntryCaseInsensitive(containerId, type, attr);
        if (!others.isEmpty())
        {
            if (!caseSensitive)
            {
                // Use the first attribute, sorted by case
                int rowId = others.get(0)._rowId;

                // If more than one attribute matches, check that all are pointing to the same preferred attribute
                if (others.size() > 1)
                {
                    int preferredId = others.get(0)._aliasId;
                    if (others.size() > 1 && others.stream().anyMatch(item -> item._aliasId != preferredId))
                        throw new FlowCasingMismatchException("Can't create " + type + " with same casing as other " + type + "s when there is more than one preferred attribute.", sampleLabel, type, others, attr);
                }

                // everything is a-ok - but let's just log a message for goodness
                _log.info(FlowCasingMismatchException.casingMismatchMessage("Using existing attribute " + rowId + " with different casing", sampleLabel, type, others, attr));
                return rowId;
            }

            // Issue 37449: casing mismatch: check if we have an alias of the provided casing
            // Disallow creating a new entry unless we are creating an alias and the allowCaseChangeAlias flag is true
            if (!allowCaseChangeAlias || aliasId <= 0)
                throw new FlowCasingMismatchException("Can't create " + type + " with same casing as other " + type + "s.", sampleLabel, type, others, attr);

            // If we allow an alias to be created for an item that differs only by case,
            // the 'to-be-aliased' item must be present in the set of alternate casings.
            FlowEntry itemToBeAliased = others.stream().filter(item -> item._rowId == aliasId).findFirst().orElse(null);
            if (itemToBeAliased == null)
                throw new FlowCasingMismatchException("Item to be aliased wasn't found in the set of alternate cased items", sampleLabel, type, others, attr);

            // everything is a-ok - but let's just log a message for goodness
            _log.info(FlowCasingMismatchException.casingMismatchMessage("Creating alias", sampleLabel, type, others, attr));
        }


        Map<String, Object> map = new HashMap<>();
        map.put("Container", containerId);
        map.put("Name", attr);
        map.put("Id", aliasId);

        TableInfo table = attributeTable(type);
        map = Table.insert(null, table, map);

        // Set Id to RowId if we aren't inserting an alias
        Integer rowId = (Integer)map.get("RowId");
        assert rowId != null;
        if (aliasId <= 0)
        {
            map.put("Id", map.get("RowId"));
            Table.update(null, table, map, rowId);
        }

        return rowId;
    }

    private int ensureAttributeName(Container container, String sampleLabel, AttributeType type, String name)
    {
        return ensureAttributeName(container, sampleLabel, type, name, -1, false);
    }


    public int ensureStatisticName(Container c, String sampleLabel, String name, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, sampleLabel, AttributeType.statistic, name, Collections.emptyList(), uncache);
    }


    public int ensureKeywordName(Container c, String sampleLabel, String name, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, sampleLabel, keyword, name, Collections.emptyList(), uncache);
    }


    public int ensureGraphName(Container c, String sampleLabel, String name, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, sampleLabel, AttributeType.graph, name, Collections.emptyList(), uncache);
    }


    public int ensureStatisticNameAndAliases(Container c, String sampleLabel, String name, Collection<? extends Object> aliases, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, sampleLabel, AttributeType.statistic, name, aliases, uncache);
    }


    public int ensureKeywordNameAndAliases(Container c, String sampleLabel, String name, Collection<? extends Object> aliases, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, sampleLabel, keyword, name, aliases, uncache);
    }


    public int ensureGraphNameAndAliases(Container c, String sampleLabel, String name, Collection<? extends Object> aliases, boolean uncache)
    {
        return ensureAttributeNameAndAliases(c, sampleLabel, AttributeType.graph, name, aliases, uncache);
    }


    private int ensureAttributeNameAndAliases(Container c, String sampleLabel, AttributeType type, String name, Collection<? extends Object> aliases, boolean uncache)
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
                FlowEntry entry = getAttributeEntryCaseSensitive(c.getId(), type, s);
                if (entry != null)
                {
                    aliasId = entry._aliasId;
                    break;
                }
            }

            // If no existing primary attribute was found, insert the provided name as the preferred attribute name
            // otherwise, add the name as an alias of the preferred attribute name.
            if (aliasId == null)
                aliasId = ensureAttributeName(c, sampleLabel, type, name);
            else
                ensureAttributeName(c, sampleLabel, type, name, aliasId, false);

            if (!aliases.isEmpty())
            {
                FlowEntry entry = getAttributeEntryForAliasing(type, aliasId);
                for (Object alias : aliases)
                    ensureAlias(entry, alias.toString(), false, false, false);
            }

            return aliasId;
        }
        finally
        {
            if (uncache)
                AttributeCache.forType(type).uncacheNow(c);
        }
    }

    @NotNull
    private FlowEntry getAttributeEntryForAliasing(@NotNull AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            throw new IllegalArgumentException("Attribute not found");

        if (entry.isAlias())
            throw new IllegalArgumentException("Can't create alias of an alias");

        return entry;
    }

    public void ensureAlias(@NotNull AttributeType type, int rowId, @NotNull String aliasName, boolean allowCaseChangeAlias, boolean transact, boolean uncache)
    {
        ensureAlias(getAttributeEntryForAliasing(type, rowId), aliasName, allowCaseChangeAlias, transact, uncache);
    }

    private void ensureAlias(@NotNull FlowEntry entry, @NotNull String aliasName, boolean allowCaseChangeAlias, boolean transact, boolean uncache)
    {
        final AttributeType type = entry._type;
        final int rowId = entry._rowId;

        Container c = ContainerManager.getForId(entry._containerId);
        if (c == null)
            throw new IllegalArgumentException("Container not found: " + entry._containerId);

        // Find existing attribute for the provided alias name
        FlowEntry existing = getAttributeEntryCaseSensitive(entry._containerId, type, aliasName);
        if (existing != null)
        {
            try (DbScope.Transaction tx = transact ? getSchema().getScope().ensureTransaction() : DbScope.NO_OP_TRANSACTION)
            {
                if (entry.equals(existing))
                    return;

                // If this existing entry is already an alias of entry, do nothing
                if (existing._aliasId.equals(entry._rowId))
                    return;

                // If this existing entry doesn't have any aliases, we can make this existing entry an alias of the entry.
                if (existing.isAlias())
                    throw new IllegalArgumentException("The " + type.name() + " attribute '" + aliasName + "' is already an alias of '" + getAttributeEntry(type, existing._aliasId)._name + "'");

                if (!getAliases(type, existing._rowId).isEmpty())
                    throw new IllegalArgumentException("The " + type.name() + " attribute '" + aliasName + "' has aliases and can't be made an alias of '" + entry._name + "'");

                // update usages of the existing attribute to point at the new parent, keeping the original attribute id the same
                updateAttributeValuesPreferredId(existing._containerId, type, existing._rowId, entry._rowId);

                // parent the existing entry to the other attribute
                updateAttribute(c, existing, existing._name, rowId, uncache);
                tx.commit();
            }
            finally
            {
                if (uncache)
                    AttributeCache.forType(type).uncacheNow(c);
            }
        }
        else
        {
            // attribute wasn't found for the aliasName, so insert a new one
            // NOTE: Alias will fail to insert if an there is an existing attribute with different casing and allowCaseChangeAlias is false
            try
            {
                ensureAttributeName(c, null, type, aliasName, entry._rowId, allowCaseChangeAlias);
            }
            finally
            {
                if (uncache)
                    AttributeCache.forType(type).uncacheNow(c);
            }
        }
    }

    public void updateAttribute(@NotNull Container container, @NotNull AttributeType type, int rowId, @NotNull String name, int aliasId, boolean uncache)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            throw new IllegalArgumentException("Attribute not found");

        updateAttribute(container, entry, name, aliasId, uncache);
    }

    private void updateAttribute(@NotNull Container container, @NotNull FlowEntry entry, @NotNull String name, int aliasId, boolean uncache)
    {
        final AttributeType type = entry._type;
        final int rowId = entry._rowId;

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
                AttributeCache.forType(type).uncacheNow(container);
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

    public void deleteAttribute(@NotNull Container c, AttributeType type, int rowId, boolean uncache)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return;

        if (!c.getId().equals(entry._containerId))
            throw new IllegalArgumentException("container");

        Collection aliases = getAliases(entry);
        if (!aliases.isEmpty())
            throw new IllegalArgumentException(type + " '" + entry._name + "' has " + aliases.size() + " aliases and can't be deleted");

        Collection<FlowDataObject> usages = FlowManager.get().getUsages(entry);
        if (!usages.isEmpty())
            throw new IllegalArgumentException(type + " '" + entry._name + "' has " + usages.size() + " usages and can't be deleted");

        try
        {
            Table.delete(attributeTable(type), entry._rowId);
        }
        finally
        {
            if (uncache)
                AttributeCache.forType(type).uncacheNow(c);
        }
    }


    /**
     * Checks for existence of a alias by type and rowId.
     * DOES NOT USE CACHE
     */
    public boolean isAlias(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return false;

        return entry.isAlias();
    }

    /**
     * Return the preferred name for the rowId or null if rowId is not an alias id.
     * DOES NOT USE CACHE
     */
    public FlowEntry getAliased(AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null || !entry.isAlias())
            return null;

        return getAttributeEntry(type, entry._aliasId);
    }

    /**
     * Return the preferred/primary name for the rowId or null if rowId is not an alias id.
     * DOES NOT USE CACHE
     */
    public FlowEntry getAliased(FlowEntry entry)
    {
        if (entry == null || !entry.isAlias())
            return null;

        return getAttributeEntry(entry._type, entry._aliasId);
    }

    /**
     * Get aliases for the preferred/primary attribute rowId or empty collection if rowId is not a preferred attribute.
     * DOES NOT USE CACHE
     */
    public Collection<FlowEntry> getAliases(final AttributeType type, int rowId)
    {
        FlowEntry entry = getAttributeEntry(type, rowId);
        if (entry == null)
            return Collections.emptyList();

        return getAliases(entry);
    }

    /**
     * Get aliases for the preferred/primary attribute rowId or empty collection if rowId is not a preferred attribute.
     * DOES NOT USE CACHE
     */
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
        selector.forEachMap(row -> {
            Integer rowId = (Integer)row.get("RowId");
            String name = (String)row.get("Name");
            String containerId = (String)row.get("Container");
            Integer aliasId = (Integer)row.get("Id");
            FlowEntry alias = new FlowEntry(entry._type, rowId, containerId, name, aliasId);

            aliases.add(alias);
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
        selector.forEachMap(row -> {
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
        selector.forEachMap(row -> {
            Integer rowId = (Integer)row.get("RowId");
            String name = (String)row.get("Name");
            String containerId = (String)row.get("Container");
            Integer aliasId = (Integer)row.get("Id");
            FlowEntry alias = new FlowEntry(type, rowId, containerId, name, aliasId);

            unused.add(alias);
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
        return getUsages(getAttributeEntry(type, rowId));
    }

    /**
     * Get usages for an attribute, excluding its aliases.
     */
    public Collection<FlowDataObject> getUsages(FlowEntry entry)
    {
        if (entry == null)
            return Collections.emptyList();

        TableInfo attrTable = attributeTable(entry._type);
        TableInfo valueTable = valueTable(entry._type);
        String valueTableAttrIdColumn = valueTableAttrIdColumn(entry._type);
        String valueTableOriginalAttrIdColumn = valueTableOriginalAttrIdColumn(entry._type);

        SQLFragment sql = new SQLFragment()
                .append("SELECT fo.rowid, fo.dataid,")
                .append(" val.").append(valueTableAttrIdColumn).append(" AS AttrId,")
                .append(" val.").append(valueTableOriginalAttrIdColumn).append(" AS OriginalAttrId\n")
                .append("FROM ")
                .append(valueTable, "val").append(", ")
                .append(getTinfoObject(), "fo").append("\n")
                .append("WHERE fo.rowid = val.objectid\n")
                .append("  AND val.").append(valueTableOriginalAttrIdColumn).append(" = ").append(entry._rowId).append("\n");

        final List<FlowDataObject> usages = new ArrayList<>();
        SqlSelector selector = new SqlSelector(getSchema(), sql);
        selector.forEachMap(row -> {
            Integer dataId = (Integer)row.get("DataId");

            FlowDataObject fdo = FlowDataObject.fromRowId(dataId);
            usages.add(fdo);
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
        selector.forEachMap(row -> {
            Integer attributeRowId = (Integer)row.get("OriginalAttrId");
            Integer dataId = (Integer)row.get("DataId");

            Collection<FlowDataObject> datas = usages.get(attributeRowId);
            if (datas == null)
                usages.put(attributeRowId, datas = new ArrayList<>());

            FlowDataObject fdo = FlowDataObject.fromRowId(dataId);
            datas.add(fdo);
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
        _log.debug("flow object modification bump");
        flowObjectModificationCount.incrementAndGet();
    }

    
    public AttrObject createAttrObject(ExpData data, ObjectType type, URI uri)
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
            newObject.setUri(FileUtil.uriToString(uri));
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

    public void setKeyword(Container c, User user, ExpData data, String keyword, String value)
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

        String sampleLabel = data.getName();
        ensureKeywordName(c, sampleLabel, keyword, true);

        AttributeCache.Entry a = AttributeCache.KEYWORDS.byAttribute(c, keyword);
        assert a != null : "Expected to find keyword entry for '" + keyword + "'";
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
            addKeywordAuditEvent(c, user, keyword, value, oldValue, data.getName(), data.getLSID());
            transaction.commit();
        }

    }

    private void addKeywordAuditEvent(Container c, User user, String keyword, String newValue, String oldValue, String fileName, String lsid)
    {

        FlowKeywordAuditProvider.FlowKeywordAuditEvent event =  new FlowKeywordAuditProvider.FlowKeywordAuditEvent(c.getId(),"keyword");
        event.setKeywordName(keyword);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setFile(fileName);
        event.setLsid(lsid);
        AuditLogService.get().addEvent(user, event);
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

    public void setScript(User user, ExpData data, String scriptText)
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

    // Get usage metrics for all flow containers
    public Map<String, Object> getUsageMetrics()
    {
        _log.debug("Collecting flow usage metrics");

        return DbScope.getLabKeyScope().executeWithRetryReadOnly( (tx) -> {
            User user = User.getSearchUser();

            Map<String, Object> allMetrics = new HashMap<>();
            allMetrics.put("flowTempTableCount", getTempTableCount());

            // distinct keyword/statistic/graph count, alias count
            allMetrics.put("keyword", getAttributeMetrics(AttributeType.keyword));
            allMetrics.put("statistic", getAttributeMetrics(AttributeType.statistic));
            allMetrics.put("graph", getAttributeMetrics(AttributeType.graph));

            final FlowModule flowModule = ModuleLoader.getInstance().getModule(FlowModule.class);
            Set<Container> containers = ContainerManager.getAllChildrenWithModule(ContainerManager.getRoot(), flowModule);
            for (Container c : containers)
            {
                Map<String, Object> containerMetrics = getUsageMetrics(user, c, false);
                merge(allMetrics, containerMetrics);
            }

            _log.debug("Collected flow usage metrics:\n" + allMetrics);
            return allMetrics;
        });
    }

    /**
     * merge the values from <code>map</code> into the target map.
     * - numbers are summed
     * - dates are maxed
     * - maps are recursively merged
     */
    private static void merge(Map<String, Object> into, Map<String, Object> map)
    {
        for (Map.Entry<String, Object> kv : map.entrySet())
        {
            String key = kv.getKey();
            Object value = kv.getValue();
            if (value == null)
                continue;

            if (value instanceof Number)
            {
                // sum
                into.merge(key, value, (a,b) -> Long.sum(((Number)a).longValue(), ((Number)b).longValue()));
            }
            else if (value instanceof Date)
            {
                // max
                into.merge(key, value, (a,b) -> {
                    Date aDate = (Date)a;
                    Date bDate = (Date)b;
                    return aDate.after(bDate) ? aDate : bDate;
                });
            }
            else if (value instanceof Map)
            {
                var intoSub = (Map<String, Object>)into.computeIfAbsent(key, k -> new HashMap<String, Object>());
                merge(intoSub, (Map<String,Object>)value);
            }
            else
            {
                throw new IllegalStateException("unexpected type " + value.getClass());
            }
        }
    }

    // Get usage metrics in a single container
    public Map<String, Object> getUsageMetrics(User user, Container c, boolean includeAttributeCounts)
    {
        _log.debug("Collecting metrics in '" + c.getPath() + "'");
        Map<String, Object> metrics = new LinkedHashMap<>();

        FlowSchema schema = new FlowSchema(user, c);
        metrics.put("scriptRuns", getAnalysisScriptRunCount(schema));
        metrics.put("workspaceRuns", getWorkspaceRunCount(schema));
        metrics.put("externalRuns", getExternalAnalysisRunCount(schema));
        metrics.put("fcsFileRuns", getFCSFileOnlyRunCount(schema));

        metrics.put("analysisFolderCount", getAnalysisFolderCount(schema));
        metrics.put("analysisScriptCount", getAnalysisScriptCount(schema));

        // protocol properties
        Map<String, Object> protocolMetrics = new LinkedHashMap<>();
        metrics.put("protocol", protocolMetrics);
        FlowProtocol protocol = FlowProtocol.getForContainer(c);
        if (protocol != null)
        {
            ExpSampleType st = protocol.getSampleType(user);
            if (st != null)
            {
                // put sample count at top-level since it isn't really specific to the protocol
                metrics.put("sampleCount", getSampleCount(c, user, st));
            }

            ICSMetadata metadata = protocol.getICSMetadata();
            if (metadata != null)
            {
                protocolMetrics.put("hasMetadata", 1);
                if (metadata.hasCompleteStudyMeta())
                    protocolMetrics.put("hasStudyMeta", 1);
                if (metadata.hasCompleteBackground())
                    protocolMetrics.put("hasCompleteBackground", 1);
            }

            if (!protocol.getSampleTypeJoinFields().isEmpty())
                protocolMetrics.put("hasSampleJoin", 1);

            if (protocol.isCaseSensitiveKeywords())
                protocolMetrics.put("caseSensitiveKeywords", 1);
            if (protocol.isCaseSensitiveStatsAndGraphs())
                protocolMetrics.put("caseSensitiveStatsAndGraphs", 1);
        }

        if (includeAttributeCounts)
        {
            // distinct keyword/statistic/graph count, alias count
            metrics.put("keyword", getAttributeMetrics(AttributeType.keyword));
            metrics.put("statistic", getAttributeMetrics(AttributeType.statistic));
            metrics.put("graph", getAttributeMetrics(AttributeType.graph));
        }

        _log.debug("Collected metrics in '" + c.getPath() + "':\n" + metrics);
        return metrics;
    }

    // get the count of analysis folders
    public int getAnalysisFolderCount(FlowSchema schema)
    {
        return QueryService.get().selector(schema, "SELECT COUNT(*) FROM " + FlowSchema.SCHEMANAME + "." + FlowTableType.Analyses.name()).getObject(Long.class).intValue();
    }

    // get the count of analysis scripts
    public int getAnalysisScriptCount(FlowSchema schema)
    {
        return QueryService.get().selector(schema, "SELECT COUNT(*) FROM " + FlowSchema.SCHEMANAME + "." + FlowTableType.AnalysisScripts.name()).getObject(Long.class).intValue();
    }

    // get the count of samples in the sample set
    public int getSampleCount(Container c, User user, ExpSampleType st)
    {
        UserSchema schema = QueryService.get().getUserSchema(user, c, SamplesSchema.SCHEMA_SAMPLES);
        return QueryService.get().selector(schema, "SELECT COUNT(*) FROM samples." + st.getName()).getObject(Long.class).intValue();
    }

    public int getTempTableCount()
    {
        return new SqlSelector(DbScope.getLabKeyScope(), "SELECT COUNT(*) FROM information_schema.tables \n" +
                "WHERE TABLE_SCHEMA = 'temp' and (table_name LIKE 'ffo%' or table_name LIKE 'fbg%')").getArrayList(Integer.class).get(0);
    }

    public Map<String, Object> getAttributeMetrics(AttributeType attributeType)
    {
        TableInfo table = attributeType.getAttributeTable();
        return removeRowNum(new SqlSelector(DbScope.getLabKeyScope(), new SQLFragment(
                "SELECT " +
                        "COUNT(*) AS Count, " +
                        "SUM(CASE WHEN a.id != a.rowid THEN 1 ELSE 0 END) AS AliasCount, " +
                        "COUNT(DISTINCT(a.name)) AS DistinctCount, " +
                        "COUNT(DISTINCT(LOWER(a.name))) AS DistinctLowerCount " +
                        "FROM "
        ).append(table, "a")).getMap());
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
        TableInfo table = schema.getTable(FlowTableType.FCSFiles, null);
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

        TableInfo table = schema.getTable(FlowTableType.FCSFiles, null);
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
        TableInfo table = schema.getTable(FlowTableType.Runs, null);
        List<Aggregate> aggregates = Collections.singletonList(new Aggregate("RowId", Aggregate.BaseType.COUNT));
        List<ColumnInfo> columns = Collections.singletonList(table.getColumn("RowId"));
        Map<String, List<Aggregate.Result>> agg = new TableSelector(table, columns, filter, null).getAggregates(aggregates);
        Aggregate.Result result = agg.get("RowId").get(0);
        if (result != null && result.getValue() instanceof Number)
            return ((Number)result.getValue()).intValue();

        return 0;
    }

    public Map<String, Object> removeRowNum(Map<String, Object> row)
    {
        row.remove("_row");
        return row;
    }

    // count of runs created from an Analysis Script
    public Map<String, Object> getAnalysisScriptRunCount(FlowSchema schema)
    {
        return removeRowNum(QueryService.get().selector(schema,
                "SELECT\n" +
                "  COUNT(*) AS RunCount,\n" +
                "  MAX(Created) AS CreatedMax,\n" +
                "  SUM(FCSAnalysisCount) AS FCSAnalysisCount,\n" +
                "  SUM(CompensationControlCount) AS CompControlCount,\n" +
                "  SUM(FCSFileCount) AS FCSFileCount\n" +
                "FROM flow.Runs\n" +
                "WHERE AnalysisScript IS NOT NULL").getMap());
    }

    // count of runs created from a FlowJo Workspace
    public Map<String, Object> getWorkspaceRunCount(FlowSchema schema)
    {
        return removeRowNum(QueryService.get().selector(schema,
                "SELECT\n" +
                "  COUNT(*) AS RunCount,\n" +
                "  MAX(Created) AS CreatedMax,\n" +
                "  SUM(FCSAnalysisCount) AS FCSAnalysisCount,\n" +
                "  SUM(CompensationControlCount) AS CompControlCount,\n" +
                "  SUM(FCSFileCount) AS FCSFileCount\n" +
                "FROM flow.Runs\n" +
                "WHERE Workspace IS NOT NULL").getMap());
    }

    // count of runs created from an analysis archive import
    public Map<String, Object> getExternalAnalysisRunCount(FlowSchema schema)
    {
        return removeRowNum(QueryService.get().selector(schema,
                "SELECT\n" +
                "  COUNT(*) AS RunCount,\n" +
                "  MAX(Created) AS CreatedMax,\n" +
                "  SUM(FCSAnalysisCount) AS FCSAnalysisCount,\n" +
                "  SUM(CompensationControlCount) AS CompControlCount,\n" +
                "  SUM(FCSFileCount) AS FCSFileCount\n" +
                "FROM flow.Runs\n" +
                "WHERE AnalysisEngine = '" + AnalysisEngine.Archive.name() + "'").getMap());
    }

    public Map<String, Object> getFCSFileOnlyRunCount(FlowSchema schema)
    {
        return removeRowNum(QueryService.get().selector(schema,
                "SELECT\n" +
                "  COUNT(*) AS RunCount,\n" +
                "  MAX(Created) AS CreatedMax,\n" +
                "  SUM(FCSAnalysisCount) AS FCSAnalysisCount,\n" +
                "  SUM(CompensationControlCount) AS CompControlCount,\n" +
                "  SUM(FCSFileCount) AS FCSFileCount\n" +
                "FROM flow.Runs\n" +
                "WHERE ProtocolStep = 'Keywords'").getMap());
    }

    public int getRunCount(Container container, ObjectType type)
    {
        String sqlFCSRunCount = """
                SELECT COUNT (DISTINCT d.runid) FROM exp.data d INNER JOIN flow.object o ON
                o.dataid = d.rowid
                AND d.container = ?
                AND d.container = o.container
                AND o.typeid = ?""";
        return new SqlSelector(getSchema(), sqlFCSRunCount, container.getId(), type.getTypeId()).getObject(Integer.class);
    }

    public int getFCSRunCount(Container container)
    {
        String sqlFCSRunCount = """
                SELECT COUNT (r.RowId) FROM exp.experimentrun r
                WHERE r.RowId IN (SELECT d.runid FROM exp.data d INNER JOIN flow.object o ON
                o.dataid = d.rowid
                AND d.container = ?
                AND d.container = o.container
                AND o.typeid = ?) AND r.FilePathRoot IS NOT NULL""";
        return new SqlSelector(getSchema(), sqlFCSRunCount, container.getId(), ObjectType.fcsKeywords.getTypeId()).getObject(Integer.class);
    }

    public void deleteContainer(Container container)
    {
        SQLFragment sqlOIDs = new SQLFragment("SELECT flow.object.rowid FROM flow.object INNER JOIN exp.data ON flow.object.dataid = exp.data.rowid AND exp.data.container = ?", container.getId());
        deleteObjectIds(sqlOIDs, Collections.singleton(container));
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTinfoKeywordAttr() + " WHERE container=?", container);
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTinfoStatisticAttr() + " WHERE container=?", container);
        new SqlExecutor(getSchema()).execute("DELETE FROM " + getTinfoGraphAttr() + " WHERE container=?", container);
    }

    public void setFileDateForAllFCSFiles(@NotNull User user)
    {
        try (DbScope.Transaction tx = getSchema().getScope().ensureTransaction())
        {
            for (String containerId : new SqlSelector(getSchema(), "SELECT DISTINCT container FROM flow.object").getCollection(String.class))
            {
                Container c = ContainerManager.getForId(containerId);
                setFileDateForAllFCSFiles(c, user);
            }
            tx.commit();
        }
    }

    public void setFileDateForAllFCSFiles(@NotNull Container c, @NotNull User user)
    {
        final PropertyDescriptor fileDatePd = FlowProperty.FileDate.getPropertyDescriptor();
        if (fileDatePd == null)
            throw new IllegalStateException("FileDate property descriptor required");

        final List<PropertyDescriptor> descriptors = Collections.singletonList(fileDatePd);
        final OntologyManager.ImportHelper helper = new OntologyManager.ImportHelper()
        {
            @Override
            public String beforeImportObject(Map<String, Object> map) throws SQLException
            {
                String lsid = (String)map.get("lsid");
                assert lsid != null;
                return lsid;
            }

            @Override
            public void afterBatchInsert(int currentRow) throws SQLException { }

            @Override
            public void updateStatistics(int currentRow) throws SQLException { }
        };

        String sqlSelectDateTime = "" +
                "SELECT\n" +
                "       x.container,\n" +
                "       x.rowid,\n" +
                "       x.dataid,\n" +
                "       x.lsid,\n" +
                "       x.name,\n" +
                "       x.currentvalue,\n" +
                "       CASE\n" +
                "         WHEN x.\"$DATE\" IS NOT NULL AND x.\"$BTIM\" IS NOT NULL\n" +
                "                 THEN CONCAT(x.\"$DATE\", ' ', x.\"$BTIM\")\n" +
                "         ELSE x.\"EXPORT TIME\"\n" +
                "           END AS datetime\n" +
                "FROM\n" +
                "     (\n" +
                "     SELECT\n" +
                "            fo.container,\n" +
                "            fo.rowid,\n" +
                "            fo.dataid,\n" +
                "            d.name,\n" +
                "            d.lsid,\n" +
                "\n" +
                "            (SELECT k.value\n" +
                "             FROM flow.keyword k\n" +
                "                    INNER JOIN flow.keywordattr ka ON k.keywordid = ka.rowid\n" +
                "             WHERE k.objectid = fo.rowid AND ka.name = 'EXPORT TIME'\n" +
                "            ) AS \"EXPORT TIME\",\n" +
                "\n" +
                "            (SELECT k.value\n" +
                "             FROM flow.keyword k\n" +
                "                    INNER JOIN flow.keywordattr ka ON k.keywordid = ka.rowid\n" +
                "             WHERE k.objectid = fo.rowid AND ka.name = '$DATE'\n" +
                "            ) AS \"$DATE\",\n" +
                "\n" +
                "            (SELECT k.value\n" +
                "             FROM flow.keyword k\n" +
                "                    INNER JOIN flow.keywordattr ka ON k.keywordid = ka.rowid\n" +
                "             WHERE k.objectid = fo.rowid AND ka.name = '$BTIM'\n" +
                "            ) AS \"$BTIM\",\n" +
                "\n" +
                "            (SELECt op.datetimevalue\n" +
                "             FROM exp.objectproperty op\n" +
                "                    INNER JOIN exp.propertydescriptor pd ON op.propertyid = pd.propertyid\n" +
                "                    INNER JOIN exp.object o ON op.objectid = o.objectid\n" +
                "             WHERE o.objecturi = d.lsid\n" +
                "               AND pd.propertyuri = '" + fileDatePd.getPropertyURI() + "'\n" +
                "            ) AS currentvalue\n" +
                "\n" +
                "     FROM flow.object fo\n" +
                "            INNER JOIN exp.data d ON fo.dataid = d.rowid\n" +
                "     WHERE fo.typeid = " + ObjectType.fcsKeywords._typeId + "\n" +
                "        AND fo.container = '" + c.getId() + "'" +
                "     ) x";

        try (DbScope.Transaction tx = getSchema().getScope().ensureTransaction())
        {
            List<Map<String, Object>> propMaps = new ArrayList<>(1000);

            SqlSelector ss = new SqlSelector(getSchema(), sqlSelectDateTime);
            ss.mapStream().forEach(row -> {

                // parse the date
                String dateStr = (String) row.get("datetime");
                if (dateStr == null)
                {
                    _log.info("Skipping update for row; no datetime keywords for row: " + row);
                    return;
                }

                long date = DateUtil.parseDateTime(dateStr);
                Date d = new Date(date);

                // get the existing property if any
                Date currentDate = (Date) row.get("currentvalue");
                if (currentDate != null)
                {
                    if (!d.equals(currentDate))
                    {
                        _log.warn("Current date value '" + currentDate + "' does not match parsed date '" + d + "' for row: " + row);
                    }
                    else
                    {
                        _log.debug("Skipping update for row; current date value matches the parsed date '" + d + "' for row: " + row);
                    }
                }
                else
                {
                    Map<String, Object> propMap = new HashMap<>();
                    propMap.put("lsid", row.get("lsid"));
                    propMap.put(fileDatePd.getPropertyURI(), d);
                    propMaps.add(propMap);
                }
            });

            OntologyManager.insertTabDelimited(c, user, null, helper, descriptors, propMaps, true);

            tx.commit();
        }
        catch (ValidationException | SQLException ex)
        {
            throw new UnexpectedException(ex);
        }
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


    public static class TestCase
    {
        @Test
        public void metrics()
        {
            Map<String, Object> metrics = FlowManager.get().getUsageMetrics();
            assertNotNull(metrics.get("flowTempTableCount"));
        }
    }
}
