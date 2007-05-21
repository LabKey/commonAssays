<%@ page import="org.labkey.flow.data.FlowProtocolStep"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<% ScriptController.CopyProtocolForm form = (ScriptController.CopyProtocolForm) this.form; %>
<form class="normal" action="<%=urlFor(ScriptController.Action.copy)%>" method="POST">
    <p>
        What do you want to call the new script?<br>
        <input type="text" name="name" value="<%=h(form.name)%>">
    </p>
    <p>
        Which sections of the '<%=form.analysisScript.getName()%>' script do you want to copy?<br>
<% if (form.analysisScript.hasStep(FlowProtocolStep.calculateCompensation)) { %>
        <input type="checkbox" name="copyCompensationCalculation" value="true"<%=form.copyCompensationCalculation ? " checked" : ""%>>Compensation Calculation<br>
<% } %>
<% if (form.analysisScript.hasStep(FlowProtocolStep.analysis)) { %>
        <input type="checkbox" name="copyAnalysis" value="true"<%=form.copyAnalysis ? " checked" : ""%>>Analysis<br>
<% } %>
    </p>
    <input type="submit" value="Make Copy">

</form>