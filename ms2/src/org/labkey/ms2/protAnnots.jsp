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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    AnnotationView.AnnotViewBean bean = ((JspView<AnnotationView.AnnotViewBean>) HttpView.currentView()).getModelBean();
%>
<table class="lk-fields-table">
<tr>
   <td class="labkey-form-label" nowrap="true">Description</td><td><%=h(bean.seqDesc)%></td>
</tr>
<tr>
   <td class="labkey-form-label" nowrap="true">Gene name(s)</td><td><%=HtmlString.join(bean.geneNameLinks, HtmlString.unsafe(", "))%></td>
</tr>
<tr>
   <td class="labkey-form-label" nowrap="true">Organisms</td>
   <td><%
        int nOfOrgs = bean.seqOrgs.size();

        if (1 == nOfOrgs)
        {
            for (String orgName : bean.seqOrgs)
                out.println(h(orgName));
        }
        else
        { %>
      <select name="Orgs" size="<%=Math.min(3, nOfOrgs)%>"><%
            for(String orgName : bean.seqOrgs)
            { %>
         <option><%=h(orgName)%></option><%
            } %>
      </select><%
        } %>
   </td>
</tr>
</table>
<br>
<table class="labkey-data-region-legacy labkey-show-borders labkey-prot-annots">
<tr>
    <td class="labkey-column-header">Genbank IDs</td>
    <td class="labkey-column-header">GIs</td>
    <td class="labkey-column-header">Swiss-Prot Accessions</td>
    <td class="labkey-column-header">Swiss-Prot Names</td>
    <td class="labkey-column-header">Ensembl</td>
    <td class="labkey-column-header">IPI numbers</td>
    <td class="labkey-column-header">GO Categories</td>
</tr>
<tr valign="top">
   <td><%
    if (bean.genBankLinks.isEmpty()) { %><em>none loaded</em><% }
    for (HtmlString link : bean.genBankLinks)
    {
        out.print(link);
        out.println(unsafe("<br>"));
    }
    %>
   </td>
    <td><%
     if (bean.GIs.isEmpty()) { %><em>none loaded</em><% }
     for (HtmlString link : bean.GIs)
     {
         out.print(link);
         out.println(unsafe("<br>"));
     }
     %>
    </td>
    <td><%
     if (bean.swissProtAccns.isEmpty()) { %><em>none loaded</em><% }
     for (HtmlString link : bean.swissProtAccns)
     {
         out.print(link);
         out.println(unsafe("<br>"));
     }
     %>
    </td>
    <td><%
     if (bean.swissProtNames.isEmpty()) { %><em>none loaded</em><% }
     for (HtmlString link : bean.swissProtNames)
     {
         out.print(link);
         out.println(unsafe("<br>"));
     }
     %>
    </td>
    <td><%
     if (bean.ensemblIds.isEmpty()) { %><em>none loaded</em><% }
     for (HtmlString link : bean.ensemblIds)
     {
         out.print(link);
         out.println(unsafe("<br>"));
     }
     %>
    </td>
    <td><%
     if (bean.IPI.isEmpty()) { %><em>none loaded</em><% }
     for (HtmlString link : bean.IPI)
     {
         out.print(link);
         out.println(unsafe("<br>"));
     }
     %>
    </td>
    <td><%
     if (bean.goCategories.isEmpty()) { %><em>none loaded</em><% }
     for (HtmlString link : bean.goCategories)
     {
         out.print(link);
         out.println(unsafe("<br>"));
     }
     %>
    </td>
</tr>
</table>
