<%@ page import="org.labkey.ms2.MS2Controller.ManageViewsBean" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<ManageViewsBean> me = (HttpView<ManageViewsBean>) HttpView.currentView();
    ManageViewsBean bean = me.getModelBean();
%>
<table class="dataRegion">
  <tr><td>Select one or more views and press Delete</td></tr>
  <tr><td><form method="post" action="<%=bean.postUrl%>">
    <table class="dataRegion" border="0">
      <tr><td><%=bean.select%></td></tr>
      <tr><td align=center><%=buttonImg("Delete")%></td></tr>
    </table>
  </form></td></tr>
</table>