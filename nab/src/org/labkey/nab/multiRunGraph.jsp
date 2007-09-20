<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="org.labkey.nab.DilutionSummary"%>
<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.study.WellData" %>
<%@page extends="org.labkey.api.jsp.JspBase"%>

<%
    JspView<NabController.GraphSelectedBean> me = (JspView<NabController.GraphSelectedBean>) HttpView.currentView();
    org.labkey.nab.NabController.GraphSelectedBean bean = me.getModelBean();

    String errs = PageFlowUtil.getStrutsError(request, "main");
    if (null != StringUtils.trimToNull(errs))
    {
        out.write("<span class=\"labkey-error\">");
        out.write(errs);
        out.write("</span>");
    }

    StringBuilder chartURL = new StringBuilder("renderMultiRunChart.view?");
    DilutionSummary[] summaries = bean.getDilutionSummaries();
    for (int i = 0; i < summaries.length; i++)
    {
        DilutionSummary summary = summaries[i];
        chartURL.append("wellGroupId=").append(summary.getWellGroup().getRowId());
        if (i < summaries.length - 1)
            chartURL.append("&");
    }

    String labelStyle = "text-align:left;vertical-align:middle;font-weight:bold";
%>
<table class="normal">
    <tr>
        <td valign="bottom">
            <table>
                <tr>
                    <th colspan=3>Selected Samples</th>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Sample Id</td>
                    <td style="<%= labelStyle %>">Initial Dilution</td>
                    <td style="<%= labelStyle %>">Dilution Factor</td>
                <%
                for (DilutionSummary dilSummary : bean.getDilutionSummaries())
                {
                %>
                    <tr><td class=normal>
                        <%= h(dilSummary.getSampleId()) %>
                    </td>
                    <td class="normal">
                        <%= dilSummary.getInitialDilution() %>
                    </td>
                    <td class="normal">
                        <%= dilSummary.getFactor() %>
                    </td>

                </tr>
            <%
                }
            %>
            </table>
        </td>
        <td valign="bottom">
            <table bgcolor="#FFFFA0">
                <tr>
                    <th colspan=3>Absolute Cutoff Dilutions<br>(Curve Based)</th>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <%
                        for (int cutoff : bean.getCutoffs())
                        {
                    %>
                    <td style="<%= labelStyle %>"><%= cutoff %>%</td>
                    <%
                        }
                    %>
                </tr>
                <%
                    for (DilutionSummary summary : bean.getDilutionSummaries())
                    {
                %>
                <tr>
                    <td class="normal">
                        <%=h(summary.getSampleId())%>
                    </td>
                    <%
                        for (int cutoff : bean.getCutoffs())
                        {
                    %>
                    <td class="normal">
                        <%
                            double val = summary.getCutoffDilution(cutoff / 100.0);
                            if (val == Double.NEGATIVE_INFINITY)
                                out.write("&lt; " + NabController.intString(summary.getMinDilution()));
                            else if (val == Double.POSITIVE_INFINITY)
                                out.write("&gt; " + NabController.intString(summary.getMaxDilution()));
                            else
                                out.write(NabController.intString(val));

                        %>
                    </td>
                    <%
                        }
                    %>
                </tr>
                <%
                    }
                %>
            </table>
        </td>
        <td valign="bottom">
            <table>
                <tr>
                    <th colspan=3>Curve Properties</th>
                </tr>
                <tr>
                    <td>&nbsp;</td><td style="<%= labelStyle %>">Fit Error</td>
                </tr>
                <%
                    DecimalFormat shortDecFormat = new DecimalFormat("0.0");
                    for (DilutionSummary summary : bean.getDilutionSummaries())
                    {
                %>
                <tr>
                    <td><%= h(summary.getSampleId()) %></td>
                    <td align="right"><%= shortDecFormat.format(summary.getFitError()) %></td>
                </tr>
                <%
                    }
                %>
            </table>
        </td>
    </tr>
    <tr>
        <td colspan="3">
            <img src="<%= chartURL %>">
        </td>
    </tr>
</table><br>

<table class="normal">
    <tr>
        <%
            for (DilutionSummary summary : bean.getDilutionSummaries())
            {
        %>
        <td>
            <table>
                <tr>
                    <th colspan="4"><%= summary.getSampleId() %></th>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>" align="right"><%= summary.getMethod().getAbbreviation() %></td>
                    <td style="<%= labelStyle %>" align="left" colspan="3">Neut.</td>
                </tr>
                <%
                    for (WellData data : summary.getWellData())
                    {
                %>
                <tr>
                    <td class=normal align=right><%= NabController.intString(summary.getDilution(data)) %></td>
                    <td class=normal
                        align=right><%= NabController.percentString(summary.getPercent(data)) %></td>
                    <td class=normal>&plusmn;</td>
                    <td class=normal
                        align=right><%= NabController.percentString(summary.getPlusMinus(data)) %></td>
                </tr>
                <%
                    }
                %>
            </table>
        </td>
        <%
            }
        %>
    </tr>
</table>
