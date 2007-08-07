<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.HeaderBean> me = (JspView<org.labkey.nab.NabController.HeaderBean>) HttpView.currentView();
    NabController.HeaderBean bean = me.getModelBean();
%>
<%
    if (bean.showNewRunLink())
    {
%>
<%= textLink("New Run", "begin.view?.lastFilter=1") %>
<%
    }
%>
<%= textLink("Previous Runs", "runs.view?.lastFilter=1") %>
<%= textLink("Previous Runs By Sample", "sampleList.view?.lastFilter=1") %>
<%
    if (bean.showPrintView())
    {
%>
[<a href="<%= bean.getPrintURL().getEncodedLocalURIString() %>" target="_blank">Print View</a>]
<%
    }
    if (bean.getDatafileURL() != null)
    {
%>
[<a href="<%= bean.getDatafileURL().getEncodedLocalURIString() %>">Download Datafile</a>]
<%
    }
    if (bean.getCustomizeURL() != null)
    {
%>
[<a href="<%= bean.getCustomizeURL().getEncodedLocalURIString() %>">Customize View</a>]
<%
    }
%>
<br>
<span class="labkey-error"><%=PageFlowUtil.getStrutsError(request, "main")%></span>
