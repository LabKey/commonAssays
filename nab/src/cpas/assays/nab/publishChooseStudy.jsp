<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="Nab.NabController" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.PublishBean> me = (JspView<NabController.PublishBean>) HttpView.currentView();
    NabController.PublishBean bean = me.getModel();
%>
<form action="publishVerify.post" method="POST">
    <input type="hidden" name="plateIds" value="<%= bean.isPlateIds() %>">
    <%
        for (Integer plateId : bean.getIds())
        {
    %>
        <input type="hidden" name="id" value="<%= plateId %>">
    <%
        }
    %>
    <table class="normal">
        <tr>
            <td>Choose Target Study:</td>
            <td>
                <select name="targetContainerId">
                <%
                    for (Map.Entry<String,Container> entry : bean.getValidTargets().entrySet())
                    {
                %>
                    <option value="<%= h(entry.getValue().getId()) %>"><%= h(entry.getValue().getPath()) %> (<%= h(entry.getKey()) %>)</option>
                <%
                    }
                %>
                </select>
            </td>
        </tr>
    </table>
    <%= buttonLink("Cancel", "begin.view") %> <%= buttonImg("Next") %>
</form>