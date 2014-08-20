<%
/*
 * Copyright (c) 2010-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<labkey:form method="post">
    <table width="75%">
        <tr>
            <td>This upgrade will inspect all existing MS2 runs in the system. It will load MS scan counts for each fraction.
                This is safe to run multiple times - only newly
                discovered files will be added.
                The upgrade task may be long running but will be
                invoked as a pipeline job; you will be able to monitor progress and view log information from
                the <a href="<%=urlProvider(PipelineUrls.class).urlBegin(ContainerManager.getRoot())%>">pipeline status </a>page.</td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr><td><%= button("Import Scan Counts").submit(true) %></td></tr>
</table>
</labkey:form>

