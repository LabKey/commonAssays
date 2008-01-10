package org.labkey.flow;

import org.labkey.api.view.ActionURL;
import org.labkey.api.util.PageFlowUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.labkey.flow.controllers.FlowController;

public enum FlowPreference
{
    graphSize("300"),
    showGraphs("1"),
    editScriptRunId(null),
    editScriptCompId(null),
    editScriptWellId(null)
    ;

    FlowPreference(String defValue)
    {
        _defValue = defValue;
    }
    String _defValue;

    public String getValue(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);
        if (session == null)
            return null;
        String value = (String) session.getAttribute(getKey());
        if (value == null)
            return _defValue;
        return value;
    }

    public void setValue(HttpServletRequest request, String value)
    {
        HttpSession session = request.getSession(true);
        session.setAttribute(getKey(), value);
    }

    public int getIntValue(HttpServletRequest request)
    {
        try
        {
            return Integer.valueOf(getValue(request));
        }
        catch (Exception e)
        {
            if (_defValue == null)
                return 0;
            return Integer.valueOf(_defValue);
        }
    }

    public String urlUpdate()
    {
        ActionURL url = PageFlowUtil.urlFor(FlowController.Action.savePreferences, "");
        url.addParameter(name(), "");
        return url.toString();
    }

    private String getKey()
    {
        return FlowPreference.class.getName() + "." + name();
    }

    static public void update(HttpServletRequest request)
    {
        for (FlowPreference pref : values())
        {
            String value = request.getParameter(pref.name());
            if (value != null)
            {
                pref.setValue(request, value);
            }
        }
    }
}
