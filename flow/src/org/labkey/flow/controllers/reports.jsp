<%@ page import="org.labkey.api.reports.ReportService" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.Report" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.api.reports.report.ReportIdentifier" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="java.util.TreeMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = getViewContext();
    User user = context.getUser();
    Container c = context.getContainer();

    boolean canEdit = c.hasPermission(user, UpdatePermission.class);
    ActionURL editURL = new ActionURL(ReportsController.UpdateAction.class, c);
    ActionURL copyURL = new ActionURL(ReportsController.CopyAction.class, c);

    ReportService.I svc = ReportService.get();
    Report[] all = svc.getReports(user, c);
    TreeMap<String,Report> reports = new TreeMap<String,Report>(String.CASE_INSENSITIVE_ORDER);
    for (Report r : all)
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        reports.put(r.getDescriptor().getReportName(), r);
    }

    %><table><%
    for (Report r : reports.values())
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        ReportDescriptor d = r.getDescriptor();
        String id = d.getReportId().toString();
        editURL.replaceParameter("reportId", id);
        copyURL.replaceParameter("reportId", id);
        %><tr>
        <td><a href="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></a></td><%
        if (canEdit){
        %><td><%=PageFlowUtil.generateButton("edit",editURL)%></td>
        <td><form id="form<%=h(id)%>" method=POST action="<%=h(copyURL.getLocalURIString())%>"><%=PageFlowUtil.generateSubmitButton("copy")%></form></td>
        <%}%>
        </tr><%
    }
    %><tr><td>[<a href="<%=h(new ActionURL(ReportsController.UpdateAction.class,c))%>">create qc report</a>]</td></tr>
    </table>