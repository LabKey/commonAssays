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

package org.labkey.ms1;

import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.Portal;

import java.util.Arrays;

/**
 * Implements an MS1 Folder type
 * User: Dave
 * Date: Nov 2, 2007
 * Time: 11:11:50 AM
 */
public class MS1FolderType extends MultiPortalFolderType
{
    public MS1FolderType(MS1Module module)
    {
        super("MS1",
                "View and analyze MS1 quantitation results along with MS2 data.",
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart("MS1 Runs").createWebPart()
            ),
            Arrays.asList(
                Portal.getPortalPart("Run Groups").createWebPart(),
                Portal.getPortalPart("Run Types").createWebPart()
            ),
            getDefaultModuleSet(module, getModule("MS2"), getModule("Pipeline"), getModule("Experiment")),
            module);
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        return new HelpTopic("ms1");
    }
}
