/*
 * Copyright (c) 2008-2013 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.Portal;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.exp.api.ExperimentService;

import java.util.Arrays;

/**
 * User: jeckels
 * Date: Jan 2, 2008
 */
public class MicroarrayFolderType extends MultiPortalFolderType
{
    public MicroarrayFolderType(MicroarrayModule module)
    {
        super("Microarray",
                "Import and analyze microarray data",
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart(MicroarrayModule.WEBPART_MICROARRAY_STATISTICS).createWebPart()
            ),
            Arrays.asList(
                Portal.getPortalPart(MicroarrayModule.WEBPART_MICROARRAY_RUNS).createWebPart(),
                Portal.getPortalPart("Assay Runs").createWebPart(),
                Portal.getPortalPart(MicroarrayModule.WEBPART_PENDING_FILES).createWebPart(),
                Portal.getPortalPart("Assay List").createWebPart()
            ),
            getDefaultModuleSet(module, getModule(MicroarrayModule.NAME), getModule(PipelineService.MODULE_NAME), getModule(ExperimentService.MODULE_NAME)),
            module);
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        return new HelpTopic("microarrayTutorial");
    }
}
