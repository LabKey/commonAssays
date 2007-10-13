<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.study.WellData"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.api.study.PlateQueryView" %>
<%@ page import="org.labkey.nab.*" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.exp.PropertyDescriptor" %>
<%@ page import="org.labkey.api.exp.PropertyType" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.labkey.api.study.assay.AbstractAssayProvider" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.common.util.Pair" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    Luc5Assay assay = bean.getAssay();
    ViewContext context = me.getViewContext();

    List<Pair<String, Map<PropertyDescriptor, Object>>> sampleProperties = bean.getSampleProperties();
    Map<PropertyDescriptor, Object> firstSample = sampleProperties.get(0).getValue();
    Set<PropertyDescriptor> samplePropertyDescriptors = firstSample.keySet();

    Map<PropertyDescriptor, Object> runProperties = bean.getRunProperties();

    boolean writer = context.getContainer().hasPermission(context.getUser(), ACL.PERM_INSERT);
    String headerTDStyle = "text-align:left;background-color:#EEEEEE;border-top:solid 1px";

    String errs = PageFlowUtil.getStrutsError(request, "main");
    if (null != StringUtils.trimToNull(errs))
    {
        out.write("<span class=\"labkey-error\">");
        out.write(errs);
        out.write("</span>");
    }
    String labelStyle = "text-align:left;vertical-align:middle;font-weight:bold";

    QueryView duplicateDataFileView = bean.getDuplicateDataFileView(me.getViewContext());

    if (bean.isNewRun())
    {
%>
    This run has been automatically saved. <%= buttonLink("Delete Run", "deleteRun.view?rowId=" + assay.getRunRowId())%><br>
<%
    }
    if (!bean.isPrintView() &&  duplicateDataFileView != null)
    {
%>
<table>
    <tr>
        <th style="<%= headerTDStyle %>">Warnings</th>
    </tr>
    <tr>
        <td class="ms-searchform">
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
<tr>
    <th style="<%= headerTDStyle %>">Run Summary: <%= h(assay.getName()) %></th>
</tr>
    <tr>
        <td>
            <table width="100%">
                <%
                    Iterator<Map.Entry<PropertyDescriptor, Object>> propertyIt = runProperties.entrySet().iterator();
                    while (propertyIt.hasNext())
                    {
                %>
                    <tr>
                    <%
                        for (int col = 0; col < 3; col++)
                        {
                            Map.Entry<PropertyDescriptor, Object> property = propertyIt.hasNext() ? propertyIt.next() : null;
                    %>
                        <td style="<%= labelStyle %>"><%= property != null ? h(property.getKey().getLabel()) : "&nbsp;"  %></td>
                        <td ><%= property != null ? h(property.getValue()) : "&nbsp;"  %></td>
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
<form method="post" action="upload.view" enctype="multipart/form-data" class="normal">
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
                                        <td class="normal">
                                            <%=h(sampleProperties.get(i).getKey())%>
                                        </td>
                                        <%
                                            for (int cutoff : assay.getCutoffs())
                                            {
                                        %>
                                        <td class="normal" align="right">
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
                        <table cellspacing="0" cellpadding="3">
                            <tr>
                                <td style="<%= labelStyle %>"><%= h(AbstractAssayProvider.SPECIMENID_PROPERTY_CAPTION) %></td>
                            <%
                                PropertyDescriptor sampleDescPD = null;
                                for (PropertyDescriptor pd : samplePropertyDescriptors)
                                {
                                    if (pd.getName().equals(NabAssayProvider.SAMPLE_DESCRIPTION_PROPERTY_NAME))
                                    {
                                        sampleDescPD = pd;
                                        continue;
                                    }
                                    if (pd.getName().equals(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME) ||
                                            pd.getName().equals(AbstractAssayProvider.VISITID_PROPERTY_NAME) ||
                                            pd.getName().equals(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME))
                                        continue;
                            %>
                                <td style="<%= labelStyle %>"><%= h(pd.getLabel()) %></td>
                            <%
                                }
                            %>
                            </tr>
                            <%
                                for (Pair<String, Map<PropertyDescriptor, Object>> sample : sampleProperties)
                                {
                                    Map<PropertyDescriptor, Object> properties = sample.getValue();
                            %>
                                <tr>
                                    <td><%= h(sample.getKey()) %></td>
                            <%
                                for (Map.Entry<PropertyDescriptor, Object> entry : properties.entrySet())
                                {
                                    PropertyDescriptor pd = entry.getKey();

                                    if (pd.getName().equals(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME) ||
                                            pd.getName().equals(AbstractAssayProvider.VISITID_PROPERTY_NAME) ||
                                            pd.getName().equals(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME))
                                        continue;

                                    Object value = entry.getValue();
                                    if (pd.getFormat() != null)
                                    {
                                        if (pd.getPropertyType() == PropertyType.DOUBLE)
                                        {
                                            DecimalFormat format = new DecimalFormat(pd.getFormat());
                                            value = format.format((Double) value);
                                        }
                                        if (pd.getPropertyType() == PropertyType.DATE_TIME)
                                        {
                                            DateFormat format = new SimpleDateFormat(pd.getFormat());
                                            value = format.format((Date) value);
                                        }
                                    }
                            %>
                                    <td><%= h(value) %></td>
                            <%
                                    }
                            %>
                                </tr>
                            <%
                                }
                            %>
                        </table><br>
                        <%
                            if (sampleDescPD != null)
                            {
                        %>
                        <table cellspacing="0" cellpadding="3" width="100%">
                            <tr>
                                <td style="<%= labelStyle %>"><%= h(AbstractAssayProvider.SPECIMENID_PROPERTY_CAPTION) %></td>
                                <td style="<%= labelStyle %>"><%= h(NabAssayProvider.SAMPLE_DESCRIPTION_PROPERTY_CAPTION) %></td>
                            </tr>
                            <%
                                for (Pair<String, Map<PropertyDescriptor, Object>> sample : sampleProperties)
                                {
                                    Map<PropertyDescriptor, Object> properties = sample.getValue();
                                    String desc = (String) properties.get(sampleDescPD);
                            %>
                            <tr>
                                <td><%= h(sample.getKey()) %></td>
                                <td><%= h(desc) %></td>
                            </tr>
                            <%
                                }
                            %>
                        </table>
                        <%
                            }
                        %>
                    </td>
                    <td>
                        <img src="graph.view?rowId=<%= bean.getRunId() %>">
                    </td>
                </tr>
                <tr>
                    <td>
                        <table width="100%">
                            <tr>
                                <td style="<%= labelStyle %>">Range</td>
                                <td class="normal"
                                    align=left><%=Luc5Assay.intString(assay.getControlRange())%></td>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>">Virus Control</td>
                                <td class="normal" align="left"><%=Luc5Assay.intString(assay.getVirusControlMean())%> &plusmn; <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus())%></td>
                            </tr>
                            <tr>
                                <td style="<%= labelStyle %>">Cell Control</td>
                                <td class="normal" align=left><%=Luc5Assay.intString(assay.getCellControlMean())%> &plusmn; <%=Luc5Assay.percentString(assay.getCellControlPlusMinus())%></td>
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
                                <th colspan="4"><%= h(sampleProperties.get(i).getKey()) %></th>
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
                                <td class=normal align=right><%= shortDecFormat.format(summary.getDilution(data)) %></td>
                                <td class=normal
                                    align=right><%= Luc5Assay.percentString(summary.getPercent(data)) %></td>
                                <td class=normal>&plusmn;</td>
                                <td class=normal
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
                                <td class="normal" style="font-weight:bold"><%=c %></td>
                                <%
                                    }
                                %>
                            </tr>
                            <%
                                for (int row = 0; row < assay.getPlate().getRows(); row++)
                                {
                            %>
                            <tr>
                                <td class="normal" style="font-weight:bold"><%=(char) ('A' + row)%></td>

                                <%
                                    for (int col = 0; col < assay.getPlate().getColumns(); col++)
                                    {
                                %>
                                <td class=normal align=right>
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
    <tr>
        <th style="<%= headerTDStyle %>">Discussions</th>
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
