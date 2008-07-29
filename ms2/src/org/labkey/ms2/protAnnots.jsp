<%
/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.AnnotViewBean bean = ((JspView<MS2Controller.AnnotViewBean>) HttpView.currentView()).getModelBean();
%>
<table>
<tr>
   <td align="left">Sequence name:</td><td><%=h(bean.seqName)%></td>
</tr>
<tr>
   <td align="left">Description:</td><td><%=h(bean.seqDesc)%></td>
</tr>
<tr>
   <td align="left">Gene Name(s):</td><td><%=bean.geneName%><% // geneName is either an href or empty string, so don't filter %></td>
</tr>
<tr>
   <td align="left">Organisms(s):</td>
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
<table class="labkey-prot-annots">
<tr>
   <th>Genbank IDs</th><th>GIs</th><th>Swiss-Prot Accessions</th><th>Swiss-Prot Names</th><th>Ensembl</th><th>IPI numbers</th><th>GO Categories</th>
</tr>
<tr valign="top">
   <td><%
    for(String id : bean.genBankUrls)
        out.println(id + "<br>");
    %>
   </td>
    <td><%
     for(String id : bean.GIs)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     for(String id : bean.swissProtAccns)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     for(String id : bean.swissProtNames)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     for(String id : bean.ensemblIds)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     for(String id : bean.IPI)
         out.println(id + "<br>");
     %>
    </td>
    <td><%
     for(String id : bean.goCategories)
         out.println(id + "<br>");
     %>
    </td>
</tr>
</table>
