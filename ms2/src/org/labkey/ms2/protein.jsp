<%
/*
 * Copyright (c) 2004-2017 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.protein.ProteinManager" %>
<%@ page import="java.text.Format" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
    Format intFormat = Formats.commaf0;
    Format percentFormat = Formats.percent;
%>
<table class="lk-fields-table">
    <% if (bean.run != null) { %>
        <tr><td class="labkey-form-label">Run</td><td><%= h(bean.run.getDescription()) %></td></tr>
    <% } %>
    <tr><td class="labkey-form-label">Sequence Mass</td><td><%=h(intFormat.format(bean.protein.getMass()))%></td><td>&nbsp;</td></tr><%
    if (bean.showPeptides)
    { %>
        <tr><td class="labkey-form-label">AA Coverage</td><td><%=h(percentFormat.format(bean.protein.getAAPercent(bean.run)))%> (<%=h(intFormat.format(bean.protein.getAACoverage(bean.run)))%> / <%=h(intFormat.format(bean.protein.getSequence().length()))%>)</td></tr>
        <tr><td class="labkey-form-label">Mass Coverage</td><td><%=h(percentFormat.format(bean.protein.getMassPercent(bean.run)))%> (<%=h(intFormat.format(bean.protein.getMassCoverage(bean.run)))%> / <%=h(intFormat.format(bean.protein.getMass()))%>)</td></tr><%
    }

    if (bean.enableAllPeptidesFeature)
    {
        ActionURL urlProteinDetailsPage = getViewContext().cloneActionURL();
        urlProteinDetailsPage.deleteParameter(MS2Controller.ProteinViewBean.ALL_PEPTIDES_URL_PARAM); %>
        <tr>
            <td class="labkey-form-label">Peptides<%= PageFlowUtil.helpPopup("Peptides", "<p><strong>Show only peptides assigned by search engine</strong><br/>The page displays only the set of peptides that the search engine has chosen as matching the subject protein, based on engine-specific scoring.</p><p><strong>Show all peptides with sequence matches</strong><br/>The coverage map and peptide grid show all the filtered trimmed peptides from the run that match a sequence within the subject protein, regardless of whether the protein was chosen by the search engine as matching that specific peptide.</p>", true) %></td>
            <td>
                <labkey:form action="<%= urlProteinDetailsPage %>" method="GET">
                    <% for (Pair<String, String> param : urlProteinDetailsPage.getParameters()) { %>
                        <input type="hidden" name="<%= h(param.getKey()) %>" value="<%= h(param.getValue()) %>" />
                    <% } %>
                    <select name="<%= h(MS2Controller.ProteinViewBean.ALL_PEPTIDES_URL_PARAM) %>" onchange="this.form.submit();">
                        <option value="false">Show only peptides assigned by search engine</option>
                        <option value="true" <%=selected(ProteinManager.showAllPeptides(getActionURL(), getUser()))%>>Show all peptides with sequence matches</option>
                    </select>
                </labkey:form>
            </td>
        </tr><%
    } %>
</table>
