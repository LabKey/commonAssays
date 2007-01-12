<%@ page import="Nab.NabController"%>
<%@ page import="cpas.assays.nab.DilutionSummary"%>
<%@ page import="cpas.assays.nab.Luc5Assay"%>
<%@ page import="cpas.assays.nab.SampleInfo"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.study.WellData"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.RenderAssayBean> me = (JspView<NabController.RenderAssayBean>) HttpView.currentView();
    NabController.RenderAssayBean bean = me.getModel();
    Luc5Assay assay = bean.getAssay();
    String headerTDStyle = "text-align:left;background-color:#EEEEEE;border-top:solid 1px";

    String errs = PageFlowUtil.getStrutsError(request, "main");
    if (null != StringUtils.trimToNull(errs))
    {
        out.write("<span class=\"cpas-error\">");
        out.write(errs);
        out.write("</span>");
    }
    String labelStyle = "text-align:left;vertical-align:middle;font-weight:bold";

    if (bean.isNewRun())
    {
%>
    This run has been automatically saved. <%= buttonLink("Delete Run", "deleteRun.view?rowId=" + assay.getRunRowId())%><br>
<%
    }
%>
<form method="post" action="upload.view" enctype="multipart/form-data" class="normal">
<input type="hidden" name="rowId" value="<%= assay.getRunRowId() %>">
<table>
<tr>
    <th style="<%= headerTDStyle %>">Run Summary: <%= h(assay.getName()) %></th>
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
                    <%
                        String expDate = null;
                        if (assay.getExperimentDate() != null)
                            expDate = formatDate(assay.getExperimentDate());
                    %>
                    <td><%= h(expDate) %></td>
                    <td colspan="4">&nbsp;</td>
                </tr>
            </table>
        </td>
    </tr>
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
                                <table cellspacing="0" cellpadding="3" bgcolor="#FFFFA0">
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
                                        <td class="ms-vb">
                                            <%=h(summary.getSampleId())%>
                                        </td>
                                        <%
                                            for (int cutoff : assay.getCutoffs())
                                            {
                                        %>
                                        <td class="ms-vb" align="right">
                                            <%
                                                double val = curveBased ? summary.getCutoffDilution(cutoff / 100.0) :
                                                        summary.getInterpolatedCutoffDilution(cutoff / 100.0);
                                                if (val == Double.NEGATIVE_INFINITY)
                                                    out.write("&lt; " + Luc5Assay.intString(summary.getMinDilution()));
                                                else if (val == Double.POSITIVE_INFINITY)
                                                    out.write("&gt; " + Luc5Assay.intString(summary.getMaxDilution()));
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
                        <table cellspacing="0" cellpadding="3" width="100%">
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

                        <table cellspacing="0" cellpadding="3">
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
                                <td class="ms-vb"
                                    align=left><%=Luc5Assay.intString(assay.getControlRange())%></td>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>">Virus Control</td>
                                <td class="ms-vb" align="left"><%=Luc5Assay.intString(assay.getVirusControlMean())%> <%=h("�")%> <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus())%></td>
                                <td style="<%= labelStyle %>">Cell Control</td>
                                <td class="ms-vb" align=left><%=Luc5Assay.intString(assay.getCellControlMean())%> <%=h("�")%> <%=Luc5Assay.percentString(assay.getCellControlPlusMinus())%></td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <th style="<%= headerTDStyle %>">Sample Information</th>
    </tr>
    <tr>
        <td>
            <table class="normal">
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
                                <td class=ms-vb align=right><%= shortDecFormat.format(summary.getDilution(data)) %></td>
                                <td class=ms-vb
                                    align=right><%= Luc5Assay.percentString(summary.getPercent(data)) %></td>
                                <td class=ms-vb><%= h("�") %></td>
                                <td class=ms-vb
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
    <tr>
        <th style="<%= headerTDStyle %>">Plate Data</th>
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
                                <td class="ms-vb" style="font-weight:bold"><%=c %></td>
                                <%
                                    }
                                %>
                            </tr>
                            <%
                                for (int row = 0; row < assay.getPlate().getRows(); row++)
                                {
                            %>
                            <tr>
                                <td class="ms-vb" style="font-weight:bold"><%=(char) ('A' + row)%></td>

                                <%
                                    for (int col = 0; col < assay.getPlate().getColumns(); col++)
                                    {
                                %>
                                <td class=ms-vb align=right>
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
</table>
</form>