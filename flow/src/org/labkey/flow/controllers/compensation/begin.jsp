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
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController.Action"%>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="java.util.List"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<% List<FlowCompensationMatrix> matrices = FlowCompensationMatrix.getUploadedCompensationMatrices(getContainer()); %>
<% if (matrices.size() == 0) { %>
<p>No compensation matrices have been uploaded to this folder.</p>
<% } else { %>
<table class="normal">
    <tr><th>Name</th><th>Created</th></tr>
    <% for (FlowCompensationMatrix matrix : matrices) { %>
        <tr><td><a href="<%=h(matrix.urlShow())%>"><%=matrix.getName()%></a></td><td><%=formatDateTime(matrix.getExpObject().getCreated())%></td></tr>
    <% } %>
</table>
<% } %>
<labkey:link href="<%=PageFlowUtil.urlFor(Action.upload, getContainer())%>" text="Upload a new compensation matrix" />

