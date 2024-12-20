<%
/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.security.permissions.AdminOperationsPermission" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.api.util.HelpTopic" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    Container container = getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = container.hasPermission(getUser(), AdminOperationsPermission.class)
                                    && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>
<p>You may either upload an <%=helpLink("flowAnalysisArchiveFormat", "analysis archive")%> or FlowJo workspace from your local computer or browse the pipeline
    for an analysis archive or FlowJo workspace available to the server.<br>
    FlowJo .wsp files and Mac FlowJo .jo workspaces exported as xml are supported (more on <%=helpLink("flowJoVersions", "FlowJo versions")%>.)
</p>
<hr/>
<input type="radio" name="selectWorkspace" id="uploadWorkspace" value="uploadWorkspace" />
Upload file from your computer
<div style="padding: 5px 0 10px 2em;">
    <input type="file" id="workspace.file" name="workspace.file" style="border: none; background-color: transparent;">
    <script type="text/javascript" nonce="<%=getScriptNonce()%>">
        document.getElementById("workspace.file")['onchange'] = function()
        {
            document.getElementById("uploadWorkspace").checked = true;
        }
    </script>
</div>
<input type="radio" name="selectWorkspace" id="browseWorkspace" value="browseWorkspace" />
Browse the pipeline
<div style="padding-left: 2em; padding-bottom: 1em;">
    <% if (hasPipelineRoot) {
        String inputId = "workspace.path";
        String href = new HelpTopic("flowAnalysisArchiveFormat").getHelpTopicHref();
    %>
    Select either a FlowJo workspace (.wsp and .xml) or an <a href="<%=h(href)%>">analysis archive</a>.<br/><br/>
    <%  if (!form.getWorkspace().getHiddenFields(getViewContext()).containsKey("path")) { %>
    <input type="hidden" id="<%=unsafe(inputId)%>" name="<%=unsafe(inputId)%>" value=""/>
    <%  }  %>
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
                document.getElementById("browseWorkspace").checked = true;
                document.getElementById("workspace.file").value = null;
            }
            // setTitle...
        }

        Ext4.onReady(function()
        {
            Ext4.QuickTips.init();

            fileSystem = Ext4.create('File.system.Webdav', {
                rootPath : <%=q(pipeRoot.getWebdavURL())%>,
                rootName : <%=q(AppProps.getInstance().getServerName())%>
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
                ,fileFilter : {test: function(data){ return !data.file || endsWith(data.name,".xml") || endsWith(data.name, ".wsp") || endsWith(data.name, ".zip"); }}
                ,gridConfig : {selModel : {selType: 'checkboxmodel', mode : 'SINGLE'}}
                ,listeners: {
                    doubleclick: function(record) {
                        if (record && !record.data.collection) {
                            selectRecord(record.data.id.replace(fileBrowser.getBaseURL(), '/'));
                            document.forms["importAnalysis"].submit();
                            return false;
                        }
                        return true;
                    },
                    selectionchange: function() {
                        var path = null;
                        var record = fileBrowser.getGrid().getSelectionModel().getSelection();
                        if (record && record.length == 1 && !record[0].data.collection) {
                            path = record[0].data.id.replace(fileBrowser.getBaseURL(), '/');
                        }
                        selectRecord(path);
                        return true;
                    }
                }
            });

            fileBrowser.render('treeDiv');
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
%><%= button("Set pipeline root").href(urlProvider(PipelineUrls.class).urlSetup(container)) %><%
} else {
%>Contact your administrator to set the pipeline root for this folder.<%
        }
    } %>
</div>

