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
