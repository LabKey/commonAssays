<%
/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.ms2.pipeline.PipelineController"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<form method="post" action="<%=urlFor(PipelineController.AddSequenceDBAction.class)%>" enctype="multipart/form-data">
<labkey:errors />
<table border="0">
    <tr><td class='ms-searchform'>FASTA File:</td>
        <td class='ms-vb'><input size="70" type="file" name="sequenceDBFile"></td></tr>
    <tr><td><labkey:button text="Add"/>&nbsp;<labkey:button text="Cancel" href="<%=urlProvider(PipelineUrls.class).urlReferer(getContainer())%>"/></td></tr>
</table>
</form>
<script for=window event=onload>
try {document.getElementById("sequenceDBFile").focus();} catch(x){}
</script>
