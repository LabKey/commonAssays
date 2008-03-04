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
   <td align="left">Gene Name(s):</td><td><%=h(bean.geneName)%></td>
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
<table cellspacing="10">
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
