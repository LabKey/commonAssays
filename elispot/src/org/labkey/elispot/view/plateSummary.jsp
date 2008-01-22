<%@ page import="org.labkey.api.data.ColumnInfo"%>
<%@ page import="org.labkey.api.data.TableInfo"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.elispot.ElispotRunUploadForm" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ page import="org.labkey.api.study.Well" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ElispotRunUploadForm> me = (JspView<ElispotRunUploadForm>)HttpView.currentView();
    ElispotRunUploadForm bean = me.getModelBean();
    Plate plate = (Plate)request.getAttribute("plate");

%>

<table border=0 cellspacing=2 cellpadding=2>
<%
    // column header
    out.print("<tr>");
    out.print("<td><div style=\"width:35px; height:25px; text-align:center;\"></div></td>");
    for (int col=0; col < plate.getColumns(); col++)
    {
        out.print("<td><div style=\"width:35px; height:25px; text-align:center;\">" + (col + 1) + "</div></td>");
    }
    out.print("</tr>");

    char rowLabel = 'A';
    for (int row=0; row < plate.getRows(); row++){
%>
    <tr>
    <%
        out.println("<td><div style=\"width:35px; height:25px; text-align:center;\">" + rowLabel++ + "</div></td>");
        for (int col=0; col < plate.getColumns(); col++)
        {
            Well well = plate.getWell(row, col);
    %>
        <td><div style="border:1px solid gray; width:35px; height:25px; text-align:center; background-color:beige;"><%=plate.getWell(row, col).getValue()%></div></td>
    <%
        }
    %>
    </tr>
<%
    }
%>
</table>

