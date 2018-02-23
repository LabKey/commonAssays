/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
import org.labkey.api.assay.viability.ViabilityService;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.study.SpecimenService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.WebPartFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ViabilityModule extends DefaultModule
{
    public static final String NAME = "Viability";

    public String getName()
    {
        return NAME;
    }

    public double getVersion()
    {
        return 18.10;
    }

    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    protected void init()
    {
        addController(ViabilityController.NAME, ViabilityController.class);
        ServiceRegistry.get().registerService(ViabilityService.class, ViabilityServiceImpl.get());
    }

    public void doStartup(ModuleContext moduleContext)
    {
        ExperimentService.get().registerExperimentDataHandler(new ViabilityTsvDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new GuavaDataHandler());
        AssayService.get().registerAssayProvider(new ViabilityAssayProvider());
        SpecimenService.get().registerSpecimenChangeListener(new ViabilitySpecimenChangeListener());
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
        return new HashSet<>(Arrays.asList(
                ViabilityManager.TestCase.class,
                ViabilityAssayDataHandler.TestCase.class
        ));
    }
}
