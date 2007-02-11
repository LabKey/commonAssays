<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<cpas:errors />
<form method="POST" action="<%=formAction(ScriptController.Action.uploadCompensationCalculation)%>"
      enctype="multipart/form-data">
    <p>
        The compensation calculation tells <%=FlowModule.getLongProductName()%> how
        to identify the compensation controls in an experiment run, and what gates
        to apply.  A compensation control is identified as having a particular value
        for a specific keyword.
    </p>

    <p>
        You can define the compensation calculation by uploading a Flow Jo XML workspace<br>
        <input type="file" name="workspaceFile"><br>
        Important: This workspace must contain only one set of compensation controls, or you will not be able to select
        the keywords that you want to.
    </p>
        <input type="submit" value="Submit">
</form>
