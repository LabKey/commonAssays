<%
    /*
    * Copyright (c) 2011-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.flow.analysis.model.Workspace" %>
<%@ page import="org.labkey.flow.analysis.model.IWorkspace" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = context.getUser().isSiteAdmin() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));

    IWorkspace workspace = form.getWorkspace().getWorkspaceObject();
    List<String> warnings = workspace.getWarnings();
    if (warnings.size() > 0)
    {
        %><p class="labkey-warning-messages"><b>Warning:</b><br><%
        int len = Math.min(10, warnings.size());
        int i = 0;
        for (; i < len; i++)
        {
            %><%=h(warnings.get(i))%><br><%
        }
        if (i < warnings.size()) {
            %><em>not showing <%=warnings.size() - i%> more warnings ...</em><%
        }
        %></p><%
    }
%>
<script type="text/javascript">
    function clearExistingRunIdCombo()
    {
        var combo = document.forms.importAnalysis.existingKeywordRunId;
        if (combo && combo.tagName.toLowerCase() == "select")
            combo.selectedIndex = 0;
    }

    function clearFileBrowserSelection(selectedValue)
    {
        if (fileBrowser && fileBrowser.rendered)
        {
            fileBrowser.grid.getSelectionModel().clearSelections();
            selectRecord(null);
            fileBrowser.setDisabled(selectedValue != "<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>");
        }
    }

    function clearSelections(selectedValue)
    {
        //clearExistingRunIdCombo();
        clearFileBrowserSelection(selectedValue);
        return true;
    }
</script>

<p><em>Optionally</em>, you can browse the pipeline for the FCS files used in the workspace.
    Once the workspace and FCS files are associated, you will be able to use <%=h(FlowModule.getLongProductName())%>
    to see additional graphs, or calculate additional statistics.
    The FCS files themselves will not be modified, and will remain in the file system.
</p>
<hr/>

<input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.None%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.None%>"
       <%=checked(form.getSelectFCSFilesOption() == ImportAnalysisForm.SelectFCSFileOption.None)%>
       onclick="clearSelections(this.value);" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.None%>">Don't associate FCS files with workspace.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    Statistics from the FlowJo workspace will be imported but no graphs will be generated.<br>
    <em>NOTE:</em> Choosing this option will advance past the analysis engine step.
</div>

<%
    // XXX: Disable all other options if the archive includes FCS files.
    if (form.getWorkspace().isIncludesFCSFiles())
    {
%>
<input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.Included%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.Included%>"
       <%=checked(form.getSelectFCSFilesOption() == ImportAnalysisForm.SelectFCSFileOption.Included)%>
       onclick="clearSelections(this.value);" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.Included%>">Import FCS files included in the analysis archive.</label>
<%
    }
%>

<%
    FlowRun[] allKeywordRuns = FlowRun.getRunsForContainer(container, FlowProtocolStep.keywords);
    Map<FlowRun, String> keywordRuns = new LinkedHashMap<>(allKeywordRuns.length);
    for (FlowRun keywordRun : allKeywordRuns)
    {
        if (keywordRun.getPath() == null)
            continue;

        FlowExperiment experiment = keywordRun.getExperiment();
        if (experiment != null && (experiment.isWorkspace() || experiment.isAnalysis()))
            continue;

        File keywordRunFile = new File(keywordRun.getPath());
        if (keywordRunFile.exists())
        {
            String keywordRunPath = pipeRoot.relativePath(keywordRunFile);
            if (keywordRunPath != null && keywordRun.hasRealWells())
                keywordRuns.put(keywordRun, keywordRunPath);
        }
    }
%>
<input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.Previous%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.Previous%>"
        <%=checked(form.getSelectFCSFilesOption() == ImportAnalysisForm.SelectFCSFileOption.Previous)%>
        <%=disabled(keywordRuns.isEmpty())%>
       onclick="clearSelections(this.value);" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.Previous%>" style="<%=text(keywordRuns.isEmpty() ? "color:silver;" : "")%>">Previously imported FCS files.</label>
<div style="padding-left: 2em; padding-bottom: 1em; <%=text(keywordRuns.isEmpty() ? "color:silver;" : "")%>">
    <%=h(FlowModule.getLongProductName())%> will attempt to match the samples in the FlowJo workspace with previously imported FCS files.
</div>

<input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>"
        <%=checked(form.getSelectFCSFilesOption() == ImportAnalysisForm.SelectFCSFileOption.Browse)%>
       onclick="clearSelections(this.value); renderFileBrowser();" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>">Browse the pipeline for a directory of FCS files.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    <% if (hasPipelineRoot)
    {
        String inputId = "keywordDir";
        String name = form.getWorkspace().getPath();
        if (name == null)
            name = form.getWorkspace().getName();

        // UNDONE: support selecting multiple directories
        String keywordDir = form.getKeywordDir() != null ? form.getKeywordDir()[0] : "";
    %>
    Select a <b>directory</b> containing the FCS files below if you want
    to associate the workspace <em>'<%=h(name)%>'</em> with a set of FCS files.
    <br>
    The sample's keywords stored in the workspace will be used instead of those from the FCS files.
    <br/><br/>
    <input type="hidden" id="<%=text(inputId)%>" name="<%=text(inputId)%>" value="<%=h(keywordDir)%>"/>


    <div id="treeDiv" class="extContainer"></div>
    <script type="text/javascript">
        LABKEY.requiresScript("applet.js");
        LABKEY.requiresScript("fileBrowser.js");
        LABKEY.requiresScript("applet.js",true);
        LABKEY.requiresScript("FileUploadField.js");
    </script>
    <script type="text/javascript">
        var inputId=<%=q(inputId)%>;
        var fileSystem;
        var fileBrowser;
        function selectRecord(path)
        {
            Ext.get(inputId).dom.value=path;
            if (path)
            {
                clearExistingRunIdCombo();
                document.getElementById("<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>").checked = true;
            }
            // setTitle...
        }

        function renderFileBrowser()
        {
            if (!fileBrowser)
            {
                Ext.QuickTips.init();

                fileSystem = new LABKEY.FileSystem.WebdavFileSystem({
                    baseUrl:<%=q(pipeRoot.getWebdavURL())%>,
                    rootName:<%=PageFlowUtil.jsString(AppProps.getInstance().getServerName())%>});

                fileBrowser = new LABKEY.ext.FileBrowser({
                    fileSystem:fileSystem
                    ,helpEl:null
                    ,showAddressBar:false
                    ,showFolderTree:true
                    ,showDetails:false
                    ,showFileUpload:false
                    ,allowChangeDirectory:true
                    ,tbarItems:[]
                    ,fileFilter : {test: function(data){ return !data.file || endsWith(data.name,".fcs") || endsWith(data.name,".facs")|| endsWith(data.name, ".lmd"); }}
                });

                fileBrowser.on(LABKEY.FileSystem.BROWSER_EVENTS.doubleclick, function(record){
                    if (!record || !record.data.file)
                        return;
                    var path = fileSystem.parentPath(record.data.path);
                    selectRecord(path);
                    document.forms["importAnalysis"].submit();
                    return true;
                });
                fileBrowser.on(LABKEY.FileSystem.BROWSER_EVENTS.selectionchange, function(record){
                    var path = null;
                    if (record)
                    {
                        path = record.data.path;
                        if (record.data.file)
                            path = fileSystem.parentPath(path); // parent directory of selected .fcs file
                    }
                    selectRecord(path);
                    return true;
                });

                fileBrowser.render('treeDiv');
                var path = <%=q(keywordDir)%>;
                fileBrowser.start(path);
            }
            return true;
        }

        Ext.onReady(function()
        {
            <% if (form.getSelectFCSFilesOption() == ImportAnalysisForm.SelectFCSFileOption.Browse) { %>
            renderFileBrowser();
            <% } %>
        });
    </script>
    <%
    }
    else
    {
    %><p><em>The pipeline root has not been set for this folder.</em><br>
    You can safely skip this step, however no graphs can be generated
    when importing the FlowJo workspace without the FCS files.</p><%
    if (canSetPipelineRoot) {
%><%=generateButton("Set pipeline root", urlProvider(PipelineUrls.class).urlSetup(container))%><%
} else {
%>Contact your administrator to set the pipeline root for this folder.<%
        }
    } %>
</div>

