<%
/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:form action="<%=form.urlFor(ScriptController.EditCompensationCalculationAction.class)%>" method="POST">
<p>
    In order to define the compensation calculation, you need to tell <%=FlowModule.getLongProductName()%> which keyword
    values identify which compensation well.  Choose an experiment run which has compensation controls in it.  On the next page
    you will then have the opportunity to choose which keywords in that experiment run identify the compensation controls.
</p>
<p><b>Note:</b> Experiment runs created by uploading FlowJo workspaces from the browser can't be used to define the compensation calculation.</p>
<p>
    Which experiment run do you want to use?<br>
    <select name="selectedRunId">
        <labkey:options value="<%=0%>" map="<%=form.getExperimentRuns()%>"/>
    </select>
</p>
<labkey:button text="Next Step" />
</labkey:form>
