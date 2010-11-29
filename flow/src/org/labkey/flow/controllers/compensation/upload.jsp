<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController.UploadAction" %>
<%@ page import="org.labkey.flow.controllers.compensation.UploadCompensationForm" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% UploadCompensationForm form = (UploadCompensationForm) __form; %>
<labkey:errors/>
<form method="POST" action="<%=new ActionURL(UploadAction.class, getContainer())%>" enctype="multipart/form-data">
    <p>Give your new compensation matrix a name.<br>
        <input type="text" name="ff_compensationMatrixName" value="<%=h(form.ff_compensationMatrixName)%>"/>
    </p>

    <p>You can upload a compensation matrix file that was saved from FlowJo.<br>
        <input type="file" name="ff_compensationMatrixFile">
    </p>
    <labkey:button text="submit"/>
</form>