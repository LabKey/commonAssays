/*
 * Copyright (c) 2016 LabKey Corporation
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

package org.labkey.signaldata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SignalDataModule extends DefaultModule
{
    public static final String NAME = "SignalData";
    public static final String QC_PROVIDER_PROPERTY_NAME = "QCViewProviderModule";

    SignalDataModule()
    {
        super();

        ModuleProperty qcViewProperty;
        qcViewProperty = new ModuleProperty(this, QC_PROVIDER_PROPERTY_NAME);
        qcViewProperty.setDescription("Name of module that will provide the QC view");
        qcViewProperty.setCanSetPerContainer(true);
        qcViewProperty.setDefaultValue("signaldata");
        addModuleProperty(qcViewProperty);
    }

    @Override
    public String getName()
    {
        return NAME;
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
        addController(SignalDataController.NAME, SignalDataController.class);
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 26.000;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    public @NotNull Collection<String> getSchemaNames()
    {
        return List.of("signaldata");
    }

    @Override
    public @Nullable UpgradeCode getUpgradeCode()
    {
        return new SignalDataUpgradeCode();
    }

}