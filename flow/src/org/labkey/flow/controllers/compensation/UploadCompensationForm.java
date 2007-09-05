package org.labkey.flow.controllers.compensation;

import org.labkey.api.view.ViewForm;

public class UploadCompensationForm extends ViewForm
{
    public String ff_compensationMatrixName;

    public void setFf_compensationMatrixName(String name)
    {
        ff_compensationMatrixName = name;
    }
}
