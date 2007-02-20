<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView me = (JspView) HttpView.currentView();
    ViewContext ctx = me.getViewContext();
    ViewURLHelper url = ctx.getViewURLHelper().clone();
    url.setPageFlow("MS2");
    url.setAction("doProteinSearch.view");
    url.deleteParameters();
%>
<form action="<%= url %>" method="get">
    <table>
        <tr>
            <td>Name:</td>
            <td><input size="15" type="text" name="identifier" /> <%= helpPopup("Protein Search: Name", "This is required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from another file. The search will only find exact matches.") %></td>
        </tr>
        <tr>
            <td>Prob &gt;=</td>
            <td><input type="text" size="3" name="minimumProbability" /> <%= helpPopup("Protein Search: Probability", "If entered, only ProteinProphet protein groups that have an associated probability greater than or equal to the value will be included.") %></td>
        </tr>
        <tr>
            <td>Error &lt;=</td>
            <td><input type="text" size="3" name="maximumErrorRate" /> <%= helpPopup("Protein Search: Error Rate", "If entered, only ProteinProphet protein groups that have an associated error rate less than or equal to the value will be included.") %></td>
        </tr>
        <tr>
            <td>Subfolders:</td>
            <td><input type="checkbox" name="includeSubfolders" checked="true" /> <%= helpPopup("Protein Search: Subfolders", "If checked, the search will also look in all of this folder's children.") %></td>
        </tr>
        <tr>
            <td></td>
            <td><cpas:button text="Search" /></td>
        </tr>
    </table>
</form>