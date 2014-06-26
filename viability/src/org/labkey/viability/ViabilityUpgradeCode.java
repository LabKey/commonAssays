/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.viability;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleUpgrader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: kevink
 * Date: 5/18/14
 */
public class ViabilityUpgradeCode implements UpgradeCode
{
    /**
     * Called from viability-14.11-14.12 upgrade script
     */
    @DeferredUpgrade
    public void updateViabilityTargetStudy(final ModuleContext context)
    {
        if (context.isNewInstall())
            return;

        DbScope scope = ViabilitySchema.getSchema().getScope();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            ModuleUpgrader.getLogger().info("Upgrading viability target study");
            _updateViabilityTargetStudy(context);
            transaction.commit();
            ModuleUpgrader.getLogger().info("Finished viability target study");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void _updateViabilityTargetStudy(ModuleContext context) throws SQLException
    {
        // get all the viability assay instances
        for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider instanceof ViabilityAssayProvider)
            {
                Container c = protocol.getContainer();
                User user = context.getUpgradeUser();

                // First, fix SampleNum
                fixupSampleNumProperty(provider, protocol);

                // Next, copy targetStudy
                copyTargetStudy(c, user, provider, protocol);

                // Finally, update all specimen aggregates
                ModuleUpgrader.getLogger().info(String.format("viability assay '%s': calculating aggregates...", protocol.getName()));
                ViabilityManager.updateSpecimenAggregates(context.getUpgradeUser(), c, provider, protocol, null);
            }
        }
    }

    // Change 'SampleNum' property from varchar to int type.
    // The viability.results.samplenum column is already an int so no conversion errors should occur.
    private void fixupSampleNumProperty(AssayProvider provider, ExpProtocol protocol)
    {
        Domain resultsDomain = provider.getResultsDomain(protocol);
        DomainProperty dp = resultsDomain.getPropertyByName(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME);
        if (dp != null && dp.getPropertyDescriptor() != null)
        {
            PropertyDescriptor pd = dp.getPropertyDescriptor();
            if (pd.getJdbcType() != null && pd.getJdbcType().isText())
            {
                ModuleUpgrader.getLogger().info(String.format("viability assay '%s': changing SampleNum type to int", protocol.getName()));
                pd.setJdbcType(JdbcType.INTEGER, 0);
                OntologyManager.updatePropertyDescriptor(pd);
            }
        }
    }

    // Copy the TargetStudy value from batch, run, or result domain to the viability.result.targetstudy column
    private void copyTargetStudy(Container c, User user, AssayProvider provider, ExpProtocol protocol) throws SQLException
    {
        // Find the 'TargetStudy' property
        Pair<ExpProtocol.AssayDomainTypes, DomainProperty> pair = provider.findTargetStudyProperty(protocol);
        if (pair != null)
        {
            ModuleUpgrader.getLogger().info(String.format("viability assay '%s': update target study...", protocol.getName()));
            if (pair.first == ExpProtocol.AssayDomainTypes.Batch)
                copyBatchTargetStudy(c, user, provider, protocol, pair.second);
            else if (pair.first == ExpProtocol.AssayDomainTypes.Run)
                copyRunTargetStudy(c, user, provider, protocol, pair.second);
            else if (pair.first == ExpProtocol.AssayDomainTypes.Result)
                copyResultTargetStudy(c, user, provider, protocol, pair.second);

            SchemaTableInfo table = ViabilitySchema.getTableInfoResults();
            new SqlExecutor(table.getSchema()).execute(table.getSqlDialect().getAnalyzeCommandForTable(table.toString()));
        }
    }

    private void copyBatchTargetStudy(Container c, User user, AssayProvider provider, ExpProtocol protocol, DomainProperty targetStudyProperty) throws SQLException
    {
        DbSchema schema = ViabilitySchema.getSchema();

        String updateSql = "UPDATE viability.results SET targetStudy=? WHERE runid=?";
        int count = 0;
        ArrayList<Collection<Object>> paramList = new ArrayList<>();
        for (ExpExperiment batch : protocol.getBatches())
        {
            String targetStudy = (String)batch.getProperty(targetStudyProperty);
            if (targetStudy != null)
            {
                for (ExpRun run : batch.getRuns())
                {
                    ArrayList<Object> params = new ArrayList<>();
                    params.add(targetStudy);
                    params.add(run.getRowId());
                    paramList.add(params);
                    count++;

                    if (0 == (count % 1000))
                    {
                        Table.batchExecute(schema, updateSql, paramList);
                        paramList.clear();
                        ModuleUpgrader.getLogger().info(String.format("viability assay '%s': updated targetStudy on %d runs...", protocol.getName(), count));
                    }
                }
            }
        }
        Table.batchExecute(schema, updateSql, paramList);
        ModuleUpgrader.getLogger().info(String.format("viability assay '%s': updated targetStudy on all %d runs.", protocol.getName(), count));
    }

    private void copyRunTargetStudy(Container c, User user, AssayProvider provider, ExpProtocol protocol, DomainProperty targetStudyProperty) throws SQLException
    {
        DbSchema schema = ViabilitySchema.getSchema();

        String updateSql = "UPDATE viability.results SET targetStudy=? WHERE runid=?";
        int count = 0;
        ArrayList<Collection<Object>> paramList = new ArrayList<>();
        for (ExpRun run : protocol.getExpRuns())
        {
            String targetStudy = (String)run.getProperty(targetStudyProperty);
            if (targetStudy != null)
            {
                ArrayList<Object> params = new ArrayList<>();
                params.add(targetStudy);
                params.add(run.getRowId());
                paramList.add(params);
                count++;

                if (0 == (count % 1000))
                {
                    Table.batchExecute(schema, updateSql, paramList);
                    paramList.clear();
                    ModuleUpgrader.getLogger().info(String.format("viability assay '%s': updated targetStudy on %d runs...", protocol.getName(), count));
                }
            }
        }
        Table.batchExecute(schema, updateSql, paramList);
        ModuleUpgrader.getLogger().info(String.format("viability assay '%s': updated targetStudy on all %d runs.", protocol.getName(), count));
    }

    private void copyResultTargetStudy(Container c, User user, AssayProvider provider, ExpProtocol protocol, DomainProperty targetStudyProperty) throws SQLException
    {
        DbSchema schema = ViabilitySchema.getSchema();
        TableInfo resultsTable = ViabilitySchema.getTableInfoResults();
        final PropertyDescriptor targetStudyPd = targetStudyProperty.getPropertyDescriptor();

        int resultsCount = 0;
        List<? extends ExpRun> runs = protocol.getExpRuns();
        for (ExpRun run : runs)
        {
            // For each run, copy the targetStudy property to the viability.results table
            SQLFragment updateFrag = new SQLFragment();
            updateFrag.append("UPDATE viability.results\n");
            updateFrag.append("SET targetStudy = (\n");
            updateFrag.append("   SELECT op.stringvalue\n");
            updateFrag.append("   FROM exp.objectproperty op\n");
            updateFrag.append("   WHERE op.objectid=results.objectid\n");
            updateFrag.append("       AND op.propertyid=?\n");
            updateFrag.add(targetStudyPd.getPropertyId());
            updateFrag.append("       AND results.runid=?\n");
            updateFrag.add(run.getRowId());
            updateFrag.append(")\n");
            updateFrag.append("WHERE EXISTS (\n");
            updateFrag.append("   SELECT op.stringvalue\n");
            updateFrag.append("   FROM exp.objectproperty op\n");
            updateFrag.append("   WHERE op.objectid=results.objectid\n");
            updateFrag.append("       AND op.propertyid=?\n");
            updateFrag.add(targetStudyPd.getPropertyId());
            updateFrag.append("       AND results.runid=?\n");
            updateFrag.add(run.getRowId());
            updateFrag.append(")\n");

            SqlExecutor updateExec = new SqlExecutor(schema);
            resultsCount += updateExec.execute(updateFrag);

            // delete the targetStudy property values
            SQLFragment deleteFrag = new SQLFragment();
            deleteFrag.append("DELETE FROM ").append(OntologyManager.getTinfoObjectProperty()).append("\n");
            deleteFrag.append("WHERE ObjectProperty.propertyid=?\n");
            deleteFrag.add(targetStudyProperty.getPropertyId());
            deleteFrag.append("AND ObjectProperty.objectid=(\n");
            deleteFrag.append("  SELECT r.objectid FROM ").append(resultsTable, "r").append("\n");
            deleteFrag.append("  WHERE ObjectProperty.objectid=r.objectid\n");
            deleteFrag.append("      AND r.runid=?\n");
            deleteFrag.add(run.getRowId());
            deleteFrag.append("      AND ObjectProperty.propertyid=?\n");
            deleteFrag.add(targetStudyProperty.getPropertyId());
            deleteFrag.append(")\n");

            SqlExecutor deleteExec = new SqlExecutor(schema);
            deleteExec.execute(deleteFrag);

        }

        OntologyManager.clearPropertyCache();
        ModuleUpgrader.getLogger().info(String.format("viability assay '%s': updated targetStudy on %d results in all %d runs.", protocol.getName(), resultsCount, runs.size()));
    }

}
