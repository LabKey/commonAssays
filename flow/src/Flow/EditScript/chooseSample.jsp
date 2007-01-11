<%@ page import="org.fhcrc.cpas.flow.data.FlowRun" %>
<%@ page import="Flow.EditScript.ScriptController" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.fhcrc.cpas.flow.data.FlowWell"%>
<%@ page import="java.util.List"%>
<%@ page extends="Flow.EditScript.ScriptController.Page" %>
<%  ScriptController.ChooseSampleForm form = (ScriptController.ChooseSampleForm) this.form;
    if (form.redirect != null)
{ %>
<p>
    The '<%=h(form.title)%>' page requires that you choose a particular <%=form.compRequired ? "compensation matrix and " : ""%> sample to use as a reference.
</p>
<% } %>

<%

    FlowRun[] runs = FlowRun.getRunsForContainer(getContainer());
    List<FlowCompensationMatrix> comps = FlowCompensationMatrix.getCompensationMatrices(getContainer());
%>
<% if (runs.length == 0)
{ %>
<p>
    Unfortunately, there are no runs to choose from. Please edit the run definition, and then load at least one run from
    the file system, and come back here.
</p>
<%}
else
{
%>
<form action="<%=formAction(ScriptController.Action.chooseSample)%>" method="POST">

    <p>Which run would you like to use?<br>
        <select name="curRunId">
            <% for (FlowRun run : runs)
            { %>
            <option value="<%=run.getRunId()%>"<%= run.getRunId() == form.curRunId ? " selected" : ""%>><%=h(run.getName())%></option>
            <% } %>
        </select>
    </p>
    <% if (form.run != null)
    {
    %>
    <p>And which well from that run?<br>
        <select name="curWellId">
            <% for (FlowWell well : form.run.getWells())
            { %>
            <option value="<%=well.getWellId()%>"<%=well.getWellId() == form.curWellId ? " selected" : ""%>><%=h(well.getName())%></option>
            <% } %>
        </select>
    </p>
    <% } %>

<% if (comps.size() > 0) { %>
    <p>Which compensation matrix would you like to use?<br>
        <select name="curCompId">
        <% for (FlowCompensationMatrix comp : comps) { %>
                <option value="<%=comp.getRowId()%>"<%=comp.getRowId() == form.curCompId ? " selected" : ""%>><%=h(comp.getLabel())%></option>
        <% } %>
        </select>
    </p>
<% } else if (form.compRequired) { %>
    <p>Unfortunately, there are no compensation matrices to choose from.  Run your analysis at least once to generate a compensation matrix to use.</p>
<%}%>

    <input type="submit" value="Submit">
    <input type="hidden" name="redirect" value="<%=h(form.redirect)%>">
    <input type="hidden" name="title" value="<%=h(form.title)%>">
</form>
<% } %>
