/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LuminexModule extends DefaultModule
{
    private static final Logger _log = Logger.getLogger(LuminexModule.class);

    public String getName()
    {
        return "Luminex";
    }

    public double getVersion()
    {
        return 9.20;
    }

    protected void init()
    {
        addController("luminex", LuminexController.class);
    }

    protected Collection<? extends WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    public boolean hasScripts()
    {
        return true;
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void startup(ModuleContext moduleContext)
    {
        AssayService.get().registerAssayProvider(new LuminexAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new LuminexExcelDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new LuminexTsvDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new LuminexExcelRunPropsDataHandler());
    }

    public Set<String> getSchemaNames()
    {
        return Collections.singleton("luminex");
    }

    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(LuminexSchema.getSchema());
    }

    @Override
    public void afterUpdate(ModuleContext moduleContext)
    {
        super.afterUpdate(moduleContext);

        // Need to upgrade the protocolid for any luminex assay data that's been copied to a study
        if (!moduleContext.isNewInstall() && moduleContext.getInstalledVersion() < 8.31)
        {
            LuminexProtocolUpgrader.upgrade();
        }
    }
}