<%
/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.search.ProteinSearchBean" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("MS2/inlineViewDesigner.js");
    }
%>
<%
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModelBean();
    ViewContext ctx = getViewContext();

    String viewName = bean.getPeptideCustomViewName(ctx);
%>
<labkey:form action="<%= new ActionURL(MS2Controller.DoProteinSearchAction.class, getContainer()) %>">
    <table class="lk-fields-table">
        <tr>
            <td class="labkey-form-label">Protein name *<%= helpPopup("Protein name", "Required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from an annotations file. You may comma separate multiple names.") %></td>
            <td nowrap><input size="20" type="text" name="identifier" value="<%= h(bean.getForm().getIdentifier()) %>"/></td>

            <td nowrap class="labkey-form-label">Minimum prob<%= helpPopup("Minimum probability", "If entered, only ProteinProphet protein groups that have an associated probability greater than or equal to the value will be included.") %></td>
            <td nowrap><input type="text" size="4" name="minimumProbability" <% if (bean.getForm().getMinimumProbability() != null ) { %>value="<%= bean.getForm().getMinimumProbability() %>"<% } %>/></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Search in subfolders<%= helpPopup("Search in subfolders", "If checked, the search will also look in all of this folder's children.") %></td>
            <td nowrap><input type="checkbox" name="includeSubfolders"<%=checked(bean.getForm().isIncludeSubfolders())%>/></td>

            <td nowrap class="labkey-form-label">Maximum error rate<%= helpPopup("Maximum error rate", "If entered, only ProteinProphet protein groups that have an associated error rate less than or equal to the value will be included.") %></td>
            <td nowrap><input type="text" size="4" name="maximumErrorRate"  <% if (bean.getForm().getMaximumErrorRate() != null ) { %>value="<%= bean.getForm().getMaximumErrorRate() %>"<% } %>/></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Exact matches only<%= helpPopup("Exact matches only", "If checked, the search will only find proteins with an exact name match. If not checked, proteins that start with the name entered will also match, but the search may be significantly slower.") %></td>
            <td nowrap><input type="checkbox" name="exactMatch"<%=checked(bean.getForm().isExactMatch())%>/></td>

            <td class="labkey-form-label">Restrict proteins<%= helpPopup("Restrict proteins", "If checked, the search will only look for proteins that are in FASTA files that have been searched by the included runs. If not checked, the list of Matching Proteins will include all proteins that match the criteria.") %></td>
            <td nowrap><input type="checkbox" name="restrictProteins"<%=checked(bean.getForm().isRestrictProteins())%> /></td>
        </tr>
        <tr>
            <td valign="center" height="100%" class="labkey-form-label">Peptide criteria<%= helpPopup("Peptide criteria", "If specified, at least one peptide assigned to the protein group must meet the criteria.") %></td>
            <td colspan="100">
                <div><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="<%= h(MS2Controller.ProphetFilterType.none.name()) %>" <%=checked(bean.getForm().isNoPeptideFilter())%> />None</div>
                <div style="padding-top: 5px"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="<%= h(MS2Controller.ProphetFilterType.probability.name()) %>" <%=checked(bean.getForm().isPeptideProphetFilter())%>/>Minimum PeptideProphet prob <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="4" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= bean.getForm().getPeptideProphetProbability() == null ? "" : bean.getForm().getPeptideProphetProbability() %>" /></div>
                <div style="padding-top: 5px"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="customViewRadioButton" value="<%= h(MS2Controller.ProphetFilterType.customView.name()) %>"<%=checked(bean.getForm().isCustomViewPeptideFilter())%>/>Custom filter:
                    <% String peptideViewSelectId = bean.getPeptideView(ctx).renderViewList(request, out, viewName); %>
                    <%= PageFlowUtil.textLink("Create or Edit View", (ActionURL)null, "showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;", "editPeptidesViewLink") %>
                </div>
                <span id="peptidesCustomizeView"></span>
            </td>
        </tr>
        <tr>
            <td colspan="4" style="padding-top: 10px;">
                <labkey:button text="Search" />
            </td>
        </tr>
    </table>
</labkey:form>
