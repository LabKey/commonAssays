<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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



