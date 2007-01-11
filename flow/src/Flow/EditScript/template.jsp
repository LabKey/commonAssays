<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page extends="Flow.EditScript.TemplatePage" %>
<%=PageFlowUtil.getStrutsError(request, null)%>
<%renderBody(out);%>