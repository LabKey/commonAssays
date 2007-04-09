<%@ page import="org.labkey.ms2.protein.CustomAnnotationType" %>
<%@ page import="org.labkey.ms2.protein.ProteinController" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>

<%
    JspView<ProteinController.UploadAnnotationsForm> me = (JspView<ProteinController.UploadAnnotationsForm>) HttpView.currentView();
    ProteinController.UploadAnnotationsForm bean = me.getModel();

    String errors = PageFlowUtil.getStrutsError(request, "main");
%>

<span class="labkey-error"><%=errors%></span>

<form action="uploadCustomProteinAnnotations.post" method="POST">
    <table>
        <tr>
            <td>
                Name:
            </td>
            <td>
                <input type="text" name="name" size="50" value="<%= h(bean.getName()) %>" />
            </td>
        </tr>
        <tr>
            <td>
                Type:
            </td>
            <td>
                <select name="annotationType">
                    <% for (CustomAnnotationType type : CustomAnnotationType.values())
                    { %>
                        <option <% if (type.toString().equals(bean.getAnnotationType())) { %> selected <% } %> value="<%= type.toString() %>"><%= type.getDescription() %></option>
                    <% } %>
                </select>
            </td>
        </tr>
        <tr>
            <td>
                Annotations:
            </td>
            <td>
                The first line of the file should be the column headings. The first column must be the name that refers to the protein.<br/>
                <textarea rows="10" cols="50" name="annotationsText"><%= h(bean.getAnnotationsText()) %></textarea>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><cpas:button text="Submit" /></td>
        </tr>
    </table>
</form>