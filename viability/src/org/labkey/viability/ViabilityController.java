/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.portal.ProjectUrls;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

public class ViabilityController extends SpringActionController
{
    public static final String NAME = "viability";

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ViabilityController.class);

    public ViabilityController() throws Exception
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ActionURL url = PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(getContainer());
            HttpView.throwRedirect(url);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }
}