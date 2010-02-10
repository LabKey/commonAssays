<%
/*
 * Copyright (c) 2009 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>

<form action="upgradeNabAUC.view" method="post">
    <table width="75%">
        <tr class="labkey-wp-header">
            <th colspan=2>NAb Positive AUC Conversion</th>
        </tr>
        <tr><td>&nbsp;</td></tr>
        <tr>
            <td><i>Calculation of Positive NAb area under the curve (AUC) values for all curve fit methods will be performed for
                all existing NAb runs. The upgrade task may be long running but will be
                invoked as a pipeline job, you will be able to monitor progress and view log information from
                the <a href="<%=PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(ContainerManager.getRoot())%>">pipeline status </a>page.</i></td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr><td><input type="submit" value="Upgrade"> </td></tr>
</table>
<input type="hidden" name="upgradeType" value="pauc">
</form>

<form action="upgradeNabAUC.view" method="post">
    <table width="75%">
        <tr class="labkey-wp-header">
            <th colspan=2>NAb AUC Conversion</th>
        </tr>
        <tr><td>&nbsp;</td></tr>
        <tr>
            <td><i>Calculation of NAb area under the curve (AUC) values for all curve fit methods will be performed for
                all existing NAb runs.
                Additionally, IC calculations will be performed for all curve fit methods. The upgrade task may be long
                running but will be
                invoked as a pipeline job, you will be able to monitor progress and view log information from
                the <a href="<%=PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(ContainerManager.getRoot())%>">pipeline status </a>page.</i></td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr><td><input type="submit" value="Upgrade"> </td></tr>
</table>
<input type="hidden" name="upgradeType" value="auc">
</form>

