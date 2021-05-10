<%
/*
 * Copyright (c) 2005-2018 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.AnnotationView" %>
<%@ page import="java.util.Collection" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    AnnotationView.AnnotViewBean bean = ((JspView<AnnotationView.AnnotViewBean>) HttpView.currentView()).getModelBean();
%>
<table class="lk-fields-table">
<tr>
   <td class="labkey-form-label" nowrap>Description</td><td><%=h(bean.seqDesc)%></td>
</tr>
<tr>
   <td class="labkey-form-label" nowrap>Gene name<%= h(bean.geneNameLinks.size() <= 1 ? "" : "s") %></td><td><%=HtmlString.join(bean.geneNameLinks, HtmlString.unsafe(", "))%></td>

</tr>
<tr>
   <td class="labkey-form-label" nowrap>Organism<%= h(bean.seqOrgs.size() <= 1 ? "" : "s") %></td>

   <td><%
        for(String orgName : bean.seqOrgs)
        { %>
            <%= h(orgName) %><br/> <%
        } %>
   </td>
</tr>
</table>
<br>
<table class="labkey-data-region-legacy labkey-show-borders labkey-prot-annots">
<tr>
    <% for (String header : bean.annotations.keySet()) { %>
       <td class="labkey-column-header"><%= h(header) %></td>
    <% } %>
</tr>
<tr valign="top">
    <% for (Collection<HtmlString> values : bean.annotations.values()) { %>
        <td><%
    if (values.isEmpty()) { %><em>none loaded</em><% }
    for (HtmlString link : values)
    {
        out.print(link);
        out.println(unsafe("<br>"));
    }
    %>
   </td>
    <% } %>
</tr>
</table>
