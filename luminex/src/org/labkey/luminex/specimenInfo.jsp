<%@ page import="org.labkey.luminex.AnalyteInfo" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.luminex.LuminexDataRow" %>
<%@ page import="org.labkey.luminex.SpecimenInfo" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.NumberFormat" %>

<%
    JspView<SpecimenInfo> me = (JspView<SpecimenInfo>) HttpView.currentView();
    SpecimenInfo specimenInfo = me.getModelBean();
    NumberFormat format = new DecimalFormat("0.000");
%>
&nbsp;<br/>
<a name="<%= specimenInfo.getName() %>">
<table cellpadding="2" cellspacing="0" style="border-bottom: black solid 1px; border-left: black solid 1px;">
    <tr>
        <%
            for (LuminexDataRow value : specimenInfo.getValues())
            { %>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong><%= value.getAnalyteInfo().getAnalyteName() %></strong></td>
        <% } %>
    </tr>
    <tr><%
        for (LuminexDataRow value : specimenInfo.getValues())
        { %>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><%= format.format(value.getConcInRange()) %>&nbsp;</td>
        <% } %>
    </tr>
</table></a>
&nbsp;<br/>
&nbsp;<br/>

