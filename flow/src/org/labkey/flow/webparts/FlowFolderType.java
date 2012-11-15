/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.flow.webparts;

import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.security.User;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.BaseFlowController;

import java.util.Arrays;
import java.util.Collections;

public class FlowFolderType extends MultiPortalFolderType
{
    public FlowFolderType(FlowModule module)
    {
        super("Flow", "Perform statistical analysis and create graphs for high-volume, highly standardized flow experiments. Organize, archive and track statistics and keywords for FlowJo experiments.",
                null, null, null, null);
        
        requiredParts = Collections.emptyList();
        preferredParts = Arrays.asList(
                FlowSummaryWebPart.FACTORY.createWebPart(),
                OverviewWebPart.FACTORY.createWebPart(),
                AnalysesWebPart.FACTORY.createWebPart(),
                AnalysisScriptsWebPart.FACTORY.createWebPart());
        activeModules = getDefaultModuleSet(module, getModule("Pipeline"));
        defaultModule = module;
    }

    public ActionURL getStartURL(Container c, User user)
    {
        ActionURL ret = super.getStartURL(c, user);
        ret.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
        return ret;
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        return BaseFlowController.DEFAULT_HELP_TOPIC;
    }
}
