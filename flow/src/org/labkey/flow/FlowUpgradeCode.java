package org.labkey.flow;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleUpgrader;
import org.labkey.api.query.FieldKey;
import org.labkey.flow.persist.FlowManager;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import static org.labkey.flow.analysis.model.PopulationName.MAC_EMDASH;
import static org.labkey.flow.analysis.model.PopulationName.MAC_ENDASH;

/**
 * User: kevink
 * Date: 6/23/13
 */
public class FlowUpgradeCode implements UpgradeCode
{

    /**
     * Called from 13.10 -> 13.11.
     * Issue 17371: flow: population names may contain funny unicode dash character from mac workspaces.
     * Converts MacRoman character 208 (endash) and 209 (emdash) found in statistic names, graph names, and analysis scripts into '-'.
     */
    public void removeMacRomanDashes(final ModuleContext context)
    {
        if (context.isNewInstall())
            return;

        ModuleUpgrader.getLogger().info("Started upgrading flow MacRoman dashes");
        removeMacDashAttribute(context, FlowManager.get().getTinfoGraphAttr(), FlowManager.get().getTinfoGraph(), "graphid");
        removeMacDashAttribute(context, FlowManager.get().getTinfoStatisticAttr(), FlowManager.get().getTinfoStatistic(), "statisticid");
        removeMacDashScript(context);
        ModuleUpgrader.getLogger().info("Finished upgrading flow MacRoman dashes");
    }

    private void removeMacDashAttribute(final ModuleContext context, final TableInfo attributeTable, final TableInfo valuesTable, final String idColName)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("name"), MAC_ENDASH + ";" + MAC_EMDASH, CompareType.CONTAINS_ONE_OF);
        TableSelector selector = new TableSelector(attributeTable, filter, null);
        selector.forEachMap(new Selector.ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Container c = ContainerManager.getForId((String)row.get("container"));
                Integer rowId = (Integer)row.get("rowid");
                Integer id = (Integer)row.get("id");
                String name = (String)row.get("name");

                String newName = replaceMacDashChars(name);

                ModuleUpgrader.getLogger().info(String.format(
                        "Replacing MacRoman dashes in flow.%s: rowid=%d, id=%d, container=%s, name='%s', newName='%s'",
                        attributeTable.getName(), rowId, id, c.getPath(), name, newName));

                // Check for any existing attributes with the new name in the same container
                SimpleFilter existingNewNameFilter = SimpleFilter.createContainerFilter(c);
                existingNewNameFilter.addCondition(FieldKey.fromParts("name"), newName);
                TableSelector existingNewNameSelector = new TableSelector(attributeTable, existingNewNameFilter, null);
                Map<String, Object> existing = existingNewNameSelector.getMap();
                if (existing == null)
                {
                    // If the new name doesn't exist, just update to the new name in place
                    ModuleUpgrader.getLogger().debug("    no conflict with new name");
                    Table.update(context.getUpgradeUser(), attributeTable, Collections.singletonMap("name", newName), rowId);
                }
                else
                {
                    // The new name already exists...
                    Integer existingRowId = (Integer)existing.get("rowid");
                    ModuleUpgrader.getLogger().info("    existing rowid=" + existingRowId);

                    // Update any attributes that are aliases for the old name
                    SQLFragment updateAliases = new SQLFragment();
                    updateAliases.append("UPDATE ").append(attributeTable, "q").append("\n");
                    updateAliases.append("SET id=? WHERE id=?");
                    updateAliases.add(existingRowId);
                    updateAliases.add(rowId);

                    SqlExecutor executor = new SqlExecutor(attributeTable.getSchema().getScope());
                    int updated = executor.execute(updateAliases);
                    ModuleUpgrader.getLogger().info("    updated " + updated + " aliases in flow." + attributeTable.getName());

                    // Replace all usages of the old name with the new name
                    SQLFragment updateUsages = new SQLFragment();
                    updateUsages.append("UPDATE ").append(valuesTable, "q").append("\n");
                    updateUsages.append("SET ").append(idColName).append(" = ?\n");
                    updateUsages.add(existingRowId);
                    updateUsages.append("WHERE ").append(idColName).append(" = ?\n");
                    updateUsages.add(rowId);

                    executor = new SqlExecutor(attributeTable.getSchema().getScope());
                    updated = executor.execute(updateUsages);
                    ModuleUpgrader.getLogger().info("    updated " + updated + " usages in flow." + valuesTable.getName());

                    // Finally, delete the old name
                    Table.delete(attributeTable, rowId);
                    ModuleUpgrader.getLogger().info("    deleted old name from flow." + attributeTable.getName());
                }
            }
        });
    }

    private void removeMacDashScript(final ModuleContext context)
    {
        final TableInfo scriptTable = FlowManager.get().getTinfoScript();

        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("text"), MAC_ENDASH + ";" + MAC_EMDASH, CompareType.CONTAINS_ONE_OF);
        TableSelector selector = new TableSelector(scriptTable, filter, null);
        selector.forEachMap(new Selector.ForEachBlock<Map<String, Object>>()
        {
            @Override
            public void exec(Map<String, Object> row) throws SQLException
            {
                Integer rowId = (Integer)row.get("rowid");
                String text = (String)row.get("text");

                String newText = replaceMacDashChars(text);

                ModuleUpgrader.getLogger().info("Replacing MacRoman dashes in analysis script flow.script: rowid=" + rowId);
                Table.update(context.getUpgradeUser(), scriptTable, Collections.singletonMap("text", newText), rowId);
            }
        });
    }

    private String replaceMacDashChars(String s)
    {
        return s.replaceAll("[" + MAC_ENDASH + MAC_EMDASH + "]", "-");
    }
}
