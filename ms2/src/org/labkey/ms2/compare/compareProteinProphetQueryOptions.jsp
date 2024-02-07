<%
/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.query.FilterView" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.proteinProphetProbability" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.peptideProphetProbability" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.proteinGroupFilterType" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.peptideFilterType" %>
<%@ page import="static org.labkey.ms2.query.MS2Schema.HiddenTableType.ProteinGroupsFilter" %>
<%@ page import="static org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter" %>
<%@ page import="static org.labkey.ms2.MS2Controller.ProphetFilterType.customView" %>
<%@ page import="static org.labkey.ms2.MS2Controller.ProphetFilterType.probability" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.pivotType" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.orCriteriaForEachRun" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PivotType.run" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PivotType.fraction" %>
<%@ page import="static org.labkey.ms2.MS2Controller.ProphetFilterType.none" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.targetURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean bean = view.getModelBean();
MS2Controller.PeptideFilteringComparisonForm form = bean.getForm();
String peptideViewName = form.getPeptideCustomViewName(getViewContext());
String proteinGroupViewName = form.getProteinGroupCustomViewName(getViewContext());
%>

<%=getScriptTag("MS2/inlineViewDesigner.js")%>

<labkey:form action="<%= new ActionURL(MS2Controller.ProteinDisambiguationRedirectAction.class, getContainer()) %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <input name="<%= targetURL %>" type="hidden" value="<%=h(bean.getTargetURL())%>" />
    <p>This comparison view is based on ProteinProphet data so the runs must be associated with ProteinProphet data.
        All proteins in all ProteinProphet protein groups will be shown in the comparison, subject to the filter criteria.</p>
    <p style="width:100%" class="labkey-title-area-line"></p>
    <p>Protein groups to use in the comparison:</p>
    <div class="labkey-indented"><input type="radio" name="<%= proteinGroupFilterType %>" value="<%= none %>"<%=checked(form.isNoProteinGroupFilter())%>/> All protein groups</div>
    <div class="labkey-indented"><input type="radio" name="<%= proteinGroupFilterType %>" id="proteinProphetRadioButton" value="<%= probability %>"<%=checked(form.isProteinProphetFilter())%>/> Protein groups with ProteinProphet probability &ge; <input type="text" size="2" id="<%= proteinProphetProbability %>" name="<%= proteinProphetProbability %>" value="<%=h(form.getProteinProphetProbability())%>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= proteinGroupFilterType %>" id="<%= unsafe(FilterView.PROTEIN_GROUPS_CUSTOM_VIEW_RADIO_BUTTON) %>" value="<%= customView %>"<%=checked(form.isCustomViewProteinGroupFilter())%>/>
        Protein groups that meet the filter criteria in a custom view:
        <% String proteinGroupViewSelectId = bean.getProteinGroupView().renderViewList(request, out, proteinGroupViewName); %>
        <%=link("Create or Edit View").onClick("showViewDesigner('" + ProteinGroupsFilter + "', 'proteinGroupsCustomizeView', " + PageFlowUtil.jsString(proteinGroupViewSelectId) + "); return false;").id("editProteinGroupsViewLink") %>

        <br/>
        <br/>
        <span id="proteinGroupsCustomizeView"></span>
    </div>
    <p style="width:100%" class="labkey-title-area-line"></p>
    <p>Peptide requirements for the protein groups:</p>
    <div class="labkey-indented"><input type="radio" name="<%= peptideFilterType %>" value="<%= none %>"<%=checked(form.isNoPeptideFilter())%>/> All peptides</div>
    <div class="labkey-indented"><input type="radio" name="<%= peptideFilterType %>" id="peptideProphetRadioButton" value="<%= probability %>"<%=checked(form.isPeptideProphetFilter())%>/> Peptides with PeptideProphet probability &ge; <input type="text" size="2" id="<%= peptideProphetProbability %>" name="<%= peptideProphetProbability %>" value="<%=h(form.getPeptideProphetProbability())%>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= peptideFilterType %>" id="<%= unsafe(FilterView.PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON) %>" value="<%= customView %>"<%=checked(form.isCustomViewPeptideFilter())%>/>
        Peptides that meet the filter criteria in a custom view:
        <% String peptideViewSelectId = bean.getPeptideView().renderViewList(request, out, peptideViewName); %>
        <%=link("Create or Edit View").onClick("showViewDesigner('" + PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;") %>

        <br/>
        <br/>
        <span id="peptidesCustomizeView"></span>
    </div>

    <p style="width:100%" class="labkey-title-area-line"></p>

    <p>Compare protein groups by:</p>
    <div class="labkey-indented"><input type="radio" name="<%= pivotType %>" value="<%= run %>"<%=checked(run == form.getPivotTypeEnum())%> /> Run</div>
    <div class="labkey-indented"><input type="radio" name="<%= pivotType %>" value="<%= fraction %>"<%=checked(fraction == form.getPivotTypeEnum())%> /> Fraction</div>

    <p style="width:100%" class="labkey-title-area-line"></p>

    <p>For each run or fraction:</p>
    <div class="labkey-indented"><input type="radio" name="<%= orCriteriaForEachRun %>" value="false"<%=checked(!form.isOrCriteriaForEachRun())%> /> only show the protein if it meets the filter criteria in that run/fraction.</div>
    <div class="labkey-indented"><input type="radio" name="<%= orCriteriaForEachRun %>" value="true" <%=checked(form.isOrCriteriaForEachRun())%> /> show the protein if the run/fraction contains that protein and the protein meets the filter criteria in any of the compared runs/fractions.</div>

    <p style="width:100%" class="labkey-title-area-line"></p>
    <div class="labkey-indented"><input type="checkbox" name="normalizeProteinGroups"<%=checked(form.isNormalizeProteinGroups())%> value="true" /> Normalize protein groups across runs</div>
    <p><labkey:button text="Compare"/></p>
</labkey:form>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    LABKEY.Utils.onReady(function() {
        document.getElementById('<%=proteinProphetProbability%>')['onfocus'] = function() { document.getElementById('proteinProphetRadioButton').checked = true; };
        document.getElementById('<%=peptideProphetProbability%>')['onclick'] = function() { document.getElementById('peptideProphetRadioButton').checked = true; };
    });
</script>
