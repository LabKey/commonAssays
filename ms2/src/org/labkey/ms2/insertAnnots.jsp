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
<form method="post" action="insertAnnots.post" enctype="multipart/form-data">
<table border="0">
    <tr>
      <td>Full file path:</td>
      <td><input type="text" name="fileName" id='fname' size="70" value="<%= h(bean.getFileName())%>"></td>
    </tr>
    <tr>
      <td>Comment:</td>
      <td><input type='text' name='comment' size='70' value="<%= h(bean.getComment())%>"></td>
    </tr>
    <tr>
      <td>Type:</td>
      <td>
       <select name='fileType' onchange="document.getElementById('fastaOnly').style.display = 'fasta' == this.value ? 'block' : 'none'; document.getElementById('uniprotOnly').style.display = 'uniprot' == this.value ? 'block' : 'none';">
          <option value='uniprot' <% if ("uniprot".equals(bean.getFileType())) { %>selected<% } %>>uniprot</option>
          <option value='fasta' <% if ("fasta".equals(bean.getFileType())) { %>selected<% } %>>fasta</option>
       </select>
      </td>
    </tr>
    <tr>
        <td/>
        <td>
            <table id="fastaOnly" style="display: <%= "fasta".equals(bean.getFileType()) ? "block" : "none" %>;">
                <tr>
                   <td>Try to guess organism?</td>
                   <td><input type='checkbox' name='shouldGuess' <%= "1".equals(bean.getShouldGuess()) ? "checked" : ""%> value='1'></td>
                </tr>
                <tr>
                   <td>Default Organism:</td>
                   <td><input type='text' name='defaultOrganism' size='50' value='<%= bean.getDefaultOrganism() %>'></td>
                </tr>
            </table>
            <table id="uniprotOnly" style="display: <%= "uniprot".equals(bean.getFileType()) ? "block" : "none" %>;">
                <tr>
                   <td>Clear existing identifiers and annotations?</td>
                   <td><input type='checkbox' name='clearExisting' <%= bean.isClearExisting() ? "checked" : ""%>><%= helpPopup("Clear Existing", "By default, LabKey Server will merge protein identifiers and annotations with the ones it has already loaded. By checking this box, you can make the server clear out any existing identifiers and annotations it might have, so that the ones in the new file replace them.")%></td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
      <td/>
      <td><input type="image" src="<%= PageFlowUtil.buttonSrc("Load Annotations") %>" /></td>
   </tr>
</table>
</form>
<script for=window event=onload>
try {document.getElementById("fname").focus();} catch(x){}
</script>