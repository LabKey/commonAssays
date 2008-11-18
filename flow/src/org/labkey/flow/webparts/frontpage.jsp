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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.persist.ObjectType" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.webparts.FlowFrontPage" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.security.User" %>  
<%@page extends="org.labkey.api.jsp.JspBase" %>
<%
    FlowFrontPage front = (FlowFrontPage) HttpView.currentModel();
    Container c = front.c;
    User user = getViewContext().getUser();

    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(c);
    boolean _hasPipelineRoot = pipeRoot != null && pipeRoot.getUri(c) != null;
    boolean _canSetPipelineRoot = user.isAdministrator() && (pipeRoot == null || c.equals(pipeRoot.getContainer()));
    boolean _canInsert = c.hasPermission(user, ACL.PERM_INSERT);
    boolean _canUpdate = c.hasPermission(user, ACL.PERM_UPDATE);
    boolean _canCreateFolder = c.getParent() != null && !c.getParent().isRoot() &&
            c.getParent().hasPermission(user, ACL.PERM_ADMIN);

    int _fcsFileCount = FlowManager.get().getObjectCount(c, ObjectType.fcsKeywords);
    int _fcsRunCount = FlowManager.get().getRunCount(c, ObjectType.fcsKeywords);
    int _fcsRealRunCount = FlowManager.get().getFCSRunCount(c);
    int _fcsAnalysisCount = FlowManager.get().getObjectCount(c, ObjectType.fcsAnalysis);
    int _fcsAnalysisRunCount = FlowManager.get().getRunCount(c, ObjectType.fcsAnalysis);
    int _compensationMatrixCount = FlowManager.get().getObjectCount(c, ObjectType.compensationMatrix);
    int _compensationRunCount = FlowManager.get().getRunCount(c, ObjectType.compensationControl);
    FlowScript[] _scripts = FlowScript.getAnalysisScripts(c);
    FlowProtocol _protocol = FlowProtocol.getForContainer(c);
%>
<style type="text/css">
    .bigbutton {height:30px; background:pink; border:2px solid black; font-weight:bold;}
</style>
<div align="center">
<table cellspacing=4 cellpadding="5"><tr>
    <td class="bigbutton">Files</td>
    <td class="bigbutton">Scripts and Compensation</td>
    <td class="bigbutton">Results</td>
</tr></table>
</div>