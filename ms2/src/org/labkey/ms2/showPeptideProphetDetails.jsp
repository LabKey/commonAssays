<%@ page import="org.labkey.api.view.ViewURLHelper"%>
<%@ page import="org.labkey.ms2.MS2Run"%>

<%@ include file="showSensitivityDetails.jsp" %>

<table>
<%
    MS2Run run = (MS2Run)me.get("run");

    ViewURLHelper dist = me.cloneViewURLHelper().setAction("showPeptideProphetDistributionPlot");
    ViewURLHelper distCumulative = (ViewURLHelper)dist.clone().addParameter("cumulative", "1");

    ViewURLHelper versus = me.cloneViewURLHelper().setAction("showPeptideProphetObservedVsModelPlot");
    ViewURLHelper versusCumulative = (ViewURLHelper)versus.clone().addParameter("cumulative", "1");

    ViewURLHelper versusPP = me.cloneViewURLHelper().setAction("showPeptideProphetObservedVsPPScorePlot");

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
<% if (run.getNegativeHitCount() > run.getPeptideCount() / 3) { %>
<tr>
    <td><img src="<%=versusPP.replaceParameter("charge", charge).getEncodedLocalURIString()%>" alt="Charge <%=charge%>+ Observed vs. Prophet"></td>
</tr>
<% } %>
<%   }  %>
</table>
