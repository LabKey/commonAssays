<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
/*
 * Copyright (c) 2006-2009 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@page extends="org.labkey.api.jsp.JspBase"%>

<labkey:errors/>
<%
    JspView<NabAssayController.GraphSelectedBean> me = (JspView<NabAssayController.GraphSelectedBean>) HttpView.currentView();
    NabAssayController.GraphSelectedBean bean = me.getModelBean();

    StringBuilder chartURL = new StringBuilder("multiGraph.view?");
    for (int dataId : bean.getGraphableObjectIds())
    {
        chartURL.append("id=").append(dataId);
        chartURL.append("&");
    }
    chartURL.append("protocolId=").append(bean.getProtocol().getRowId());
    if (bean.getCaptionColumn() != null)
        chartURL.append("&captionColumn=").append(u(bean.getCaptionColumn()));
    if (bean.getChartTitle() != null)
        chartURL.append("&chartTitle=").append(u(bean.getChartTitle()));
%>
<img src="<%= chartURL %>">
<br>
<% me.include(bean.getQueryView(), out); %>
