<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.SaveViewBean bean = ((JspView<MS2Controller.SaveViewBean>) HttpView.currentView()).getModelBean();
%>
<form method="post" action="saveView.view" class="dataRegion">
    <table>
        <tr>
            <td>Name:</td>
            <td class="normal">
                <input name="name" id="name" style="width:200px;">
                <input type=hidden value="<%=h(bean.viewParams)%>" name="viewParams">
                <input type=hidden value="<%=h(bean.returnURL)%>" name="returnUrl">
            </td>
        </tr><%
if (bean.canShare)
{ %>
        <tr>
            <td colspan=2><input name=shared type=checkbox> Share view with all users of this folder</td>
        </tr><%
} %>
        <tr>
            <td colspan=2><input type="image" src="<%=PageFlowUtil.buttonSrc("Save View")%>"> <%=PageFlowUtil.buttonLink("Cancel", bean.returnURL)%></td>
        </tr>
    </table>
</form>