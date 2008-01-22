<%@ page import="org.labkey.ms2.query.SpectraCountConfiguration" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.labkey.api.query.QueryPicker" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean bean = view.getModelBean();
QueryView peptidesView = bean.getPeptideView();
%>
<form action="<%= bean.getTargetURL() %>">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <p>
        Group by:<br/>
        <%
        boolean first = true;
        for (SpectraCountConfiguration spectraCountConfig : SpectraCountConfiguration.VALID_CONFIGS)
        {
            %><input onclick="document.getElementById('spectraCountRadioButton').checked = true;" type="radio" <%= first ? "checked=\"true\"" : "" %><% first = false; %> name="spectraConfig" value="<%= spectraCountConfig.getTableName()%>" /><%= h(spectraCountConfig.getDescription())%><br/><%
        }
        %>
    </p>
    <p>
        You may use a customized Peptides view to establish criteria for which peptides to include in the spectra counts.
        <%
        QueryPicker picker = peptidesView.getColumnListPicker(request);
        picker.setAutoRefresh(false);
        PrintWriter writer = new PrintWriter(out);
        peptidesView.renderCustomizeViewLink(writer);
        writer.flush();
        %>
        <%= picker.toString()%>
    </p>
    <p><labkey:button text="Go"/></p>
</form>
