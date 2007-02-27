<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.ms2.search.ProteinSearchBean" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModel();
    ViewContext ctx = me.getViewContext();
    ViewURLHelper url = ctx.getViewURLHelper().clone();
    url.setPageFlow("MS2");
    url.setAction("doProteinSearch.view");
    url.deleteParameters();
    String separator = bean.isHorizontal() ? "<td>&nbsp;&nbsp;</td>" : "</tr><tr>";
%>
<form action="<%= url %>" method="get">
    <table>
        <tr>
            <td>Name:</td>
            <td><input size="15" type="text" name="identifier" value="<%= h(bean.getIdentifier()) %>"/> <%= helpPopup("Protein Search: Name", "This is required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from another file. The search will only find exact matches.") %></td>
        <%= separator %>
            <td>Prob &gt;=</td>
            <td><input type="text" size="3" name="minimumProbability" <% if (bean.getMinProbability() != null ) { %>value="<%= bean.getMinProbability()%>"<% } %>/> <%= helpPopup("Protein Search: Probability", "If entered, only ProteinProphet protein groups that have an associated probability greater than or equal to the value will be included.") %></td>
        <%= separator %>
            <td>Error &lt;=</td>
            <td><input type="text" size="3" name="maximumErrorRate"  <% if (bean.getMaxErrorRate() != null ) { %>value="<%= bean.getMaxErrorRate()%>"<% } %>/> <%= helpPopup("Protein Search: Error Rate", "If entered, only ProteinProphet protein groups that have an associated error rate less than or equal to the value will be included.") %></td>
        <%= separator %>
            <td>Subfolders:</td>
            <td><input type="checkbox" name="includeSubfolders" <% if (bean.isIncludeSubfolders()) { %>checked="true" <% } %> /> <%= helpPopup("Protein Search: Subfolders", "If checked, the search will also look in all of this folder's children.") %></td>
        <%= separator %>
            <td></td>
            <td><cpas:button text="Search" /></td>
        </tr>
    </table>
</form>