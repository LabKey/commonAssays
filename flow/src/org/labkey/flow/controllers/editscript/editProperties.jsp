<%@ page import="org.labkey.flow.controllers.editscript.EditPropertiesForm"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController.Action" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% EditPropertiesForm form = (EditPropertiesForm) getForm(); %>
<form action="<%=h(form.urlFor(Action.editProperties))%>" method="POST">
    <p>Description:<br>
        <textarea rows="5" cols="40" name="ff_description"><%=h(form.ff_description)%></textarea>
    </p>
    <cpas:button text="Update" /> <cpas:button text="Cancel" href="<%=form.urlFor(Action.begin)%>" /> 
</form>