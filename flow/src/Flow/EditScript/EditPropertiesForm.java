package Flow.EditScript;

import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;

public class EditPropertiesForm extends EditScriptForm
{
    public String ff_description;

    public void reset(ActionMapping mapping, HttpServletRequest request)
    {
        super.reset(mapping, request);
        ff_description = analysisScript.getExpObject().getComment();
    }

    public void setFf_description(String description)
    {
        ff_description = description;
    }
}
