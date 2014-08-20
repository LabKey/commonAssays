<%
/*
 * Copyright (c) 2010-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<String> me = (HttpView<String>) HttpView.currentView();
    String url = me.getModelBean();
%>
<labkey:form method="POST">
    <table>
        <tr>
            <td class="labkey-form-label">Base&nbsp;URL<%= PageFlowUtil.helpPopup("Base URL", "The sequence will be appended to the end of the URL to create links to the BLAST server")%></td>
            <td><input size="100" type="text" name="blastServerBaseURL" value="<%=h(url)%>"/></td>
            <td><%= button("Save").submit(true) %></td>
        </tr>
    </table>
</labkey:form>
