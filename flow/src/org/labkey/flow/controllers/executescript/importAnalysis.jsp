<%
/*
 * Copyright (c) 2008 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    ActionURL cancelUrl = PageFlowUtil.urlProvider(ProjectUrls.class).urlStart(container);
    boolean hasPipelineRoot = pipeRoot != null && pipeRoot.getUri(container) != null;
    boolean canSetPipelineRoot = context.getUser().isAdministrator() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>
<table border="0" style="border-collapse:collapse;" cellpadding="4">
    <tr>
        <% for (AnalysisScriptController.ImportAnalysisStep step : AnalysisScriptController.ImportAnalysisStep.values())
        {
            boolean currentStep = step.getNumber() == form.getStep();
            boolean futureStep = step.getNumber() > form.getStep();
            if (step.getNumber() > 0) {
                %><td style="border-bottom:1px solid <%=futureStep ? "silver" : "black"%>" width="40"/><%
            }
            %>
            <td width="60" style="text-align:center;color:<%=futureStep ? "silver" : "black"%>" rowspan="2" valign="top">
                <span style="font-size:2em;font-weight:<%=currentStep ? "bold":"normal"%>;<%=step.getNumber() == 0 ? "visibility:hidden;":""%>"><%=step.getNumber()%></span><br/>
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
    <tr>
        <% for (AnalysisScriptController.ImportAnalysisStep step : AnalysisScriptController.ImportAnalysisStep.values())
        {
            if (step.getNumber() > 0) {
                %><td /><%
            }
        } %>
    </tr>
</table>

<labkey:errors/>
<%--<%=currentView.renderErrors(true)%>--%>

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

<% if (form.getWizardStep() == AnalysisScriptController.ImportAnalysisStep.INIT) { %>
    <p>You can import results from a FlowJo workspace containing statistics that FlowJo has calculated.
        To do this, use FlowJo to save the workspace as XML.</p>
    <%=PageFlowUtil.generateSubmitButton("Begin")%>
    <%=PageFlowUtil.generateButton("Cancel", cancelUrl)%>
<% } else if (form.getWizardStep().getNumber() < AnalysisScriptController.ImportAnalysisStep.CONFIRM.getNumber()) { %>

    <% if (form.getWizardStep() == AnalysisScriptController.ImportAnalysisStep.UPLOAD_WORKSPACE) { %>
        <p>You may either upload a FlowJo workspace from your local computer or browse the pipeline
           for a FlowJo workspace available to the server.  Be sure to save your FlowJo workspace
           as XML so <%=FlowModule.getShortProductName()%> can read it.
        </p>
        <hr/>
        <h3>Option 1: Upload from your computer</h3>
        <div style="padding-left: 2em; padding-bottom: 1em;">
            Which file do you want to upload?<br/><br/>
            <input type="file" name="workspace.file">
        </div>
        <h3>Option 2: Browse the pipeline</h3>
        <div style="padding-left: 2em; padding-bottom: 1em;">
            <% if (hasPipelineRoot) {
                String inputId = "workspace.path";
                %>
                You can browse the pipeline directories and find the <b>workspace XML</b> to import.<br/><br/>
                <%  if (!form.getWorkspace().getHiddenFields().containsKey("path")) { %>
                    <input type="hidden" id="<%=inputId%>" name="<%=inputId%>" value=""/>
                <%  }  %>
                <script type="text/javascript">
                   LABKEY.requiresClientAPI(true);
                   LABKEY.requiresScript("ColumnTree.js",false);
                   LABKEY.requiresScript("FileTree.js",false);
                </script>
                <div id='tree' class='extContainer'></div>
                <script type="text/javascript">
                Ext.onReady(function ()
                {
                   var tree = new LABKEY.ext.FileTree({
                     id : 'tree',
                     renderTo : 'tree',
                     title : "Select FlowJo Workspace XML file",
                     inputId : "<%=inputId%>",
                     dirsSelectable : false,
                     browsePipeline : true,
                     relativeToRoot : true,
                     fileFilter : /^.*\.xml/,
                     listeners : {
                         dblclick : function (node, e) {
                             if (node.isLeaf() && !node.disabled)
                                 document.forms["importAnalysis"].submit();
                         }
                     }
                   });
                   tree.render();
                   tree.root.expand();
                });
                </script>
                <%
            } else {
                %><p><em>The pipeline root has not been set for this folder.</em><br>
                    Once the pipeline root has been set, you can save the workspace to
                    the pipeline file server and manage your workspace and FCS files
                    from a central location.
                    </p><%
                if (canSetPipelineRoot) {
                    %><%=PageFlowUtil.generateButton("Set pipeline root", PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(container))%><%
                } else {
                    %>Contact your administrator to set the pipeline root for this folder.<%
                }
            } %>
        </div>
    <% } %>

    <% if (form.getWizardStep() == AnalysisScriptController.ImportAnalysisStep.ASSOCIATE_FCSFILES) { %>
        <p>Optionally, you can browse the pipeline for the FCS files used in the workspace.
            Once the workspace and FCS files are associated, you will be able to use <%=FlowModule.getLongProductName()%>
            to see additional graphs, or calculate additional statistics. If you choose not to associate the FCS files,
            graphs won't be generated but all the statistics calculated by FlowJo will be available.
            The FCS files themselves will not be modified, and will remain in the file system.
        </p>
        <hr/>
        <div style="padding-left: 2em; padding-bottom: 1em;">
            <% if (hasPipelineRoot) {
                String inputId = "runFilePathRoot";
                String name = form.getWorkspace().getPath();
                if (name == null)
                    name = form.getWorkspace().getName();
                %>
                Select a <b>directory</b> containing the FCS files below if you want
                to associate the workspace <em>'<%=h(name)%>'</em> with a set of FCS files.
                <br/><br/>
                <input type="hidden" id="<%=inputId%>" name="<%=inputId%>" value="<%=form.getRunFilePathRoot() == null ? "" : form.getRunFilePathRoot()%>"/>
                <script type="text/javascript">
                   LABKEY.requiresClientAPI(true);
                   LABKEY.requiresScript("ColumnTree.js",false);
                   LABKEY.requiresScript("FileTree.js",false);
                </script>
                <div id='tree' class='extContainer'></div>
                <script type="text/javascript">
                Ext.onReady(function ()
                {
                   var tree = new LABKEY.ext.FileTree({
                     id : 'tree',
                     renderTo : 'tree',
                     title : "Select directory of FCS files",
                     inputId : "<%=inputId%>",
                     filesSelectable : false,
                     browsePipeline : true,
                     relativeToRoot : true,
                     fileFilter : /^.*\.fcs/,
                     initialSelection : '<%=form.getRunFilePathRoot()%>',
                     listeners : {
                         dblclick : function (node, e) {
                             if (!node.isLeaf() && !node.disabled)
                                 document.forms["importAnalysis"].submit();
                         }
                     }
                   });
                   tree.render();
                   tree.root.expand();
                });
                </script>
                <%
            } else {
                %><p><em>The pipeline root has not been set for this folder.</em><br>
                    You can safely skip this step, however no graphs can be generated
                    when importing the FlowJo workspace without the FCS files.</p><%
                if (canSetPipelineRoot) {
                    %><%=PageFlowUtil.generateButton("Set pipeline root", PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(container))%><%
                } else {
                    %>Contact your administrator to set the pipeline root for this folder.<%
                }
            } %>
        </div>
    <% } %>

    <% if (form.getWizardStep() == AnalysisScriptController.ImportAnalysisStep.CHOOSE_ANALYSIS) { %>
        <input type="hidden" name="runFilePathRoot" id="runFilePathRoot" value="<%=h(form.getRunFilePathRoot())%>">

        <p>The statistics in this workspace that have been calculated by FlowJo will be imported to <%=FlowModule.getLongProductName()%>.<br><br>
        <%=FlowModule.getLongProductName()%> organizes results into different "analysis folders".  The same FCS file should only
        be analyzed once in a given analysis folder.  If you want to analyze the same FCS file in two different ways,
        those results should be put into different analysis folders.</p>
        <hr/>
        <%
            FlowExperiment[] analyses = FlowExperiment.getAnalyses(container);
        %>
        <div style="padding-left: 2em; padding-bottom: 1em;">
            <% if (analyses.length == 0) { %>
                What do you want to call the new analysis folder?  You will be able to use this name for multiple uploaded workspaces.<br><br>
                <input type="text" name="newAnalysisName" value="<%=FlowExperiment.DEFAULT_ANALYSIS_NAME%>">
            <% } else { %>
                <p>
                <% int recentId = FlowExperiment.getMostRecentAnalysis(container).getExperimentId(); %>
                Which analysis folder do you want to put the results into?<br>
                <select name="existingAnalysisId">
                    <% for (FlowExperiment analysis : analyses) { %>
                        <option value="<%=h(analysis.getExperimentId())%>" <%=analysis.getExperimentId() == recentId ? "selected":""%>><%=h(analysis.getName())%></option>
                    <% } %>
                </select>
                </p>
                <p>
                or create a new analysis folder named:<br>
                <%
                    String newAnalysisName = form.getNewAnalysisName();
//                    if (StringUtils.isEmpty(newAnalysisName))
//                        newAnalysisName = FlowExperiment.DEFAULT_ANALYSIS_NAME + analyses.length;
                %>
                <input type="text" name="newAnalysisName" value="<%=h(newAnalysisName)%>">
                </p>
            <% } %>
        </div>
    <% } %>

    <%=PageFlowUtil.generateButton("Back", "javascript:window.history.back();")%>
    <%=PageFlowUtil.generateSubmitButton("Next")%>
    <%=PageFlowUtil.generateButton("Cancel", cancelUrl)%>
    
<% } else { %>
    <input type="hidden" name="runFilePathRoot" id="runFilePathRoot" value="<%=h(form.getRunFilePathRoot())%>">
    <input type="hidden" name="existingAnalysisId" id="existingAnalysisId" value="<%=h(form.getExistingAnalysisId())%>">
    <input type="hidden" name="newAnalysisName" id="newAnalysisName" value="<%=h(form.getNewAnalysisName())%>">

    <p>You are about to import the analysis from the workspace with the following settings:</p>
    <%
        FlowJoWorkspace workspace = form.getWorkspace().getWorkspaceObject();
    %>
    <ul>
        <li style="padding-bottom:0.5em;">
            <% if (form.getNewAnalysisName() != null) { %>
                <b>New Analysis Folder:</b> <%=h(form.getNewAnalysisName())%>
            <% } else { %>
                <b>Existing Analysis Folder:</b> <%=h(FlowExperiment.fromExperimentId(form.getExistingAnalysisId()).getName())%>
            <% } %>
        </li>
        <li style="padding-bottom:0.5em;">
            <b>FCS File Path:</b>
            <% if (form.getRunFilePathRoot() == null) { %>
                <i>none set</i>
            <% } else { %>
                <%=h(form.getRunFilePathRoot())%>
            <% } %>
        </li>
        <li style="padding-bottom:0.5em;">
            <%
                String name = form.getWorkspace().getPath();
                if (name == null)
                    name = form.getWorkspace().getName();
            %>
            <b>Workspace:</b> <%=h(name)%><br/>
            <table border="0" style="margin-left:1em;">
                <tr>
                    <td><b>Sample Count:</b></td>
                    <td><%=h(workspace.getSamples().size())%></td>
                </tr>
                <tr>
                    <td><b>Comp. Matrices:</b></td>
                    <td><%=workspace.getCompensationMatrices().size()%></td>
                </tr>
                <tr>
                    <td><b>Parameter Names:</b></td>
                    <td><%=h(StringUtils.join(workspace.getParameters(), ", "))%></td>
                </tr>
            </table>
        </li>
    </ul>

    <%=PageFlowUtil.generateButton("Back", "javascript:window.history.back();")%>
    <%=PageFlowUtil.generateSubmitButton("Finish")%>
    <%=PageFlowUtil.generateButton("Cancel", cancelUrl)%>
<% } %>
</form>
