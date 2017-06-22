/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableResultSet;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.TestContext;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.query.FlowSchema;

import java.net.URI;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: kevink
 * Date: 4/30/14
 */
public class PersistTests
{
    private Container c;
    private User user;

    @Before
    public void setup()
    {
        JunitUtil.deleteTestContainer();
        c = JunitUtil.getTestContainer();
        user = TestContext.get().getUser();

        // Ensure we don't have any attributes in the test container
        assertEquals(0, new SqlSelector(FlowManager.get().getSchema(), "SELECT rowid FROM flow.object WHERE container = ?", c.getId()).getRowCount());
        assertEquals(0, new SqlSelector(FlowManager.get().getSchema(), "SELECT rowid FROM flow.keywordattr WHERE container = ?", c.getId()).getRowCount());
    }

    @Test
    public void keywordAliases() throws Exception
    {
        ExpData data = ExperimentService.get().createData(c, FlowDataType.FCSFile, this.getClass().getSimpleName());
        URI dataFileURI = new URI("file:///attributes.flowdata.xml");
        data.setDataFileURI(dataFileURI);
        data.save(user);

        AttributeSet set = new AttributeSet(ObjectType.fcsKeywords, dataFileURI);

        // create 'keyword1' with 'value1' and an alias of 'keyword1-alias' to 'keyword1'
        set.setKeyword("keyword1", "value1");
        set.addKeywordAlias("keyword1", "keyword1-alias");

        // create 'keyword2-alias' with 'value2' and an alias 'keyword2-alias' to 'keyword2'
        set.setKeyword("keyword2-alias", "value2");
        set.addKeywordAlias("keyword2", "keyword2-alias");

        AttributeSetHelper.save(set, user, data);

        AttrObject obj = FlowManager.get().getAttrObject(data);
        assertNotNull(obj);

        // verify keyword1
        {
            // verify keyword inserted into database
            assertEquals("value1", FlowManager.get().getKeyword(data, "keyword1"));
            assertEquals("value1", FlowManager.get().getKeyword(data, "keyword1-alias"));

            // verify keyword
            FlowManager.FlowEntry entry = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword1");
            assertEquals(c.getId(), entry._containerId);
            assertEquals("keyword1", entry._name);
            assertEquals(AttributeType.keyword, entry._type);

            // verify keyword value stored using both keywordid and original keywordid
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("objectid"), obj.getRowId());
            filter.addCondition(FieldKey.fromParts("keywordid"), entry._rowId);
            filter.addCondition(FieldKey.fromParts("originalkeywordid"), entry._rowId);
            TableSelector selector = new TableSelector(FlowManager.get().getTinfoKeyword(), Collections.singleton("value"), filter, null);
            assertEquals("value1", selector.getObject(String.class));

            // verify alias
            Collection<FlowManager.FlowEntry> aliases = FlowManager.get().getAliases(entry);
            assertEquals(1, aliases.size());
            FlowManager.FlowEntry alias = aliases.iterator().next();
            assertEquals(c.getId(), alias._containerId);
            assertEquals("keyword1-alias", alias._name);
            assertEquals(AttributeType.keyword, alias._type);

            // verify FlowEntry equality
            FlowManager.FlowEntry aliased = FlowManager.get().getAliased(alias);
            assertTrue(entry != aliased);
            assertEquals(entry, aliased);

            // verify cached keyword
            AttributeCache.KeywordEntry cacheEntry = AttributeCache.KEYWORDS.byName(c, "keyword1");
            assertEquals("keyword1", cacheEntry.getAttribute());
            assertEquals(c, cacheEntry.getContainer());
            assertEquals(entry._rowId.intValue(), cacheEntry.getRowId());

            // verify cached keyword alias
            Collection<AttributeCache.KeywordEntry> cacheAliases = cacheEntry.getAliases();
            assertEquals(1, cacheAliases.size());
            AttributeCache.KeywordEntry cacheAlias = cacheAliases.iterator().next();
            assertEquals("keyword1-alias", cacheAlias.getAttribute());
            assertEquals(AttributeType.keyword, cacheAlias.getType());

            // verify usages
            Collection<FlowDataObject> usages = cacheEntry.getUsages();
            assertEquals(1, usages.size());
            FlowDataObject usage = usages.iterator().next();
            assertEquals(data.getRowId(), usage.getData().getRowId());

            // verify query
            FlowSchema schema = new FlowSchema(user, c);
            ResultSet results = QueryService.get().select(schema, "SELECT " +
                    "A.Name, " +
                    "A.Keyword.keyword1 AS k1, " +
                    "A.Keyword.\"keyword1-alias\" AS k1_alias, " +
                    "A.Keyword('keyword1') AS k1_method, " +
                    "A.Keyword('keyword1-alias') AS k1_alias_method " +
                    "FROM flow.FCSFiles AS A");
            assertTrue(results.next());
            assertEquals(this.getClass().getSimpleName(), results.getString("Name"));
            assertEquals("value1", results.getString("k1"));
            assertEquals("value1", results.getString("k1_alias"));
            assertEquals("value1", results.getString("k1_method"));
            assertEquals("value1", results.getString("k1_alias_method"));

            assertFalse(results.next());
            results.close();
        }

        // verify keyword2
        {
            // verify keyword inserted into database
            assertEquals("value2", FlowManager.get().getKeyword(data, "keyword2"));
            assertEquals("value2", FlowManager.get().getKeyword(data, "keyword2-alias"));

            // verify cached keyword
            AttributeCache.KeywordEntry cacheEntry = AttributeCache.KEYWORDS.byName(c, "keyword2");
            assertEquals("keyword2", cacheEntry.getAttribute());
            assertEquals(c, cacheEntry.getContainer());

            // verify cached keyword alias
            Collection<AttributeCache.KeywordEntry> cacheAliases = cacheEntry.getAliases();
            assertEquals(1, cacheAliases.size());
            AttributeCache.KeywordEntry cacheAlias = cacheAliases.iterator().next();
            assertEquals("keyword2-alias", cacheAlias.getAttribute());
            assertEquals(AttributeType.keyword, cacheAlias.getType());

            // verify aliased keyword value stored using aliasid=keywordid and entry=originalkeywordid
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("objectid"), obj.getRowId());
            filter.addCondition(FieldKey.fromParts("keywordid"), cacheAlias.getAliasedId());
            filter.addCondition(FieldKey.fromParts("originalkeywordid"), cacheAlias.getRowId());
            TableSelector selector = new TableSelector(FlowManager.get().getTinfoKeyword(), Collections.singleton("value"), filter, null);
            assertEquals("value2", selector.getObject(String.class));

            // verify usages of 'keyword2-alias' entry
            Collection<FlowDataObject> aliasUsages = cacheAlias.getUsages();
            assertEquals(1, aliasUsages.size());

            // verify usages of 'keyword2' entry (excludes any alias usages)
            Collection<FlowDataObject> usages = cacheEntry.getUsages();
            assertEquals(0, usages.size());

            // verify all usages (includes aliases)
            Map<AttributeCache.KeywordEntry, Collection<FlowDataObject>> allUsages = cacheEntry.getAllUsages();
            assertEquals(2, allUsages.size());

            assertEquals(0, allUsages.get(cacheEntry).size());

            Collection<FlowDataObject> onlyAliasUsages = allUsages.get(cacheAlias);
            assertEquals(1, onlyAliasUsages.size());
            FlowDataObject usage = onlyAliasUsages.iterator().next();
            assertEquals(data.getRowId(), usage.getData().getRowId());

            // verify query
            FlowSchema schema = new FlowSchema(user, c);
            ResultSet results = QueryService.get().select(schema, "SELECT " +
                    "A.Name, " +
                    "A.Keyword.keyword2 AS k2, " +
                    "A.Keyword.\"keyword2-alias\" AS k2_alias, " +
                    "A.Keyword('keyword2') AS k2_method, " +
                    "A.Keyword('keyword2-alias') AS k2_alias_method " +
                    "FROM flow.FCSFiles AS A");
            assertTrue(results.next());
            assertEquals(this.getClass().getSimpleName(), results.getString("Name"));
            assertEquals("value2", results.getString("k2"));
            assertEquals("value2", results.getString("k2_alias"));
            assertEquals("value2", results.getString("k2_method"));
            assertEquals("value2", results.getString("k2_alias_method"));

            assertFalse(results.next());
            results.close();
        }

        // verify updating keyword value
        {
            assertEquals("value1", FlowManager.get().getKeyword(data, "keyword1"));
            FlowManager.get().setKeyword(c, data, "keyword1", "value1-updated");
            assertEquals("value1-updated", FlowManager.get().getKeyword(data, "keyword1"));

            assertEquals("value2", FlowManager.get().getKeyword(data, "keyword2"));
            FlowManager.get().setKeyword(c, data, "keyword2-alias", "value2-updated");
            assertEquals("value2-updated", FlowManager.get().getKeyword(data, "keyword2"));
        }

        // verify deleting keyword
        {
            FlowManager.get().setKeyword(c, data, "keyword1", null);
            assertNull(FlowManager.get().getKeyword(data, "keyword1"));

            FlowManager.get().setKeyword(c, data, "keyword2-alias", null);
            assertNull(FlowManager.get().getKeyword(data, "keyword2"));
        }
    }

    @Test
    public void statisticAliases() throws Exception
    {
        // Insert stat values
        {
            String name = "well1";
            ExpData data = ExperimentService.get().createData(c, FlowDataType.FCSAnalysis, name);
            URI dataFileURI = new URI("file:///attributes.flowdata.xml");
            data.setDataFileURI(dataFileURI);
            data.save(user);

            AttributeSet set = new AttributeSet(ObjectType.fcsAnalysis, dataFileURI);
            set.setStatistic(new StatisticSpec("X:Count"), 1.0);
            AttributeSetHelper.save(set, user, data);
        }

        // Insert stat values using aliases
        {
            String name = "well2";
            ExpData data = ExperimentService.get().createData(c, FlowDataType.FCSAnalysis, name);
            URI dataFileURI = new URI("file:///attributes.flowdata.xml");
            data.setDataFileURI(dataFileURI);
            data.save(user);

            AttributeSet set = new AttributeSet(ObjectType.fcsAnalysis, dataFileURI);
            set.setStatistic(new StatisticSpec("X-alias:Count"), 2.0);
            set.addStatisticAlias(new StatisticSpec("X:Count"), new StatisticSpec("X-alias:Count"));
            AttributeSetHelper.save(set, user, data);
        }

        // verify stat values
        FlowSchema schema = new FlowSchema(user, c);
        TableResultSet rs = (TableResultSet)QueryService.get().select(schema, "SELECT " +
                "A.Name, " +
                "A.Statistic.\"X:Count\" AS stat, " +
                "A.Statistic.\"X-alias:Count\" AS stat_alias, " +
                "A.Statistic('X:Count') AS stat_method, " +
                "A.Statistic('X-alias:Count') AS stat_alias_method " +
                "FROM flow.FCSAnalyses AS A ORDER BY Name");
        assertEquals(2, rs.getSize());

        assertTrue(rs.next());
        Map<String, Object> rowMap = rs.getRowMap();
        assertEquals("well1", rowMap.get("name"));
        assertEquals(1.0, rowMap.get("stat"));
        assertEquals(1.0, rowMap.get("stat_alias"));
        assertEquals(1.0, rowMap.get("stat_method"));
        assertEquals(1.0, rowMap.get("stat_alias_method"));

        assertTrue(rs.next());
        rowMap = rs.getRowMap();
        assertEquals("well2", rowMap.get("name"));
        assertEquals(2.0, rowMap.get("stat"));
        assertEquals(2.0, rowMap.get("stat_alias"));
        assertEquals(2.0, rowMap.get("stat_method"));
        assertEquals(2.0, rowMap.get("stat_alias_method"));

        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void unused() throws Exception
    {
        // Insert some keywords and aliases
        ExpData data1;
        {
            data1 = ExperimentService.get().createData(c, FlowDataType.FCSFile, "well1");
            URI dataFileURI = new URI("file:///attributes.flowdata.xml");
            data1.setDataFileURI(dataFileURI);
            data1.save(user);

            AttributeSet set = new AttributeSet(ObjectType.fcsKeywords, dataFileURI);

            // create 'keyword1' with 'value1' and an alias of 'keyword1-alias' to 'keyword1'
            set.setKeyword("keyword1", "value1");
            set.addKeywordAlias("keyword1", "keyword1-alias");

            // create 'keyword2-alias' with 'value2' and an alias 'keyword2-alias' to 'keyword2'
            set.setKeyword("keyword2-alias", "value2");
            set.addKeywordAlias("keyword2", "keyword2-alias");

            AttributeSetHelper.save(set, user, data1);
        }

        // Insert another with only keyword2-alias usage
        ExpData data2;
        {
            data2 = ExperimentService.get().createData(c, FlowDataType.FCSFile, "well2");
            URI dataFileURI = new URI("file:///attributes.flowdata.xml");
            data2.setDataFileURI(dataFileURI);
            data2.save(user);

            AttributeSet set = new AttributeSet(ObjectType.fcsKeywords, dataFileURI);

            // create 'keyword2-alias' with 'value2' and an alias 'keyword2-alias' to 'keyword2'
            set.setKeyword("keyword2-alias", "value2");
            set.addKeywordAlias("keyword2", "keyword2-alias");

            AttributeSetHelper.save(set, user, data2);
        }

        // verify no unused
        Collection<FlowManager.FlowEntry> unused = FlowManager.get().getUnused(c, AttributeType.keyword);
        assertEquals(0, unused.size());

        // delete the first ExpData and the associated keyword usages
        data1.delete(user);
        AttrObject obj = FlowManager.get().getAttrObject(data1);
        assertNull(obj);

        // verify only 'keyword1' and 'keyword1-alias' are considered unused
        unused = FlowManager.get().getUnused(c, AttributeType.keyword);
        assertEquals(2, unused.size());
        Set<String> expected = new HashSet<>(Arrays.asList("keyword1", "keyword1-alias"));
        for (FlowManager.FlowEntry entry : unused)
        {
            if (expected.contains(entry._name))
                expected.remove(entry._name);
            else
                fail("Unexpected attribute: " + entry._name);
        }
        assertTrue("Expected '" + expected + "' unused attribute", expected.isEmpty());

        // delete unused
        FlowManager.get().deleteUnused(c);

        // verify no unused
        unused = FlowManager.get().getUnused(c, AttributeType.keyword);
        assertEquals(0, unused.size());

        // delete the second ExpData and the associated keyword usages
        data2.delete(user);
        obj = FlowManager.get().getAttrObject(data2);
        assertNull(obj);

        // verify only 'keyword2' and 'keyword2-alias' keywords are unused
        unused = FlowManager.get().getUnused(c, AttributeType.keyword);
        assertEquals(2, unused.size());
        expected = new HashSet<>(Arrays.asList("keyword2", "keyword2-alias"));
        for (FlowManager.FlowEntry entry : unused)
        {
            if (expected.contains(entry._name))
                expected.remove(entry._name);
            else
                fail("Unexpected attribute: " + entry._name);
        }
        assertTrue("Expected '" + expected + "' unused attribute", expected.isEmpty());

        // delete unused
        FlowManager.get().deleteUnused(c);

        // verify delete unused
        unused = FlowManager.get().getUnused(c, AttributeType.keyword);
        assertEquals(0, unused.size());
    }

    @Test
    public void ensureAlias() throws Exception
    {
        ExpData data = ExperimentService.get().createData(c, FlowDataType.FCSFile, this.getClass().getSimpleName());
        URI dataFileURI = new URI("file:///attributes.flowdata.xml");
        data.setDataFileURI(dataFileURI);
        data.save(user);

        AttributeSet set = new AttributeSet(ObjectType.fcsKeywords, dataFileURI);

        // create 'keyword1' with 'value1' and an alias of 'keyword1-alias' to 'keyword1'
        set.setKeyword("keyword1", "value1");
        set.addKeywordAlias("keyword1", "keyword1-alias");

        // create 'keyword2' with 'value2' and an alias 'keyword2-alias' to 'keyword2'
        set.setKeyword("keyword2", "value2");
        set.addKeywordAlias("keyword2", "keyword2-alias");

        // create 'keyword3' with 'value3' and no aliases
        set.setKeyword("keyword3", "value3");

        AttributeSetHelper.save(set, user, data);

        AttrObject obj = FlowManager.get().getAttrObject(data);
        assertNotNull(obj);

        // create 'keyword4' and 'keyword5' that are not used by the data
        FlowManager.get().ensureKeywordName(c, "keyword4", true);
        FlowManager.get().ensureKeywordName(c, "keyword5", true);

        // attempt to create alias for nonexistent keyword
        try
        {
            FlowManager.get().ensureAlias(AttributeType.keyword, -1, "keyword2", true);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Attribute not found", e.getMessage());
        }

        FlowManager.FlowEntry keyword1 = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword1");
        FlowManager.FlowEntry keyword1_alias = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword1-alias");

        // attempt to create alias of an alias
        try
        {
            assertTrue(keyword1_alias.isAlias());
            FlowManager.get().ensureAlias(AttributeType.keyword, keyword1_alias._rowId, "keyword2", true);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Can't create alias of an alias", e.getMessage());
        }

        // attempt to create alias of an existing keyword that is an alias
        try
        {
            FlowManager.get().ensureAlias(AttributeType.keyword, keyword1._rowId, "keyword2-alias", true);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("The keyword attribute 'keyword2-alias' is already an alias of 'keyword2'", e.getMessage());
        }

        // attempt to create alias of an existing keyword that is has aliases
        try
        {
            FlowManager.get().ensureAlias(AttributeType.keyword, keyword1._rowId, "keyword2", true);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("The keyword attribute 'keyword2' has aliases and can't be made an alias of 'keyword1'", e.getMessage());
        }

        // attempt to create alias of existing keyword; both the alias and the aliased have a value on the same data object
        try
        {
            FlowManager.get().ensureAlias(AttributeType.keyword, keyword1._rowId, "keyword3", true);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("There are objects that have both attributes: 1", e.getMessage());
        }

        // verify we can create an alias from an existing, unused keyword
        {
            FlowManager.get().ensureAlias(AttributeType.keyword, keyword1._rowId, "keyword4", true);
            FlowManager.FlowEntry keyword1mod = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword1");
            Collection<FlowManager.FlowEntry> aliases = FlowManager.get().getAliases(keyword1mod);
            assertEquals(2, aliases.size());
            assertTrue("Expected to see 'keyword1-alias' as an alias", aliases.stream().anyMatch(e -> e._name.equals("keyword1-alias")));
            assertTrue("Expected to see 'keyword4' as an alias", aliases.stream().anyMatch(e -> e._name.equals("keyword4")));
        }

        // verify we can create an alias from an existing, used keyword, if the aliased name is not used
        {
            FlowManager.FlowEntry keyword5 = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword5");
            assertFalse(keyword5.isAlias());
            assertTrue(FlowManager.get().getAliases(keyword5).isEmpty());

            FlowManager.FlowEntry keyword3 = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword3");
            assertFalse(keyword3.isAlias());
            assertTrue(FlowManager.get().getAliases(keyword3).isEmpty());

            FlowManager.get().ensureAlias(AttributeType.keyword, keyword5._rowId, "keyword3", true);

            FlowManager.FlowEntry keyword5mod = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword5");
            assertFalse(keyword5mod.isAlias());
            Collection<FlowManager.FlowEntry> aliases = FlowManager.get().getAliases(keyword5mod);
            assertEquals(1, aliases.size());
            assertTrue("Expected to see 'keyword3' as an alias", aliases.stream().anyMatch(e -> e._name.equals("keyword3")));

            FlowManager.FlowEntry keyword3mod = FlowManager.get().getAttributeEntry(c.getId(), AttributeType.keyword, "keyword3");
            assertTrue(keyword3mod.isAlias());
            assertTrue(FlowManager.get().getAliases(keyword3mod).isEmpty());
        }
  }

}
