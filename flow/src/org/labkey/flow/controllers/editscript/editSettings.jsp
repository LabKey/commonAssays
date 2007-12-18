<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.editscript.EditSettingsForm" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<% EditSettingsForm form = (EditSettingsForm) this.form;
boolean canEdit = form.canEdit();
String contextPath = request.getContextPath();
%>
<labkey:errors/>
<form action="<%=form.urlFor(ScriptController.Action.editSettings)%>" method="POST">
    <p>These settings apply to both the compensation and analysis steps.</p>

    <p>
        <b>Filter experimental wells by keyword/value pairs:</b><br/>
        You may enter a set of keyword and value pairs which are <i>must</i> be present in the
        FCS header to be included in the calculated compensation matrix and the analysis.  The
        value is a regex pattern as used by Java's
        <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html">Pattern</a> class.
    </p>
    <table>
        <tbody id="criteria.table">
            <tr><th></th><th>Keyword</th><th>Pattern</th></tr>
        <% if (form.ff_criteria_keyword.length == 0) { %>
            <tr id="no_criteria"><td colspan="2"><i>No criteria defined.</i></td></tr>
        <% } else {
            for (int i = 0; i < form.ff_criteria_keyword.length; i++)
            {
                String keyword = form.ff_criteria_keyword[i];
                String pattern = form.ff_criteria_pattern[i];
                %>
                <tr>
                <% if (canEdit) { %>
                    <td><img src="<%=contextPath%>/_images/partdelete.gif" title="Delete Criteria" alt="delete" onclick="deleteCriteria(this);"/></td>
                    <td><input type="text" name="ff_criteria_keyword" value="<%=h(keyword)%>"></td>
                    <td><input type="text" name="ff_criteria_pattern" value="<%=h(pattern)%>"></td>
                <% } else { %>
                    <td/>
                    <td><%=h(keyword)%></td>
                    <td><%=h(pattern)%></td>
                <% } %>
                </tr>
                <%
            }
        } %>
        </tbody>
    </table>

    <% if (canEdit) { %>

        <labkey:button text="Add Criteria" onclick="addNewCriteria();return false;" href="#"/>
        <labkey:button text="Update" />
        <labkey:button text="Cancel" href="<%=form.urlFor(ScriptController.Action.begin)%>" />
    <% } else { %>
        <labkey:button text="Go Back" href="<%=form.urlFor(ScriptController.Action.begin)%>" />
    <% } %>

    <p>
        <b>Edit Minimum Values:</b><br/>
        For each parameter, specify the minimum value.  This value will be used when drawing graphs.
        Also, for the purpose of calculating statistics, and applying gates, values will be constrained to be greater than
        or equal to this minimum value.
    </p>
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

<script type="text/javascript">
    var contextPath = <%=q(contextPath)%>;
</script>
<script type="text/javascript" src="<%=request.getContextPath()%>/Flow/editSettings.js"></script>
