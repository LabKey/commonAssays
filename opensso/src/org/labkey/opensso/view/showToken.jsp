<%@ page import="com.iplanet.sso.SSOToken"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<SSOToken> me = (HttpView<SSOToken>) HttpView.currentView();
    SSOToken token = me.getModelBean();
%>
<table>
    <tr colspan=2><td>Looks like a valid token to me</td></tr>
    <tr><td>host</td><td><%=h(token.getHostName())%></td></tr>
    <tr><td>principal</td><td><%=h(token.getPrincipal().getName())%></td></tr>
    <tr><td>authType</td><td><%=h(token.getAuthType())%></td></tr>
    <tr><td>level</td><td><%=h(token.getAuthLevel())%></td></tr>
    <tr><td>ipAddress</td><td><%=h(token.getIPAddress().getHostAddress())%></td></tr>
    <tr><td>maxTime</td><td><%=h(token.getMaxSessionTime())%></td></tr>
    <tr><td>idleTime</td><td><%=h(token.getIdleTime())%></td></tr>
    <tr><td>maxIdleTime</td><td><%=h(token.getMaxIdleTime())%></td></tr>
</table>