<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.OldMS2Controller" %>
<%@ page import="java.text.Format" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    OldMS2Controller.ProteinViewBean bean = ((JspView<OldMS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
    Format intFormat = Formats.commaf0;
    Format percentFormat = Formats.percent;
%>
<table class="dataRegion" width=100%>
    <col width=15%><col width=85%>
    <tr><td>&nbsp;</td><td>&nbsp;</td></tr>
    <tr><td>Sequence Mass:</td><td><%=h(intFormat.format(bean.protein.getMass()))%></td></tr><%

    if (bean.showPeptides)
    { %>
    <tr><td>AA Coverage:</td><td><%=h(percentFormat.format(bean.protein.getAAPercent()))%> (<%=intFormat.format(bean.protein.getAACoverage())%> / <%=intFormat.format(bean.protein.getSequence().length())%>)</td></tr>
    <tr><td>Mass Coverage:</td><td><%=h(percentFormat.format(bean.protein.getMassPercent()))%> (<%=intFormat.format(bean.protein.getMassCoverage())%> / <%=intFormat.format(bean.protein.getMass())%>)</td></tr><%
    } %>
    <tr><td>&nbsp;</td><td>&nbsp;</td></tr>
    <tr><td colspan=2><big><tt><%=bean.protein.getFormattedSequence()%></tt></big></td></tr>
</table>

<script language="javascript 1.1" type="text/javascript">
    function grabFocus()
    {
        self.focus();
    }
    window.onload = grabFocus;
</script>