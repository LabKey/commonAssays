<%
/*
 * Copyright (c) 2004-2012 Fred Hutchinson Cancer Research Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.MS2AdminBean bean = ((JspView<MS2Controller.MS2AdminBean>)HttpView.currentView()).getModelBean();
%>
<table class="labkey-data-region labkey-show-borders" cellpadding="4" cellspacing="4">
<tr><td>&nbsp;</td><td align="right"><strong>Runs</strong></td><td align="right"><strong>Peptides</strong></td><td align="right"><strong>Spectra</strong></td></tr>
<tr class="labkey-row"><td class="labkey-form-label">Successful</td><td align="right"><a href="<%=h(bean.successfulURL)%>"><%=bean.stats.get("successfulRuns")%></a></td><td align="right"><%=bean.stats.get("successfulPeptides")%></td><td align="right"><%=bean.stats.get("successfulSpectra")%></td></tr>
<tr class="labkey-row"><td class="labkey-form-label">In-Process</td><td align="right"><a href="<%=h(bean.inProcessURL)%>"><%=bean.stats.get("inProcessRuns")%></a></td><td align="right"><%=bean.stats.get("inProcessPeptides")%></td><td align="right"><%=bean.stats.get("inProcessSpectra")%></td></tr>
<tr class="labkey-row"><td class="labkey-form-label">Failed</td><td align="right"><a href="<%=h(bean.failedURL)%>"><%=bean.stats.get("failedRuns")%></a></td><td align="right"><%=bean.stats.get("failedPeptides")%></td><td align="right"><%=bean.stats.get("failedSpectra")%></td></tr>
<tr class="labkey-row"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
<tr class="labkey-row"><td class="labkey-form-label">Deleted</td><td align="right"><a href="<%=h(bean.deletedURL)%>"><%=bean.stats.get("deletedRuns")%></a></td><td align="right"><%=bean.stats.get("deletedPeptides")%></td><td align="right"><%=bean.stats.get("deletedSpectra")%></td></tr>
<tr class="labkey-row"><td class="labkey-form-label">To Be Purged</td><td align="right"><%=bean.stats.get("purgedRuns")%></td><td align="right"><%=bean.stats.get("purgedPeptides")%></td><td align="right"><%=bean.stats.get("purgedSpectra")%></td></tr>
</table><br>

<%
    if (null != bean.purgeStatus)
    { %>
<table class="labkey-data-region"><tr><td><%=bean.purgeStatus%> Refresh this page to update status.</td></tr></table><%
    }
    else
    { %>
<form method="post" action="<%=h(new ActionURL(MS2Controller.PurgeRunsAction.class, ContainerManager.getRoot()))%>">
<table class="labkey-data-region"><tr><td>Currently set to purge all MS2 runs deleted <input name="days" value="<%=bean.days%>" size="2"> days ago or before&nbsp;<%=PageFlowUtil.generateSubmitButton("Update", "this.form.action='showMS2Admin.view';")%></td></tr>
<tr><td><%=generateSubmitButton("Purge Deleted MS2 Runs")%></td></tr></table></form><%
    }
%>
