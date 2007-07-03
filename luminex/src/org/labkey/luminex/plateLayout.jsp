<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexRun" %>
<%@ page import="org.labkey.luminex.SpecimenInfo" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="org.labkey.api.study.Well" %>
<%@ page import="org.labkey.luminex.LuminexWell" %>
<%@ page import="org.labkey.luminex.LuminexColorGenerator" %>
<%
    JspView<LuminexRun> me = (JspView<LuminexRun>) HttpView.currentView();
    List<SpecimenInfo> specimens = me.getModelBean().getSpecimenInfos();
%>
&nbsp;<br/>
<table cellpadding="6" cellspacing="0" style="border-bottom: black solid 1px; border-left: black solid 1px;">
    <tr>
        <td style="border-top: black solid 1px; border-right: black solid 1px;">&nbsp;</td>
        <% for (int i = 1; i <= 12; i++) { %>
            <td style="border-top: black solid 1px; border-right: black solid 1px; text-align: center;"><strong><%= i %></strong></td>
        <% } %>
    </tr>
    <%
        for (char c = 'A'; c <= 'H'; c++)
        { %>
        <tr>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong><%= c %></strong></td>
            <% for (int i = 1; i <= 12; i++) {
                String bgColor = "#FFFFFF";
                Well w = new LuminexWell(c - 'A' + 1, i);
                for (SpecimenInfo specimen : specimens) {
                    if (specimen.getWells().contains(w)) {
                        bgColor = specimen.getColorAsString();
                    }
                } %>
                <td style="border-top: black solid 1px; border-right: black solid 1px; background-color: <%= bgColor %>;"><%
                String separator = "";
                    boolean foundSpecimen = false;
                for (SpecimenInfo specimen : specimens) {
                    if (specimen.getWells().contains(w)) { %>
                        <%= separator %><a href="#<%= specimen.getName() %>"><%= specimen.getName() %></a><%
                        separator = "<br/>";
                        foundSpecimen = true;
                    }
                }
                if (!foundSpecimen) { %>&nbsp;<% } %>
                </td>
            <% } %>
        </tr>
    <% } %>
</table>
&nbsp;<br/>
&nbsp;<br/>