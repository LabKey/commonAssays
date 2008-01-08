<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="java.util.Map" %>
<%
    MS2Controller.ModificationBean bean = ((JspView<MS2Controller.ModificationBean>)HttpView.currentView()).getModelBean();
    Map<String, String> fixed = bean.fixed;
    Map<String, String> var = bean.var;
%>
<table style="width:100%;">
<%  if (0 == (var.size() + fixed.size()))
        out.print("<tr><td colspan=2><b>None</b></td></tr>\n");

    if (0 != fixed.size())
    {
        out.print("<tr><td colspan=2><b>Fixed</b></td></tr>\n");

        for (String key : fixed.keySet())
            out.print("<tr><td>" + key + "</td><td align=right>" + fixed.get(key) + "</td></tr>\n");
    }

    if (0 != var.size())
    {
        if (0 != fixed.size())
            out.print("<tr><td colspan=2>&nbsp;</td></tr>\n");

        out.print("<tr><td colspan=2><b>Variable</b></td></tr>\n");

        for (String key : var.keySet())
            out.print("<tr><td>" + key + "</td><td align=right>" + var.get(key) + "</td></tr>\n");
    }
%></table>