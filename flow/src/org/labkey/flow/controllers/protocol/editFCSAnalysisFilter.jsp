<%@ page import="org.labkey.flow.controllers.protocol.EditFCSAnalysisFilterForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%! void addCompare(Map<String, String> options, CompareType ct)
{
    options.put(ct.getUrlKey(), ct.getDisplayValue()); 
}%>
<% EditFCSAnalysisFilterForm form = (EditFCSAnalysisFilterForm) __form;
    Map<FieldKey, String> fieldOptions = new LinkedHashMap();
    fieldOptions.put(null, "");
    fieldOptions.putAll(form.getKeywordFieldMap());
    Map<String, String> opOptions = new LinkedHashMap();
    addCompare(opOptions, CompareType.EQUAL);
    addCompare(opOptions, CompareType.NEQ_OR_NULL);
    addCompare(opOptions, CompareType.ISBLANK);
    addCompare(opOptions, CompareType.NONBLANK);
    addCompare(opOptions, CompareType.STARTS_WITH);
    addCompare(opOptions, CompareType.CONTAINS);
    int clauseCount = Math.max(form.ff_field.length + 2, 4);
%>
<labkey:errors />
<p>
    Use this page to specify which FCS files should be analyzed.
</p>
<form action="<%=form.getProtocol().urlFor(ProtocolController.Action.editFCSAnalysisFilter)%>" method="POST">
    <table>
        <tr class="wpHeader"><th colspan="3" class="wpTitle" align="left">Only process FCS files where:</th></tr>
        <% for (int i = 0; i < clauseCount; i ++) {
        FieldKey field = null;
        String op = null;
            String value = null;

        if (i < form.ff_field.length)
        {
            field = form.ff_field[i];
            op = form.ff_op[i];
            value = form.ff_value[i];
        }
        %>
            <% if (i != 0) { %>
        <tr><td class="normal">and</td></tr>
            <% } %>
        <tr><td class="normal"><select name="ff_field"><labkey:options value="<%=field%>" map="<%=fieldOptions%>" /> </select></td>
            <td class="normal"><select name="ff_op"><labkey:options value="<%=op%>" map="<%=opOptions%>" /></select></td>
            <td class="normal"><input name="ff_value" type="text" value="<%=h(value)%>" /></td>

        </tr>
        <% } %>

    </table>
    <labkey:button text="Set filter" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</form>
