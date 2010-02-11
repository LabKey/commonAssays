<%
/*
 * Copyright (c) 2006-2010 LabKey Corporation
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
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.util.URIUtil" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportRunsForm" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.pipeline.browse.PipelinePathForm" %>
<%@ page import="java.net.URI" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileFilter" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.flow.analysis.model.FCS" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.util.FileUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ImportRunsForm> me = (JspView<ImportRunsForm>) HttpView.currentView();
    ImportRunsForm form = me.getModelBean();
    ViewContext context = HttpView.getRootContext();

    Container c = context.getContainer();
    PipeRoot pr = PipelineService.get().findPipelineRoot(c);
    URI rootURI = pr.getUri();

    Map<String, String> paths = form.getNewPaths();

    %><labkey:errors/><%

    if (paths != null && paths.size() != 0)
    {
        %><form method="POST" action="<%=new ActionURL(AnalysisScriptController.ImportRunsAction.class, c)%>">
        <input type="hidden" name="path" value="<%=h(form.getPath())%>">
        <input type="hidden" name="current" value="<%=form.isCurrent()%>">
        <input type="hidden" name="confirm" value="true">
        <p>
            The following directories within <em><%=h(form.getDisplayPath())%></em> contain the FCS files for your experiment runs.
            <%=FlowModule.getLongProductName()%> will read the keywords from these FCS files into the database.  The FCS files
            themselves will not be modified, and will remain in the file system.
        </p>
        <table class="labkey-indented"><%

        for (Map.Entry<String, String> entry : paths.entrySet())
        {
            %><tr>
            <td><input type="hidden" name="file" value="<%=h(entry.getKey())%>"></td>
            <td><%=h(entry.getValue())%></td>
            </tr><%
        }

        %></table>
        <br />
        <labkey:button text="Import Selected Runs" action="<%=new ActionURL(AnalysisScriptController.ImportRunsAction.class, c)%>"/>
        <labkey:button text="Cancel" href="<%=form.getReturnURLHelper()%>"/>
        </form><%
    }
    else
    {
        %><labkey:button text="Browse for more runs" href="<%=form.getReturnURLHelper()%>"/><%
    }

%>