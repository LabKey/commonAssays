/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

package org.labkey.elisa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.plate.AbstractPlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.data.Container;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ElisaModule extends DefaultModule
{
    public static final String EXPERIMENTAL_MULTI_PLATE_SUPPORT = "elisaMultiPlateSupport";

    @Override
    public String getName()
    {
        return "Elisa";
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 21.000;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    @Override
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(ElisaProtocolSchema.ELISA_DB_SCHEMA_NAME);
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    protected void init()
    {
        addController("elisa", ElisaController.class);
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        PlateService.get().registerPlateTypeHandler(new ElisaPlateTypeHandler());
        ExperimentService.get().registerExperimentDataHandler(new ElisaDataHandler());

        AbstractPlateBasedAssayProvider provider = new ElisaAssayProvider();

        AssayService.get().registerAssayProvider(provider);

        AdminConsole.addExperimentalFeatureFlag(EXPERIMENTAL_MULTI_PLATE_SUPPORT,
                "ELISA Multi-plate, multi well data support",
                "Allows ELISA assay import of high-throughput data file formats which contain multiple plates and multiple analyte values per well.",
                false);
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    public @Nullable UpgradeCode getUpgradeCode()
    {
        return new ElisaUpgradeCode();
    }
}