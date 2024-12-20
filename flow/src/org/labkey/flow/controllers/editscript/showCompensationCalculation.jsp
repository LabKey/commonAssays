<%
/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
<%@ page import="org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors/>
<%  CompensationCalculationDef calc = compensationCalculationDef();
    if (calc != null)
    {
%>
<table class="labkey-data-region-legacy labkey-show-borders">
    <tr>
        <th rowspan="2">Channel</th>
        <th colspan="3">Positive</th>
        <th colspan="3">Negative</th>
    </tr>
    <tr><th>Keyword</th><th>Value</th><th>Subset</th><th>Keyword</th><th>Value</th><th>Subset</th></tr>
    <%
        for (int i = 0; i < form.parameters.length; i++)
        {
            String channel = form.parameters[i];
            if (channel == null)
                continue;
    %>
    <tr class="<%=getShadeRowClass(i)%>">
        <td><%=h(channel)%></td>
        <td><%=h(form.positiveKeywordName[i])%></td>
        <td><%=h(form.positiveKeywordValue[i])%></td>
        <td><%=h(form.positiveSubset[i])%></td>
        <td><%=h(form.negativeKeywordName[i])%></td>
        <td><%=h(form.negativeKeywordValue[i])%></td>
        <td><%=h(form.negativeSubset[i])%></td>
    </tr>
    <%
        }
    %>
</table>
<% } %>
<% if (form.canEdit()) { %>
    <br/>
    <p>
        This compensation calculation may be edited in a number of ways:<br>
        <%=link("Upload a FlowJo workspace").href(form.urlFor(ScriptController.UploadCompensationCalculationAction.class))%><br>
        <%=link("Switch keywords or gates").href(form.urlFor(ScriptController.ChooseCompensationRunAction.class))%><br>
        <%=link("Rename gates").href(form.getFlowScript().urlFor(ScriptController.EditGateTreeAction.class, FlowProtocolStep.calculateCompensation))%><br>
        <%=link("Script main page").href(form.urlFor(AnalysisScriptController.BeginAction.class))%>
    </p>
<% } %>
