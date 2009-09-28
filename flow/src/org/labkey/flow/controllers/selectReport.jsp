<%@ page import="org.labkey.api.reports.ReportService" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.Report" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="org.labkey.api.reports.report.ReportIdentifier" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = getViewContext();
    User user = context.getUser();
    Container c = context.getContainer();

    ReportIdentifier id = ((ReportsController.IdForm) HttpView.currentModel()).getReportId();

    ReportService.I svc = ReportService.get();
    Report[] all = svc.getReports(user, c);
    TreeMap<String,Report> reports = new TreeMap<String,Report>(String.CASE_INSENSITIVE_ORDER);
    for (Report r : all)
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        reports.put(r.getDescriptor().getReportName(), r);
    }
    %><select onchange="window.location=this.value"><%
    for (Report r : reports.values())
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        ReportDescriptor d = r.getDescriptor();
        boolean selected = id != null && id.equals(d.getReportId());
        %><option <%=selected?"selected":""%> value="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></option><%
    }
    %></select>