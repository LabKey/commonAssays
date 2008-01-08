<%@ page import="org.labkey.ms2.MS2Controller.ManageViewsBean" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<MS2Controller.ManageViewsBean> me = (HttpView<MS2Controller.ManageViewsBean>) HttpView.currentView();
    MS2Controller.ManageViewsBean bean = me.getModelBean();
%>
<table class="dataRegion">
  <tr><td>Select one or more views and press Delete</td></tr>
  <tr><td><form method="post" action="<%=h(bean.postUrl)%>">
    <table class="dataRegion" border="0">
      <tr><td><%=bean.select%></td></tr>
      <tr><td align=center><%=buttonImg("Delete")%></td></tr>
    </table>
  </form></td></tr>
</table>