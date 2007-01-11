<%@ page import="org.fhcrc.cpas.view.HttpView"%>
<%@ page import="org.fhcrc.cpas.view.ViewContext"%>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page extends="org.fhcrc.cpas.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
%>
Hello, and welcome to the @@MODULE_NAME@@ module.