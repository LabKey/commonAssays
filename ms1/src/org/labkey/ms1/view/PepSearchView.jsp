<%@ page import="org.labkey.ms1.model.PepSearchModel" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PepSearchModel> me = (JspView<PepSearchModel>) HttpView.currentView();
    PepSearchModel model = me.getModelBean();
%>

<form action="<%=model.getResultsUri()%>" method="get">
    <p>Peptide Sequence:
        <% //TODO: use enums from target form for parameter names %>
        <input type="text" name="pepSeq"
               value="<%=model.getPepSeq()%>" size="40"/>
        <%=helpPopup("Peptide Sequence", "Enter the peptide sequence to find, or multiple sequences separated by commas. If you enter a sequence containing modifiers, an exact match will be assumed.")%>
        &nbsp;
        <input id="cbxExact" type="checkbox" name="exact" style="vertical-align:middle" <%=model.isExact() ? "checked" : ""%> />
        <label for="cbxExact">Exact Match</label>
        <%=helpPopup("Exact Match", "If checked, the search will match the peptides exactly; if unchecked, it will match any peptide that starts with the specified sequence and ignore modifications.")%>
        &nbsp;
        <input type="image" name="submit" src="<%=PageFlowUtil.buttonSrc("Search")%>" style="vertical-align:middle"/>
    </p>
</form>
