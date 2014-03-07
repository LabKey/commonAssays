<%
/*
 * Copyright (c) 2005-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Run" %>
<%@ page import="org.labkey.ms2.scoring.ScoringController" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<ScoringController.ChartForm> me = (JspView<ScoringController.ChartForm>) HttpView.currentView();
    ScoringController.ChartForm form = me.getModelBean();
    
    StringBuffer params = new StringBuffer();

    MS2Run[] runs = form.getRuns();
%>
<form method=get action="<%=h(buildURL(ScoringController.CompareAction.class))%>">
<labkey:errors/>
<% if (runs.length > 0)
{ %>
<table>
    <tr><td colspan=2 class="labkey-header-large">
    Choose discriminate values to display in ROC chart.<br>
    </td></tr>
    <tr><td valign="top">
<table>
<%
    int validRuns = 0;
    for (int i = 0; i < runs.length; i++)
    {
        MS2Run run = runs[i];
        if (run == null)
            continue;
%>
    <tr><td colspan="2" class="labkey-header-large"><%=h(run.getDescription())%></td></tr>
    <tr><td>&nbsp;</td>
<%      if (run.getNegativeHitCount() < run.getPeptideCount() / 3)
        {
            %><td class="labkey-error">Insufficient negative hit data to perform analysis.</td><%
        }
        else
        {
            %><td>
                <input type="hidden" name="runIds_<%=i%>" value="<%=run.getRun()%>"><%

            if (validRuns > 0)
                params.append('&');
            params.append("runIds_").append(i).append("=").append(run.getRun());
            validRuns++;

            String s = run.getDiscriminateExpressions();
            String[] discriminates = s.split("\\s*,\\s*");
            int discIndex = 0;
            for (String discriminate : discriminates)
            {
                if (discriminate.startsWith("-"))
                    discriminate = discriminate.substring(1);

                %><input type="checkbox"<%=checked(form.getDiscriminates()[i][discIndex])%> name="discriminates_<%=i%>_<%=discIndex%>"> <%=h(discriminate)%><br><%
                discIndex++;
            }
            %></td><%
        }
    %></tr><%
    }

    if (validRuns > 0)
    { %>
        <tr><td>&nbsp;</td></tr>
        <tr><td colspan=2>
            <table>
                <tr><td class="labkey-form-label">Title</td>
                    <td><input type="text" size="15" name="title" value="<%=form.getTitle()%>"></td></tr>
                <tr><td class="labkey-form-label">Correct Amino Acids</td>
                    <td><input type="text" size="4" name="percentAACorrect" value="<%=form.getPercentAACorrect()%>"> %</td></tr>
                <tr><td class="labkey-form-label">Increment</td>
                    <td><input type="text" size="4" name="increment" value="<%=form.getIncrement()%>"></td></tr>
                <tr><td class="labkey-form-label">Limit</td>
                    <td><input type="text" size="4" name="limit" value="<%=form.getLimit()%>"></td></tr>
                <tr><td class="labkey-form-label">Chart width</td>
                    <td><input type="text" size="4" name="width" value="<%=form.getWidth()%>"></td></tr>
                <tr><td class="labkey-form-label">Chart height</td>
                    <td><input type="text" size="4" name="height" value="<%=form.getHeight()%>"></td></tr>
                <tr><td class="labkey-form-label">Marks</td>
                    <td><input type="text" size="15" name="marks" value="<%=form.getMarks()%>"></td></tr>
                <tr><td class="labkey-form-label">Mark FDR</td>
                    <td><input type="checkbox" name="markFdr"<%=checked(form.isMarkFdr())%>"></td></tr>
                <tr><td class="labkey-form-label">Save TSVs</td>
                    <td><input type="checkbox" name="saveTsvs"<%=checked(form.isSaveTsvs())%>"></td></tr>
            </table>
        </td></tr>
        <tr>
            <td colspan="2"><%= button("Submit").submit(true) %></td>
        </tr>
<%
        params.append("&size=").append(validRuns);
    } %>
    <input type="hidden" name="size" value="<%=validRuns%>">
</table>
</td>
<%
    params.append("&title=").append(u(form.getTitle()))
        .append("&increment=").append(u(Double.toString(form.getIncrement())))
        .append("&limit=").append(u(Integer.toString(form.getLimit())))
        .append("&width=").append(u(Integer.toString(form.getWidth())))
        .append("&height=").append(u(Integer.toString(form.getHeight())))
        .append("&marks=").append(u(form.getMarks()));

    if (form.isMarkFdr())
        params.append("&markFdr=true");
    if (form.isSaveTsvs())
        params.append("&saveTsvs=true");

    boolean hasData = false;
    int d1 = 0;
    boolean[][] discs = form.getDiscriminates();
    while(d1 < discs.length)
    {
        int d2 = 0;
        while (d2 < discs[d1].length)
        {
            if (discs[d1][d2])
            {
                params.append("&discriminates_").append(d1).append("_").append(d2).append("=on");
                hasData = true;
            }

            d2++;
        }

        d1++;
    }

    if (hasData)
    {    %>
    <td valign="top">
        <img src="chartCompare.view?<%=params%>"><br><br>
        <img src="chartCompareProt.view?<%=params%>">
    </td>
<%  } %>

</tr>
</table>
<% } %>
</form>
