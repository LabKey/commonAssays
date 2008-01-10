package org.labkey.flow.controllers;

import org.labkey.api.jsp.JspBase;
import org.labkey.api.util.AppProps;

import java.util.List;
import java.util.ArrayList;

abstract public class BaseFlowPage extends JspBase
{
    List _errors = new ArrayList();

    public void addError(String error)
    {
        _errors.add(error);
    }

    public List getErrors()
    {
        return _errors;
    }

    public boolean anyErrors()
    {
        return _errors.size() != 0;
    }

    public String formatErrors()
    {
        return formatErrors("Errors were encountered:");
    }

    public String formatErrors(String message)
    {
        if (_errors.size() == 0)
            return "";
        StringBuilder ret = new StringBuilder();
        ret.append("<p style=\"color:red\">");
        ret.append(message + "<br>");
        for (Object error : _errors)
        {
            ret.append(h(error.toString()));
            ret.append("<br>");
        }
        ret.append("</p>");
        return ret.toString();
    }
    public String resourceURL(String name)
    {
        return AppProps.getInstance().getContextPath() + "/Flow/" + name;
    }
}
