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
<%@ page import="org.labkey.api.data.MultiValuedForeignKey" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.MS2Manager" %>
<%@ page import="org.labkey.ms2.query.FilterView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms2.query.MS2Schema" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean bean = view.getModelBean();
MS2Controller.PeptideFilteringComparisonForm form = bean.getForm();
String peptideViewName = form.getPeptideCustomViewName(getViewContext());
String proteinGroupViewName = form.getProteinGroupCustomViewName(getViewContext());
%>

<script type="text/javascript" src="<%= org.labkey.api.settings.AppProps.getInstance().getContextPath() %>/MS2/inlineViewDesigner.js"></script>

<form action="<%= bean.getTargetURL() %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <p>This comparison view is based on ProteinProphet data so the runs must be associated with ProteinProphet data.
        All proteins in all ProteinProphet protein groups will be shown in the comparison, subject to the filter criteria.</p>
    <p style="width:100%" class="labkey-title-area-line"></p>
    <p>Protein groups to use in the comparison:</p>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.proteinGroupFilterType %>" value="<%= MS2Controller.ProphetFilterType.none %>" <%= form.isNoProteinGroupFilter() ? "checked=\"true\"" : "" %> /> All protein groups</div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.proteinGroupFilterType %>" id="proteinProphetRadioButton" value="<%= MS2Controller.ProphetFilterType.probability %>" <%= form.isProteinProphetFilter() ? "checked=\"true\"" : "" %>/> Protein groups with ProteinProphet probability &ge; <input onfocus="document.getElementById('proteinProphetRadioButton').checked=true;" type="text" size="2" name="<%= MS2Controller.PeptideFilteringFormElements.proteinProphetProbability %>" value="<%= form.getProteinProphetProbability() == null ? "" : form.getProteinProphetProbability() %>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.proteinGroupFilterType %>" id="<%= FilterView.PROTEIN_GROUPS_CUSTOM_VIEW_RADIO_BUTTON %>" value="<%= MS2Controller.ProphetFilterType.customView %>" <%= form.isCustomViewProteinGroupFilter() ? "checked=\"true\"" : "" %>/>
        Protein groups that meet the filter criteria in a custom view:
        <% String proteinGroupViewSelectId = bean.getProteinGroupView().renderViewList(request, out, proteinGroupViewName); %>
        <%= PageFlowUtil.textLink("Create or Edit View", (String)null, "showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.ProteinGroupsFilter + "', 'proteinGroupsCustomizeView', " + PageFlowUtil.jsString(proteinGroupViewSelectId) + "); return false;", "editProteinGroupsViewLink") %>

        <br/>
        <br/>
        <span id="proteinGroupsCustomizeView"></span>
    </div>
    <p style="width:100%" class="labkey-title-area-line"></p>
    <p>Peptide requirements for the protein groups:</p>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="<%= MS2Controller.ProphetFilterType.none %>" <%= form.isNoPeptideFilter() ? "checked=\"true\"" : "" %> /> All peptides</div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="<%= MS2Controller.ProphetFilterType.probability %>" <%= form.isPeptideProphetFilter() ? "checked=\"true\"" : "" %>/> Peptides with PeptideProphet probability &ge; <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="2" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= form.getPeptideProphetProbability() == null ? "" : form.getPeptideProphetProbability() %>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="<%= FilterView.PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON %>" value="<%= MS2Controller.ProphetFilterType.customView %>" <%= form.isCustomViewPeptideFilter() ? "checked=\"true\"" : "" %>/>
        Peptides that meet the filter criteria in a custom view:
        <% String peptideViewSelectId = bean.getPeptideView().renderViewList(request, out, peptideViewName); %>
        <%= PageFlowUtil.textLink("Create or Edit View", (String)null, "showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;", "editPeptidesViewLink") %>

        <br/>
        <br/>
        <span id="peptidesCustomizeView"></span>
    </div>

    <p style="width:100%" class="labkey-title-area-line"></p>

    <p>Compare protein groups by:</p>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.pivotType %>" value="<%= MS2Controller.PivotType.run %>" <%= MS2Controller.PivotType.run == form.getPivotTypeEnum() ? "checked=\"true\"" : "" %> /> Run</div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.pivotType %>" value="<%= MS2Controller.PivotType.fraction %>" <%= MS2Controller.PivotType.fraction == form.getPivotTypeEnum() ? "checked=\"true\"" : "" %> /> Fraction</div>

    <p style="width:100%" class="labkey-title-area-line"></p>

    <p>For each run or fraction:</p>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.orCriteriaForEachRun %>" value="false" <%= !form.isOrCriteriaForEachRun() ? " checked=\"true\"" : "" %> /> only show the protein if it meets the filter criteria in that run/fraction.</div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.orCriteriaForEachRun %>" value="true" <%= form.isOrCriteriaForEachRun() ? " checked=\"true\"" : "" %> /> show the protein if the run/fraction contains that protein and the protein meets the filter criteria in any of the compared runs/fractions.</div>

    <p style="width:100%" class="labkey-title-area-line"></p>
    <div class="labkey-indented"><input type="checkbox" name="normalizeProteinGroups" <%= form.isNormalizeProteinGroups() ? " checked=\"true\"" : "" %> value="true" /> Normalize protein groups across runs</div>
    <p><labkey:button text="Compare"/></p>
</form>
