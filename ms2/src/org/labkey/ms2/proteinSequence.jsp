<%
/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
<%@ page import="org.labkey.ms2.protein.ProteinViewBean" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ProteinViewBean bean = ((JspView<ProteinViewBean>) HttpView.currentView()).getModelBean();
    if (bean.showPeptides)
    {
        ActionURL toggleURL = getActionURL().clone();
        toggleURL.replaceParameter("simpleSequenceView", "false");
    %>
    <table class="labkey-tab-strip">
        <tr>
            <td class="labkey-tab-space"><img width="5" src="<%= getWebappURL("_.gif") %>"></td>
            <td class="labkey-tab" style="margin-bottom: 0;"><a href="<%=h(toggleURL)%>">Detail View</a></td>
            <td class="labkey-tab-space"><img width="5" src="<%= getWebappURL("_.gif") %>"></td>
            <td class="labkey-tab-selected" style="margin-bottom: 0;"><a href="#">Summary View</a></td>
            <td class="labkey-tab-space" width="100%"></td>
            <td class="labkey-tab-space"><img width="5" src="<%= getWebappURL("_.gif") %>"></td>
        </tr>
    </table>
<% } %>
<big><tt><%= bean.protein.getFormattedSequence(bean.run) %></tt></big>
