<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.view.AdminViewContext" %>
<%
    JspView<AdminViewContext> me = (JspView<AdminViewContext>)HttpView.currentView();
    AdminViewContext ctx = me.getModelBean();
%>

<table>
    <tr>
        <td>Data Files Awaiting Deletion:</td>
        <td><%=ctx.getNumDeleted()%></td>
    </tr>
</table>

<% if(ctx.getNumDeleted() > 0 && (!(ctx.isPurgeRunning()))) { %>
<p>Data marked for deletion will be automatically purged during the scheduled system maintenance process,
but you can manually start a purge now by clicking the button below.
</p>
<p><a href="<%=ctx.getPurgeNowUrl()%>"><%=PageFlowUtil.buttonImg("Purge Deleted MS1 Data Now")%></a></p>
<% } %>

<% if(ctx.isPurgeRunning()) { %>
<p>MS1 data is currently being purged...</p>
<% } %>