/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.nab.multiplate.CrossPlateDilutionNabAssayProvider;
import org.labkey.nab.multiplate.CrossPlateDilutionNabDataHandler;
import org.labkey.nab.multiplate.SinglePlateDilutionNabAssayProvider;
import org.labkey.nab.multiplate.SinglePlateDilutionNabDataHandler;
import org.labkey.nab.query.NabProtocolSchema;
import org.labkey.nab.query.NabProviderSchema;

import java.util.Collection;
import java.util.Collections;
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
        return 14.20;
    }

    protected void init()
    {
//        addController("nab", NabController.class);
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
        return Collections.singleton(NabProtocolSchema.NAB_DBSCHEMA_NAME);
    }

    public void doStartup(ModuleContext moduleContext)
    {
/*        PlateService.get().registerDetailsLinkResolver(new PlateService.PlateDetailsResolver()
        {
            public ActionURL getDetailsURL(Plate plate)
            {
                // for 2.0, we'll accept all plate types: only NAB uses the plate service.
                ActionURL url = new ActionURL(NabController.DisplayAction.class, plate.getContainer());
                url.addParameter("rowId", "" + plate.getRowId());
                return url;
            }
        });
*/
        PlateService.get().registerPlateTypeHandler(new NabPlateTypeHandler());
        AssayService.get().registerAssayProvider(new NabAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new SinglePlateNabDataHandler());
        AssayService.get().registerAssayProvider(new CrossPlateDilutionNabAssayProvider());
        AssayService.get().registerAssayProvider(new SinglePlateDilutionNabAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new CrossPlateDilutionNabDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new SinglePlateDilutionNabDataHandler());
        ContainerManager.addContainerListener(new NabContainerListener());
    }


    @Override
    public ActionURL getTabURL(Container c, User user)
    {
        ActionURL defaultURL = super.getTabURL(c, user);

        // this is a bit of a hack: while we're supporting both old and new assay-based NAB
        // implementations, it's less confusing to the user if the NAB tab keeps them from switching
        // from the new implementation to the old, so we swap out the pageflow of the tab URL:
        ViewContext context = HttpView.getRootContext();
        String pageFlow = context != null ? context.getActionURL().getController() : null;
        if ("assay".equals(pageFlow) || "NabAssay".equals(pageFlow))
            defaultURL.setController("assay");

        return defaultURL;
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new NabUpgradeCode();
    }
}
