<%
/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.TestMascotForm form = ((JspView<MS2Controller.TestMascotForm>)HttpView.currentView()).getModelBean();

    if (!StringUtils.isBlank(form.getMessage()))
    {
        boolean errorMessage = form.getMessage().contains("Test failed.");

        if (errorMessage) { %><span class="labkey-error"><% } %>
        <strong><%= h(form.getMessage())%></strong>
        <% if (errorMessage) { %></span><% }
    }
%>

<div><strong>Mascot settings tested</strong></div>
<table class="lk-fields-table">
    <tr><td colspan=2></td></tr>
    <tr><td class="labkey-form-label">Server</td><td><%= h(StringUtils.isBlank(form.getMascotServer()) ? "<not set>" : form.getMascotServer()) %></td></tr>
    <tr><td class="labkey-form-label">User account</td><td><%= h(StringUtils.isBlank(form.getMascotUserAccount()) ? "<not set>" : form.getMascotUserAccount()) %></td></tr>
    <tr><td class="labkey-form-label">Password</td><td><%= h(StringUtils.isBlank(form.getMascotUserPassword()) ? "<not set>" : form.getMascotUserPassword()) %></td></tr>
    <tr><td class="labkey-form-label">HTTP Proxy URL</td><td><%= h(StringUtils.isBlank(form.getMascotHTTPProxy()) ? "<not set>" : form.getMascotHTTPProxy()) %></td></tr>
<%
if (!StringUtils.isBlank(form.getParameters())) { %>
    <tr><td colspan=2><br><strong>Your Mascot Server Configurations</strong></td></tr>
    <tr><td class="labkey-form-label">Mascot.dat</td><td><code><%=h(form.getParameters())%></code></td></tr>
<% } %>
</table>

<%
if (0 != form.getStatus())
{
%>
<br>
If you're unfamiliar with your organization's Mascot services configuration you should consult with your Mascot administrator.

<ul>
<li>Server is typically of the form <em>mascot.server.org</em></li>
<li>User account is the userid for logging in to your Mascot server.  It is mandatory if Mascot security is enabled.</li>
<li>Password is the pass phrase to authenticate you to your Mascot server.  It is mandatory if Mascot security is enabled.</li>
<li>HTTP Proxy URL is typically of the form <em>http://proxyservername.domain.org:8080/</em> to make HTTP requests on your behalf if necessary.</li>
<li><%=helpLink("configMascot", "More information...")%></li>
</ul>
<%
}
%>
