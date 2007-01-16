<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page extends="org.labkey.flow.controllers.log.LogController.Page" %>
<table class="normal" border="1"><tr>
    <%  List<String[]> rows = new ArrayList();
        String[] headers = log.getHeadersAndEntries(rows);
        for (String header : headers)
        { %>
    <th><%=h(header)%></th>
    <%
        }
    %>
</tr>
    <% for (String[] row : rows)
    { %>
    <tr><% for (int i = 0; i < row.length; i ++)
    { %>
        <td><%=formatValue(i, headers, row)%></td>
        <% } %>
    </tr>
    <% } %>
</table>



