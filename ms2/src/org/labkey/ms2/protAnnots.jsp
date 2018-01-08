<%
/*
 * Copyright (c) 2005-2017 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
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
   <td class="labkey-form-label" nowrap="true">Gene name(s)</td><td><%=bean.geneName%><% // geneName is either an href or empty string, so don't filter %></td>
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
    if (bean.genBankUrls.isEmpty()) { %><em>none loaded</em><% }
    for(String id : bean.genBankUrls)
        out.println(id + "<br>");
    %>
   </td>
    <td><%
     if (bean.GIs.length == 0) { %><em>none loaded</em><% }
     for(String id : bean.GIs)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     if (bean.swissProtAccns.length == 0) { %><em>none loaded</em><% }
     for(String id : bean.swissProtAccns)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     if (bean.swissProtNames.length == 0) { %><em>none loaded</em><% }
     for(String id : bean.swissProtNames)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     if (bean.ensemblIds.length == 0) { %><em>none loaded</em><% }
     for(String id : bean.ensemblIds)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     if (bean.IPI.length == 0) { %><em>none loaded</em><% }
     for(String id : bean.IPI)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     if (bean.goCategories.length == 0) { %><em>none loaded</em><% }
     for(String id : bean.goCategories)
         out.println(id + "<br>");
     %>
    </td>
</tr>
</table>
