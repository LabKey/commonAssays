<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
    MS2Controller.RenameBean bean = ((JspView<MS2Controller.RenameBean>) HttpView.currentView()).getModelBean();
%>
<labkey:form action="<%=h(buildURL(MS2Controller.RenameRunAction.class))%>" method="post">
<%=generateReturnUrlFormField(bean.returnURL)%>
<input type="hidden" name="run" value="<%=bean.run.getRun()%>"/>
<table class="lk-fields-table">
    <tr>
        <td class='labkey-form-label'>Description:</td>
        <td><input type="text" size="70" name="description" id="description" value="<%=h(bean.description)%>"/></td>
    </tr>
    <tr>
        <td colspan="2" style="padding-top: 10px;">
            <%= button("Rename").submit(true) %>
            <%= button("Cancel").href(bean.returnURL) %>
        </td>
    </tr>
</table>
</labkey:form>