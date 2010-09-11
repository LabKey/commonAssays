<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.nab.Luc5Assay" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.nab.SampleInfo" %>
<%@ page import="org.labkey.api.study.WellData" %>
<%@ page import="org.labkey.nab.DilutionSummary" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
%>
<table>
    <tr>
        <%
            int count = 0;
            int maxPerRow = 6;
            for (NabAssayRun.SampleResult results : bean.getSampleResults())
            {
                DilutionSummary summary = results.getDilutionSummary();
        %>
        <td>
            <table>
                <tr>
                    <th colspan="4" style="text-align:center"><%= h(results.getCaption()) %></th>
                </tr>
                <tr>
                    <th align="right"><%= summary.getMethod().getAbbreviation() %></th>
                    <th align="center" colspan="3">Neut.</th>
                </tr>
                <%
                    List<WellData> dataList = summary.getWellData();
                    for (int dataIndex = dataList.size() - 1; dataIndex >= 0; dataIndex--)
                    {
                        WellData data = dataList.get(dataIndex);
                        DecimalFormat shortDecFormat;
                        if (summary.getMethod() == SampleInfo.Method.Concentration)
                            shortDecFormat = new DecimalFormat("0.###");
                        else
                            shortDecFormat = new DecimalFormat("0");
                %>
                <tr>
                    <td align=right><%= shortDecFormat.format(summary.getDilution(data)) %></td>
                    <td
                        align=right><%= Luc5Assay.percentString(summary.getPercent(data)) %></td>
                    <td>&plusmn;</td>
                    <td
                        align=right><%= Luc5Assay.percentString(summary.getPlusMinus(data)) %></td>
                </tr>
                <%
                    }
                %>
            </table>
        </td>
        <%
                if (++count % maxPerRow == 0)
                {
        %>
                    </tr><tr>
        <%
                }
            }
        %>
    </tr>
</table>