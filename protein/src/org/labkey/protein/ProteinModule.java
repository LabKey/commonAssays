/*
 * Copyright (c) 2024 LabKey Corporation
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

package org.labkey.protein;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.annotations.Migrate;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.TableSelector;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.CustomAnnotationSetManager;
import org.labkey.api.protein.ProteinAnnotationPipelineProvider;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.fasta.FastaDbLoader;
import org.labkey.api.protein.query.CustomAnnotationSchema;
import org.labkey.api.protein.query.ProteinUserSchema;
import org.labkey.api.usageMetrics.UsageMetricsService;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProteinModule extends DefaultModule
{
    public static final String NAME = "Protein";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Migrate
    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 0.000; // TODO: Switch to 24.000 once prot scripts move here
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return List.of(
            new BaseWebPartFactory(CustomProteinListView.NAME)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    CustomProteinListView result = new CustomProteinListView(portalCtx, false);
                    result.setFrame(WebPartView.FrameType.PORTAL);
                    result.setTitle(CustomProteinListView.NAME);
                    result.setTitleHref(ProteinController.getBeginURL(portalCtx.getContainer()));
                    return result;
                }
            }
        );
    }

    @Override
    protected void init()
    {
        // TODO: Merge with protein controller
        addController("annot", AnnotController.class);
        addController("protein", ProteinController.class);

        ProteinUserSchema.register(this);
        CustomAnnotationSchema.register(this);
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        ContainerManager.addContainerListener(new ProteinContainerListener());
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new ProteinAnnotationPipelineProvider(this));
        UsageMetricsService.get().registerUsageMetrics(getName(), () -> Map.of("hasGeneOntologyData", new TableSelector(ProteinSchema.getTableInfoGoTerm()).exists()));
        AnnotController.registerAdminConsoleLinks();
    }

    @Override
    @NotNull
    public Collection<String> getSummary(Container c)
    {
        Collection<String> list = new LinkedList<>();
        int customAnnotationCount = CustomAnnotationSetManager.getCustomAnnotationSets(c, false).size();
        if (customAnnotationCount > 0)
        {
            list.add(customAnnotationCount + " custom protein annotation set" + (customAnnotationCount > 1 ? "s" : ""));
        }
        return list;
    }

    @Migrate
    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        // TODO: Switch to "prot" when scripts move
        return Collections.singleton(ProteinSchema.getSchemaName());
    }

    @Override
    public @NotNull Set<Class> getIntegrationTests()
    {
        return Set.of
        (
            AnnotController.TestCase.class
        );
    }

    @Override
    public @NotNull Set<Class> getUnitTests()
    {
        return Set.of
        (
            FastaDbLoader.TestCase.class
        );
    }
}