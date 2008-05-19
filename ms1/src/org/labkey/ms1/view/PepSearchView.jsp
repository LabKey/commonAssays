<%
/*
 * Copyright (c) 2008 LabKey Corporation
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
<%@ page import="org.labkey.ms1.model.PepSearchModel" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms1.MS1Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PepSearchModel> me = (JspView<PepSearchModel>) HttpView.currentView();
    PepSearchModel model = me.getModelBean();
%>
<% if(model.hasErrorMsg()) { %>
<p class="error"><%=model.getErrorMsg()%></p>
<% } %>
<form action="<%=model.getResultsUri()%>" method="get">
    <input type="hidden" name="<%=MS1Controller.PepSearchForm.ParamNames.runIds.name()%>" value="<%=PageFlowUtil.filter(model.getRunIds())%>"/>
    <p>Peptide Sequence:
        <input type="text" name="<%=MS1Controller.PepSearchForm.ParamNames.pepSeq.name()%>"
               value="<%=PageFlowUtil.filter(model.getPepSeq())%>" size="40"/>
        <%=helpPopup("Peptide Sequence", "Enter the peptide sequence to find, or multiple sequences separated by commas. Use * to match any sequence of characters.")%>
        &nbsp;
        <input id="cbxExact" type="checkbox" name="<%=MS1Controller.PepSearchForm.ParamNames.exact.name()%>"
               style="vertical-align:middle" <%=model.isExact() ? "checked=\"1\"" : ""%> />
        <label for="cbxExact">Exact Match</label>
        <%=helpPopup("Exact Match", "If checked, the search will match the peptides exactly; if unchecked, it will match any peptide that starts with the specified sequence and ignore modifications.")%>
        &nbsp;
        <input id="cbxSubfolders" type="checkbox" name="<%=MS1Controller.PepSearchForm.ParamNames.subfolders.name()%>"
               style="vertical-align:middle" <%=model.includeSubfolders() ? "checked" : ""%> />
        <label for="cbxSubfolders">Search Subfolders</label>
        <%=helpPopup("Search Subfolders", "Check to search this folder and all of its descendants.")%>
        &nbsp;
        <input id="btnSearch" type="image" name="submit"
               src="<%=PageFlowUtil.buttonSrc("Search")%>"
               style="vertical-align:middle"/>
    </p>
</form>
