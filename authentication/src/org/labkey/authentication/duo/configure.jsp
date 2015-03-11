<%
    /*
     * Copyright (c) 2008-2014 LabKey Corporation
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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.security.LoginUrls" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.authentication.duo.DuoController" %>
<%@ page import="org.labkey.authentication.duo.DuoController.Config" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<Config> me = (JspView<Config>)HttpView.currentView();
    Config bean = me.getModelBean();
%>
<labkey:form action="configure.post" method="post">
    <table>
        <%=formatMissedErrorsInTable("form", 2)%>
        <tr>
            <td class="labkey-form-label">Integration Key<%= PageFlowUtil.helpPopup("Integration Key", "Find definition")%></td>
            <td><input type="text" name="integrationKey" size="50" value="<%=h(bean.getIntegrationKey())%>"></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Secret Key<%= PageFlowUtil.helpPopup("Secret Key", "Find definition")%></td>
            <td><input type="text" name="secretKey" size="50" value="<%=h(bean.getSecretKey())%>"></td>
        </tr>
        <tr>
            <td class="labkey-form-label">API Hostname<%=PageFlowUtil.helpPopup("API Hostname", "Find definition")%></td>
            <td><input type="text" name="apiHostname"  size="50" value="<%=h(bean.getApiHostname())%>"></td>
        </tr>
        <tr><td colspan="2">&nbsp;</td></tr>
        <tr>
            <td colspan=2>
                <%= button("Test").href(urlFor(DuoController.TestDuoAction.class).addReturnURL(getActionURL()))%>
            </td>
        </tr>
        <tr><td>&nbsp;</td></tr>
        <tr>
            <td colspan=2>
                <%= button("Save").submit(true) %>
                <%= button("Cancel").href(urlProvider(LoginUrls.class).getConfigureURL()) %>
            </td>
        </tr>
        <tr><td colspan="2">&nbsp;</td></tr>
    </table>
</labkey:form>
