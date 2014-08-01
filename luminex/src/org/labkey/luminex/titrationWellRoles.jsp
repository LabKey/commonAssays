<%
/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexRunUploadForm" %>
<%@ page import="org.labkey.luminex.LuminexUploadWizardAction" %>
<%@ page import="org.labkey.luminex.Titration" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.TreeMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<LuminexRunUploadForm> me = (JspView<LuminexRunUploadForm>) HttpView.currentView();
    LuminexRunUploadForm bean = me.getModelBean();
    Map<String, Titration> titrationsWithTypes = bean.getParser().getTitrationsWithTypes();

    // separate the titrations into two groups unknowns and non-unknowns
    Map<String, Titration> unknownTitrations = new TreeMap<>();
    Map<String, Titration> nonUnknownTitrations = new TreeMap<>();
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

    // retrieve a set of all SinglePointControls
    Set<String> trackedSinglePointControls = bean.getParser().getSinglePointControls();

    // show a table for the user to select which titrations are Standards and/or QC Controls
    if (nonUnknownTitrations.size() > 0)
    {
%>
        <table>
            <tr>
                <td>&nbsp;</td>
                <td class="labkey-form-label">Standard</td>
                <td class="labkey-form-label">QC Control</td>
                <td class="labkey-form-label">Other Control<%= PageFlowUtil.helpPopup("Other Control", "AUC and EC50 values are valculated for 'Other Control' titrations but they are not added to Levey-Jennings tracking plots")%></td>
            </tr>
<%
        for (Map.Entry<String, Titration> titrationEntry : nonUnknownTitrations.entrySet())
        {
%>
            <tr>
                <td class="labkey-form-label"><%= h(titrationEntry.getValue().getName()) %></td>
                <td>
                    <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);showHideAnalytePropertyColumn();' />
                </td>
                <td>
                    <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.qccontrol, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);' />
                </td>
                <td>
                    <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.othercontrol, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);' />
                </td>
            </tr>
<%
        }
%>
        </table>
<%
    }

%>
    <table><tr> <!-- Show a table containing both Titrated Unknowns and Tracked single point controls -->
<%

    // show a table for the user to select which titrations are Titrated Unknowns
    if (unknownTitrations.size() > 0)
    {
%>
        <td valign="top">
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
                <td class="labkey-form-label"><%= h(titrationEntry.getValue().getName()) %></td>
                <td>
                    <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.unknown, titrationEntry.getValue())) %>'
                           value='1' onClick='titrationRoleChecked(this);' />
                </td>
            </tr>
<%
        }
%>
        </table>
        </td>
<%
    }
    if (trackedSinglePointControls.size() >0)
    {
%>
        <td valign="top">
            <table>
                <tr>
                <td>&nbsp;</td>
                <td class="labkey-form-label">Tracked Single Point Controls</td>
                </tr>
<%
                    for (String trackedSinglePointControl : trackedSinglePointControls)
                    {
%>
                <tr>
                    <td class="labkey-form-label"><%= h(trackedSinglePointControl) %></td>
                    <td>
                        <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getSinglePointControlCheckboxName(trackedSinglePointControl)) %>'
                               value='1' onClick='titrationRoleChecked(this);' />
                    </td>
                </tr>
<%
                    }
%>

            </table>
        </td>
<%
    }
%>
    </tr></table>


<script type="text/javascript">
    // function to handle click of titration well role checkbox to set the corresponding hidden form element accordingly
    function titrationRoleChecked(el)
    {
        var hiddenEl = getHiddenFormElement(el.name);
        if (hiddenEl != null)
            hiddenEl.value = el.checked ? "true" : "";
    }

    function showHideAnalytePropertyColumn()
    {
<%
        for (Map.Entry<String, Titration> titrationEntry : nonUnknownTitrations.entrySet())
        {
%>
            var titrationRoleName = '<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue())) %>';
            var titrationCellName = '<%= h(LuminexUploadWizardAction.getTitrationColumnCellName(titrationEntry.getValue().getName())) %>';
            var isChecked = document.getElementsByName(titrationRoleName)[0].checked;

            // set the hidden helper showcol field value
            var showcols = document.getElementsByName(titrationRoleName + "_showcol");
            if (showcols.length == 1)
                showcols[0].value = (isChecked ? "true" : "");

            // show/hide the column associated with this titration
            var elements = Ext.select('*[name=' + titrationCellName + ']').elements;
            for (var i = 0; i < elements.length; i++)
            {
                if (isChecked)
                {
                    elements[i].style.display = "table-cell";
                }
                else
                {
                    elements[i].style.display = "none";

                    // also need to make sure all input checkboxes are unchecked if hiding column cell (except for the "Same" checkbox)
                    var cellInputs = elements[i].getElementsByTagName("input");
                    if (cellInputs.length == 1 && cellInputs[0].id.indexOf("CheckBox") == -1)
                    {
                        cellInputs[0].checked = false;
                    }
                }
            }
<%
        }
%>
    }

    function getHiddenFormElement(elName)
    {
        var els = document.getElementsByName(elName);
        for (var i = 0; i < els.length; i++)
        {
            if (els[i].type == "hidden")
            {
                return els[i];
            }
        }
        return null;
    }

    function getInputFormElement(elName)
    {
        var els = document.getElementsByName(elName);
        for (var i = 0; i < els.length; i++)
        {
            if (els[i].type == "checkbox")
            {
                return els[i];
            }
        }
        return null;
    }

    Ext.onReady(setInitialWellRoles);
    function setInitialWellRoles()
    {
<%
        for (Map.Entry<String, Titration> titrationEntry : titrationsWithTypes.entrySet())
        {
            for (Titration.Type t : Titration.Type.values())
            {
%>
                var propertyName = <%=PageFlowUtil.jsString(LuminexUploadWizardAction.getTitrationTypeCheckboxName(t, titrationEntry.getValue())) %>;
                var hiddenEl = getHiddenFormElement(propertyName);
                var inputEl = getInputFormElement(propertyName);
                if (hiddenEl && inputEl)
                {
                    inputEl.checked = hiddenEl.value == "true";
                }
<%
            }
        }
        for (String singlePointControl : trackedSinglePointControls)
        {
%>
            var propertyName = <%=PageFlowUtil.jsString(LuminexUploadWizardAction.getSinglePointControlCheckboxName(singlePointControl)) %>;
            var hiddenEl = getHiddenFormElement(propertyName);
            var inputEl = getInputFormElement(propertyName);
            if (hiddenEl && inputEl)
            {
                inputEl.checked = hiddenEl.value == "true";
            }
<%
        }
%>
    }
</script>