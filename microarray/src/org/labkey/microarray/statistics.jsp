<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.microarray.MicroarrayStatisticsView" %>
<%@ page import="org.labkey.api.view.JspView" %><%
    JspView<MicroarrayStatisticsView.MicroarraySummaryBean> view = (JspView<MicroarrayStatisticsView.MicroarraySummaryBean>) HttpView.currentView();
    MicroarrayStatisticsView.MicroarraySummaryBean bean = view.getModelBean();
%>
There
<%
if (bean.getRunCount() == 1)
{ %>
    is <%= bean.getRunCount() %> hyb <%
}
else
{ %>
are <%= bean.getRunCount() %> hybs <%
} %>
in this folder.
