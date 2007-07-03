<%@ page import="org.labkey.luminex.AnalyteInfo" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.luminex.LuminexDataRow" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.text.DecimalFormat" %>

<%
    JspView<AnalyteInfo> me = (JspView<AnalyteInfo>) HttpView.currentView();
    AnalyteInfo analyteInfo = me.getModelBean();
    NumberFormat format = new DecimalFormat("0.000");

%>
&nbsp;<br/>
<a name="<%= analyteInfo.getAnalyteName() %>">
<table cellpadding="2" cellspacing="0" style="border-bottom: black solid 1px; border-left: black solid 1px;">
    <tr>
        <%
        for (String header : analyteInfo.getValueDescriptions())
        { %>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong><%= header %></strong></td>
        <% } %>
    </tr>
    <% for (LuminexDataRow dataRow : analyteInfo.getDataRows())
    { %>
    <tr>
        <% for (String value : dataRow.getValues().values()) { %>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><%= value %>&nbsp;</td>
        <% } %>
    </tr>
    <% } %>
</table></a>
&nbsp;<br/>
&nbsp;<br/>

