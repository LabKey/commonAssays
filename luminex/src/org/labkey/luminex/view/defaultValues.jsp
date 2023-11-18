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
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page import="org.labkey.luminex.LuminexController.DefaultValuesForm" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<DefaultValuesForm> me = (JspView<DefaultValuesForm>) HttpView.currentView();
    DefaultValuesForm bean = me.getModelBean();
    List<String> analytes = bean.getAnalytes();
    List<String> positivityThresholds = bean.getPositivityThresholds();
    List<String> negativeBeads = bean.getNegativeBeads();
%>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
    <%-- cheap trick -- watch out for if this is ever nested in any other code --%>
    <table id="defaultValues" class="lk-default-val">
        <tr>
            <td class="lk-default-val-header">Analyte</td>
            <td class="lk-default-val-header">Positivity Threshold</td>
            <td class="lk-default-val-header">Negative Bead</td>
        </tr>

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

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    const rowCount = 1;
    let table;

    LABKEY.Utils.onReady(function() {
        table = document.getElementById("defaultValues");
        <%  if (!analytes.isEmpty())
            {
                int i; for (i = 0; i < analytes.size(); i++)
                {
        %>
        addRow(<%=q(analytes.get(i))%>, <%=q(positivityThresholds.get(i))%>, <%=q(negativeBeads.get(i))%>);
        <%
                }
            }
            else
            {
        %>
        addRow();
        <%
            }
        %>
    });

    function addRow(analyte, threshold, bead) {
        const row = table.insertRow(table.rows.length - 1);

        const analyteCell = row.insertCell(-1);
        analyteCell.innerHTML = "<input name=\"analytes\" value=\"" + (analyte || "") + "\" size=30>";
        const positivityCell = row.insertCell(-1);
        positivityCell.innerHTML = "<input name=\"positivityThresholds\" value=\"" + (threshold || "") + "\" size=20>";
        const negativeBeads = row.insertCell(-1);
        negativeBeads.innerHTML = "<input name=\"negativeBeads\" value=\"" + (bead || "") + "\" size=30>";
        const deleteRowButton = row.insertCell(-1);
        deleteRowButton.innerHTML = "<a><i class=\"fa fa-close\"></i></a>";
        deleteRowButton['onclick'] = function(event){ deleteRow(event.target.parentElement.parentElement.parentElement); };
    }

    function deleteRow(row) {
        if (table.rows.length > 2)
        {
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