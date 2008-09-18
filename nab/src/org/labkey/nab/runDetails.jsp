<%
/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.exp.PropertyDescriptor"%>
<%@ page import="org.labkey.api.exp.PropertyType"%>
<%@ page import="org.labkey.api.query.QueryView"%>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.api.study.WellData" %>
<%@ page import="org.labkey.api.study.assay.AbstractAssayProvider" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.*" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.*" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    Luc5Assay assay = bean.getAssay();
    ViewContext context = me.getViewContext();

    List<NabAssayController.SampleResult> sampleInfos = bean.getSampleResults();
    Map<PropertyDescriptor, Object> firstSample = sampleInfos.get(0).getProperties();
    Set<PropertyDescriptor> samplePropertyDescriptors = firstSample.keySet();

    Map<PropertyDescriptor, Object> runProperties = bean.getRunProperties();
    Map<PropertyDescriptor, Object> nonNullRunProperties = new LinkedHashMap<PropertyDescriptor, Object>();
    for (Map.Entry<PropertyDescriptor, Object> prop : runProperties.entrySet())
    {
        if (prop.getValue() != null)
            nonNullRunProperties.put(prop.getKey(), prop.getValue());
    }

    boolean writer = context.getContainer().hasPermission(context.getUser(), ACL.PERM_INSERT);

    String errs = PageFlowUtil.getStrutsError(request, "main");
    if (null != StringUtils.trimToNull(errs))
    {
        out.write("<span class=\"labkey-error\">");
        out.write(errs);
        out.write("</span>");
    }

    QueryView duplicateDataFileView = bean.getDuplicateDataFileView(me.getViewContext());

    if (bean.isNewRun())
    {
%>
    This run has been automatically saved.
<%
        if (getViewContext().getContainer().hasPermission(getViewContext().getUser(), ACL.PERM_DELETE))
        {
%>
<%= generateButton("Delete Run", "deleteRun.view?rowId=" + bean.getRunId(), "return confirm('Permanently delete this run?')")%>
<%
        }
%>
<br>
<%
    }
    if (!bean.isPrintView() &&  duplicateDataFileView != null)
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
                <%
                    Iterator<Map.Entry<PropertyDescriptor, Object>> propertyIt = nonNullRunProperties.entrySet().iterator();
                    while (propertyIt.hasNext())
                    {
                %>
                    <tr>
                    <%
                        for (int col = 0; col < 2; col++)
                        {
                            Map.Entry<PropertyDescriptor, Object> property = propertyIt.hasNext() ? propertyIt.next() : null;
                            Object value = null;
                            if (property != null)
                                value = formatValue(property.getKey(), property.getValue());
                    %>
                        <th class="labkey-header"><%= property != null ? h(property.getKey().getNonBlankLabel()) : "&nbsp;"  %></th>
                        <td><%= property != null ? h(value) : "&nbsp;"  %></td>
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
<form method="post" action="upload.view" enctype="multipart/form-data">
    <tr>
        <td>
            <table>
                    <tr>
                        <td valign="top" rowspan="2">
                            <img src="graph.view?rowId=<%= bean.getRunId() %>">
                        </td>
                        <td valign="top">
                        <table class="labkey-data-region labkey-show-borders" style="background-color:#FFFFA0;border:0">
                            <tr>
                                <td style="background-color:#FFFFFF;border:0">&nbsp;</td>
                                <th colspan="<%= 2* assay.getCutoffs().length %>">Cutoff Dilutions</th>
                            </tr>
                            <tr>
                                <td style="background-color:#FFFFFF;border:0">&nbsp;</td>
                                <th style="text-align:center" colspan=<%= assay.getCutoffs().length %>>Curve Based</th>
                                <th style="text-align:center" colspan=<%= assay.getCutoffs().length %>>Point Based</th>
                            </tr>
                            <tr>
                                <td style="background-color:#FFFFFF;border:0 1px 0 0">&nbsp;</td>
                                <%
                                    for (int set = 0; set < 2; set++)
                                    {
                                        for (int cutoff : assay.getCutoffs())
                                        {
                                    %>
                                    <th  style="text-align:center"><%= cutoff %>%</th>
                                    <%
                                        }
                                    }
                                %>
                            </tr>
                            <%
                                for (NabAssayController.SampleResult results : bean.getSampleResults())
                                {
                                    DilutionSummary summary = results.getDilutionSummary();
                            %>
                            <tr>
                                <td class="labkey-header">
                                    <%=h(results.getKey())%>
                                </td>
                                <%
                                    for (int set = 0; set < 2; set++)
                                    {
                                        for (int cutoff : assay.getCutoffs())
                                        {
                                            %>
                                <td style="text-align:right">
                                            <%
                                            boolean curveBased = set == 0;

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
                                            %>
                                </td>
                                            <%
                                            }
                                        }
                                    }
                                %>
                            </tr>
                            <%
                                }
                            %>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" valign="top">
                        <table width="100%">
                            <tr>
                                <th class="labkey-header">Range</th>
                                <td
                                    align=left><%=Luc5Assay.intString(assay.getControlRange())%></td>
                            </tr>
                            <tr>
                                <th class="labkey-header">Virus Control</th>
                                <td align="left"><%=Luc5Assay.intString(assay.getVirusControlMean())%> &plusmn; <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus())%></td>
                            </tr>
                            <tr>
                                <th class="labkey-header">Cell Control</th>
                                <td align=left><%=Luc5Assay.intString(assay.getCellControlMean())%> &plusmn; <%=Luc5Assay.percentString(assay.getCellControlPlusMinus())%></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                </table>
                <table class="labkey-data-region labkey-show-borders">
                    <colgroup><%
                        Set<String> pdsWithData = new HashSet<String>();
                        for (NabAssayController.SampleResult results : bean.getSampleResults())
                        {
                            for (Map.Entry<PropertyDescriptor, Object> entry : results.getProperties().entrySet())
                            {
                                if (entry.getValue() != null)
                                    pdsWithData.add(entry.getKey().getName());
                            }
                        }

                        for (PropertyDescriptor pd : samplePropertyDescriptors)
                        {
                            if (!pdsWithData.contains(pd.getName()))
                                continue;
                            %>
                            <col>
                            <%
                        }

                    %></colgroup>
                    <tr class="labkey-col-header">
                    <%


                        for (PropertyDescriptor pd : samplePropertyDescriptors)
                        {
                            if (!pdsWithData.contains(pd.getName()))
                                continue;

                    %>
                        <th><%= h(pd.getLabel()) %></th>
                    <%
                        }
                    %>
                    </tr>
                    <%
                        int rowNumber = 0;
                        for (NabAssayController.SampleResult results : bean.getSampleResults())
                        {
                            rowNumber++;
                    %>
                        <tr <%= rowNumber % 2 == 0 ? "class=\"labkey-alternate-row\"" : ""%>>
                    <%
                        for (Map.Entry<PropertyDescriptor, Object> entry : results.getProperties().entrySet())
                        {
                            PropertyDescriptor pd = entry.getKey();
                            if (!pdsWithData.contains(pd.getName()))
                                continue;

                            Object value = formatValue(pd, entry.getValue());
                    %>
                            <td><%= h(value) %></td>
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
    <tr class="labkey-wp-header">
        <th>Sample Information</th>
    </tr>
    <tr>
        <td>
            <table>
                <tr>
                    <%
                        for (NabAssayController.SampleResult results : bean.getSampleResults())
                        {
                            DilutionSummary summary = results.getDilutionSummary();
                    %>
                    <td>
                        <table>
                            <tr>
                                <th colspan="4" class="labkey-header" style="text-align:center"><%= h(results.getKey()) %></th>
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
<%!
    Object formatValue(PropertyDescriptor pd, Object value)
    {
        if (pd.getFormat() != null)
        {
            if (pd.getPropertyType() == PropertyType.DOUBLE)
            {
                DecimalFormat format = new DecimalFormat(pd.getFormat());
                value = format.format(value);
            }
            if (pd.getPropertyType() == PropertyType.DATE_TIME)
            {
                DateFormat format = new SimpleDateFormat(pd.getFormat());
                value = format.format((Date) value);
            }
        }
        else if (pd.getPropertyType() == PropertyType.DATE_TIME && value instanceof Date)
        {
            Date date = (Date) value;
            if (date.getHours() == 0 &&
                    date.getMinutes() == 0 &&
                    date.getSeconds() == 0)
            {
                value = formatDate(date);
            }
            else
            {
                value = formatDateTime(date);
            }
        }
        return value;
    }
%>
