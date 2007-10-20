<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.opensso.OpenSSOController.ConfigProperties" %>
<%@ page import="org.labkey.opensso.OpenSSOController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ConfigProperties> me = (JspView<ConfigProperties>)HttpView.currentView();
    ConfigProperties bean = me.getModelBean();
%>
<table><%

    for (String key : bean.props.keySet())
    {
        String value = bean.props.get(key);
%>
<tr><td><%=key%></td><td><%=value%></td></tr><%
    }
%>
</table><br>
<%=PageFlowUtil.buttonLink("Update", OpenSSOController.getConfigureUrl(getViewContext().getViewURLHelper()))%>
<%=PageFlowUtil.buttonLink("Done", bean.getReturnUrl())%>
