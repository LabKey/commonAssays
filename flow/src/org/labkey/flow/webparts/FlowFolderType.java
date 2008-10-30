/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Portal;
import org.labkey.flow.FlowModule;

import java.util.Arrays;

public class FlowFolderType extends DefaultFolderType
{
    public FlowFolderType(FlowModule module)
    {
        super("Flow",
                "Perform statistical analysis and create graphs for high-volume, highly standardized flow experiments. Organize, archive and track statistics and keywords for FlowJo experiments.",
                Arrays.asList(OverviewWebPart.FACTORY.createWebPart()),
                Arrays.asList(AnalysesWebPart.FACTORY.createWebPart(),
                        AnalysisScriptsWebPart.FACTORY.createWebPart(),
                        Portal.getPortalPart("Messages").createWebPart()),
                getDefaultModuleSet(module, getModule("Pipeline")), module);
    }

    public ActionURL getStartURL(Container c, User user)
    {
        ActionURL ret = super.getStartURL(c, user);
        ret.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
        return ret;
    }
}
