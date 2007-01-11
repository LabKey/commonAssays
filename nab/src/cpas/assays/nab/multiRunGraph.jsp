<%@ page import="cpas.assays.nab.Luc5Assay"%>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="cpas.assays.nab.DilutionSummary"%>
<%@ page import="Nab.NabController"%>
<%@ page import="org.fhcrc.cpas.view.HttpView"%>
<%@ page import="org.fhcrc.cpas.view.JspView"%>
<%@ page import="org.fhcrc.cpas.study.WellData" %>
<%@page extends="org.fhcrc.cpas.jsp.JspBase"%>

<%
    JspView<NabController.GraphSelectedBean> me = (JspView<NabController.GraphSelectedBean>) HttpView.currentView();
    NabController.GraphSelectedBean bean = me.getModel();

    String errs = PageFlowUtil.getStrutsError(request, "main");
    if (null != StringUtils.trimToNull(errs))
    {
        out.write("<span class=\"cpas-error\">");
        out.write(errs);
        out.write("</span>");
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
                    <tr><td class=ms-vb>
                        <%= h(dilSummary.getSampleId()) %>
                    </td>
                    <td class="ms-vb">
                        <%= dilSummary.getInitialDilution() %>
                    </td>
                    <td class="ms-vb">
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
                    <td class="ms-vb">
                        <%=h(summary.getSampleId())%>
                    </td>
                    <%
                        for (int cutoff : bean.getCutoffs())
                        {
                    %>
                    <td class="ms-vb">
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
                    <td>&nbsp;</td><td style="<%= labelStyle %>">Slope</td><td style="<%= labelStyle %>">Fit Error</td>
                </tr>
                <%
                    DecimalFormat shortDecFormat = new DecimalFormat("0.0");
                    for (DilutionSummary summary : bean.getDilutionSummaries())
                    {
                %>
                <tr>
                    <td><%= h(summary.getSampleId()) %></td>
                    <td align="right"><%= shortDecFormat.format(summary.getSlope().doubleValue()) %></td>
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
            <img src="renderMultiRunChart.view">
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
                    <td class=ms-vb align=right><%= NabController.intString(summary.getDilution(data)) %></td>
                    <td class=ms-vb
                        align=right><%= NabController.percentString(summary.getPercent(data)) %></td>
                    <td class=ms-vb><%= h("±") %></td>
                    <td class=ms-vb
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