<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.action.ReturnUrlForm" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.RenameBean bean = ((JspView<MS2Controller.RenameBean>) HttpView.currentView()).getModelBean();
%>
<form action="renameRun.post" method="post">
<input type="hidden" name="<%=ReturnUrlForm.Params.returnUrl%>" value="<%=h(bean.returnURL)%>"/>
<input type="hidden" name="run" value="<%=bean.run.getRun()%>"/>
<table class="dataRegion">
    <tr>
        <td class='ms-searchform'>Description:</td>
        <td class='ms-vb'><input type="text" size="70" name="description" id="description" value="<%=h(bean.description)%>"/></td>
    </tr>
    <tr>
        <td colspan="2"><input type="image" src="<%=PageFlowUtil.buttonSrc("Rename")%>"/> <%=PageFlowUtil.buttonLink("Cancel", bean.returnURL)%></td>
    </tr>
</table>
</form>