/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
package org.labkey.nab;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;
import org.labkey.nab.multiplate.CrossPlateDilutionNabAssayProvider;
import org.labkey.nab.multiplate.CrossPlateDilutionNabDataHandler;
import org.labkey.nab.multiplate.SinglePlateDilutionNabAssayProvider;
import org.labkey.nab.multiplate.SinglePlateDilutionNabDataHandler;
import org.labkey.nab.query.NabProtocolSchema;
import org.labkey.nab.query.NabProviderSchema;
import org.labkey.nab.query.NabVirusDomainKind;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: migra
 * Date: Feb 15, 2006
 * Time: 10:39:44 PM
 */
public class NabModule extends DefaultModule
{
    public String getName()
    {
        return "Nab";
    }

    public double getVersion()
    {
        return 18.10;
    }

    protected void init()
    {
        addController("nabassay", NabAssayController.class);

        NabProviderSchema.register(this);
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
    @Override
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(NabProtocolSchema.NAB_DBSCHEMA_NAME, NabProtocolSchema.NAB_VIRUS_SCHEMA_NAME);
    }

    @NotNull
    @Override
    public Collection<String> getProvisionedSchemaNames()
    {
        return Collections.singleton(NabProtocolSchema.NAB_VIRUS_SCHEMA_NAME);
    }

    public void doStartup(ModuleContext moduleContext)
    {
        PlateService.get().registerPlateTypeHandler(new NabPlateTypeHandler());
        AssayService.get().registerAssayProvider(new NabAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new SinglePlateNabDataHandler());
        AssayService.get().registerAssayProvider(new CrossPlateDilutionNabAssayProvider());
        AssayService.get().registerAssayProvider(new SinglePlateDilutionNabAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new CrossPlateDilutionNabDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new SinglePlateDilutionNabDataHandler());
        ContainerManager.addContainerListener(new NabContainerListener());

        PropertyService.get().registerDomainKind(new NabVirusDomainKind());
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new NabUpgradeCode();
    }

    @NotNull
    @Override
    public Set<Class> getUnitTests()
    {
        return new HashSet<>(Arrays.<Class>asList(PlateParserTests.class));
    }
}
