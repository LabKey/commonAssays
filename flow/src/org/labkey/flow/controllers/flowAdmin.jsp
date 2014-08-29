<%
/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.admin.AdminUrls"%>
<%@ page import="org.labkey.api.data.ContainerManager"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.FlowAdminForm" %>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<% FlowAdminForm form = (FlowAdminForm) getModelBean(); %>
<labkey:errors />
<labkey:form method="POST" action="<%=h(new ActionURL(FlowController.FlowAdminAction.class, ContainerManager.getRoot()))%>">
    <p>
        Which directory should the flow module use to do work in?  By default, it will use the system temporary directory.<br>
        <input size=50 type="text" id="workingDirectory" name="workingDirectory" value="<%=h(form.getWorkingDirectory())%>">
    </p>
    <p>
        <labkey:checkbox name="deleteFiles" id="deleteFiles" value="true" checked="<%=form.isDeleteFiles()%>"/>
        <label for="deleteFiles">Delete temporary analysis files after jobs are successfully completed?</label><br>
    </p>
    <p>
        <labkey:checkbox name="normalizationEnabled" id="normalizationEnabled" value="true" checked="<%=form.isNormalizationEnabled()%>"/>
        <label for="normalizationEnabled">Enable experimental R normalization in FlowJo import wizard?</label><br>
        R will need to be configured and the flowWorkspace library installed to use normalization.
    </p>
    <labkey:button text="update" />
    <labkey:button text="cancel" href="<%=urlProvider(AdminUrls.class).getAdminConsoleURL()%>" />
</labkey:form>
