<%
/*
 * Copyright (c) 2005-2013 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.protein.AnnotationInsertion" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    AnnotationInsertion insertion = ((JspView<AnnotationInsertion>) HttpView.currentView()).getModelBean();
%>
<p><%=h(insertion.getComment())%></p>
<p>Upload started at <%=formatDateTime(insertion.getInsertDate())%>, <%=insertion.getMouthsful()%> batch<%=unsafe(insertion.getMouthsful() != 1 ? "es" : "")%> processed</p>
<table>
<tr>
   <td>&nbsp;</td><td align="right" class="labkey-form-label"><strong>Total</strong></td><td align='right' class="labkey-form-label"><strong>Most Recent Batch</strong></td>
</tr>
<tr>
   <td align='left' class="labkey-form-label">Completed</td><td align='right'><%=insertion.getCompletionDate() == null ? h("Not complete") : formatDateTime(insertion.getCompletionDate())%></td><td align='right'><%=formatDateTime(insertion.getChangeDate())%></td>
</tr>
<tr>
   <td align='left' class="labkey-form-label">Records</td><td align='right'><%=insertion.getRecordsProcessed()%></td><td align='right'><%=h(insertion.getMrmSize())%></td>
</tr>
<tr>
   <td align='left' class="labkey-form-label">Organisms</td><td align='right'><%=insertion.getOrganismsAdded()%></td><td align='right'><%=h(insertion.getMrmOrganismsAdded())%></td>
</tr>
<tr>
   <td align='left' class="labkey-form-label">Sequences</td><td align='right'><%=insertion.getSequencesAdded()%></td><td align='right'><%=h(insertion.getMrmSequencesAdded())%></td>
</tr>
<tr>
   <td align='left' class="labkey-form-label">Identifiers</td><td align='right'><%=insertion.getIdentifiersAdded()%></td><td align='right'><%=h(insertion.getMrmIdentifiersAdded())%></td>
</tr>
<tr>
   <td align='left' class="labkey-form-label">Annotations</td><td align='right'><%=insertion.getAnnotationsAdded()%></td><td align='right'><%=h(insertion.getMrmAnnotationsAdded())%></td>
</tr>
</table>
