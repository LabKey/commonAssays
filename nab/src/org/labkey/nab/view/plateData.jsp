<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.nab.Luc5Assay" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();
%>
<table>
<%
    for (Plate plate : assay.getPlates())
    {
%>
<tr>
    <td valign=top>
        <table>
            <tr>
                <td>&nbsp;</td>
                <%
                    for (int c = 1; c <= plate.getColumns(); c++)
                    {
                %>
                <th><%=c %></th>
                <%
                    }
                %>
            </tr>
            <%
                for (int row = 0; row < plate.getRows(); row++)
                {
            %>
            <tr>
                <th><%=(char) ('A' + row)%></th>

                <%
                    for (int col = 0; col < plate.getColumns(); col++)
                    {
                %>
                <td align=right>
                    <%=Luc5Assay.intString(plate.getWell(row, col).getValue())%></td>
                <%
                    }
                %>
            </tr>
            <%
                }
            %>
        </table>
    </td>
</tr>
<%
    }
%>
</table>