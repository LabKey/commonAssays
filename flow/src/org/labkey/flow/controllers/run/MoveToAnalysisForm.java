package org.labkey.flow.controllers.run;

public class MoveToAnalysisForm extends RunForm
{
    private int experimentId;
    public void setExperimentId(int experimentId)
    {
        this.experimentId = experimentId;
    }

    public int getExperimentId()
    {
        return experimentId;
    }
}
