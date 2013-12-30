<%@ page import="org.labkey.api.view.ViewContext" %>
<%
/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
<%@ include file="showSensitivityDetails.jsp" %>

<table>
<%
    ViewContext context = getViewContext();
    ActionURL dist = context.cloneActionURL().setAction(MS2Controller.ShowPeptideProphetDistributionPlotAction.class);
    ActionURL distCumulative = dist.clone().addParameter("cumulative", "1");

    ActionURL versus = context.cloneActionURL().setAction(MS2Controller.ShowPeptideProphetObservedVsModelPlotAction.class);
    ActionURL versusCumulative = versus.clone().addParameter("cumulative", "1");

    ActionURL versusPP = context.cloneActionURL().setAction(MS2Controller.ShowPeptideProphetObservedVsPPScorePlotAction.class);

    for (int i=1; i<4; i++)
    {
        String charge = Integer.toString(i);
%>
<tr>
    <td><img src="<%=dist.replaceParameter("charge", charge).getEncodedLocalURIString()%>" alt="Charge <%=charge%>+ Distribution"></td>
    <td><img src="<%=distCumulative.replaceParameter("charge", charge).getEncodedLocalURIString()%>" alt="Charge <%=charge%>+ Cumulative Distribution"></td>
</tr>
<tr>
    <td><img src="<%=versus.replaceParameter("charge", charge).getEncodedLocalURIString()%>" alt="Charge <%=charge%>+ Observed vs. Model"></td>
    <td><img src="<%=versusCumulative.replaceParameter("charge", charge).getEncodedLocalURIString()%>" alt="Charge <%=charge%>+ Cumulative Observed vs. Model"></td>
</tr>
<% if (bean.run.getNegativeHitCount() > bean.run.getPeptideCount() / 3) { %>
<tr>
    <td><img src="<%=versusPP.replaceParameter("charge", charge).getEncodedLocalURIString()%>" alt="Charge <%=charge%>+ Observed vs. Prophet"></td>
</tr>
<% } %>
<%   }  %>
</table>
