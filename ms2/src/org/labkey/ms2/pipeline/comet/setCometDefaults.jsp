<%
/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container"%>
<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.ms2.pipeline.PipelineController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
    }
%>
<%
    JspView<PipelineController.SetDefaultsForm> view = (JspView<PipelineController.SetDefaultsForm>) HttpView.currentView();
    PipelineController.SetDefaultsForm form = view.getModelBean();
    Container c = getContainer();
%>
<labkey:errors />
<labkey:form method="post" action="<%=urlFor(PipelineController.SetCometDefaultsAction.class)%>">
    <div>
        <textarea style="width: 100%" id="configureXml" name="configureXml" cols="90" rows="20"><%=unsafe(form.getConfigureXml())%></textarea>
    </div>
    <div>
        For detailed explanations of all available input parameters, see the
            <a href="http://comet-ms.sourceforge.net/" target="_blank" rel="noopener noreferrer">Comet documentation</a> and <%= helpLink("pipelineComet", "LabKey Server Comet documentation")%> on-line.
    </div>
    <div>
        <labkey:button text="Set Defaults"/> <labkey:button text="Cancel" href="<%=urlProvider(PipelineUrls.class).urlSetup(c)%>"/>
    </div>
</labkey:form>
<script for=window event=onload nonce="<%=getScriptNonce()%>">
try {document.getElementById("analysisName").focus();} catch(x){}
Ext.EventManager.on('configureXml', 'keydown', LABKEY.ext.Utils.handleTabsInTextArea);
</script>
