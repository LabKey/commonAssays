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

package org.labkey.flow.controllers.executescript;

import org.labkey.api.data.Container;
import org.labkey.api.view.HttpView;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowExperiment;

public class UploadWorkspaceResultsForm
{
    private FlowExperiment[] availableAnalyses;
    private WorkspaceData workspace = new WorkspaceData();

    public String ff_newAnalysisName;
    public int ff_existingAnalysisId;
    public boolean ff_confirm;

    public UploadWorkspaceResultsForm()
    {
        Container container = HttpView.currentContext().getContainer();
        availableAnalyses = FlowExperiment.getAnalyses(container);
        if (availableAnalyses.length == 0)
        {
            ff_newAnalysisName = FlowExperiment.DEFAULT_ANALYSIS_NAME;
        }
        else
        {
            ff_existingAnalysisId = FlowExperiment.getMostRecentAnalysis(container).getExperimentId();
        }
    }

    public WorkspaceData getWorkspace()
    {
        return workspace;
    }

    public void setFf_confirm(boolean b)
    {
        ff_confirm = b;
    }

    public void setFf_newAnalysisName(String ff_newAnalysisName)
    {
        this.ff_newAnalysisName = ff_newAnalysisName;
    }

    public void setFf_existingAnalysisId(int ff_existingAnalysisId)
    {
        this.ff_existingAnalysisId = ff_existingAnalysisId;
    }

    public FlowExperiment[] getAvailableAnalyses()
    {
        return availableAnalyses;
    }
}
