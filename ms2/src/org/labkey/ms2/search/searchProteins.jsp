<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>

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
            <td colspan="2"><input size="15" type="text" name="identifier" /></td>
        </tr>
        <tr>
            <td align="center" colspan="2">Criteria</td>
        </tr>
        <tr>
            <td>Probability &gt;=</td>
            <td><input type="text" size="3" name="minimumProbability" /></td>
        </tr>
        <tr>
            <td>Error rate &lt;=</td>
            <td><input type="text" size="3" name="maximumErrorRate" /></td>
        </tr>
        <tr>
            <td></td>
            <td><cpas:button text="Go" /></td>
        </tr>
    </table>
</form>