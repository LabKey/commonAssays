<%
/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.protein.search.ProteinSearchBean" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.ms2.MS2Controller.PeptideFilteringFormElements" %>
<%@ page import="org.labkey.ms2.query.FilterView" %>
<%@ page import="org.labkey.ms2.query.MS2Schema.HiddenTableType" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    // This panel is optionally rendered by proteinPanel.jsp for MS2 deployments only. It requires "MS2/inlineViewDesigner.js",
    // which is optionally included by searchProteins.jsp.
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModelBean();
    ViewContext ctx = getViewContext();
    FilterView peptideView = new FilterView(ctx, true);
    String viewName = bean.getPeptideCustomViewName(ctx);
%>
        <tr>
            <td valign="center" height="100%" class="labkey-form-label">Peptide criteria<%= helpPopup("Peptide criteria", "If specified, at least one peptide assigned to the protein group must meet the criteria.") %></td>
            <td colspan="100">
                <div><input type="radio" name="<%=PeptideFilteringFormElements.peptideFilterType%>" value="<%=ProphetFilterType.none%>" <%=checked(bean.getForm().isNoPeptideFilter())%> />None</div>
                <div style="padding-top: 5px"><input type="radio" name="<%=PeptideFilteringFormElements.peptideFilterType%>" id="peptideProphetRadioButton" value="<%=ProphetFilterType.probability%>" <%=checked(bean.getForm().isPeptideProphetFilter())%>/>Minimum PeptideProphet prob <input type="text" size="4" id="<%=PeptideFilteringFormElements.peptideProphetProbability%>" name="<%=PeptideFilteringFormElements.peptideProphetProbability%>" value="<%=h(bean.getForm().getPeptideProphetProbability() == null ? "" : bean.getForm().getPeptideProphetProbability())%>" /></div>
                <% addHandler(PeptideFilteringFormElements.peptideProphetProbability.name(), "focus", "document.getElementById('peptideProphetRadioButton').checked=true;"); %>
                <div style="padding-top: 5px"><input type="radio" name="<%=PeptideFilteringFormElements.peptideFilterType%>" id="customViewRadioButton" value="<%=ProphetFilterType.customView%>"<%=checked(bean.getForm().isCustomViewPeptideFilter())%>/>Custom filter:
                    <% String peptideViewSelectId = peptideView.renderViewList(request, out, viewName); %>
                    <%=link("Create or Edit View").onClick("showViewDesigner('" + HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;") %>
                </div>
                <span id="peptidesCustomizeView"></span>
            </td>
        </tr>
