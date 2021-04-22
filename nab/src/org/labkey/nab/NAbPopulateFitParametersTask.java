/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.SystemMaintenance.MaintenanceTask;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.pipeline.NabPopulateFitParametersPipelineJob;

public class NAbPopulateFitParametersTask implements MaintenanceTask
{
    @Override
    public String getDescription()
    {
        return "NAb Populate Fit Parameters Pipeline Job";
    }

    @Override
    public String getName()
    {
        return "NabPopulateFitParametersPipelineJob";
    }

    @Override
    public boolean isRecurring()
    {
        return false;
    }

    @Override
    public void run(Logger log)
    {
        try
        {
            ViewBackgroundInfo info = new ViewBackgroundInfo(ContainerManager.getRoot(), null, null);
            PipeRoot root = PipelineService.get().findPipelineRoot(ContainerManager.getRoot());
            PipelineService.get().queueJob(new NabPopulateFitParametersPipelineJob(info, root));
        }
        catch (Exception e)
        {}
    }
}
