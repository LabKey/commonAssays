package Flow;

import org.fhcrc.cpas.view.ViewForm;
import org.fhcrc.cpas.flow.FlowSettings;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;

public class FlowAdminForm extends ViewForm
{
    public String ff_workingDirectory;

    public void reset(ActionMapping actionMapping, HttpServletRequest request)
    {
        super.reset(actionMapping, request);
        ff_workingDirectory = FlowSettings.getWorkingDirectoryPath();
    }

    public void setFf_workingDirectory(String path)
    {
        ff_workingDirectory = path;
    }
}
