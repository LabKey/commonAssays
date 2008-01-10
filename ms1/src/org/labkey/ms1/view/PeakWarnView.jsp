<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%
    JspView me = (JspView)HttpView.currentView();
    ActionURL urlPipeline = new ActionURL("Pipeline", "begin", me.getViewContext().getContainer());
%>
<table cellpadding="2px" width="100%">
    <tr>
        <td style="background-color:#FFF8DC; border:1px solid #8B8878">
            <b>Warning:</b> Peak data for these features are still loading, or may have failed to load.
            See the <b><a href="<%=urlPipeline.getLocalURIString()%>">Pipeline status</a></b> for more information.
        </td>
    </tr>
</table>