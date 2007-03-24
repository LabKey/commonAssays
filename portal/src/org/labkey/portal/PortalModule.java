/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.portal;

import org.labkey.api.module.Module;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.view.*;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.security.User;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.beans.PropertyChangeEvent;

/**
 * User: migra
 * Date: Jul 25, 2005
 * Time: 1:56:13 PM
 */
public class PortalModule extends DefaultModule
{
    public static final String NAME = "Portal";
    private static final Logger _log = Logger.getLogger(DefaultModule.class);

    public PortalModule()
    {
        // NOTE:  the version number of the portal module does not govern the scripts run for the
        // portal schema.  Bump the core module version number to cause a portal-xxx.sql script to run
        super(NAME, 1.79, "/org/labkey/portal", null,
            new WebPartFactory("Search"){
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws IllegalAccessException, InvocationTargetException
                {
                    return new ProjectController.SearchWebPart();
                }
            },
            new WebPartFactory("Narrow Search", "right"){
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws IllegalAccessException, InvocationTargetException
                {
                    return new ProjectController.SearchWebPart();
                }
            });
        addController("Project", ProjectController.class);
    }


    @Override
    public TabDisplayMode getTabDisplayMode()
    {
        return Module.TabDisplayMode.DISPLAY_USER_PREFERENCE_DEFAULT;
    }


    @Override
    public void startup(ModuleContext context)
    {
        ContainerManager.addContainerListener(new ContainerManager.ContainerListener()
        {
            public void containerCreated(Container c)
            {
            }

            public void containerDeleted(Container c)
            {
                try
                {
                    Portal.containerDeleted(c);
                }
                catch (Exception e)
                {
                    _log.error("Unable to delete WebParts for container " + c.getPath() + " Error:  " + e.getMessage());
                }
            }

            public void propertyChange(PropertyChangeEvent evt)
            {
            }
        });
        super.startup(context);
    }
}
