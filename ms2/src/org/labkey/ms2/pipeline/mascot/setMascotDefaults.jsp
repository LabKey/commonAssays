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
<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.ms2.pipeline.PipelineController"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
PipelineController.SetDefaultsForm form = (PipelineController.SetDefaultsForm) getForm();
%>
<form method="post" action="<%=urlFor(PipelineController.SetMascotDefaultsAction.class)%>">
<labkey:errors />
<table border="0">
    <tr><td class='ms-searchform'>Mascot<br>Default XML:</td>
<!--
need to change this to to use a generic default
it need not be a X! Tandem default
-->
        <td class='ms-vb'><textarea name="configureXml" cols="90" rows="20"><%=form.getConfigureXml()%></textarea><br>
                    For detailed explanations of all available input parameters, see the
                    <a href="http://www.matrixscience.com/help_index.html" target="_api">Mascot API Documentation</a> on-line.</td></tr>
    <tr><td colspan="2"><labkey:button text="Set Defaults"/>&nbsp;<labkey:button text="Cancel" href="<%=urlProvider(PipelineUrls.class).urlReferer(getContainer())%>"/></td></tr>
</table>
</form>
<script for=window event=onload>
try {document.getElementById("analysisName").focus();} catch(x){}
</script>
