<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page extends="org.labkey.flow.controllers.editscript.TemplatePage" %>
<%=PageFlowUtil.getStrutsError(request, null)%>
<%renderBody(out);%>