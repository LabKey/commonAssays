<%
/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.model.Software" %>
<%@ page import="org.labkey.ms1.model.SoftwareParam" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<Software[]> me = (JspView<Software[]>) HttpView.currentView();
    Software[] swares = me.getModelBean();
%>
<table>
    <% for(Software sware : swares) {%>
    <tr class="labkey-alternate-row">
        <td><b><%=h(sware.getName())%></b>
        <%
        String version = sware.getVersion();
        if (null != version && version.length() > 0)
            out.print(" version " + h(version));

        String author = sware.getAuthor();
        if (null != author && author.length() > 0)
            out.print(" (" + h(author) + ")");
%>
        </td>
    </tr>
    <tr>
        <td>
            <table>
                <% SoftwareParam[] params = sware.getParameters();
                    if (null != params && params.length > 0)
                    {
                        for (SoftwareParam param : sware.getParameters()) { %>
                <tr>
                    <td><%=h(param.getName())%>:</td>
                    <td><%=h(param.getValue())%></td>
                </tr>
                <%}}%>
            </table>
        </td>
    </tr>
    <%}%>
</table>