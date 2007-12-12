<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.OldMS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    OldMS2Controller.PickNameBean bean = ((JspView<OldMS2Controller.PickNameBean>) HttpView.currentView()).getModelBean();
%>
<form method="get" action="saveView.view" class="dataRegion"><table>
  <tr><td>Name:</td><td class="normal"><input name="name" style="width:200px;">
  <input type=hidden value="<%=h(bean.viewParams)%>" name="viewParams"><input type=hidden value="<%=h(bean.returnUrl)%>" name="returnUrl">
  </td></tr><%
if (bean.canShare)
{ %>
  <tr><td colspan=2><input name=shared type=checkbox> Share view with all users of this folder</td></tr><%
} %>
  <tr><td colspan=2><input type="image" src="<%=PageFlowUtil.buttonSrc("Save View")%>"></td></tr>
</table></form><script for=window event=onload>try {document.getElementById("name").focus();} catch(x){}</script>