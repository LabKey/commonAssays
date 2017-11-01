<%
/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.search.ProteinSearchBean" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModelBean();

    ActionURL url = new ActionURL(MS2Controller.DoProteinSearchAction.class, getContainer());
%>

<labkey:form action="<%= url %>" method="get">
    <input type="hidden" name="restrictProteins" value="true" />
    <table class="lk-fields-table">
        <tr>
            <td class="labkey-form-label">Protein name *<%= helpPopup("Protein name", "Required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from an annotations file. You may comma separate multiple names.") %></td>
            <td nowrap><input size="12" type="text" name="identifier" value="<%= h(bean.getForm().getIdentifier()) %>"/></td>
        </tr>
        <tr>
            <td nowrap class="labkey-form-label">Minimum prob<%= helpPopup("Minimum probability", "If entered, only ProteinProphet protein groups that have an associated probability greater than or equal to the value will be included.") %></td>
            <td nowrap><input type="text" size="1" name="minimumProbability" <% if (bean.getForm().getMinimumProbability() != null ) { %>value="<%= bean.getForm().getMinimumProbability() %>"<% } %>/></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Search in subfolders<%= helpPopup("Search in subfolders", "If checked, the search will also look in all of this folder's children.") %></td>
            <td nowrap><input type="checkbox" name="includeSubfolders"<%=checked(bean.getForm().isIncludeSubfolders())%>/></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Exact matches only<%= helpPopup("Exact matches only", "If checked, the search will only find proteins with an exact name match. If not checked, proteins that start with the name entered will also match, but the search may be significantly slower.") %></td>
            <td nowrap><input type="checkbox" name="exactMatch"<%=checked(bean.getForm().isExactMatch())%>/></td>
        </tr>
        <tr>
            <td colspan="2" style="padding-top: 10px;">
                <labkey:button text="Search" />
            </td>
        </tr>
    </table>
</labkey:form>
