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
<%@ page import="org.labkey.api.util.HelpTopic"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%
    MS2Controller.TestSequestForm form = ((JspView<MS2Controller.TestSequestForm>) HttpView.currentView()).getModelBean();

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
    <tr><td colspan=2><b>Sequest settings tested:</b></td></tr>
    <tr><td>Server:</td><td align="left"><%
if (!"".equals(form.getSequestServer()))
{
    out.print(form.getSequestServer());
}
else
{
%>&lt;not set&gt;
<%}
%></td></tr>
<%
if (!"".equals(form.getParameters()))
{
%>
    <tr><td colspan=2><br><b>Your Sequest Server Configurations:</b></td></tr>
    <tr><td colspan=2><pre><%=form.getParameters()%></pre></td></tr>
<%
}
%>
</table>

<%
if (0 != form.getStatus())
{
%>
<br>
If you're unfamiliar with your organization's Sequest services configuration you should consult with your Sequest administrator.

<ul>
<li>Server is typically of the form http://servername.org/SequestQueue</li>
<li><a href="<%=(new HelpTopic("configSequest", HelpTopic.Area.SERVER)).getHelpTopicLink()%>">More information...</a>
</ul>
<%
}
%>
