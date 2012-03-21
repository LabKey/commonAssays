<%
/*
 * Copyright (c) 2006-2012 LabKey Corporation
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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.TestMascotForm form = ((JspView<MS2Controller.TestMascotForm>)HttpView.currentView()).getModelBean();

    if (null != form.getMessage() && !"".equals(form.getMessage()))
    {
        boolean errorMessage;
        errorMessage = (-1 != form.getMessage().indexOf("Test failed."));
        if (errorMessage)
            out.print("<font class=\"labkey-error\">");
        out.print("<b>" + form.getMessage() + "</b><br><br>");
        if (errorMessage)
            out.print("</font>");
    }
%>

<table>
    <tr><td colspan=2><b>Mascot settings tested:</b></td></tr>
    <tr><td>Server:</td><td><%
if (!"".equals(form.getMascotServer()))
{
    out.print(form.getMascotServer());
}
else
{
%>&lt;not set&gt;
<%}
%></td></tr>
    <tr><td>User account:</td><td><%
if (!"".equals(form.getMascotUserAccount()))
{
    out.print(form.getMascotUserAccount());
}
else
{
%>&lt;not set&gt;
<%}
%></td></tr>
    <tr><td>Password:</td><td><%
if (!"".equals(form.getMascotUserPassword()))
{
    out.print(form.getMascotUserPassword());
}
else
{
%>&lt;not set&gt;
<%}
%></td></tr>
    <tr><td>HTTP Proxy URL:</td><td><%
if (!"".equals(form.getMascotHTTPProxy()))
{
    out.print(form.getMascotHTTPProxy());
}
else
{
%>&lt;not set&gt;
<%}
%></td></tr>
<%
if ("".equals(form.getParameters()))
{
%>
    <tr><td colspan=2><br><b>Your Mascot Server Configurations:</b></td></tr>
    <tr><td>Mascot.dat:</td><td><textarea cols="40" rows="4"><%=form.getParameters()%></textarea></td></tr>
<%
}
%>
</table>

<%
if (0 != form.getStatus())
{
%>
<br>
If you're unfamiliar with your organization's Mascot services configuration you should consult with your Mascot administrator.

<ul>
<li>Server is typically of the form mascot.server.org</li>
<li>User account is the userid for logging in to your Mascot server.  It is mandatory if Mascot security is enabled.</li>
<li>Password is the pass pharse to authenticate you to your Mascot server.  It is mandatory if Mascot security is enabled.</li>
<li>HTTP Proxy URL is typically of the form http://proxyservername.domain.org:8080/ to make HTTP requests on your behalf if necessary.</li>
<li><%=helpLink("configMascot", "More information...")%></li>
</ul>
<%
}
%>
