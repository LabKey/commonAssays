<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.controllers.run.RunForm" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% RunForm form = (RunForm) __form;
    FlowRun run = form.getRun();
%>
<labkey:errors />
<form action="<%=h(run.urlFor(RunController.Action.moveToWorkspace))%>" method="POST">
    <p>After you move this run into the workspace, you will be able to edit the gates on individual FCS files.</p>
    <labkey:button text="Move" />
    <labkey:button text="Cancel" href="<%=run.urlShow()%>" />
</form>