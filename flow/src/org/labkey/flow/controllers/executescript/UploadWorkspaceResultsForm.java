package org.labkey.flow.controllers.executescript;

import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowExperiment;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;

public class UploadWorkspaceResultsForm extends ViewForm
{
    private FlowExperiment[] availableAnalyses;
    private String path;
    public String ff_newAnalysisName;
    public int ff_existingAnalysisId;


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

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
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
