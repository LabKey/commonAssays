<%
/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.security.permissions.InsertPermission"%>
<%@ page import="org.labkey.api.study.PlateQueryView"%>
<%@ page import="org.labkey.api.study.WellData"%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.assay.dilution.DilutionSummary" %>
<%@ page import="org.labkey.api.assay.nab.Luc5Assay" %>
<%@ page import="org.labkey.nab.NabController" %>
<%@ page import="org.labkey.api.assay.dilution.SampleInfo" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.nab.OldNabAssayRun" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.RenderAssayBean> me = (JspView<NabController.RenderAssayBean>) HttpView.currentView();
    NabController.RenderAssayBean bean = me.getModelBean();
    OldNabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();
    boolean writer = context.getContainer().hasPermission(context.getUser(), InsertPermission.class);
    %>
<labkey:errors/>
<%
    String labelStyle = "text-align:left;vertical-align:middle;font-weight:bold";

    PlateQueryView duplicateDataFileView = bean.getDuplicateDataFileView(me.getViewContext(), assay);

    if (bean.isNewRun())
    {
%>
    This run has been automatically saved. <%= generateButton("Delete Run", buildURL(NabController.DeleteRunAction.class, "rowId=" + assay.getRunRowId()))%><br>
<%
    }
    if (!bean.isPrintView() &&  duplicateDataFileView.hasRecords())
    {
%>
<table>
    <tr class="labkey-wp-header">
        <th>Warnings</th>
    </tr>
    <tr>
        <td class="labkey-form-label">
            <span class="labkey-error"><b>WARNING</b>: The following runs use a data file by the same name.</span><br><br>
            <% include(duplicateDataFileView, out); %>
        </td>
    </tr>
</table>
    <%
        }
    %>

<input type="hidden" name="rowId" value="<%= assay.getRunRowId() %>">
<table>
<tr class="labkey-wp-header">
    <th>Run Summary: <%= h(assay.getName()) %></th>
</tr>
    <tr>
        <td>
            <table width="100%">
                <tr>
                    <td style="<%= labelStyle %>">File ID</td>
                    <td><%= h(assay.getFileId()) %></td>
                    <td style="<%= labelStyle %>">Study Name</td>
                    <td><%= h(assay.getStudyName()) %></td>
                    <td style="<%= labelStyle %>">Experiment Performer</td>
                    <td colspan="3"><%= h(assay.getExperimentPerformer()) %></td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Experiment ID</td>
                    <td><%= h(assay.getExperimentId()) %></td>
                    <td style="<%= labelStyle %>">Incubation Time</td>
                    <td><%= h(assay.getIncubationTime()) %></td>
                    <td style="<%= labelStyle %>">Plate Number</td>
                    <td><%= h(assay.getPlateNumber()) %></td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Experiment Date</td>
                    <td><%= h(formatDate(assay.getExperimentDate())) %></td>
                    <td colspan="4">&nbsp;</td>
                </tr>
            </table>
        </td>
    </tr>
<form method="post" action="upload.view" enctype="multipart/form-data">
    <tr>
        <td>
            <table>
                <tr>
                    <td rowspan="2" valign="top">
                        <table>
                            <tr>
                    <%
                        for (int pass = 0; pass < 2; pass++)
                        {
                            boolean curveBased = (pass == 0);
                    %>
                                <td>
                                <table class="labkey-form-label">
                                    <tr>
                                        <th align="center" colspan=<%= assay.getCutoffs().length + 1%>>Cutoff Dilutions<br>(<%= curveBased ? "Curve Based" : "Point Based" %>)</th>
                                    </tr>
                                    <tr>
                                        <td>&nbsp;</td>
                                        <%
                                            for (int cutoff : assay.getCutoffs())
                                            {
                                        %>
                                        <td style="<%= labelStyle %>" align="center"><%= cutoff %>%</td>
                                        <%
                                            }
                                        %>
                                    </tr>
                                    <%
                                        for (int i = 0; i < assay.getSummaries().length; i++)
                                        {
                                            DilutionSummary summary = assay.getSummaries()[i];
                                    %>
                                    <tr>
                                        <td>
                                            <%=h(summary.getSampleId())%>
                                        </td>
                                        <%
                                            for (int cutoff : assay.getCutoffs())
                                            {
                                        %>
                                        <td align="right">
                                            <%
                                                double val = curveBased ? summary.getCutoffDilution(cutoff / 100.0, assay.getRenderedCurveFitType()) :
                                                        summary.getInterpolatedCutoffDilution(cutoff / 100.0, assay.getRenderedCurveFitType());
                                                if (val == Double.NEGATIVE_INFINITY)
                                                    out.write("&lt; " + Luc5Assay.intString(summary.getMinDilution(assay.getRenderedCurveFitType())));
                                                else if (val == Double.POSITIVE_INFINITY)
                                                    out.write("&gt; " + Luc5Assay.intString(summary.getMaxDilution(assay.getRenderedCurveFitType())));
                                                else
                                                {
                                                    DecimalFormat shortDecFormat;
                                                    if (summary.getMethod() == SampleInfo.Method.Concentration)
                                                        shortDecFormat = new DecimalFormat("0.###");
                                                    else
                                                        shortDecFormat = new DecimalFormat("0");

                                                    out.write(shortDecFormat.format(val));
                                                }
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
                            <%
                                }
                            %>
                            </tr>
                        </table><br>
                        <table>
                            <tr>
                                <td style="<%= labelStyle %>">Sample Id</td>
                                <td style="<%= labelStyle %>">Description</td>
                            </tr>
                            <%
                                for (int sampId = 0; sampId < assay.getSummaries().length; sampId++)
                                {
                                    DilutionSummary summary = assay.getSummaries()[sampId];
                            %>
                            <tr>
                                <td><%= h(summary.getSampleId()) %></td>
                                <td><%= h(summary.getSampleDescription()) %></td>
                            </tr>
                            <%
                                }
                            %>
                        </table><br>

                        <table>
                            <tr>
                                <td style="<%= labelStyle %>">Sample Id</td>
                                <td style="<%= labelStyle %>">Initial Dil.</td>
                                <td style="<%= labelStyle %>">Factor</td>
                                <td style="<%= labelStyle %>">Curve&nbsp;Error</td>
                            </tr>
                            <%
                                for (int sampId = 0; sampId < assay.getSummaries().length; sampId++)
                                {
                                    DilutionSummary summary = assay.getSummaries()[sampId];
                                    DecimalFormat shortDecFormat = new DecimalFormat("0.0");
                            %>
                            <tr>
                                <td><%= h(summary.getSampleId()) %></td>
                                <td align="right"><%= summary.getInitialDilution() %></td>
                                <td align="right"><%= summary.getFactor() %></td>
                                <td align="right"><%= shortDecFormat.format(summary.getFitError()) %></td>
                            </tr>
                            <%
                                }
                            %>
                        </table>


                    </td>
                    <td>
                        <img src="renderChart.view?rowId=<%= assay.getRunRowId() %>">
                    </td>
                </tr>
                <tr>
                    <td>
                        <table width="100%">
                            <tr>
                                <td style="<%= labelStyle %>">Virus Name</td>
                                <td colspan="3"><%= h(assay.getVirusName()) %></td>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>">Virus ID</td>
                                <td colspan="3"><%= h(assay.getVirusId()) %></td>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>">Host Cell</td>
                                <td><%= h(assay.getHostCell()) %></td>
                                <td style="<%= labelStyle %>">Range</td>
                                <td
                                    align=left><%=Luc5Assay.intString(assay.getControlRange())%></td>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>">Virus Control</td>
                                <td align="left"><%=Luc5Assay.intString(assay.getVirusControlMean())%> &plusmn; <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus())%></td>
                                <td style="<%= labelStyle %>">Cell Control</td>
                                <td align=left><%=Luc5Assay.intString(assay.getCellControlMean())%> &plusmn; <%=Luc5Assay.percentString(assay.getCellControlPlusMinus())%></td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr class="labkey-wp-header">
        <th>Sample Information</th>
    </tr>
    <tr>
        <td>
            <table>
                <tr>
                    <%
                        for (int i = 0; i < assay.getSummaries().length; i++)
                        {
                            DilutionSummary summary = assay.getSummaries()[i];
                    %>
                    <td>
                        <table>
                            <tr>
                                <th colspan="4"><%= summary.getSampleId() %></th>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>" align="right"><%= summary.getMethod().getAbbreviation() %></td>
                                <td style="<%= labelStyle %>" align="center" colspan="3">Neut.</td>
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
                        }
                    %>
                </tr>
            </table>
        </td>
    </tr>
    <tr class="labkey-wp-header">
        <th>Plate Data</th>
    </tr>
    <tr>
        <td>
            <table>
                <tr>
                    <td valign=top>
                        <table>
                            <tr>
                                <td>&nbsp;</td>
                                <%
                                    for (int c = 1; c <= assay.getPlate().getColumns(); c++)
                                    {
                                %>
                                <td style="font-weight:bold"><%=c %></td>
                                <%
                                    }
                                %>
                            </tr>
                            <%
                                for (int row = 0; row < assay.getPlate().getRows(); row++)
                                {
                            %>
                            <tr>
                                <td style="font-weight:bold"><%=(char) ('A' + row)%></td>

                                <%
                                    for (int col = 0; col < assay.getPlate().getColumns(); col++)
                                    {
                                %>
                                <td align=right>
                                    <%=Luc5Assay.intString(assay.getPlate().getWell(row, col).getValue())%></td>
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
            </table>
        </td>
    </tr>
</form>
<%
    if (!bean.isPrintView() && writer)
    {
%>
    <tr class="labkey-wp-header">
        <th>Discussions</th>
    </tr>
    <tr>
        <td>
            <% me.include(bean.getDiscussionView(HttpView.getRootContext()), out); %>
        </td>
    </tr>
<%
    }
%>
</table>
