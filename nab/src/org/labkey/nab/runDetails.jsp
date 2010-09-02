<%
/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.Lsid"%>
<%@ page import="org.labkey.api.exp.OntologyManager" %>
<%@ page import="org.labkey.api.exp.PropertyDescriptor" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.security.permissions.DeletePermission" %>
<%@ page import="org.labkey.api.security.permissions.InsertPermission" %>
<%@ page import="org.labkey.api.study.DilutionCurve" %>
<%@ page import="org.labkey.api.study.WellData" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.*" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<labkey:errors/>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();

    Map<String, Object> runProperties = bean.getRunDisplayProperties();

    boolean writer = context.getContainer().hasPermission(context.getUser(), InsertPermission.class);

    QueryView duplicateDataFileView = bean.getDuplicateDataFileView(me.getViewContext());
    int columnCount = 2;

    // the data for the sample properties table
    List<Map<PropertyDescriptor, Object>> sampleData = new ArrayList<Map<PropertyDescriptor, Object>>();
    Set<String> pdsWithData = new HashSet<String>();

    String aucPropertyName = bean.getFitType() == null ? NabDataHandler.AUC_PREFIX : assay.getDataHandler().getPropertyName(NabDataHandler.AUC_PREFIX, bean.getFitType());
    Lsid aucURI = new Lsid(NabDataHandler.NAB_PROPERTY_LSID_PREFIX, assay.getProtocol().getName(), aucPropertyName);
    String paucPropertyName = bean.getFitType() == null ? NabDataHandler.pAUC_PREFIX : assay.getDataHandler().getPropertyName(NabDataHandler.pAUC_PREFIX, bean.getFitType());
    Lsid pAucURI = new Lsid(NabDataHandler.NAB_PROPERTY_LSID_PREFIX, assay.getProtocol().getName(), paucPropertyName);
    PropertyDescriptor aucPD = OntologyManager.getPropertyDescriptor(aucURI.toString(), context.getContainer());
    PropertyDescriptor pAucPD = OntologyManager.getPropertyDescriptor(pAucURI.toString(), context.getContainer());

    for (NabAssayRun.SampleResult result : bean.getSampleResults())
    {
        Map<PropertyDescriptor, Object> sampleProps = new LinkedHashMap<PropertyDescriptor, Object>(result.getSampleProperties());

        if (aucPD != null)
        {
            Object aucValue = result.getDataProperties().get(aucPD);
            if (aucValue != null)
                sampleProps.put(aucPD, aucValue);
        }

        if (pAucPD != null)
        {
            Object paucValue = result.getDataProperties().get(pAucPD);
            if (paucValue != null)
                sampleProps.put(pAucPD, paucValue);
        }

        sampleData.add(sampleProps);

        // calculate which columns have data
        for (Map.Entry<PropertyDescriptor, Object> entry : sampleProps.entrySet())
        {
            if (entry.getValue() != null && !pdsWithData.contains(entry.getKey().getName()))
                pdsWithData.add(entry.getKey().getName());
        }
    }
    ActionURL reRunURL = new ActionURL(NabUploadWizardAction.class, context.getContainer());
    reRunURL.addParameter("rowId", assay.getProtocol().getRowId());
    reRunURL.addParameter("reRunId", bean.getRunId());

    boolean needCurveNote = assay.getRenderedCurveFitType() != assay.getSavedCurveFitType();
    boolean needNewRunNote = !bean.isPrintView() && bean.isNewRun();
    boolean needDupFileNote = !bean.isPrintView() &&  duplicateDataFileView != null;
%>
<input type="hidden" name="rowId" value="<%= assay.getRunRowId() %>">
<table>
<%
    if (needCurveNote || needNewRunNote || needDupFileNote)
    {
%>
    <tr class="labkey-wp-header">
        <th align="left">Notes</th>
    </tr>
    <%
        if (needCurveNote)
        {
            boolean deleteAndInsertPerms = getViewContext().getContainer().hasPermission(getViewContext().getUser(), DeletePermission.class) &&
            getViewContext().getContainer().hasPermission(getViewContext().getUser(), InsertPermission.class);
    %>
    <tr>
        <td class="labkey-form-label">
            This run is shown with a <strong><%= assay.getRenderedCurveFitType().getLabel() %></strong> curve fit,
           but is saved with a <strong><%= assay.getSavedCurveFitType() != null ? assay.getSavedCurveFitType().getLabel() : "unknown" %></strong> curve fit.
            To replace<br>the saved data with the displayed data,
            <%
            if (deleteAndInsertPerms)
            {
            %>
                you must <a href="<%= reRunURL.getLocalURIString() %>">delete and re-import</a> the run.
            <%
            }
            else
            {
            %>
                a user with appropriate permissions must delete and re-import the run.
            <%
            }
            %>
        </td>
    </tr>
    <%
        }
        if (needNewRunNote)
        {
    %>
    <tr>
        <td class="labkey-form-label">
            This run has been automatically saved.
        <%
                if (getViewContext().getContainer().hasPermission(getViewContext().getUser(), DeletePermission.class))
                {
                    ActionURL deleteUrl = new ActionURL(NabAssayController.DeleteRunAction.class, context.getContainer());
                    deleteUrl.addParameter("rowId", bean.getRunId());
        %>
        <%=generateButton("Delete Run", deleteUrl, "return confirm('Permanently delete this run?')")%>
        <%=generateButton("Delete and Re-Import", reRunURL)%>
        <%
                }
        %>
        </td>
    </tr>
    <%
        }
        if (needDupFileNote)
        {
    %>
    <tr>
        <td class="labkey-form-label">
            <span class="labkey-error"><b>WARNING</b>: The following runs use a data file by the same name.</span><br><br>
            <% include(duplicateDataFileView, out); %>
        </td>
    </tr>
    <%
        }
    }
%>
<tr class="labkey-wp-header">
    <th>Run Summary<%= assay.getRunName() != null ? ": " + h(assay.getRunName()) : "" %></th>
</tr>
    <tr>
        <td>
            <table width="100%">
                <%
                    Iterator<Map.Entry<String, Object>> propertyIt = runProperties.entrySet().iterator();
                    Pair<String, Object>[] entries = new Pair[runProperties.size()];
                    for (int i = 0; i < entries.length; i++)
                    {
                        Map.Entry<String, Object> entry = propertyIt.next();
                        entries[i] = new Pair<String, Object>(entry.getKey(), entry.getValue());
                    }

                    int longestColumn = (int) Math.ceil(entries.length/2.0);
                    for (int row = 0; row < longestColumn; row++)
                    {
                %>
                    <tr>
                    <%
                        for (int col = 0; col < columnCount; col++)
                        {
                            int index = col*longestColumn + row;
                            if (index < entries.length)
                            {
                                Pair<String, Object> property = index < entries.length ? entries[index] : null;
                        %>
                            <th class="labkey-header"><%= property != null ? h(property.getKey()) : "&nbsp;"  %></th>
                            <td><%= property != null ? h(property.getValue()) : "&nbsp;"  %></td>
                        <%
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
<form method="post" action="upload.view" enctype="multipart/form-data">
    <tr>
        <td>
            <table>
                    <tr>
                        <td valign="top" rowspan="2">
                            <%
                                ActionURL graphAction = new ActionURL(NabAssayController.GraphAction.class, context.getContainer());
                                graphAction.addParameter("rowId", bean.getRunId());
                                if (bean.getFitType() != null)
                                    graphAction.addParameter("fitType", bean.getFitType().name());
                            %>
                            <img src="<%= graphAction.getLocalURIString() %>">
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
                                for (NabAssayRun.SampleResult results : bean.getSampleResults())
                                {
                                    String unableToFitMessage = null;

                                    DilutionSummary summary = results.getDilutionSummary();
                                    try
                                    {
                                        summary.getCurve();
                                    }
                                    catch (DilutionCurve.FitFailedException e)
                                    {
                                        unableToFitMessage = e.getMessage();
                                    }
                            %>
                            <tr>
                                <td class="labkey-header">
                                    <%=h(results.getCaption())%>
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

                                            if (curveBased && unableToFitMessage != null)
                                            {
                                    %>
                                        N/A<%= helpPopup("Unable to fit curve", unableToFitMessage)%>
                                    <%
                                            }
                                            else
                                            {
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
                                            }
                                                %>
                                    </td>
                                                <%
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
                            <%
                                if (assay.getPlates().length > 1)
                                {
                            %>
                            <tr>
                                <th>Plate</th>
                                <%
                                    Plate[] plates = assay.getPlates();
                                    for (int i = 0; i < plates.length; i++)
                                    {
                                        Plate plate = plates[i];
                                %>
                                <th><%= i + 1 %></th>
                                <%
                                    }
                                %>
                            </tr>
                            <%
                                }
                            %>
                            <tr>
                                <th class="labkey-header">Range</th>
                                <%
                                    for (Plate plate : assay.getPlates())
                                    {
                                %>
                                <td align=left>
                                    <%=Luc5Assay.intString(assay.getControlRange(plate))%>
                                </td>
                                <%
                                    }
                                %>
                            </tr>
                            <tr>
                                <th class="labkey-header">Virus Control</th>
                                <%
                                    for (Plate plate : assay.getPlates())
                                    {
                                %>
                                <td align="left">
                                    <%=Luc5Assay.intString(assay.getVirusControlMean(plate))%> &plusmn;
                                    <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus(plate))%>
                                </td>
                                <%
                                    }
                                %>
                            </tr>
                            <tr>
                                <th class="labkey-header">Cell Control</th>
                                <%
                                    for (Plate plate : assay.getPlates())
                                    {
                                %>
                                <td align=left>
                                    <%=Luc5Assay.intString(assay.getCellControlMean(plate))%> &plusmn;
                                    <%=Luc5Assay.percentString(assay.getCellControlPlusMinus(plate))%>
                                </td>
                                <%
                                    }
                                %>
                            </tr>
                        </table>
                    </td>
                </tr>
                </table>
            <%
                if (sampleData.size() > 0)
                {
            %>
                <table class="labkey-data-region labkey-show-borders">
                    <colgroup><%

                        for (PropertyDescriptor pd : sampleData.get(0).keySet())
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


                        for (PropertyDescriptor pd : sampleData.get(0).keySet())
                        {
                            if (!pdsWithData.contains(pd.getName()))
                                continue;

                    %>
                        <th><%= h(StringUtils.isBlank(pd.getLabel()) ? pd.getName() : pd.getLabel()) %></th>
                    <%
                        }
                    %>
                    </tr>
                    <%
                        int rowNumber = 0;
                        for (Map<PropertyDescriptor, Object> row : sampleData)
                        {
                            rowNumber++;
                    %>
                        <tr <%= rowNumber % 2 == 0 ? "class=\"labkey-alternate-row\"" : ""%>>
                    <%
                        for (Map.Entry<PropertyDescriptor, Object> entry : row.entrySet())
                        {
                            PropertyDescriptor pd = entry.getKey();
                            if (!pdsWithData.contains(pd.getName()))
                                continue;

                            Object value = bean.formatValue(pd, entry.getValue());
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
            <%
                }
                else
                {
            %>
            <span class="labkey-error">No samples well groups were specified in the selected plate template.</span>
            <%
                }
            %>
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
                        for (NabAssayRun.SampleResult results : bean.getSampleResults())
                        {
                            DilutionSummary summary = results.getDilutionSummary();
                    %>
                    <td>
                        <table>
                            <tr>
                                <th colspan="4" class="labkey-header" style="text-align:center"><%= h(results.getCaption()) %></th>
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
                                <td style="font-weight:bold"><%=c %></td>
                                <%
                                    }
                                %>
                            </tr>
                            <%
                                for (int row = 0; row < plate.getRows(); row++)
                                {
                            %>
                            <tr>
                                <td style="font-weight:bold"><%=(char) ('A' + row)%></td>

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