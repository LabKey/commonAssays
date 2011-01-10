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
<%@ page import="org.fhcrc.cpas.flow.script.xml.ScriptDocument" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController.ChooseRunsToAnalyzeAction" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page extends="org.labkey.flow.controllers.executescript.AnalysisScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    FlowScript analysisScript = getScript();
%>
<h4>Analysis script: '<%=h(analysisScript.getName())%>'</h4>
<p>Analysis scripts tell the Flow Module how to calculate the compensation matrix, what gates to apply, what statistics
to calculate, and what graphs to generate.</p>
<%
    boolean hasCompensation = false;
    boolean hasAnalysis = false;

    try
    {
        ScriptDocument script = analysisScript.getAnalysisScriptDocument();
        if (script != null && script.getScript() != null)
        {
            hasCompensation = script.getScript().getCompensationCalculation() != null;
            hasAnalysis = script.getScript().getAnalysis() != null;
        }
    }
    catch (Exception e)
    {

    }
%>
<p>
    <b>Compensation Calculation</b><br>
    <% if (hasCompensation)
    {%>
    <a href="<%=h(analysisScript.urlFor(ChooseRunsToAnalyzeAction.class, FlowProtocolStep.calculateCompensation))%>">Calculate some
        compensation matrices.</a><br>
    <% }
    else
    { %>
    This analysis script does not contain a compensation calculation section.
    <% } %>
</p>

<p>
    <b>Analysis</b><br>
    <% if (hasAnalysis)
    { %>
    <a href="<%=h(analysisScript.urlFor(ChooseRunsToAnalyzeAction.class, FlowProtocolStep.analysis))%>">Analyze some flow runs</a>
    <br>
    <% } else { %>
    This analysis script does not contain an analysis section.
    <% } %>
</p>

<p>
    <a href="<%=h(analysisScript.urlFor(ScriptController.BeginAction.class))%>">Edit the analysis script.</a><br>
    <a href="<%=h(analysisScript.urlFor(ScriptController.CopyAction.class))%>">Make a copy of this analysis script.</a><br>
    <a href="<%=h(analysisScript.urlFor(ScriptController.DeleteAction.class))%>">Delete this analysis script.</a><br>
</p>

