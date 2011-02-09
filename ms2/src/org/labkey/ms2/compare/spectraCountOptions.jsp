<%
/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
<%@ page import="org.labkey.ms2.query.SpectraCountConfiguration" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean<MS2Controller.SpectraCountForm> bean = view.getModelBean();
MS2Controller.SpectraCountForm form = bean.getForm();
String peptideViewName = form.getPeptideCustomViewName(HttpView.currentContext());

%>

<script type="text/javascript" src="<%= org.labkey.api.settings.AppProps.getInstance().getContextPath() %>/MS2/inlineViewDesigner.js"></script>

<form action="<%= bean.getTargetURL() %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
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
                %><input type="radio" <%= spectraConfig == selectedConfig  ? "checked=\"true\"" : "" %> name="spectraConfig" value="<%= spectraConfig.getTableName()%>" /><%= h(spectraConfig.getDescription())%><br/><%
            }
            %>
        </div>
    </p>
    <p>There are three options for filtering the peptide identifications:</p>
    <p class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="none" <%= form.isNoPeptideFilter() ? "checked=\"true\"" : "" %> /> All peptides</p>
    <p class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="peptideProphet" <%= form.isPeptideProphetFilter() ? "checked=\"true\"" : "" %>/> Peptides with PeptideProphet probability &ge; <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="2" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= form.getPeptideProphetProbability() == null ? "" : form.getPeptideProphetProbability() %>" /></p>
    <p class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="customViewRadioButton" value="customView" <%= form.isCustomViewPeptideFilter() ? "checked=\"true\"" : "" %>/>
        Peptides that meet the filter criteria in a custom view:
        <% String peptideViewSelectId = bean.getPeptideView().renderViewList(request, out, peptideViewName); %>
        <%= PageFlowUtil.textLink("Create or Edit View", (String)null, "showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;", "editPeptidesViewLink") %>

        <br/>
        <span id="peptidesCustomizeView"></span>
    </p>
    <p><labkey:button text="Go"/></p>
</form>
