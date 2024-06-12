<%
/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.protein.search.ProphetFilterType" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.query.FilterView" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.peptideProphetProbability" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.peptideFilterType" %>
<%@ page import="static org.labkey.api.protein.search.ProphetFilterType.probability" %>
<%@ page import="static org.labkey.api.protein.search.ProphetFilterType.customView" %>
<%@ page import="static org.labkey.ms2.MS2Controller.PeptideFilteringFormElements.targetProtein" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean bean = view.getModelBean();
MS2Controller.PeptideFilteringComparisonForm form = bean.getForm();
String peptideViewName = form.getPeptideCustomViewName(getViewContext());
%>

<%=getScriptTag("MS2/inlineViewDesigner.js")%>

<labkey:form action="<%= new ActionURL(MS2Controller.ProteinDisambiguationRedirectAction.class, getContainer()) %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <input name="<%= MS2Controller.PeptideFilteringFormElements.targetURL %>" type="hidden" value="<%=h(bean.getTargetURL())%>" />
    <p>Peptides to include in the comparison:</p>
    <div class="labkey-indented"><input type="radio" name="<%= peptideFilterType %>" value="<%= ProphetFilterType.none %>"<%=checked(form.isNoPeptideFilter())%> /> All peptides</div>
    <div class="labkey-indented"><input type="radio" name="<%= peptideFilterType %>" id="peptideProphetRadioButton" value="<%= probability %>"<%=checked(form.isPeptideProphetFilter())%>/> Peptides with PeptideProphet probability &ge; <input type="text" size="2" id="<%= peptideProphetProbability %>" name="<%= peptideProphetProbability %>" value="<%=h(form.getPeptideProphetProbability())%>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= peptideFilterType %>" id="<%= unsafe(FilterView.PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON) %>" value="<%= customView %>"<%=checked(form.isCustomViewPeptideFilter())%>/>
        Peptides that meet the filter criteria in a custom grid:
        <% String peptideViewSelectId = bean.getPeptideView().renderViewList(request, out, peptideViewName); %>
        <%=link("Create or Edit").onClick("showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;") %>

        <br/>
        <br/>
        <div id="peptidesCustomizeView" class="labkey-indented"></div>
    </div>
    <hr/>
    <p>
        Optionally require that peptides have a sequence match in protein: <input type="text" size="30" name="<%= targetProtein %>" value="<%= h(form.getTargetProtein()==null ? "" : form.getTargetProtein()) %>" />
        <%=helpPopup("Protein Filter", "<p>Show only peptides whose sequences match against a specified protein. It need not be the protein mapped to the peptide by the search engine or ProteinProphet.</p><p>If no protein matches the name specified, or if multiple proteins match, this page will be redisplayed to correct the search.</p>", true)%>
    </p>

    <p><labkey:button text="Compare"/></p>
</labkey:form>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    LABKEY.Utils.onReady(function() {
        document.getElementById('<%=peptideProphetProbability%>')['onfocus'] = function() { document.getElementById('peptideProphetRadioButton').checked = true; };
    });
</script>
