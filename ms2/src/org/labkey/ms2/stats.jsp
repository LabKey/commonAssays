<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms2.MS2StatsWebPart" %>
<%@ page import="org.labkey.ms2.MS2StatsWebPart.StatsBean" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2StatsWebPart me = (MS2StatsWebPart) HttpView.currentView();
    StatsBean bean = me.getModelBean();
%>
<table class="dataRegion">
<tr><td>MS2 Runs:</td><td><%=bean.runs%></td></tr>
<tr><td>MS2 Peptides:</td><td><%=bean.peptides%></td></tr>
</table>