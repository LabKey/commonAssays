<%
/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
<%@ page extends="org.labkey.api.jsp.JspContext" %>
<input type="hidden" name="column" value="Protein" />
<table>
    <tr>
        <td colspan="2">Please select the columns to include in the comparison:</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="unique" value="1" checked>Unique Peptides</td>
        <td><input type="checkbox" name="total" value="1">Total Peptides</td>
    </tr>
</table>