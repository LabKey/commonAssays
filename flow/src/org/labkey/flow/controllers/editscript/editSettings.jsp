<%@ page import="org.labkey.flow.controllers.editscript.EditSettingsForm" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<% EditSettingsForm form = (EditSettingsForm) this.form;
boolean canEdit = form.canEdit();
%>
<labkey:errors/>
<form action="<%=form.urlFor(ScriptController.Action.editSettings)%>" method="POST">
    <p>These settings apply to both the compensation and analysis steps.</p>
    <p>For each parameter, specify the minimum value.  This value will be used when drawing graphs.
        Also, for the purpose of calculating statistics, and applying gates, values will be constrained to be greater than
    or equal to this minimum value.</p>
    <table>
        <tr><th>Parameter</th><th>Minimum Value</th></tr>
        <% for (int i = 0; i < form.ff_parameter.length; i ++) {
        String parameter = form.ff_parameter[i];
        String minValue = form.ff_minValue[i];
        %>
        <tr><td><%=h(parameter)%><input type="hidden" name="ff_parameter" value="<%=h(parameter)%>"></td>
            <td><% if (canEdit) { %>
                <input type="text" name="ff_minValue" value="<%=h(minValue)%>">
                <% } else { %>
                <%=h(minValue)%>
                <% } %>
                </td>
        </tr>
        <% } %>
    </table>


<% if (canEdit) { %>
    <labkey:button text="Update" />
    <labkey:button text="Cancel" href="<%=form.urlFor(ScriptController.Action.begin)%>" />
<% } else { %>
    <labkey:button text="Go Back" href="<%=form.urlFor(ScriptController.Action.begin)%>" /> 
<% } %>
</form>