<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.security.permissions.ReadPermission" %>
<%@ page import="org.labkey.api.study.Study" %>
<%@ page import="org.labkey.api.study.assay.AssayPublishService" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportRunsForm" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ImportRunsForm> me = (JspView<ImportRunsForm>) HttpView.currentView();
    ImportRunsForm form = me.getModelBean();
    Container c = getContainer();

    Map<String, String> paths = form.getNewPaths();

    // Get set of valid copy to study targets
    Set<Study> validStudies = AssayPublishService.get().getValidPublishTargets(getUser(), ReadPermission.class);
    Map<String, String> targetStudies = new LinkedHashMap<>();
    targetStudies.put("", "[None]");
    for (Study study : validStudies)
    {
        Container studyContainer = study.getContainer();
        targetStudies.put(studyContainer.getId(), studyContainer.getPath() + " (" + study.getLabel() + ")");
    }

    // Pre-select the most recent target study
    if (form.getTargetStudy() == null)
        form.setTargetStudy(FlowRun.findMostRecentTargetStudy(c));

    %><labkey:errors/><%

    if (paths != null && paths.size() != 0)
    {
        %><labkey:form method="POST" action="<%=new ActionURL(AnalysisScriptController.ImportRunsAction.class, c)%>">
        <input type="hidden" name="path" value="<%=h(form.getPath())%>">
        <input type="hidden" name="current" value="<%=form.isCurrent()%>">
        <input type="hidden" name="confirm" value="true">
        <p>
            The following directories within <em><%=h(form.getDisplayPath())%></em> contain the FCS files for your experiment runs.
            <%=h(FlowModule.getLongProductName())%> will read the keywords from these FCS files into the database.  The FCS files
            themselves will not be modified, and will remain in the file system.
        </p>
        <ul class="labkey-indented"><%
        for (Map.Entry<String, String> entry : paths.entrySet())
        {
            %>
            <li><label><%=h(entry.getValue())%></label><input type="hidden" name="file" value="<%=h(entry.getKey())%>"></li>
            <%
        }%>
        </ul>

        <% if (targetStudies.size() > 0) { %>
            <p>
                <em>Optionally,</em> select a target study for imported FCS files.  The target study will be used
                as the detault copy to study target and, if the flow metadata specifies a specimen ID column, used
                to look up specimen information from the target study's specimen repository.
            </p>
            <p class="labkey-indented">
                <label for="targetStudy">Optionally, choose a target study folder:</label><br>
                <select id="targetStudy" name="targetStudy">
                    <labkey:options value="<%=text(form.getTargetStudy())%>" map="<%=targetStudies%>"/>
                </select>
            </p>
        <% } %>

        <br />
        <labkey:button text="Import Selected Runs" action="<%=new ActionURL(AnalysisScriptController.ImportRunsAction.class, c)%>"/>
        <labkey:button text="Cancel" href="<%=form.getReturnURLHelper()%>"/>
        </labkey:form><%
    }
    else
    {
        %><labkey:button text="Browse for more runs" href="<%=form.getReturnURLHelper()%>"/><%
    }

%>
