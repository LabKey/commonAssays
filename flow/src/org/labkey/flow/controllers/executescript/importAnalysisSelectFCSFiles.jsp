<%
/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.api.security.permissions.AdminOperationsPermission" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.analysis.model.ExternalAnalysis" %>
<%@ page import="org.labkey.flow.analysis.model.IWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Objects" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    boolean keywordRunsExist = form.getKeywordRunsExist();
    Container container = getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = container.hasPermission(getUser(), AdminOperationsPermission.class)
                                    && (pipeRoot == null || container.equals(pipeRoot.getContainer()));

    IWorkspace workspace = form.getWorkspace().getWorkspaceObject();
    List<String> warnings = workspace.getWarnings();
    if (!warnings.isEmpty())
    {
        %>
        <p class="labkey-warning-messages" style="display:inline-block">
        <b>Warnings (<%=warnings.size()%>):</b><br>
        <textarea rows="10" cols="140" readonly><%
        for (String warning : warnings)
        {
            %><%=h(warning)%><%=unsafe("\n")%><%
        }
        %></textarea></p><%
    }
%>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    function clearFileBrowserSelection(selectedValue)
    {
        if (fileBrowser && fileBrowser.rendered)
        {
            fileBrowser.getGrid().getSelectionModel().clearSelections();
            selectRecord(null);
            fileBrowser.setDisabled(selectedValue !== <%=q(ImportAnalysisForm.SelectFCSFileOption.Browse)%>);
        }
    }

    function clearSelections(selectedValue)
    {
        clearFileBrowserSelection(selectedValue);
        return true;
    }
</script>

<p><em>Optionally</em>, you can browse the pipeline for the FCS files used in the <%=h(workspace.getKindName())%>.
    <% if (workspace.hasAnalysis()) { %>
    Once the workspace and FCS files are associated, you will be able to use <%=h(FlowModule.getLongProductName())%>
    to see additional graphs, or calculate additional statistics.
    <% } %>
    The FCS files themselves will not be modified, and will remain in the file system.
</p>
<hr/>

<labkey:input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.None.name()%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.None%>"
       checked="<%=(Objects.equals(form.getSelectFCSFilesOption(), ImportAnalysisForm.SelectFCSFileOption.None))%>"
       onClick="clearSelections(this.value);" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.None%>">Don't associate FCS files with the <%=h(workspace.getKindName())%>.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    <% if (workspace.hasAnalysis()) { %>
    Statistics from the <%=h(workspace.getKindName())%> will be imported but no graphs will be generated.<br>
    <% } else if (workspace instanceof ExternalAnalysis) { %>
    Keywords, statistics, graphs, and compensation matrices will be imported from the analysis archive.
    <% } %>
</div>

<%
    // XXX: Disable all other options if the archive includes FCS files.
    if (form.getWorkspace().isIncludesFCSFiles())
    {
%>
<labkey:input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.Included.name()%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.Included%>"
       checked="<%=(Objects.equals(form.getSelectFCSFilesOption(), ImportAnalysisForm.SelectFCSFileOption.Included))%>"
       onClick="clearSelections(this.value);" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.Included%>">Import FCS files included in the analysis archive.</label>
<%
    }

%>
<labkey:input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.Previous.name()%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.Previous%>"
       checked="<%=(Objects.equals(form.getSelectFCSFilesOption(), ImportAnalysisForm.SelectFCSFileOption.Previous))%>"
       isDisabled="<%=(!keywordRunsExist)%>"
       onClick="clearSelections(this.value);" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.Previous%>">
    <div style="display:inline-block; <%=unsafe(!keywordRunsExist ? "color:silver;" : "")%>">Previously imported FCS files.</div>
</label>
<div style="padding-left: 2em; padding-bottom: 1em; <%=unsafe(!keywordRunsExist ? "color:silver;" : "")%>">
    <%=h(FlowModule.getLongProductName())%> will attempt to match the samples in the <%=h(workspace.getKindName())%> with previously imported FCS files.
</div>

<labkey:input type="radio" name="selectFCSFilesOption"
       id="<%=ImportAnalysisForm.SelectFCSFileOption.Browse.name()%>" value="<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>"
       checked="<%=(Objects.equals(form.getSelectFCSFilesOption(), ImportAnalysisForm.SelectFCSFileOption.Browse))%>"
       onClick="clearSelections(this.value); renderFileBrowser();" />
<label for="<%=ImportAnalysisForm.SelectFCSFileOption.Browse%>"> Browse the pipeline for a directory of FCS files.</label>
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
    to associate the <%=h(workspace.getKindName())%> <em>'<%=h(name)%>'</em> with a set of FCS files.
    <br>
    The sample's keywords stored in the <%=h(workspace.getKindName())%> will be used instead of those from the FCS files.
    <br/><br/>
    <input type="hidden" id="<%=unsafe(inputId)%>" name="<%=unsafe(inputId)%>" value="<%=h(keywordDir)%>"/>


    <div id="treeDiv"></div>
    <script type="text/javascript" nonce="<%=getScriptNonce()%>">
        var inputId=<%=q(inputId)%>;
        var fileSystem;
        var fileBrowser;
        function selectRecord(path)
        {
            Ext.get(inputId).dom.value=path;
            if (path)
            {
                document.getElementById(<%=q(ImportAnalysisForm.SelectFCSFileOption.Browse)%>).checked = true;
            }
        }

        function renderFileBrowser()
        {
            if (!fileBrowser)
            {
                Ext4.QuickTips.init();

                fileSystem = Ext4.create('File.system.Webdav', {
                    rootPath : <%=q(pipeRoot.getWebdavURL().toString())%>,
                    rootName : <%=q(AppProps.getInstance().getServerName())%>
                    //rootOffset : <%=q(keywordDir)%>
                });

                fileBrowser = Ext4.create('File.panel.Browser', {
                    fileSystem:fileSystem
                    ,height:600
                    ,helpEl:null
                    ,showAddressBar:false
                    ,showFolderTree:true
                    ,showDetails:false
                    ,showFileUpload:false
                    ,allowChangeDirectory:true
                    ,showToolbar:false
                    ,fileFilter : {test: function(data){ return !data.file || endsWith(data.name,".fcs") || endsWith(data.name,".facs")|| endsWith(data.name, ".lmd"); }}
                    ,gridConfig : {selModel : {selType: 'checkboxmodel', mode : 'SINGLE'}}
                    ,listeners: {
                        afterrender: {
                            fn: function(f) {
                                var size = Ext4.getBody().getSize();
                                LABKEY.ext4.Util.resizeToViewport(f, size.width, size.height, 20, null);
                            },
                            single: true
                        }
                    }
                });

                fileBrowser.on("doubleclick", function(record){
                    if (record && record.data.collection)
                    {
                        var path = record.data.id.replace(fileBrowser.getBaseURL(), '/');
                        selectRecord(path);
                        document.forms["importAnalysis"].submit();
                        return false;
                    }
                    return true;
                });
                fileBrowser.on("selectionchange", function(){
                    var path = null;
                    var record = fileBrowser.getGrid().getSelectionModel().getSelection();
                    if (record && record.length === 1)
                    {
                        path = record[0].data.id.replace(fileBrowser.getBaseURL(), '/');
                        if (!record[0].data.collection)
                            path = fileSystem.getParentPath(path); // parent directory of selected .fcs file
                    }
                    selectRecord(path);
                    return true;
                });

                fileBrowser.render('treeDiv');
                var path = <%=q(keywordDir)%>;
            }
            return true;
        }

        Ext4.onReady(function()
        {
            <% if (Objects.equals(form.getSelectFCSFilesOption(), ImportAnalysisForm.SelectFCSFileOption.Browse)) { %>
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
    when importing the <%=h(workspace.getKindName())%> without the FCS files.</p><%
    if (canSetPipelineRoot) {
%><%= button("Set pipeline root").href(urlProvider(PipelineUrls.class).urlSetup(container)) %><%
} else {
%>Contact your administrator to set the pipeline root for this folder.<%
        }
    } %>
</div>

