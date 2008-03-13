<%@ include file="showSensitivityDetails.jsp" %>

<table>
<%
    ActionURL dist = me.cloneActionURL().setAction("showPeptideProphetDistributionPlot");
    ActionURL distCumulative = dist.clone().addParameter("cumulative", "1");

    ActionURL versus = me.cloneActionURL().setAction("showPeptideProphetObservedVsModelPlot");
    ActionURL versusCumulative = versus.clone().addParameter("cumulative", "1");

    ActionURL versusPP = me.cloneActionURL().setAction("showPeptideProphetObservedVsPPScorePlot");

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
