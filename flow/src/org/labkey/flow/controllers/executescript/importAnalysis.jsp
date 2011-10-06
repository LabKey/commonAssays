<%
/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.query.QueryParam" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.io.File" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = context.getUser().isAdministrator() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>
<script type="text/javascript">
    function endsWith(a,b)
    {
        return a.length >= b.length && a.indexOf(b) == (a.length-b.length);
    }
</script>
<table border="0" style="border-collapse:collapse;" cellpadding="4">
    <tr>
        <% for (AnalysisScriptController.ImportAnalysisStep step : AnalysisScriptController.ImportAnalysisStep.values())
        {
            boolean currentStep = step.getNumber() == form.getStep();
            boolean futureStep = step.getNumber() > form.getStep();
            if (step.getNumber() > 1) {
                %>
                <td valign="middle">
                    <img src="<%=context.getContextPath()%>/_.gif" style="background:<%=futureStep ? "silver" : "black"%>; width:40px; height:1px"/>
                </td>
                <%
            }
            %>
            <td width="60" style="text-align:center;color:<%=futureStep ? "silver" : "black"%>" valign="top">
                <span style="font-size:1.1em;font-weight:<%=currentStep ? "bold":"normal"%>;"><%=step.getNumber()%></span><br/>
                <span style="font-weight:<%=currentStep ? "bold":"normal"%>"><%=step.getTitle()%></span>
                <%--
                <form name="step_<%=step.name()%>" action="<%=new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, container)%>" method="POST" enctype="multipart/form-data">
                    <input type="hidden" name="step" value="<%=step.getNumber()-1%>">
                    <a href="javascript:void" onclick="document.step_<%=step.name()%>.submit()" style="text-decoration:none;color:inherit;">
                        <span style="font-size:2em;font-weight:<%=currentStep ? "bold":"normal"%>;<%=step.getNumber() == 0 ? "visibility:hidden;":""%>"><%=step.getNumber()%></span><br/>
                        <span style="font-weight:<%=currentStep ? "bold":"normal"%>"><%=step.getTitle()%></span>
                    </a>
                </form>
                --%>
            </td>
            <%
        }
        %>
    </tr>
</table>

<labkey:errors/>

<form name="importAnalysis" action="<%=new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, container)%>" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="step" value="<%=form.getStep()%>">
    <%
        Iterator i = form.getWorkspace().getHiddenFields().entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry)i.next();
            String key = "workspace." + entry.getKey();
            %><input type="hidden" id="<%=key%>" name="<%=key%>" value="<%=h(entry.getValue())%>"><%
        }
    %>

<% if (form.getWizardStep().getNumber() < AnalysisScriptController.ImportAnalysisStep.CONFIRM.getNumber()) { %>

    <%=generateBackButton()%>
    <%=generateSubmitButton("Next")%>
    <%=generateButton("Cancel", cancelUrl)%>

    <%
        JspView<ImportAnalysisForm> view = null;
        switch (form.getWizardStep())
        {
            case SELECT_WORKSPACE:
                view = new JspView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysisSelectWorkspace.jsp", form);
                break;

            case SELECT_FCSFILES:
                view = new JspView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysisSelectFCSFiles.jsp", form);
                break;

            case ANALYSIS_ENGINE:
                view = new JspView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysisAnalysisEngine.jsp", form);
                break;

            case ANALYSIS_OPTIONS:
                view = new JspView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysisAnalysisOptions.jsp", form);
                break;

            case CHOOSE_ANALYSIS:
                view = new JspView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysisChooseAnalysis.jsp", form);
                break;
        }
        include(view, out);
    %>

    <%=generateBackButton()%>
    <%=generateSubmitButton("Next")%>
    <%=generateButton("Cancel", cancelUrl)%>
    
<% } else { %>
    <%
    JspView<ImportAnalysisForm> view = new JspView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysisConfirm.jsp", form);
    include(view, out);
    %>
<% } %>
</form>
