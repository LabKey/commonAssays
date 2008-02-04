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
MS2Controller.PeptideFilteringComparisonForm form = bean.getForm();
%>
<form action="<%= bean.getTargetURL() %>">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <p>This comparison view is based on ProteinProphet data so the runs must be associated with ProteinProphet data.
        All proteins in all ProteinProphet protein groups will be shown in the comparison, subject to the filter criteria.</p>
    <p>There are three options for filtering the peptides that contribute evidence to the protein groups:</p>
    <p><input type="radio" name="peptideFilterType" value="none" <%= form.isNoPeptideFilter() ? "checked=\"true\"" : "" %> /> Use all the peptides</p>
    <p><input type="radio" name="peptideFilterType" value="peptideProphet" <%= form.isPeptideProphetFilter() ? "checked=\"true\"" : "" %>/> All peptides with PeptideProphet probability &ge; <input type="text" size="2" name="peptideProphetProbability" value="<%= form.getPeptideProphetProbability() == null ? "" : form.getPeptideProphetProbability() %>" /></p>
    <p><input type="radio" name="peptideFilterType" value="customView" <%= form.isCustomViewPeptideFilter() ? "checked=\"true\"" : "" %>/>
        Use a customized Peptides view to establish criteria for which peptides to include in the comparison.
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
