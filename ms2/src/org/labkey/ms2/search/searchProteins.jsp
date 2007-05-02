<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.ms2.search.ProteinSearchBean" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModel();
    ViewContext ctx = me.getViewContext();
    ViewURLHelper url = ctx.getViewURLHelper().clone();
    url.setPageFlow("MS2");
    url.setAction("doProteinSearch.view");
    url.deleteParameters();

    ViewURLHelper annotationsURL = ctx.getViewURLHelper().clone();
    annotationsURL.setPageFlow("protein");
    annotationsURL.setAction("begin.view");
    annotationsURL.deleteParameters();

    String separator = bean.isHorizontal() ? "<td>&nbsp;</td>" : "</tr><tr>";
%>
<form action="<%= annotationsURL %>" method="get">
    <table>
        <tr>
            <td>Name:</td>
            <td><input size="15" type="text" name="identifier" value="<%= h(bean.getIdentifier()) %>"/> <%= helpPopup("Protein Search: Name", "Required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from an annotations file. You may comma separate multiple names.") %></td>
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
            <td>Exact:</td>
            <td><input type="checkbox" name="exactMatch" <% if (bean.isExactMatch()) { %>checked="true" <% } %> /> <%= helpPopup("Protein Search: Exact Match", "If checked, the search will only find proteins with an exact name match. If not checked, proteins that start with the name entered will also match, but the search may be significantly slower.") %></td>
        <%= separator %>
            <td></td>
            <td><labkey:button text="Search" /></td>
        <%= separator %>
            <td></td>
            <td>[<a href="<%= annotationsURL.getLocalURIString() %>">manage annotations</a>]</td>
        </tr>
    </table>
</form>