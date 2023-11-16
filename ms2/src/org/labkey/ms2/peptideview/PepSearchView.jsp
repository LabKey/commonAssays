<%
/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.protein.ProteinService" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.PepSearchModel" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PepSearchModel> me = (JspView<PepSearchModel>) HttpView.currentView();
    PepSearchModel model = me.getModelBean();
%>
<% if (model.hasErrorMsg()) { %>
<p class="error"><%=h(model.getErrorMsg())%></p>
<% } %>
<labkey:form action="<%=model.getResultsUri()%>" method="get">
    <input type="hidden" name="<%=h(ProteinService.PeptideSearchForm.ParamNames.runIds.name())%>" value="<%=h(model.getRunIds())%>"/>
    <table class="lk-fields-table">
        <tr>
            <td class="labkey-form-label"><label for="pepSeq">Peptide sequence</label>*<%=helpPopup("Peptide Sequence", "Enter the peptide sequence to find, or multiple sequences separated by commas. Use * to match any sequence of characters.")%></td>
            <td><input id="pepSeq" type="text" name="<%=h(ProteinService.PeptideSearchForm.ParamNames.pepSeq.name())%>" value="<%=h(model.getPepSeq())%>" size="40"/></td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="cbxExact">Exact matches only</label><%=helpPopup("Exact matches only", "If checked, the search will match the peptides exactly; if unchecked, it will match any peptide that starts with the specified sequence and ignore modifications.")%></td>
            <td><input id="cbxExact" type="checkbox" name="<%=h(ProteinService.PeptideSearchForm.ParamNames.exact.name())%>" style="vertical-align:middle"<%=checked(model.isExact())%> />
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="cbxSubfolders">Search in subfolders</label><%=helpPopup("Search in subfolders", "Check to search this folder and all of its descendants.")%></td>
            <td><input id="cbxSubfolders" type="checkbox" name="<%=h(ProteinService.PeptideSearchForm.ParamNames.subfolders.name())%>" style="vertical-align:middle"<%=checked(model.includeSubfolders())%> /></td>
        </tr>
        <tr>
            <td colspan="2" style="padding-top: 10px;">
                <%= button("Search").submit(true).id("btnSearch").name("submit")%>
            </td>
        </tr>
    </table>
</labkey:form>
