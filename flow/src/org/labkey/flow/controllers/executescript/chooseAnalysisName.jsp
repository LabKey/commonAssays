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
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.data.DataRegion"%>
<%@ page import="org.labkey.api.data.DataRegionSelection" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController.Action" %>
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToAnalyzeForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Set" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ChooseRunsToAnalyzeForm> me = (JspView<ChooseRunsToAnalyzeForm>) HttpView.currentView();
    ViewContext context = HttpView.getRootContext();
    ChooseRunsToAnalyzeForm form = me.getModelBean();
%>
<labkey:errors/>
<form method="POST" action="analyzeSelectedRuns.post">
    <p>What do you want to call the new analysis folder?<br>
        <% String name = form.ff_analysisName;
            if (StringUtils.isEmpty(name))
            {
                Set<String> namesInUse = new HashSet<String>();
                for (FlowExperiment experiment : FlowExperiment.getExperiments(context.getContainer()))
                {
                    namesInUse.add(experiment.getName().toLowerCase());
                }
                String baseName = FlowExperiment.DEFAULT_ANALYSIS_NAME;
                name = baseName;
                int i = 0;
                while (namesInUse.contains(name.toLowerCase()))
                {
                    i ++;
                    name = baseName + i;
                }
            }
        %>

        <input type="text" name="ff_analysisName" value="<%=h(name)%>">
    </p>

    <labkey:button text="Analyze runs" action="<%=Action.analyzeSelectedRuns%>"/>
    <labkey:button text="Go back" action="<%=Action.chooseRunsToAnalyze%>"/>
    <% for (int runid : form.getSelectedRunIds()) { %>
    <input type="hidden" name="<%=DataRegion.SELECT_CHECKBOX_NAME%>" value="<%=runid%>">
    <% } %>
    <input type="hidden" name="<%=DataRegionSelection.DATA_REGION_SELECTION_KEY%>" value="<%=form.getDataRegionSelectionKey()%>"> 
    <input type="hidden" name="scriptId" value="<%=form.getProtocol().getScriptId()%>">
    <input type="hidden" name="actionSequence" value="<%=form.getProtocolStep().getDefaultActionSequence()%>">
    <input type="hidden" name="ff_compensationMatrixOption" value="<%=h(form.ff_compensationMatrixOption)%>">
</form>
