<%
/*
 * Copyright (c) 2005-2014 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<MS2Controller.LoadAnnotForm> me = (JspView<MS2Controller.LoadAnnotForm>) HttpView.currentView();
    MS2Controller.LoadAnnotForm bean = me.getModelBean();
%>
<labkey:errors />
<br>
<labkey:form method="post" action="<%=h(buildURL(MS2Controller.InsertAnnotsAction.class))%>" enctype="multipart/form-data">
<table class="lk-fields-table">
    <tr>
      <td class="labkey-form-label">Full file path</td>
      <td><input type="text" name="fileName" id='fname' size="70" value="<%= h(bean.getFileName())%>"></td>
    </tr>
    <tr>
      <td class="labkey-form-label">Comment</td>
      <td><input type='text' name='comment' size='70' value="<%= h(bean.getComment())%>"></td>
    </tr>
    <tr>
      <td class="labkey-form-label">Type</td>
      <td>
       <select name='fileType' onchange="document.getElementById('fastaGuess').style.display = ('fasta' == this.value ? 'table-row' : 'none'); document.getElementById('fastaOrganism').style.display = ('fasta' == this.value ? 'table-row' : 'none'); document.getElementById('uniprotOnly').style.display = 'uniprot' == this.value ? 'table-row' : 'none';">
          <option value='uniprot' <% if ("uniprot".equals(bean.getFileType())) { %>selected<% } %>>uniprot</option>
          <option value='fasta' <% if ("fasta".equals(bean.getFileType())) { %>selected<% } %>>fasta</option>
       </select>
      </td>
    </tr>
    <tr id="fastaOrganism" style="display: <%= "fasta".equals(bean.getFileType()) ? "table-row" : "none" %>;">
       <td class="labkey-form-label">Default Organism</td>
       <td><input type='text' name='defaultOrganism' size='50' value='<%= bean.getDefaultOrganism() %>'></td>
    </tr>
    <tr id="fastaGuess" style="display: <%= "fasta".equals(bean.getFileType()) ? "table-row" : "none" %>;">
       <td></td>
       <td><input type='checkbox' name='shouldGuess' <%=checked("1".equals(bean.getShouldGuess()))%> value='1'> Try to guess organism</td>
    </tr>
    <tr id="uniprotOnly" style="display: <%= "uniprot".equals(bean.getFileType()) ? "table-row" : "none" %>;">
        <td></td>
        <td>
           <input type='checkbox' name='clearExisting'<%=checked(bean.isClearExisting())%>>Clear existing identifiers and annotations<%= helpPopup("Clear Existing", "By default, LabKey Server will merge protein identifiers and annotations with the ones it has already loaded. By checking this box, you can make the server clear out any existing identifiers and annotations it might have, so that the ones in the new file replace them.")%>
        </td>
    </tr>
    <tr>
      <td/>
      <td><%= button("Load Annotations").submit(true) %></td>
   </tr>
</table>
</labkey:form>
<script for=window event=onload>
try {document.getElementById("fname").focus();} catch(x){}
</script>