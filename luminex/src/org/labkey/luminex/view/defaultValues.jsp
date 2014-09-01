<%
    /*
     * Copyright (c) 2014 LabKey Corporation
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

<labkey:errors/>

<labkey:form action="<%=getViewContext().getActionURL()%>" method="post">
    <p>Update default values for standard analyte properties.</p>
    <!-- cheap trick -- watch out for if this is ever nested in any other code -->
    <table id="defaultValues">
        <tr>
            <th><div class="labkey-form-label">Analyte</div></th>
            <th><div class="labkey-form-label">Positivity Threshold</div></th>
            <th><div class="labkey-form-label">Negative Bead</div></th>
        </tr>

        <% if (analytes.size() > 0) { %>
            <% for (int i=0; i<analytes.size(); i++ ) { %>
            <tr id="<%=h(analytes.get(i))%>">
                <td><input name="analytes" value="<%=h(analytes.get(i))%>" size=30></td>
                <td><input name="positivityThresholds" value="<%=h(positivityThresholds.get(i))%>" size=20></td>
                <td><input name="negativeBeads" value="<%=h(negativeBeads.get(i))%>" size=30></td>
                <td><a class='labkey-file-remove-icon labkey-file-remove-icon-enabled' onclick="deleteRow('<%=h(analytes.get(i))%>')"><span style="display: inline-block">&nbsp;</span></a></td>
            </tr>
            <% } %>
        <% } else { %>
            <tr id="InsertRow0">
                <td><input name="analytes" value="" size=30></td>
                <td><input name="positivityThresholds" value="" size=20></td>
                <td><input name="negativeBeads" value="" size=30></td>
                <td><a class='labkey-file-remove-icon labkey-file-remove-icon-enabled' onclick="deleteRow('insertRow0')"><span style="display: inline-block">&nbsp;</span></a></td>
            </tr>
        <% } %>
    </table>
    <br>
    <table>
        <tr>
            <td><%= button("Save Defaults").submit(true) %></td>
            <td><%= button("Cancel").href(bean.getReturnURLHelper()) %></td>
            <td><%= button("Import Data").href(new ActionURL(LuminexController.ImportDefaultValuesAction.class, getContainer()).addParameter("rowId", bean.getProtocol().getRowId()).addReturnURL(getViewContext().getActionURL()))%></td>
            <td><%= button("Export TSV").href(new ActionURL(LuminexController.ExportDefaultValuesAction.class, getContainer()).addParameter("rowId", bean.getProtocol().getRowId()))%></td>
            <td><%= button("Add Row").onClick("addRow()")%></td>
        </tr>
    </table>
</labkey:form>

<script type="text/javascript">
    rowCount = 1;
    function addRow() {
        var table = document.getElementById("defaultValues");
        var row = table.insertRow(-1);
        var rowId = "InsertRow"+rowCount;
        row.id = rowId;

        var analyteCell = row.insertCell(-1);
        analyteCell.innerHTML = "<input name=\"analytes\" value=\"\" size=30>";
        var positivityCell = row.insertCell(-1);
        positivityCell.innerHTML = "<input name=\"positivityThresholds\" value=\"\" size=20>";
        var negativeBeads = row.insertCell(-1);
        negativeBeads.innerHTML = "<input name=\"negativeBeads\" value=\"\" size=30>"
        var deleteRowButton = row.insertCell(-1);
        deleteRowButton.innerHTML = "<a class='labkey-file-remove-icon labkey-file-remove-icon-enabled' onclick=deleteRow('" + rowId + "')><span style=\"display: inline-block\">&nbsp;</span></a>"
    }

    function deleteRow(rowId) {
        // http://stackoverflow.com/questions/4967223/javascript-delete-a-row-from-a-table-by-id
        var row = document.getElementById(rowId);
        row.parentNode.removeChild(row);
    }
</script>