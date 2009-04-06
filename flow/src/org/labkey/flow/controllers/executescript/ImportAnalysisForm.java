/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import org.labkey.flow.controllers.WorkspaceData;

/**
 * User: kevink
 * Date: Jul 14, 2008 4:06:04 PM
 */
public class ImportAnalysisForm
{
    private int step;
    private WorkspaceData workspace = new WorkspaceData();
    private int existingKeywordRunId;
    private boolean createAnalysis;
    private String newAnalysisName;
    private int existingAnalysisId;
    private String runFilePathRoot;
    private boolean confirm;

    public int getStep()
    {
        return step;
    }

    public void setStep(int step)
    {
        this.step = step;
    }

    public AnalysisScriptController.ImportAnalysisStep getWizardStep()
    {
        return AnalysisScriptController.ImportAnalysisStep.fromNumber(step);
    }

    public void setWizardStep(AnalysisScriptController.ImportAnalysisStep step)
    {
        this.step = step.getNumber();
    }

    public WorkspaceData getWorkspace()
    {
        return workspace;
    }

    public int getExistingKeywordRunId()
    {
        return existingKeywordRunId;
    }

    public void setExistingKeywordRunId(int existingKeywordRunId)
    {
        this.existingKeywordRunId = existingKeywordRunId;
    }

    public boolean isCreateAnalysis()
    {
        return createAnalysis;
    }

    public void setCreateAnalysis(boolean createAnalysis)
    {
        this.createAnalysis = createAnalysis;
    }

    public String getNewAnalysisName()
    {
        return newAnalysisName;
    }

    public void setNewAnalysisName(String newAnalysisName)
    {
        this.newAnalysisName = newAnalysisName;
    }

    public int getExistingAnalysisId()
    {
        return existingAnalysisId;
    }

    public void setExistingAnalysisId(int existingAnalysisId)
    {
        this.existingAnalysisId = existingAnalysisId;
    }

    public String getRunFilePathRoot()
    {
        return runFilePathRoot;
    }

    public void setRunFilePathRoot(String runFilePathRoot)
    {
        this.runFilePathRoot = runFilePathRoot;
    }

    public boolean isConfirm()
    {
        return confirm;
    }

    public void setConfirm(boolean confirm)
    {
        this.confirm = confirm;
    }

}
