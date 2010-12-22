<%
    /*
    * Copyright (c) 2010 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.data.FlowObject" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.report.ScriptEngineReport" %>
<%@ page import="org.labkey.api.reports.ReportService" %>
<%@ page import="org.labkey.api.reports.Report" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%
    org.labkey.api.security.User u = getViewContext().getUser();
    Container c = getViewContext().getContainer();
    AnalysisScriptController.DemoForm form = (AnalysisScriptController.DemoForm)getModelBean();
    Integer experimentId = null;
    if (form != null)
    {
        experimentId = form.getExperimentId();
    }
    else
    {
        FlowExperiment exp = FlowExperiment.getMostRecentAnalysis(c);
        if (exp != null)
            experimentId = exp.getExperimentId();
    }

    List<FlowExperiment> experiments = new LinkedList<FlowExperiment>(Arrays.asList(FlowExperiment.getAnalyses(c)));
%>
<labkey:errors/>
<form action="demo.view" method="post">
    <label for="runName">Enter an run name:</label>
    <input type="textbox" id="runName" name="runName"><br>
    <br>

    <label for="experimentId">Analysis folder to put results in:</label>
    <select id="experimentId" name="experimentId">
        <labkey:options value="<%=experimentId == null ? null : Integer.valueOf(experimentId)%>"
                        map="<%=FlowObject.idLabelsFor(experiments, \"<create new>\")%>"/>
    </select><br>
    <br>

    <labkey:button text="Run R Analysis" />
</form>
