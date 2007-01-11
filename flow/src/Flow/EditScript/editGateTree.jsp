<%@ page import="Flow.EditScript.ScriptController"%>
<%@ page import="com.labkey.flow.web.SubsetSpec"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page extends="Flow.EditScript.ScriptController.Page" %>

<%! String indent(SubsetSpec subset)
    {
        return StringUtils.repeat("+", subset.getSubsets().length);
    }
    %>
<% ScriptController.EditGateTreeForm form = (ScriptController.EditGateTreeForm) getForm(); %>
<p>
    Use this page to rename populations.  To delete a population, delete its name.<br>
    Use the <a href="<%=urlFor(ScriptController.Action.editGates)%>">edit gates</a> page to define new populations.
</p>
<form class="normal" action="<%=formAction(ScriptController.Action.editGateTree)%>" method="POST">
    <%
        for (int i = 0; i < form.populationNames.length; i ++)
        {
    %>
    <%=indent(form.subsets[i])%> <input type="text" name="populationNames[<%=i%>]" value="<%=h(form.populationNames[i])%>"><br>
    <% } %>
    <input type="submit" value="Update">
</form>
