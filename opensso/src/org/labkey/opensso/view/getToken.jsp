<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<form action="showToken.view">
Enter an OpenSSO token:<br>
<input type="text" name="token"><br>
<input type=image src="<%=PageFlowUtil.submitSrc()%>" value="Show">
</form>