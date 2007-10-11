<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.ms1.Software" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.SoftwareParam" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%
    JspView<Software[]> me = (JspView<Software[]>) HttpView.currentView();
    Software[] swares = me.getModelBean();
%>
<table>
    <% for(Software sware : swares) {%>
    <tr bgcolor="#EEEEEE">
        <td><b><%=PageFlowUtil.filter(sware.getName())%></b>
        <%
        String version = sware.getVersion();
        if(null != version && version.length() > 0)
            out.print(" version " + PageFlowUtil.filter(version));

        String author = sware.getAuthor();
        if(null != author && author.length() > 0)
            out.print(" (" + PageFlowUtil.filter(author) + ")");
%>
        </td>
    </tr>
    <tr>
        <td>
            <table>
                <% SoftwareParam[] params = sware.getParameters();
                    if(null != params && params.length > 0)
                    {
                        for(SoftwareParam param : sware.getParameters()) { %>
                <tr>
                    <td><%=PageFlowUtil.filter(param.getName())%>:</td>
                    <td><%=PageFlowUtil.filter(param.getValue())%></td>
                </tr>
                <%}}%>
            </table>
        </td>
    </tr>
    <%}%>
</table>