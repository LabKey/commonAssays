<%
/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.view.AdminViewContext" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<AdminViewContext> me = (JspView<AdminViewContext>)HttpView.currentView();
    AdminViewContext ctx = me.getModelBean();
%>

<table>
    <tr>
        <td>Data Files Awaiting Deletion:</td>
        <td><%=ctx.getNumDeleted()%></td>
    </tr>
</table>

<% if (ctx.getNumDeleted() > 0 && (!(ctx.isPurgeRunning()))) { %>
<p>Data marked for deletion will be automatically purged during the scheduled system maintenance process,
but you can manually start a purge now by clicking the button below.
</p>
<p><%= button("Purge Deleted MS1 Data Now").href(ctx.getPurgeNowUrl()) %></p>
<% } %>

<% if (ctx.isPurgeRunning()) { %>
<p>MS1 data is currently being purged...<a href="javascript:window.location.reload(true)">refresh</a> this page to view updated status.</p>
<% } %>