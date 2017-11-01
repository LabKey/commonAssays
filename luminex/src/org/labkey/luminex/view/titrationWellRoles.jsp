<%
/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.luminex.LuminexRunUploadForm" %>
<%@ page import="org.labkey.luminex.LuminexUploadWizardAction" %>
<%@ page import="org.labkey.luminex.model.Titration" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.TreeMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>

<style type="text/css">
    .lk-luminex-well-role-table td {
        vertical-align: top;
        padding-right: 10px;
    }
</style>

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
%>
    <table class="lk-luminex-well-role-table">
        <tr>
<%
    if (nonUnknownTitrations.size() > 0)
    {
%>
        <td>
            <labkey:panel title="Titrations">
                <table class="lk-fields-table" style="width: 100%;">
                    <tr>
                        <td>&nbsp;</td>
                        <td style="padding-right: 10px;">Standard</td>
                        <td style="padding-right: 10px;">QC Control</td>
                        <td>Other Control<%= PageFlowUtil.helpPopup("Other Control", "AUC and EC50 values are calculated for 'Other Control' titrations but they are not added to Levey-Jennings tracking plots.")%></td>
                    </tr>
<%
        for (Map.Entry<String, Titration> titrationEntry : nonUnknownTitrations.entrySet())
        {
%>
                    <tr>
                        <td style="padding-right: 10px;"><%= h(titrationEntry.getValue().getName()) %></td>
                        <td align="center">
                            <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.standard, titrationEntry.getValue())) %>'
                                   value='1' onClick='titrationRoleChecked(this);showHideAnalytePropertyColumn();' />
                        </td>
                        <td align="center">
                            <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.qccontrol, titrationEntry.getValue())) %>'
                                   value='1' onClick='titrationRoleChecked(this);' />
                        </td>
                        <td align="center">
                            <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.othercontrol, titrationEntry.getValue())) %>'
                                   value='1' onClick='titrationRoleChecked(this);' />
                        </td>
                    </tr>
<%
        }
%>
            </table>
        </labkey:panel>
        </td>
<%
    }

    // show section for the user to select which titrations are Titrated Unknowns
    if (unknownTitrations.size() > 0)
    {
%>
        <td>
            <labkey:panel title="Titrated Unknowns">
                <table class="lk-fields-table" style="width: 100%;">
<%
        for (Map.Entry<String, Titration> titrationEntry : unknownTitrations.entrySet())
        {
%>
                    <tr>
                        <td style="padding-right: 10px;"><%= h(titrationEntry.getValue().getName()) %></td>
                        <td>
                            <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getTitrationTypeCheckboxName(Titration.Type.unknown, titrationEntry.getValue())) %>'
                                   value='1' onClick='titrationRoleChecked(this);' />
                        </td>
                    </tr>
<%
        }
%>
                </table>
            </labkey:panel>
        </td>
<%
    }

    // show section for the user to select trackec single point controls
    if (trackedSinglePointControls.size() >0)
    {
%>
        <td>
            <labkey:panel title="Tracked Single Point Controls">
                <table class="lk-fields-table" style="width: 100%;">
<%
                    for (String trackedSinglePointControl : trackedSinglePointControls)
                    {
%>
                    <tr>
                        <td style="padding-right: 10px;"><%= h(trackedSinglePointControl) %></td>
                        <td>
                            <input type='checkbox' name='<%= h(LuminexUploadWizardAction.getSinglePointControlCheckboxName(trackedSinglePointControl)) %>'
                                   value='1' onClick='titrationRoleChecked(this);' />
                        </td>
                    </tr>
<%
                    }
%>

                </table>
            </labkey:panel>
        </td>
<%
    }
%>
        </tr>
    </table>

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
            var elements = Ext4.select('*[name=' + titrationCellName + ']').elements;
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

    Ext4.onReady(setInitialWellRoles);
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