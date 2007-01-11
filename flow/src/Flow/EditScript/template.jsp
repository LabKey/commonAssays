<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page extends="Flow.EditScript.TemplatePage" %>
<%=PageFlowUtil.getStrutsError(request, null)%>
<%renderBody(out);%>