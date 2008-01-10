<%@ page import="org.labkey.flow.controllers.well.EditWellForm" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowDataType"%>
<%@ page import="org.labkey.flow.data.FlowWell"%>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%  EditWellForm form = (EditWellForm) __form;
FlowWell well = form.getWell();%>
<labkey:errors />
<form method="POST" action="<%=h(well.urlFor(WellController.Action.editWell))%>" class="normal">
    <table>
        <tr><td>Run Name:</td><td><a href="<%= ActionURL.toPathString("Flow-Run", "showRun", getContainer())%>?runId=<%=well.getRun().getRunId()%>"><%=h(well.getRun().getName())%></a></td></tr>
        <tr><td>Well Name:</td><td><input type="text" name="ff_name" value="<%=h(form.ff_name)%>"></td></tr>
        <tr><td>Comment:</td><td><textarea rows="5" cols="40" name="ff_comment"><%=h(form.ff_comment)%></textarea></tr>
<% if (well.getDataType() == FlowDataType.FCSFile) { %>
        <tr><th colspan="2">Keywords:</th></tr>
        <%
            for (int i = 0; i < form.ff_keywordName.length; i ++) { %>
        <tr><td>
            <input type="hidden" name="ff_keywordName" value="<%=h(form.ff_keywordName[i])%>">
            <%=h(form.ff_keywordName[i])%></td><td><input type="text" name="ff_keywordValue" value="<%=h(form.ff_keywordValue[i])%>"></tr>
        <%}
        %>
        <tr><th colspan="2">Create a new keyword:</th></tr>
        <tr><td><input type="text" name="ff_keywordName" value=""></td><td><input type="text" name="ff_keywordValue"></td></tr>
<% } %>
    </table>
    <labkey:button text="update" />
    <labkey:button text="cancel" href="<%=well.urlFor(WellController.Action.showWell)%>"/>
</form>






