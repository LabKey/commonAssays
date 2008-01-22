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
%>
<form action="<%= bean.getTargetURL() %>">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <p>Please follow the instructions at the top of the comparison page to customize the results. It is based on ProteinProphet protein groups, so the runs must be associated with ProteinProphet data.</p>
    <p>
        You may use a customized Peptides view to establish criteria for which peptides to include in the comparison.
        <%
        QueryPicker picker = bean.getPeptideView().getColumnListPicker(request);
        picker.setAutoRefresh(false);
        PrintWriter writer = new PrintWriter(out);
        bean.getPeptideView().renderCustomizeViewLink(writer);
        writer.flush();
        %>
        <%= picker.toString()%>
    </p>
    <p><labkey:button text="Go"/></p>
</form>
