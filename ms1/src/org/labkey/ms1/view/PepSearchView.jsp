<%@ page import="org.labkey.ms1.model.PepSearchModel" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.MS1Controller" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.exp.api.ExpRun" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PepSearchModel> me = (JspView<PepSearchModel>) HttpView.currentView();
    PepSearchModel model = me.getModelBean();
%>

<form action="" method="get">
    <table border="0" cellpadding="2" cellspacing="2">
        <tr>
            <td class="ms-searchform">Peptide Sequence
                <%=helpPopup("Peptide Sequence", "You may sepcify multiple peptide sequences, separated by a commas.")%>
            </td>
            <td>
                <input name="pepSeq" size="40" value="<%=model.getPepSequence()%>"/>
            </td>
        </tr>
        <tr>
            <td class="ms-searchform">Options</td>
            <td>
                <input type="checkbox" name="<%=MS1Controller.PepSearchForm.Parameters.starts.name()%>"/>Include peptides that start with the specified sequence<br/>
                <input type="checkbox" name="<%=MS1Controller.PepSearchForm.Parameters.ignoreMods.name()%>"/>Ignore modifications (e.g., "-.^")
            </td>
        </tr>
    </table>
    <p><input type="image" name="submit" src="<%=PageFlowUtil.buttonSrc("Search")%>"/></p>
</form>
