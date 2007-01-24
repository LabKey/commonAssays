<%@ page import="org.labkey.flow.controllers.protocol.EditFCSAnalysisNameForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% EditFCSAnalysisNameForm form = (EditFCSAnalysisNameForm) __form; %>
<p>
    Use this page to describe how FCSAnalyses should be named.<br>
    The name of an FCSAnalysis can be composed from keywords taken from the FCS file.<br>
</p>
<% if (form.ff_keyword != null)
{
    int selectCount = Math.max(4, form.ff_keyword.length + 2);
    Map<FieldKey, String> options = form.getKeywordFieldMap();
    Map<FieldKey, String> optionsWithNull = new LinkedHashMap();
    optionsWithNull.put(null, "");
    optionsWithNull.putAll(options);
%>
<form method="POST" action="<%=form.getProtocol().urlFor(ProtocolController.Action.editFCSAnalysisName)%>">
    <p>
        Which keywords should be used to compose the FCS analysis name?<br>
        <% FieldKey[] keywords = form.ff_keyword;
        if (keywords == null)
        {
            %>
        <b>Note: the current value of the FCS analysis name expression is too complex.  You probably should use the advanced
        text box below.</b><br>
        <%  keywords = new FieldKey[0];
            } %>

        <% for(int i = 0; i < selectCount; i ++) {
            FieldKey value = null;
            if (i < keywords.length)
            {
                value = keywords[i];
            }
        %>
            <select name="ff_keyword">
                <cpas:options value="<%=value%>" map="<%= i == 0 ? options : optionsWithNull %>" />
            </select><br>
        <% } %>

    </p>
    <cpas:button text="Set names" /> <cpas:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</form>
<% } %>
<hr>
<form method="POST" action="<%=form.getProtocol().urlFor(ProtocolController.Action.editFCSAnalysisName)%>">
    <p>(Advanced) You can also edit the expression that is used to build up the FCS analysis name.
        Use '\${' and '}' to denote substitutions.  Keyword names should be prefixed with 'Keyword'.
        <br>
        <input type="text" width="80" name="ff_rawString" value="<%=h(form.ff_rawString)%>"/>
    </p>
    <cpas:button text="Set Expression" /> <cpas:button text="Cancel" href="<%=form.getProtocol().urlShow()%>" />
</form>