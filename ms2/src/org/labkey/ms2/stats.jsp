<%
/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
<%@ page import="org.labkey.ms2.MS2StatsWebPart" %>
<%@ page import="org.labkey.ms2.MS2StatsWebPart.StatsBean" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2StatsWebPart me = (MS2StatsWebPart) HttpView.currentView();
    StatsBean bean = me.getModelBean();
%>
<table class="labkey-data-region">
<tr><td>MS2 Runs:</td><td><%=bean.runs%></td></tr>
<tr><td>MS2 Peptides:</td><td><%=bean.peptides%></td></tr>
</table>