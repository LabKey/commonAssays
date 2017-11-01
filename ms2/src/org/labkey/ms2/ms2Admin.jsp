<%
/*
 * Copyright (c) 2004-2017 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.MS2AdminBean bean = ((JspView<MS2Controller.MS2AdminBean>)HttpView.currentView()).getModelBean();
%>
<table class="labkey-data-region-legacy labkey-show-borders" cellpadding="4" cellspacing="4">
<tr>
    <th>&nbsp;</th>
    <th align="right">Runs</th>
    <th align="right">Peptides</th>
    <th align="right">Spectra</th>
</tr>
<tr class="labkey-alternate-row">
    <td style="font-weight: bold;">Successful</td>
    <td align="right"><a href="<%=h(bean.successfulURL)%>"><%=bean.stats.get("successfulRuns")%></a></td>
    <td align="right"><%=bean.stats.get("successfulPeptides")%></td>
    <td align="right"><%=bean.stats.get("successfulSpectra")%></td>
</tr>
<tr class="labkey-row">
    <td style="font-weight: bold;">In-Process</td>
    <td align="right"><a href="<%=h(bean.inProcessURL)%>"><%=bean.stats.get("inProcessRuns")%></a></td>
    <td align="right"><%=bean.stats.get("inProcessPeptides")%></td>
    <td align="right"><%=bean.stats.get("inProcessSpectra")%></td>
</tr>
<tr class="labkey-alternate-row">
    <td style="font-weight: bold;">Failed</td>
    <td align="right"><a href="<%=h(bean.failedURL)%>"><%=bean.stats.get("failedRuns")%></a></td>
    <td align="right"><%=bean.stats.get("failedPeptides")%></td>
    <td align="right"><%=bean.stats.get("failedSpectra")%></td>
</tr>
<tr class="labkey-row">
    <td colspan="4">&nbsp;</td>
</tr>
    <tr class="labkey-alternate-row">
    <td style="font-weight: bold;">Deleted</td>
    <td align="right"><a href="<%=h(bean.deletedURL)%>"><%=bean.stats.get("deletedRuns")%></a></td>
    <td align="right"><%=bean.stats.get("deletedPeptides")%></td>
    <td align="right"><%=bean.stats.get("deletedSpectra")%></td>
</tr>
<tr class="labkey-row">
    <td style="font-weight: bold;">To Be Purged</td>
    <td align="right"><%=bean.stats.get("purgedRuns")%></td>
    <td align="right"><%=bean.stats.get("purgedPeptides")%></td>
    <td align="right"><%=bean.stats.get("purgedSpectra")%></td>
</tr>
</table><br>

<%
    if (null != bean.purgeStatus)
    { %>
<table class="labkey-data-region"><tr><td><%=bean.purgeStatus%> Refresh this page to update status.</td></tr></table><%
    }
    else
    { %>
<labkey:form method="post" action="<%=new ActionURL(MS2Controller.PurgeRunsAction.class, ContainerManager.getRoot())%>">
<table class="labkey-data-region"><tr><td>Currently set to purge all MS2 runs deleted <input name="days" value="<%=bean.days%>" size="2"> days ago or before&nbsp;<%= button("Update").submit(true).onClick("this.form.action=" + qh(buildURL(MS2Controller.ShowMS2AdminAction.class)) + ";") %></td></tr>
<tr><td><%= button("Purge Deleted MS2 Runs").submit(true) %></td></tr></table></labkey:form><%
    }
%>
