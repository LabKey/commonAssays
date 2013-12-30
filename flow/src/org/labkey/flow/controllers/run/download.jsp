<%
/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.run.DownloadRunBean" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="java.io.File" %>
<%@ page extends="org.labkey.api.jsp.FormPage"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<%
    DownloadRunBean model = (DownloadRunBean)this.getModelBean();
%>
<labkey:errors/>
<%
    if (model.missing.size() > 0)
    {
        Container container = getContainer();
        PipelineService pipeService = PipelineService.get();
        PipeRoot pipeRoot = pipeService.findPipelineRoot(container);
        %>
        <p>
        <b>The following FCS files from the run are missing or are not readable:</b>
        <ul>
        <%
            for (File file : model.missing)
            {
                String path = pipeRoot.relativePath(file);
        %>
            <li><%=h(path != null ? path : file.getName())%></li>
        <%
            }
        %>
        </ul>
        <br>
        <labkey:link href="<%=model.run.urlFor(RunController.DownloadAction.class).addParameter(\"skipMissing\", true)%>" text="Download FCS Files anyway?" />
        </p>

        <p>
        FCS files that will be included in the zip archive:<br>
        <ul>
        <%
            for (File file : model.files.values())
            {
                String path = pipeRoot.relativePath(file);
        %>
            <li><%=h(path != null ? path : file.getName())%></li>
        <%
            }
        %>
        </ul>
        </p>
        <%
    }
%>
