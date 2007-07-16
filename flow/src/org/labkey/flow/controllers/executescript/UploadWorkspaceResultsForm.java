package org.labkey.flow.controllers.executescript;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.Globals;
import org.labkey.api.view.ViewForm;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowExperiment;

import javax.servlet.http.HttpServletRequest;

public class UploadWorkspaceResultsForm extends ViewForm
{
    private FlowExperiment[] availableAnalyses;
    private WorkspaceData workspace = new WorkspaceData();

    public boolean validate()
    {
        workspace.validate(this);
        if (getActionErrors() != null && !getActionErrors().isEmpty())
        {
            PageFlowUtil.getActionErrors(getRequest(), true).add(getActionErrors());
            return false;
        }
        return true;
    }

    public WorkspaceData getWorkspace()
    {
        return workspace;
    }

    public String ff_newAnalysisName;
    public int ff_existingAnalysisId;
    public boolean ff_confirm;


    public void reset(ActionMapping actionMapping, HttpServletRequest request)
    {
        super.reset(actionMapping, request);
        availableAnalyses = FlowExperiment.getAnalyses(getContainer());
        if (availableAnalyses.length == 0)
        {
            ff_newAnalysisName = FlowExperiment.DEFAULT_ANALYSIS_NAME;
        }
        else
        {
            ff_existingAnalysisId = FlowExperiment.getMostRecentAnalysis(getContainer()).getExperimentId();
        }
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
