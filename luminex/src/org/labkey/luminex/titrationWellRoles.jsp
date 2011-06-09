<%
/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexRunUploadForm" %>
<%@ page import="org.labkey.luminex.Titration" %>
<%@ page import="java.util.Map" %>

<%
    JspView<LuminexRunUploadForm> me = (JspView<LuminexRunUploadForm>) HttpView.currentView();
    LuminexRunUploadForm bean = me.getModelBean();
    java.util.Map<String, Titration> titrationsWithTypes = bean.getParser().getTitrationsWithTypes();
%>

<table>
    <tr>
        <td>&nbsp;</td>
        <td class="labkey-form-label">Standard</td>
        <td class="labkey-form-label">QC Control</td>
        <td class="labkey-form-label">Titrated Unknown</td>
    </tr>

    <%
    for (Map.Entry<String, Titration> titrationEntry : titrationsWithTypes.entrySet())
    {
    %>
        <tr>
            <td class="labkey-form-label"><%= titrationEntry.getValue().getName() %></td>
            <td>
                <input type='checkbox' name='_role_standard_<%= titrationEntry.getValue().getName() %>'
                       value='1' onChange='showHideAnalytePropertyColumn("<%= titrationEntry.getValue().getName() %>", this.checked);' 
                       <%= titrationEntry.getValue().isStandard() ? "CHECKED" : "" %> />
            </td>
            <td>
                <input type='checkbox' name='_role_qccontrol_<%= titrationEntry.getValue().getName() %>'
                       value='1' <%= titrationEntry.getValue().isQcControl() ? "CHECKED" : "" %> />
            </td>
            <td>
                <input type='checkbox' name='_role_unknown_<%= titrationEntry.getValue().getName() %>'
                       value='1' <%= titrationEntry.getValue().isUnknown() ? "CHECKED" : "" %> />                
            </td>
        </tr>
    <%
    }
    %>
</table>

<script type="text/javascript">
    function showHideAnalytePropertyColumn(titrationName, isChecked)
    {
        // show/hide the column caption
        var captionCell = document.getElementById("_caption_" + titrationName);
        captionCell.style.display = (isChecked ? "" : "none");

        // show/hide the individual input cells for the column
        var inputCells = document.getElementsByName("_inputcell_" + titrationName);
        for (var i = 0; i < inputCells.length; i++)
        {
            inputCells[i].style.display = (isChecked ? "" : "none");

            // if hiding, also uncheck all input checkboxes
            if (!isChecked)
            {
                var cellInputs = inputCells[i].getElementsByTagName("input");
                if (cellInputs.length == 1)
                {
                    cellInputs[0].checked = false;
                }
            }
        }
    }
</script>