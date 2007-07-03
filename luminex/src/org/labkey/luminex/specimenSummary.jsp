<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexRun" %>
<%@ page import="org.labkey.luminex.SpecimenInfo" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="org.labkey.luminex.LuminexDataRow" %>
<%
    JspView<LuminexRun> me = (JspView<LuminexRun>) HttpView.currentView();
    List<SpecimenInfo> specimens = me.getModelBean().getNonControlSpecimenInfos();
    NumberFormat format = new DecimalFormat("0.000");
%>
&nbsp;<br/>

<img src="showSpecimenPlot.view?fileName=<%= me.getModelBean().getFileName() %>" alt="Specimen Chart" />

&nbsp;<br/>

<img src="showAnalytePlot.view?fileName=<%= me.getModelBean().getFileName() %>" alt="Analyte Chart" />

&nbsp;<br/>
&nbsp;<br/>

<table cellpadding="2" cellspacing="0" style="border-bottom: black solid 1px; border-left: black solid 1px;">
    <% if (!specimens.isEmpty())
    { %>
    <tr><td style="border-top: black solid 1px; border-right: black solid 1px;">&nbsp;</td>
        <%
            for (LuminexDataRow value : specimens.get(0).getValues())
            {
        %>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong><%= value.getAnalyteInfo().getAnalyteName() %></strong></td>
        <%
        }
    } %>
    </tr>
    <% for (SpecimenInfo specimenInfo : specimens)
    { %>
        <tr>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><a name="<%= specimenInfo.getName() %>"><strong><%= specimenInfo.getName() %></strong></a></td>
            <% for (LuminexDataRow value : specimenInfo.getValues()) { %>
                <td style="border-top: black solid 1px; border-right: black solid 1px; text-align: right;">
                    <% if (Double.isNaN(value.getConcInRange())) { %>---<% } else { %><%= format.format(value.getConcInRange()) %><% } %></td>
            <% } %>
        </tr>
    <% } %>
</table>
&nbsp;<br/>
&nbsp;<br/>
