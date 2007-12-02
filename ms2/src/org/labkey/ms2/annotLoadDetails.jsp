<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    Map<String, String> map = ((JspView<Map<String, String>>) HttpView.currentView()).getModelBean();
%>
&nbsp;<br>
<font size='+1'>
Annotation Load Details for <%=map.get("filetype")%> file <%=map.get("filename")%> (ID #<%=map.get("insertid")%>)<br>
</font>
<br>
<blockquote style='padding-left: 2.0in'>
<i>
<%=h(map.get("comment"))%>
</i>
</blockquote>
<br>
First entered at <%=h(map.get("insertdate"))%><br><br>
<table cellspacing='15'>
<tr>
   <td>&nbsp;</td><td>Current</td><td align='center'>Most Recent Batch<br><%=h(map.get("changedate"))%></td>
</tr>
<tr><td colspan='3'><hr></td></tr>
<tr>
   <td align='left'>Records</td><td align='right'><%=map.get("recordsprocessed")%></td><td align='right'><%=map.get("mrmsize")%></td>
</tr>
<tr>
   <td align='left'>Organisms</td><td align='right'><%=map.get("organismsadded")%></td><td align='right'><%=map.get("mrmorganismsadded")%></td>
</tr>
<tr>
   <td align='left'>Sequences</td><td align='right'><%=map.get("sequencesadded")%></td><td align='right'><%=map.get("mrmsequencesadded")%></td>
</tr>
<tr>
   <td align='left'>Identifiers</td><td align='right'><%=map.get("identifiersadded")%></td><td align='right'><%=map.get("mrmidentifiersadded")%></td>
</tr>
<tr>
   <td align='left'>Annotations</td><td align='right'><%=map.get("annotationsadded")%></td><td align='right'><%=map.get("mrmannotationsadded")%></td>
</tr>
<tr><td colspan='3'><hr></td></tr>
<tr>
   <td align='left'>Batches Processed</td><td align='right'><%=map.get("mouthsful")%></td>
</tr>
</table>
<br><br>
Completed at <%=map.get("completiondate")%><br>
