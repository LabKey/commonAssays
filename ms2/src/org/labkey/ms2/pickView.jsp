<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PickViewBean bean = ((JspView<MS2Controller.PickViewBean>)HttpView.currentView()).getModelBean();
%><table class="dataRegion">
  <tr><td><%=h(bean.viewInstructions)%><br><br>
To define a view: display a run, filter the run, click "Save View", and pick a name.  The filter you've set up will be saved with the view and the name will then appear in the list below.</td></tr>
  <tr><td><form method="get" action="<%=h(bean.nextUrl)%>">
    <table class="dataRegion" border="0">
      <tr><td><%=bean.select%></td></tr>
      <tr><td><input type="hidden" name="runList" value="<%=bean.runList%>"></td></tr>
<%=bean.extraHtml%>
      <tr><td><br><input type="image" src="<%=PageFlowUtil.buttonSrc("Go")%>" name="submit"></td></tr>
    </table></form></td>
  </tr>
</table>