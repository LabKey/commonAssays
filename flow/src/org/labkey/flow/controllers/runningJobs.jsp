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
<%@ page import="org.labkey.api.pipeline.PipelineJob"%>
<%@ page import="org.labkey.api.pipeline.PipelineJobData"%>
<%@ page import="org.labkey.api.pipeline.PipelineService"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.flow.script.FlowJob"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    PipelineJobData data = PipelineService.get().getPipelineQueue().getJobData(null);
    List<FlowJob> jobs = new ArrayList();
    for (PipelineJob job : data.getPendingJobs())
    {
        if (job instanceof FlowJob)
            jobs.add((FlowJob) job);
    }
    for (PipelineJob job : data.getRunningJobs())
    {
        if (job instanceof FlowJob)
            jobs.add((FlowJob) job);
    }
%>
<% if (jobs.size() == 0) { %>
<p>There are no running or pending flow jobs.</p>
<% } else {
%>
<table>
    <tr><th>Status</th><th>Description</th><th>Folder</th><th>Owner</th></tr>
<% for (FlowJob job : jobs) {
%>
    <tr><td><a href="<%=h(job.urlStatus())%>"><%=h(job.getStatusText())%></a></td>
        <td><%=h(job.getDescription())%></td>
        <td><a href="<%=h(new ActionURL("Project", "begin", job.getContainer()))%>"><%=h(job.getContainer().getPath())%></a></td>
        <td><%=h(String.valueOf(job.getUser()))%></td><td><labkey:button text="Cancel" href="<%=job.urlCancel()%>"/></tr>
<% } %>
</table>
<% } %>
<labkey:link href="<%=h(new ActionURL("Pipeline-Status", "showList", getContainer()))%>" text="all jobs in this folder (including completed ones)"/>

