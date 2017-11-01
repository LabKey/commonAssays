<%
    /*
     * Copyright (c) 2014-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexController.DefaultValuesForm" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<DefaultValuesForm> me = (JspView<DefaultValuesForm>) HttpView.currentView();
    DefaultValuesForm bean = me.getModelBean();
    List<String> analytes = bean.getAnalytes();
    List<String> positivityThresholds = bean.getPositivityThresholds();
    List<String> negativeBeads = bean.getNegativeBeads();
%>

<script type="text/javascript">
    LABKEY.requiresCss("fileAddRemoveIcon.css");
</script>

<style type="text/css">
    table.lk-default-val td {
        padding: 0 3px 3px 0;
    }

    table.lk-default-val .lk-default-val-header {
        font-weight: bold;
    }
</style>

<labkey:errors/>

<labkey:form action="<%=getViewContext().getActionURL()%>" method="post">
    <p>Update default values for standard analyte properties.</p>
    <!-- cheap trick -- watch out for if this is ever nested in any other code -->
    <table id="defaultValues" class="lk-default-val">
        <tr>
            <td class="lk-default-val-header">Analyte</td>
            <td class="lk-default-val-header">Positivity Threshold</td>
            <td class="lk-default-val-header">Negative Bead</td>
        </tr>

        <% if (analytes.size() > 0) { %>
            <% int i; for (i=0; i<analytes.size()-1; i++ ) { %>
            <tr id="<%=h(analytes.get(i))%>">
                <td><input name="analytes" value="<%=h(analytes.get(i))%>" size=30></td>
                <td><input name="positivityThresholds" value="<%=h(positivityThresholds.get(i))%>" size=20></td>
                <td><input name="negativeBeads" value="<%=h(negativeBeads.get(i))%>" size=30></td>
                <td><a onclick="deleteRow('<%=h(analytes.get(i))%>')"><i class="fa fa-close"></i></a></td>
            </tr>
            <% } %>
            <%-- Treat last row as special case  (and yes I hate the copy pasta here too) --%>
            <tr id="<%=h(analytes.get(i))%>">
                <td><input name="analytes" value="<%=h(analytes.get(i))%>" size=30></td>
                <td><input name="positivityThresholds" value="<%=h(positivityThresholds.get(i))%>" size=20></td>
                <td><input name="negativeBeads" value="<%=h(negativeBeads.get(i))%>" size=30></td>
                <td><a onclick="deleteRow('<%=h(analytes.get(i))%>')"><i class="fa fa-close"></i></a></td>
            </tr>
        <% } else { %>
            <tr id="InsertRow0">
                <td><input name="analytes" value="" size=30></td>
                <td><input name="positivityThresholds" value="" size=20></td>
                <td><input name="negativeBeads" value="" size=30></td>
                <td><a onclick="deleteRow('InsertRow0')"><i class="fa fa-close"></i></a></td>
            </tr>
        <% } %>
        <tr>
            <td colspan="2">
                <%= button("Add Row").onClick("addRow();")%>
                <%= button("Import Data").href(new ActionURL(LuminexController.ImportDefaultValuesAction.class, getContainer()).addParameter("rowId", bean.getProtocol().getRowId()).addReturnURL(getViewContext().getActionURL()))%>
                <%= button("Export TSV").href(new ActionURL(LuminexController.ExportDefaultValuesAction.class, getContainer()).addParameter("rowId", bean.getProtocol().getRowId()))%>
            </td>
            <td align="right">
                <%= button("Cancel").href(bean.getReturnURLHelper()) %>
                <%= button("Save Defaults").submit(true) %>
            </td>
            <td>&nbsp;</td>
        </tr>
    </table>
</labkey:form>

<script type="text/javascript">
    var rowCount = 1;
    var table = document.getElementById("defaultValues");

    function addRow() {
        var row = table.insertRow(table.rows.length - 1);
        var rowId = "InsertRow"+rowCount;
        row.id = rowId;

        var analyteCell = row.insertCell(-1);
        analyteCell.innerHTML = "<input name=\"analytes\" value=\"\" size=30>";
        var positivityCell = row.insertCell(-1);
        positivityCell.innerHTML = "<input name=\"positivityThresholds\" value=\"\" size=20>";
        var negativeBeads = row.insertCell(-1);
        negativeBeads.innerHTML = "<input name=\"negativeBeads\" value=\"\" size=30>";
        var deleteRowButton = row.insertCell(-1);
        deleteRowButton.innerHTML = "<a onclick=deleteRow('" + rowId + "')><i class=\"fa fa-close\"></i></a>";
    }

    function deleteRow(rowId) {
        if (table.rows.length > 2)
        {
            // http://stackoverflow.com/questions/4967223/javascript-delete-a-row-from-a-table-by-id
            var row = document.getElementById(rowId);
            row.parentNode.removeChild(row);
        }
    }

    function getLastRow()
    {
        return table.rows[ table.rows.length - 1 ];
    }

    function getLastCell(row)
    {
        return row.cells[ row.cells.length - 1];
    }
</script>