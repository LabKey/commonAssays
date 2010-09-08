<%
/*
 * Copyright (c) 2006-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.study.WellData"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.DilutionSummary"%>
<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<labkey:errors/>
<%
    JspView<NabController.GraphSelectedBean> me = (JspView<NabController.GraphSelectedBean>) HttpView.currentView();
    NabController.GraphSelectedBean bean = me.getModelBean();

    StringBuilder chartURL = new StringBuilder("renderMultiRunChart.view?");
    DilutionSummary[] summaries = bean.getDilutionSummaries();
    for (int i = 0; i < summaries.length; i++)
    {
        DilutionSummary summary = summaries[i];
        chartURL.append("wellGroupId=").append(summary.getFirstWellGroup().getRowId());
        if (i < summaries.length - 1)
            chartURL.append("&");
    }

    String labelStyle = "text-align:left;vertical-align:middle;font-weight:bold";
%>
<table>
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
                    <tr><td>
                        <%= h(dilSummary.getSampleId()) %>
                    </td>
                    <td>
                        <%= dilSummary.getInitialDilution() %>
                    </td>
                    <td>
                        <%= dilSummary.getFactor() %>
                    </td>

                </tr>
            <%
                }
            %>
            </table>
        </td>
        <td valign="bottom">
            <table class="labkey-form-label">
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
                    <td>
                        <%=h(summary.getSampleId())%>
                    </td>
                    <%
                        for (int cutoff : bean.getCutoffs())
                        {
                    %>
                    <td>
                        <%
                            double val = summary.getCutoffDilution(cutoff / 100.0, summary.getAssay().getRenderedCurveFitType());
                            if (val == Double.NEGATIVE_INFINITY)
                                out.write("&lt; " + intString(summary.getMinDilution(summary.getAssay().getRenderedCurveFitType())));
                            else if (val == Double.POSITIVE_INFINITY)
                                out.write("&gt; " + intString(summary.getMaxDilution(summary.getAssay().getRenderedCurveFitType())));
                            else
                                out.write(intString(val));

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

<table>
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
                    <td align=right><%=intString(summary.getDilution(data)) %></td>
                    <td
                        align=right><%=percentString(summary.getPercent(data)) %></td>
                    <td>&plusmn;</td>
                    <td
                        align=right><%=percentString(summary.getPlusMinus(data)) %></td>
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
<%!
    public static String intString(double d)
    {
        return String.valueOf((int) Math.round(d));
    }

    public static String percentString(double d)
    {
        return intString(d * 100) + "%";
    }
%>
