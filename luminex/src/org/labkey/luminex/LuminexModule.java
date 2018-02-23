/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.study.assay.AssayQCFlagColumn;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LuminexModule extends DefaultModule
{
    public String getName()
    {
        return "Luminex";
    }

    public double getVersion()
    {
        return 18.10;
    }

    protected void init()
    {
        addController("luminex", LuminexController.class);
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void doStartup(ModuleContext moduleContext)
    {
        ExperimentService.get().registerExperimentDataHandler(new LuminexDataHandler());
        AssayService.get().registerAssayProvider(new LuminexAssayProvider());
        PropertyService.get().registerDomainKind(new LuminexAnalyteDomainKind());
        PropertyService.get().registerDomainKind(new LuminexDataDomainKind());
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(LuminexProtocolSchema.DB_SCHEMA_NAME);
    }

    @NotNull
    @Override
    public Set<Class> getUnitTests()
    {
        return PageFlowUtil.set(
                LuminexDataHandler.TestCase.class,
                LuminexExcelParser.TestCase.class,
                AssayQCFlagColumn.TestCase.class,
                LuminexRunAsyncContext.TestCase.class,
                LuminexSaveExclusionsForm.TestCase.class
        );
    }
}
