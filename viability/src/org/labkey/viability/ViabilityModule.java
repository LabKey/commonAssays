/*
 * Copyright (c) 2009-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayService;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.study.SpecimenService;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ViabilityModule extends DefaultModule
{
    public static final String NAME = "Viability";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 24.000;
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
        return Collections.emptyList();
    }

    @Override
    protected void init()
    {
        addController(ViabilityController.NAME, ViabilityController.class);
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        ExperimentService.get().registerExperimentDataHandler(new ViabilityTsvDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new GuavaDataHandler());
        AssayService.get().registerAssayProvider(new ViabilityAssayProvider());

        SpecimenService ss = SpecimenService.get();
        if (null != ss)
            ss.registerSpecimenChangeListener(new ViabilitySpecimenChangeListener());
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(ViabilitySchema.SCHEMA_NAME);
    }

    @Override
    @NotNull
    public Set<Class> getIntegrationTests()
    {
        return Set.of(
            ViabilityAssayDataHandler.TestCase.class,
            ViabilityManager.TestCase.class
        );
    }
}
