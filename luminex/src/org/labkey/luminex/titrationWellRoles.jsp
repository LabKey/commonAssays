<%
/*
 * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.labkey.luminex.LuminexUploadWizardAction" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>

<%
    JspView<LuminexRunUploadForm> me = (JspView<LuminexRunUploadForm>) HttpView.currentView();
    LuminexRunUploadForm bean = me.getModelBean();
    Map<String, Titration> titrationsWithTypes = bean.getParser().getTitrationsWithTypes();

    // separate the titrations into two groups unknowns and non-unknowns
    Map<String, Titration> unknownTitrations = new TreeMap<String, Titration>();
    Map<String, Titration> nonUnknownTitrations = new TreeMap<String, Titration>();
    for (Map.Entry<String, Titration> titrationEntry : titrationsWithTypes.entrySet())
    {
        if (titrationEntry.getValue().isUnknown())
        {
            unknownTitrations.put(titrationEntry.getKey(), titrationEntry.getValue());
        }
        else
        {
            nonUnknownTitrations.put(titrationEntry.getKey(), titrationEntry.getValue());
        }
    }

    // show a table for the user to select which titrations are Standards and/or QC Controls
    if (nonUnknownTitrations.size() > 0)
    {
%>
        <table>
            <tr>
                <td>&nbsp;</td>
                <td class="labkey-form-label">Standard</td>
                <td class="labkey-form-label">QC Control</td>
            </tr>
<%
        for (Map.Entry<String, Titration> titrationEntry : nonUnknownTitrations.entrySet())
        {
%>
            <tr>
                <td class="labkey-form-label"><%= titrationEntry.getValue().getName() %></td>
                <td>
                    <input type='checkbox' name='<%= PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);showHideAnalytePropertyColumn("<%= PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationColumnCellName(titrationEntry.getValue().getName())) %>", this.checked);'
                           <%= titrationEntry.getValue().isStandard() ? "CHECKED" : "" %> />
                </td>
                <td>
                    <input type='checkbox' name='<%= PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.qccontrol, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);' <%= titrationEntry.getValue().isQcControl() ? "CHECKED" : "" %> />
                </td>
            </tr>
<%
        }
%>
        </table>
<%
    }

    // show a table for the user to select which titrations are Titrated Unknowns
    if (unknownTitrations.size() > 0)
    {
%>
        <br/>
        <table>
            <tr>
                <td>&nbsp;</td>
                <td class="labkey-form-label">Titrated Unknown</td>
            </tr>
<%
        for (Map.Entry<String, Titration> titrationEntry : unknownTitrations.entrySet())
        {
%>
            <tr>
                <td class="labkey-form-label"><%= titrationEntry.getValue().getName() %></td>
                <td>
                    <input type='checkbox' name='<%= PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.unknown, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);' <%= titrationEntry.getValue().isUnknown() ? "CHECKED" : "" %> />
                </td>
            </tr>
<%
        }
%>
        </table>
<%
    }
%>


<script type="text/javascript">
    // function to handle click of titration well role checkbox to set the corresponding hidden form element accordingly
    function titrationRoleChecked(el)
    {
        // get the corresponding hidden form element
        var els = document.getElementsByName(el.name);
        for (var i = 0; i < els.length; i++)
        {
            if (els[i].type == "hidden")
            {
                // set the hidden element value to true if the input checkbox is selected
                els[i].value = el.checked ? "true" : "";
            }
        }
    }

    function showHideAnalytePropertyColumn(titrationCellName, isChecked)
    {
        // show/hide the column associated with this titration
        var elements = Ext.select('*[name=' + titrationCellName + ']').elements;
        for (var i = 0; i < elements.length; i++)
        {
            if(isChecked)
            {
                elements[i].style.display = "table-cell";
            }
            else
            {
                elements[i].style.display = "none";

                // also need to make sure all input checkboxes are unchecked if hiding column cell
                var cellInputs = elements[i].getElementsByTagName("input");
                if (cellInputs.length == 1)
                {
                    cellInputs[0].checked = false;
                }
            }
        }
    }
</script>