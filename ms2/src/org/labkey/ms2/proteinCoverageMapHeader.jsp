<%
/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();

    if (bean.showPeptides)
    {
        ActionURL showPeptidesPopupUrl = getViewContext().cloneActionURL();
        showPeptidesPopupUrl.setAction(MS2Controller.ShowPeptidePopupAction.class);
        if (null!=showPeptidesPopupUrl.getParameter("MS2Peptides.viewName"))
            showPeptidesPopupUrl.deleteParameter("MS2Peptides.viewName");
        showPeptidesPopupUrl.addParameter("MS2Peptides.viewName", "Standard");
        bean.showRunUrl = showPeptidesPopupUrl.toString();
    }
    ActionURL toggleURL = getActionURL().clone();
    toggleURL.replaceParameter("simpleSequenceView", "true");
%>
<table class="labkey-tab-strip">
    <tr>
        <td class="labkey-tab-space"><img width="5" src="<%=getWebappURL("_.gif")%>"></td>
        <td class="labkey-tab-selected" style="margin-bottom: 0;"><a href="#">Detail View</a></td>
        <td class="labkey-tab-space"><img width="5" src="<%=getWebappURL("_.gif")%>"></td>
        <td class="labkey-tab" style="margin-bottom: 0;"><a href="<%= toggleURL %>">Summary View</a></td>
        <td class="labkey-tab-space" width="100%"></td>
        <td class="labkey-tab-space"><img width="5" src="<%=getWebappURL("_.gif")%>"></td>
    </tr>
</table>

<p><%
    ActionURL exportUrl = getViewContext().cloneActionURL();
    exportUrl.setAction(MS2Controller.ExportProteinCoverageMapAction.class);
    if (null==exportUrl.getParameter("seqId"))
        exportUrl.addParameter("seqId", bean.protein.getSeqId());
%><%= button("Export").href(exportUrl) %></p>
