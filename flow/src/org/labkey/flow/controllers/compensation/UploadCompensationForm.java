package org.labkey.flow.controllers.compensation;

import org.labkey.api.view.ViewForm;
import org.apache.struts.upload.FormFile;

public class UploadCompensationForm extends ViewForm
{
    public String ff_compensationMatrixName;
    public FormFile ff_compensationMatrixFile;

    public void setFf_compensationMatrixFile(FormFile file)
    {
        ff_compensationMatrixFile = file;
    }

    public void setFf_compensationMatrixName(String name)
    {
        ff_compensationMatrixName = name;
    }
}
