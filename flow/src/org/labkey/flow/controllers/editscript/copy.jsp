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
<%@ page import="org.labkey.flow.data.FlowProtocolStep"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page import="org.labkey.flow.controllers.editscript.CopyProtocolForm" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% CopyProtocolForm form = (CopyProtocolForm) this.form; %>
<labkey:errors/>
<labkey:form action="<%=formAction(ScriptController.CopyAction.class)%>" method="POST">
    <p>
        What do you want to call the new script?<br>
        <input type="text" name="name" value="<%=h(form.name)%>">
    </p>
    <p>
        Which sections of the '<%=form.getFlowScript().getName()%>' script do you want to copy?<br>
<% if (form.getFlowScript().hasStep(FlowProtocolStep.calculateCompensation)) { %>
        <input type="checkbox" name="copyCompensationCalculation" value="true"<%=checked(form.copyCompensationCalculation)%>>Compensation Calculation<br>
<% } %>
<% if (form.getFlowScript().hasStep(FlowProtocolStep.analysis)) { %>
        <input type="checkbox" name="copyAnalysis" value="true"<%=checked(form.copyAnalysis)%>>Analysis<br>
<% } %>
    </p>
    <input type="submit" value="Make Copy">

</labkey:form>
