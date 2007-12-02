<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%
    MS2Controller.RenameBean bean = ((JspView<MS2Controller.RenameBean>) HttpView.currentView()).getModelBean();
%>
<form action="rename.post" method="post">
<input type="hidden" name="run" value="<%=bean.run.getRun()%>"/>
<table class="dataRegion">
    <tr>
        <td class='ms-searchform'>Description:</td>
        <td class='ms-vb'><input type="text" size="70" name="description" value="<%=bean.description%>"/></td>
    </tr>
    <tr>
        <td colspan="2"><input type="image" src="<%=PageFlowUtil.buttonSrc("Rename")%>"/></td>
    </tr>
</table>
</form>