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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.SaveViewBean bean = ((JspView<MS2Controller.SaveViewBean>) HttpView.currentView()).getModelBean();
%>
<labkey:form method="post" action="<%=h(buildURL(MS2Controller.SaveViewAction.class))%>" className="labkey-data-region">
    <table class="lk-fields-table">
        <tr>
            <td>Name:</td>
            <td>
                <input name="name" id="name" style="width:200px;">
                <input type=hidden value="<%=h(bean.viewParams)%>" name="viewParams">
                <%=generateReturnUrlFormField(bean.returnURL)%>
            </td>
        </tr><%
if (bean.canShare)
{ %>
        <tr>
            <td colspan=2><input name=shared type=checkbox> Share view with all users of this folder</td>
        </tr><%
} %>
        <tr>
            <td colspan=2>
                <%= button("Save View").submit(true) %>
                <%= button("Cancel").href(bean.returnURL) %>
            </td>
        </tr>
    </table>
</labkey:form>