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
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors />
<form method="POST" action="<%=formAction(ScriptController.Action.editCompensationCalculation)%>"
      enctype="multipart/form-data">
    <p>
        The compensation calculation tells <%=FlowModule.getLongProductName()%> how
        to identify the compensation controls in an experiment run, and what gates
        to apply.  A compensation control is identified as having a particular value
        for a specific keyword.
    </p>

    <p>
        You can define the compensation calculation by uploading a FlowJo XML workspace<br>
        <input type="file" name="workspaceFile"><br>
        Important: This workspace must contain only one set of compensation controls, or you will not be able to select
        the keywords that you want to.
    </p>
        <input type="submit" value="Submit">
</form>
