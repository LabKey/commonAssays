<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.MS2AdminBean bean = ((JspView<MS2Controller.MS2AdminBean>)HttpView.currentView()).getModelBean();
%>
<table class="dataRegion">
<tr><td>&nbsp;</td><td><b>MS2 Runs</b></td><td><b>MS2 Peptides</b></td><td><b>MS2 Spectra</b></td></tr>
<tr><td>Successful:</td><td><a href="<%=h(bean.successfulUrl)%>"><%=bean.stats.get("successfulRuns")%></a></td><td><%=bean.stats.get("successfulPeptides")%></td><td><%=bean.stats.get("successfulSpectra")%></td></tr>
<tr><td>In-Process:</td><td><a href="<%=h(bean.inProcessUrl)%>"><%=bean.stats.get("inProcessRuns")%></a></td><td><%=bean.stats.get("inProcessPeptides")%></td><td><%=bean.stats.get("inProcessSpectra")%></td></tr>
<tr><td>Failed:</td><td><a href="<%=h(bean.failedUrl)%>"><%=bean.stats.get("failedRuns")%></a></td><td><%=bean.stats.get("failedPeptides")%></td><td><%=bean.stats.get("failedSpectra")%></td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td>Deleted:</td><td><a href="<%=h(bean.deletedUrl)%>"><%=bean.stats.get("deletedRuns")%></a></td><td><%=bean.stats.get("deletedPeptides")%></td><td><%=bean.stats.get("deletedSpectra")%></td></tr>
<tr><td>To Be Purged:</td><td><%=bean.stats.get("purgedRuns")%></td><td><%=bean.stats.get("purgedPeptides")%></td><td><%=bean.stats.get("purgedSpectra")%></td></tr>
</table><br>

<%
    if (null != bean.purgeStatus)
    { %>
<table class="dataRegion"><tr><td><%=bean.purgeStatus%> Refresh this page to update status.</td></tr></table><%
    }
    else
    { %>
<form method="post" action="purgeRuns.post">
<table class="dataRegion"><tr><td>Currently set to purge all MS2 runs deleted <input name="days" value="<%=bean.days%>" size="2"> days ago or before&nbsp;<input type="image" src="<%=PageFlowUtil.buttonSrc("Update")%>" onclick="this.form.action='showMS2Admin.view';"></td></tr>
<tr><td><input type="image" src="<%=PageFlowUtil.buttonSrc("Purge Deleted MS2 Runs")%>"></td></tr></table></form><%
    }
%>
