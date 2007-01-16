package org.labkey.flow.controllers.protocol;

import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowProtocol;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;

public class ProtocolForm extends ViewForm
{
    private FlowProtocol _protocol;

    public void reset(ActionMapping actionMapping, HttpServletRequest request)
    {
        super.reset(actionMapping, request);
    }

    public FlowProtocol getProtocol() throws ServletException
    {
        if (_protocol != null)
            return _protocol;
        _protocol = FlowProtocol.fromURL(getUser(), getContext().getViewURLHelper(), getRequest());
        return _protocol;
    }
}
