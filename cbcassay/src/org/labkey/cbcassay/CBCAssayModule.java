/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

package org.labkey.cbcassay;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.cbcassay.data.CBCDataProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class CBCAssayModule extends DefaultModule
{
    public static final String NAME = "CBCAssay";
    private static final double VERSION = 10.10;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public double getVersion()
    {
        return VERSION;
    }

    @Override
    public boolean hasScripts()
    {
        return false;
    }

    @Override
    protected void init()
    {
        addController("cbc-assay", CBCAssayController.class);
        CBCDataProperty.register();
    }

    @Override
    protected Collection<? extends WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void startup(ModuleContext moduleContext)
    {
        ExperimentService.get().registerExperimentDataHandler(new CBCDataHandler());
        AssayService.get().registerAssayProvider(new CBCAssayProvider());
    }
}
