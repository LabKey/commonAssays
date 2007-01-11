<%@ page import="org.fhcrc.cpas.view.HttpView"%>
<%@ page import="org.fhcrc.cpas.view.JspView"%>
<%@ page import="Nab.NabController"%>
<%@ page extends="org.fhcrc.cpas.jsp.JspBase" %>
<%
    JspView<NabController.HeaderBean> me = (JspView<NabController.HeaderBean>) HttpView.currentView();
    NabController.HeaderBean bean = me.getModel();
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
[<a href="<%= bean.getPrintURL().getLocalURIString() %>" target="_blank">Print View</a>]
<%
    }
    if (bean.getDatafileURL() != null)
    {
%>
[<a href="<%= bean.getDatafileURL().getLocalURIString() %>">Download Datafile</a>]
<%
    }
    if (bean.getCustomizeURL() != null)
    {
%>
[<a href="<%= bean.getCustomizeURL().getLocalURIString() %>">Customize View</a>]
<%
    }
%>
<br>
