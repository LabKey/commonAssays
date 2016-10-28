<%
/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.query.SpectraCountConfiguration" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
    }
%>
<%
    JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
    MS2Controller.CompareOptionsBean<MS2Controller.SpectraCountForm> bean = view.getModelBean();
    MS2Controller.SpectraCountForm form = bean.getForm();
    String peptideViewName = form.getPeptideCustomViewName(getViewContext());
%>
<script type="text/javascript" src="<%= getContextPath() %>/MS2/inlineViewDesigner.js"></script>
<labkey:form action="<%= new ActionURL(MS2Controller.ProteinDisambiguationRedirectAction.class, getContainer()) %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <input name="<%= MS2Controller.PeptideFilteringFormElements.targetURL %>" type="hidden" value="<%= bean.getTargetURL() %>" />
    <p>
        Group by:<br/>
        <div class="labkey-indented">
            <%
            SpectraCountConfiguration selectedConfig = null;
            for (SpectraCountConfiguration spectraConfig : SpectraCountConfiguration.VALID_CONFIGS)
            {
                if (selectedConfig == null || spectraConfig.getTableName().equals(form.getSpectraConfig()))
                {
                    selectedConfig = spectraConfig;
                }
            }
            for (SpectraCountConfiguration spectraConfig : SpectraCountConfiguration.VALID_CONFIGS)
            {
                %><input type="radio"<%=checked(spectraConfig == selectedConfig)%> name="spectraConfig" id="<%= h(spectraConfig.getTableName()) %>" value="<%= h(spectraConfig.getTableName()) %>" /><%= h(spectraConfig.getDescription())%><br/><%
            }
            %>
        </div>
    </p>
    <p>There are three options for filtering the peptide identifications:</p>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="<%= MS2Controller.ProphetFilterType.none %>"<%=checked(form.isNoPeptideFilter())%> /> All peptides</div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="<%= MS2Controller.ProphetFilterType.probability %>"<%=checked(form.isPeptideProphetFilter())%>/> Peptides with PeptideProphet probability &ge; <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="2" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= form.getPeptideProphetProbability() == null ? "" : form.getPeptideProphetProbability() %>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="customViewRadioButton" value="<%= MS2Controller.ProphetFilterType.customView %>"<%=checked(form.isCustomViewPeptideFilter())%>/>
        Peptides that meet the filter criteria in a custom view:
        <% String peptideViewSelectId = bean.getPeptideView().renderViewList(request, out, peptideViewName); %>

        <script type="text/javascript">
            function viewSavedCallback(arg1, viewInfo)
            {
                // Get the name of the newly saved view
                var viewName = viewInfo.views[0].name;
                // Make sure we're set to use the custom view
                document.getElementById("customViewRadioButton").checked = true;
                var viewNamesSelect = document.getElementById(<%= PageFlowUtil.jsString(peptideViewSelectId) %>);
                if (!viewNamesSelect)
                {
                    window.location.reload();
                }
                else
                {
                    // Check if it already exists in our list
                    for (var i = 0; i < viewNamesSelect.options.length; i++)
                    {
                        if (viewNamesSelect.options[i].value == viewName)
                        {
                            // If so, select it
                            viewNamesSelect.options[i].selected = true;
                            return;
                        }
                    }
                    // Otherwise, add it as a new option
                    viewNamesSelect.options[viewNamesSelect.options.length] = new Option(viewName, viewName, false, true);
                }
            }
        </script>


        <%= PageFlowUtil.textLink("Create or Edit View", (ActionURL)null, "showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + ", viewSavedCallback); return false;", "editPeptidesViewLink") %>

        <br/>
        <br/>
        <span id="peptidesCustomizeView"></span>
    </div>
        <p>
        Optionally require that peptides have a sequence match in protein: <input type="text" size="30" name="<%= MS2Controller.PeptideFilteringFormElements.targetProtein %>" value="<%= h(form.getTargetProtein()==null ? "" : form.getTargetProtein()) %>" />
        <%= PageFlowUtil.helpPopup("Protein Filter", "<p>Show only peptides whose sequences match against a specified protein. It need not be the protein mapped to the peptide by the search engine or ProteinProphet.</p><p>If no protein matches the name specified, or if multiple proteins match, this page will be redisplayed to correct the search.</p>", true)%>
    </p>
    <p><labkey:button text="Compare"/></p>
</labkey:form>
