<%
/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean bean = view.getModelBean();
MS2Controller.PeptideFilteringComparisonForm form = bean.getForm();
String peptideViewName = form.getPeptideCustomViewName(getViewContext());
String proteinGroupViewName = form.getProteinGroupCustomViewName(getViewContext());
%>
<form action="<%= bean.getTargetURL() %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <p>This comparison view is based on ProteinProphet data so the runs must be associated with ProteinProphet data.
        All proteins in all ProteinProphet protein groups will be shown in the comparison, subject to the filter criteria.</p>
    <p>There are three options for filtering the peptides that contribute evidence to the protein groups:</p>
    <p class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="<%= MS2Controller.ProphetFilterType.none %>" <%= form.isNoPeptideFilter() ? "checked=\"true\"" : "" %> /> Use all the peptides</p>
    <p class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="<%= MS2Controller.ProphetFilterType.probability %>" <%= form.isPeptideProphetFilter() ? "checked=\"true\"" : "" %>/> All peptides with PeptideProphet probability &ge; <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="2" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= form.getPeptideProphetProbability() == null ? "" : form.getPeptideProphetProbability() %>" /></p>
    <p class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="<%= FilterView.PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON %>" value="<%= MS2Controller.ProphetFilterType.customView %>" <%= form.isCustomViewPeptideFilter() ? "checked=\"true\"" : "" %>/>
        Use a custom view to filter the peptides included in the comparison.
        <%
//            bean.getPeptideView().renderCustomizeViewLink(out, peptideViewName);
            bean.getPeptideView().renderViewList(request, out, peptideViewName);
        %>
    </p>


    <p><labkey:button text="Go"/></p>
</form>
