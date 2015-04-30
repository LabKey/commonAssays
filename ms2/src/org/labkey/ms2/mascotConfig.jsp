<%
    /*
     * Copyright (c) 2015 LabKey Corporation
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
<%@ page import="org.labkey.api.settings.AppProps"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.util.HelpTopic" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.admin.AdminUrls" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%=formatMissedErrors("form")%>
<%
    MS2Controller.MascotSettingsForm bean = ((JspView<MS2Controller.MascotSettingsForm>)HttpView.currentView()).getModelBean();
    AppProps.Interface appProps = AppProps.getInstance();
%>
<script type="text/javascript">

    var testMascot;

    (function(){

        testMascot = function()
        {
            var preferenceForm = document.forms['preferences'];
            var mascotForm = document.forms['mascottest'];
            if (preferenceForm.mascotServer.value.length == 0)
            {
                alert("Please specify your mascot server before testing.");
                try {preferenceForm.mascotServer.focus();} catch(x){}
                return;
            }
            mascotForm.mascotServer.value = preferenceForm.mascotServer.value;
            mascotForm.mascotUserAccount.value = preferenceForm.mascotUserAccount.value;
            mascotForm.mascotUserPassword.value = preferenceForm.mascotUserPassword.value;
            mascotForm.mascotHTTPProxy.value = preferenceForm.mascotHTTPProxy.value;

            mascotForm.action = LABKEY.ActionURL.buildURL("ms2","mascotTest","/");
            mascotForm.submit();
        };
    })();
</script>

<labkey:form name="preferences" enctype="multipart/form-data" method="post">
    <table>
        <tr>
            <th style="width: 35em;"></th>
            <th></th>
        </tr>

        <tr>
            <td colspan=2>Configure Mascot settings (<%=text(new HelpTopic("configAdmin").getSimpleLinkHtml("more info..."))%>)</td>
        </tr>
        <tr>
            <td class="labkey-form-label">Mascot server</td>
            <td><input type="text" name="mascotServer" size="64" value="<%=h(appProps.getMascotServer())%>"></td>
        </tr>
        <tr>
            <td class="labkey-form-label">User</td>
            <td><input type="text" name="mascotUserAccount" size="50" value="<%=h(appProps.getMascotUserAccount())%>" autocomplete="off"></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Password</td>
            <td><input type="password" name="mascotUserPassword" size="50" value="<%=h(appProps.getMascotUserPassword())%>" autocomplete="off"></td>
        </tr>
        <tr>
            <td class="labkey-form-label">HTTP Proxy URL</td>
            <td><input type="text" name="mascotHTTPProxy" size="64" value="<%=h(appProps.getMascotHTTPProxy())%>"></td>
        </tr>
        <tr>
            <td></td>
            <td><%=textLink("Test Mascot settings", "javascript:testMascot()")%>
            </td>
        </tr>

        <tr>
            <td>&nbsp;</td>
        </tr>

        <tr>
            <td><%= button("Save").submit(true) %> <%= button("Cancel").href(PageFlowUtil.urlProvider(AdminUrls.class).getAdminConsoleURL())%></td>
        </tr>
    </table>
</labkey:form>

<labkey:form name="mascottest" action="mascotTest.view" enctype="multipart/form-data" method="post" target='_new' >
    <input type="hidden" name="mascotServer" value="" />
    <input type="hidden" name="mascotUserAccount" value="" />
    <input type="hidden" name="mascotUserPassword" value="" />
    <input type="hidden" name="mascotHTTPProxy" value="" />
</labkey:form>
