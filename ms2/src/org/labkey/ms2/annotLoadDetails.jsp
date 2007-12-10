<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.protein.AnnotationInsertion" %>
<%@ page import="org.labkey.api.util.DateUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    AnnotationInsertion insertion = ((JspView<AnnotationInsertion>) HttpView.currentView()).getModelBean();
%>
<p><%=h(insertion.getComment())%></p>
<p>Upload started at <%=h(DateUtil.formatDateTime(insertion.getInsertDate()))%>, <%=insertion.getMouthsful()%> batch<%= insertion.getMouthsful() != 1 ? "es" : "" %> processed</p>
<table>
<tr>
   <td>&nbsp;</td><td align="right" class="ms-searchform"><strong>Total</strong></td><td align='right' class="ms-searchform"><strong>Most Recent Batch</strong></td>
</tr>
<tr>
   <td align='left' class="ms-searchform">Completed</td><td align='right'><%=insertion.getCompletionDate() == null ? "Not complete" : h(DateUtil.formatDateTime(insertion.getCompletionDate()))%></td><td align='right'><%=h(DateUtil.formatDateTime(insertion.getChangeDate()))%></td>
</tr>
<tr>
   <td align='left' class="ms-searchform">Records</td><td align='right'><%=insertion.getRecordsProcessed()%></td><td align='right'><%=insertion.getMrmSize()%></td>
</tr>
<tr>
   <td align='left' class="ms-searchform">Organisms</td><td align='right'><%=insertion.getOrganismsAdded()%></td><td align='right'><%=insertion.getMrmOrganismsAdded()%></td>
</tr>
<tr>
   <td align='left' class="ms-searchform">Sequences</td><td align='right'><%=insertion.getSequencesAdded()%></td><td align='right'><%=insertion.getMrmSequencesAdded()%></td>
</tr>
<tr>
   <td align='left' class="ms-searchform">Identifiers</td><td align='right'><%=insertion.getIdentifiersAdded()%></td><td align='right'><%=insertion.getMrmIdentifiersAdded()%></td>
</tr>
<tr>
   <td align='left' class="ms-searchform">Annotations</td><td align='right'><%=insertion.getAnnotationsAdded()%></td><td align='right'><%=insertion.getMrmAnnotationsAdded()%></td>
</tr>
</table>
