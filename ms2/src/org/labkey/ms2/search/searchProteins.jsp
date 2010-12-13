<%
/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.ms2.search.ProteinSearchBean" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModelBean();
    ViewContext ctx = me.getViewContext();

    ActionURL url = new ActionURL(MS2Controller.DoProteinSearchAction.class, ctx.getContainer());
    String viewName = bean.getPeptideCustomViewName(ctx);
    String separator = bean.isHorizontal() ? "<td>&nbsp;</td>" : "</tr><tr>";
%>
<form action="<%= url %>" method="get">
    <table>
        <tr>
            <td>Protein name:</td>
            <td nowrap><input size="12" type="text" name="identifier" value="<%= h(bean.getForm().getIdentifier()) %>"/><%= helpPopup("Protein Search: Name", "Required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from an annotations file. You may comma separate multiple names.") %></td>
        <%= separator %>
            <td nowrap>Prob &ge;</td>
            <td nowrap><input type="text" size="1" name="minimumProbability" <% if (bean.getForm().getMinimumProbability() != null ) { %>value="<%= bean.getForm().getMinimumProbability() %>"<% } %>/><%= helpPopup("Protein Search: Probability", "If entered, only ProteinProphet protein groups that have an associated probability greater than or equal to the value will be included.") %></td>
        <%= separator %>
            <td nowrap>Error &le;</td>
            <td nowrap><input type="text" size="1" name="maximumErrorRate"  <% if (bean.getForm().getMaximumErrorRate() != null ) { %>value="<%= bean.getForm().getMaximumErrorRate() %>"<% } %>/><%= helpPopup("Protein Search: Error Rate", "If entered, only ProteinProphet protein groups that have an associated error rate less than or equal to the value will be included.") %></td>
        <%= separator %>
            <td>Subfolders:</td>
            <td nowrap><input type="checkbox" name="includeSubfolders" <% if (bean.getForm().isIncludeSubfolders()) { %>checked="true" <% } %> /><%= helpPopup("Protein Search: Subfolders", "If checked, the search will also look in all of this folder's children.") %></td>
        <%= separator %>
            <td>Exact:</td>
            <td nowrap><input type="checkbox" name="exactMatch" <% if (bean.getForm().isExactMatch()) { %>checked="true" <% } %> /><%= helpPopup("Protein Search: Exact Match", "If checked, the search will only find proteins with an exact name match. If not checked, proteins that start with the name entered will also match, but the search may be significantly slower.") %></td>
        <%= separator %>
            <td>Restrict:</td>
            <td nowrap><input type="checkbox" name="restrictProteins" <% if (bean.getForm().isRestrictProteins()) { %>checked="true" <% } %> /><%= helpPopup("Protein Search: Restrict Proteins", "If checked, the search will only look for proteins that are in FASTA files that have been searched by the included runs. If not checked, the list of Matching Proteins will include all proteins that match the criteria.") %></td>
        <%= separator %>
            <td />
            <td><labkey:button text="Search" /></td>
        <%= separator %>
        </tr>
    </table>
    <% if (bean.isHorizontal()) { %>
        <table>
            <tr>
                <td valign="center" height="100%">
                    <span style="padding-right: 10px">Peptide filter: <input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="none" <%= bean.getForm().isNoPeptideFilter() ? "checked=\"true\"" : "" %> />None<%= helpPopup("Peptide Filter: None", "Do not filter the protein results based on peptide criteria.") %></span>
                    <span style="padding-right: 10px"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="peptideProphet" <%= bean.getForm().isPeptideProphetFilter() ? "checked=\"true\"" : "" %>/>Pep prob &ge; <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="1" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= bean.getForm().getPeptideProphetProbability() == null ? "" : bean.getForm().getPeptideProphetProbability() %>" /><%= helpPopup("Peptide Filter: PeptideProphet", "Only show protein groups where at least one peptide has a PeptideProphet probability above some threshold.") %></span>
                    <span style="padding-right: 10px"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="customViewRadioButton" value="customView" <%= bean.getForm().isCustomViewPeptideFilter() ? "checked=\"true\"" : "" %>/><%
                        bean.getPeptideView(ctx).renderViewList(request, out, viewName);
                        %><%= helpPopup("Peptide Filter: Custom", "Only show protein groups where at least one peptide meets a custom filter.") %>
                        <%
                        bean.getPeptideView(ctx).renderCustomizeViewLink(out, viewName);
                        %>
                    </span>
                </td>
            </tr>
        </table>
    <% } %>
</form>
