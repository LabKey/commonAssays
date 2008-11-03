/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.nab.query.NabSchema;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * User: migra
 * Date: Feb 15, 2006
 * Time: 10:39:44 PM
 */
public class NabModule extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final String NAME = "Nab";

    public NabModule()
    {
        super(NAME, 8.30, null, false);
    }

    protected void init()
    {
        addController("nab", NabController.class);
        addController("nabassay", NabAssayController.class);

        NabSchema.register();
    }

    //void wantsToDelete(Container c, List<String> messages);
    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c, User user)
    {
        try
        {
            NabManager.get().deleteContainerData(c);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    public void startup(ModuleContext moduleContext)
    {
        PlateService.get().registerDetailsLinkResolver(new PlateService.PlateDetailsResolver()
        {
            public ActionURL getDetailsURL(Plate plate)
            {
                // for 2.0, we'll accept all plate types: only NAB uses the plate service.
                ActionURL url = new ActionURL("Nab", "display", plate.getContainer());
                url.addParameter("rowId", "" + plate.getRowId());
                return url;
            }
        });

        PlateService.get().registerPlateTypeHandler(new NabPlateTypeHandler());
        AssayService.get().registerAssayProvider(new NabAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new NabDataHandler());

        super.startup(moduleContext);
    }


    public ActionURL getTabURL(Container c, User user)
    {
        ActionURL defaultURL = super.getTabURL(c, user);

        // this is a bit of a hack: while we're supporting both old and new assay-based NAB
        // implementations, it's less confusing to the user if the NAB tab keeps them from switching
        // from the new implementation to the old, so we swap out the pageflow of the tab URL:
        ViewContext context = HttpView.getRootContext();
        String pageFlow = context != null ? context.getActionURL().getPageFlow() : null;
        if ("assay".equals(pageFlow) || "NabAssay".equals(pageFlow))
            defaultURL.setPageFlow("assay");

        return defaultURL;
    }
}
