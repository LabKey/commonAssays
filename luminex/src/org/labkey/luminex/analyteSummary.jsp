<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexRun" %>
<%@ page import="org.labkey.luminex.AnalyteInfo" %>
<%
    JspView<LuminexRun> me = (JspView<LuminexRun>) HttpView.currentView();
    List<AnalyteInfo> analytes = me.getModelBean().getAnalyteInfos();
%>
<%
String separator = "";
for (AnalyteInfo analyte : analytes)
{
    %><%= separator %><a href="#<%= analyte.getAnalyteName() %>"><%= analyte.getAnalyteName() %></a><% separator = ", ";
}
%>
