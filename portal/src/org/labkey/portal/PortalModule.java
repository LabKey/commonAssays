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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.util.Search;
import org.labkey.api.util.Search.SearchWebPart;
import org.labkey.api.view.*;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;

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
        super(NAME, 2.10, "/org/labkey/portal", null,
            new SearchWebPartFactory("Search", null, 40),
            new SearchWebPartFactory("Narrow Search", "right", 0)
        );
        addController("Project", ProjectController.class);
    }


    public static class SearchWebPartFactory extends WebPartFactory
    {
        private int _width;

        public SearchWebPartFactory(String name, String location, int width)
        {
            super(name, location, true, false);
            _width = width;
        }

        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws IllegalAccessException, InvocationTargetException
        {
            boolean includeSubfolders = !"off".equals(webPart.getPropertyMap().get("includeSubfolders"));
            return new SearchWebPart(Search.ALL_MODULES, "", ProjectController.getSearchUrl(portalCtx.getContainer()), includeSubfolders, false, _width);
        }


        @Override
        public HttpView getEditView(Portal.WebPart webPart)
        {
            HttpView view = new ProjectController.CustomizeSearchPartView();
            view.addObject("webPart", webPart);
            return view;
        }
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
