<%
/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.ms2.pipeline.PipelineController" %>
<%@ page extends="org.labkey.ms2.pipeline.ConfigureSequenceDB" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<labkey:form method="POST" name="updateClusterSequenceDB" action="<%=urlFor(PipelineController.SetupClusterSequenceDBAction.class)%>">
    <labkey:errors />
    <table>
        <tr>
            <td>Path on the web server for FASTA files to be used for MS2 searches:</td>
        </tr>
        <tr>
            <td><input type="text" name="localPathRoot" size="80" value="<%= h(getLocalPathRoot()) %>"></td>
        </tr>
        <tr>
            <td><%= button("Save").submit(true) %> <%= button("Cancel").href(urlProvider(PipelineUrls.class).urlSetup(getContainer())) %></td>
        </tr>
    </table>
</labkey:form>
