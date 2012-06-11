<%
/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.scoring.ScoringController" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<ScoringController.ChartDiscForm> me = (JspView<ScoringController.ChartDiscForm>) HttpView.currentView();
    ScoringController.ChartDiscForm form = me.getModelBean();

    StringBuffer params = new StringBuffer();

    Container c = getViewContext().getContainer();
%>
<labkey:errors/>
<%
    if (form.getRun() != null)
    {
%>
<form method=get action="<%=h(buildURL(ScoringController.DiscriminateAction.class))%>">
<input type="hidden" name="runId" value="<%=form.getRunId()%>">
<table>
    <tr><td colspan=2 class="labkey-header-large">
    Enter expressions to use for verious charges.<br>
    </td></tr>
    <tr><td valign="top">
<table>
<tr><td colspan="2" class="labkey-header-large"><%=h(form.getRun().getDescription())%></td>
<tr><td colspan="2">
            <table>
                <tr><td class="labkey-form-label">Title</td>
                    <td><input type="text" size="15" name="title" value="<%=h(form.getTitle())%>"></td></tr>
                <tr><td class="labkey-form-label">Chart width</td>
                    <td><input type="text" size="4" name="width" value="<%=h(form.getWidth())%>"></td></tr>
                <tr><td class="labkey-form-label">Chart height</td>
                    <td><input type="text" size="4" name="height" value="<%=h(form.getHeight())%>"></td></tr>
                <tr><td class="labkey-form-label">Correct Amino Acids</td>
                    <td><input type="text" size="4" name="percentAACorrect" value="<%=h(form.getPercentAACorrect())%>"> %</td></tr>
                <tr><td class="labkey-form-label">Charge 1 expression</td>
                    <td><input type="text" size="15" name="expressions[0]" value="<%=h(form.getExpressions()[0])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 1 bucket</td>
                    <td><input type="text" size="15" name="buckets[0]" value="<%=h(form.getBuckets()[0])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 1 scale</td>
                    <td><input type="text" size="15" name="scaleFactors[0]" value="<%=h(form.getScaleFactors()[0])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 2 expression</td>
                    <td><input type="text" size="15" name="expressions[1]" value="<%=h(form.getExpressions()[1])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 2 bucket</td>
                    <td><input type="text" size="15" name="buckets[1]" value="<%=h(form.getBuckets()[1])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 2 scale</td>
                    <td><input type="text" size="15" name="scaleFactors[1]" value="<%=h(form.getScaleFactors()[1])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 3 expression</td>
                    <td><input type="text" size="15" name="expressions[2]" value="<%=h(form.getExpressions()[2])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 3 bucket</td>
                    <td><input type="text" size="15" name="buckets[2]" value="<%=h(form.getBuckets()[2])%>"></td></tr>
                <tr><td class="labkey-form-label">Charge 3 scale</td>
                    <td><input type="text" size="15" name="scaleFactors[2]" value="<%=h(form.getScaleFactors()[2])%>"></td></tr>
            </table>
</td></tr>
<tr>
    <td colspan="2"><%=generateSubmitButton("Submit")%></td>
</tr>
</table>
</td>

    <td valign="top">
        <img src="<%=ScoringController.urlChartDiscriminateROC(c, form)%>"><br><br>
        <img src="<%=ScoringController.urlChartDiscriminate(c, 1, form)%>"><br><br>
        <img src="<%=ScoringController.urlChartDiscriminate(c, 2, form)%>"><br><br>
        <img src="<%=ScoringController.urlChartDiscriminate(c, 3, form)%>"><br><br>
    </td>

</tr>
</table>
</form>
<%
    }
%>
