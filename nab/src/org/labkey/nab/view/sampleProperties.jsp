<%
/*
 * Copyright (c) 2010-2012 LabKey Corporation
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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.exp.Lsid" %>
<%@ page import="org.labkey.api.exp.OntologyManager" %>
<%@ page import="org.labkey.api.exp.PropertyDescriptor" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
<%@ page import="org.labkey.nab.NabDataHandler" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.labkey.nab.NabManager" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();
    // the data for the sample properties table
    List<Map<PropertyDescriptor, Object>> sampleData = new ArrayList<Map<PropertyDescriptor, Object>>();
    Set<String> pdsWithData = new HashSet<String>();

    for (NabAssayRun.SampleResult result : bean.getSampleResults())
    {
        Map<PropertyDescriptor, Object> sampleProps = new LinkedHashMap<PropertyDescriptor, Object>(result.getSampleProperties());

        Pair<PropertyDescriptor, Object> fitErrorPair = bean.getFitError(result, context.getContainer());
        if (fitErrorPair != null)
            sampleProps.put(fitErrorPair.getKey(), fitErrorPair.getValue());

        Pair<PropertyDescriptor, Object> aucPair = bean.getAuc(result, context.getContainer());
        if (aucPair != null)
            sampleProps.put(aucPair.getKey(), aucPair.getValue());

        Pair<PropertyDescriptor, Object> paucPair = bean.getPositiveAuc(result, context.getContainer());
        if (paucPair != null)
            sampleProps.put(paucPair.getKey(), paucPair.getValue());

        sampleData.add(sampleProps);

        // calculate which columns have data
        for (Map.Entry<PropertyDescriptor, Object> entry : sampleProps.entrySet())
        {
            if (entry.getValue() != null && !pdsWithData.contains(entry.getKey().getName()))
                pdsWithData.add(entry.getKey().getName());
        }
    }

%>
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