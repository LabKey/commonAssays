<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.common.util.Pair" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.CurrentFilterView.CurrentFilterBean bean = ((MS2Controller.CurrentFilterView) HttpView.currentView()).getModelBean();
%>
  <table class="dataRegion" border="0"><col width="10%"><col width="90%"><%
    if (null != bean.headers)
    {
        for (String header : bean.headers)
        { %>
    <tr><td colspan=2><%=header%></td></tr><%
        } %>
    <tr><td colspan=2>&nbsp;</td></tr><%
    }

    if (null != bean.sqlSummaries)
    {
        for (Pair<String, String> sqlSummary : bean.sqlSummaries)
        { %>
    <tr><td><%=sqlSummary.getKey().replaceAll(" ", "&nbsp;")%>:</td><td><%=sqlSummary.getValue()%></td></tr><%
        }
    } %>
  </table>
