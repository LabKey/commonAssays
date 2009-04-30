<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.study.WellData"%>
<%@ page import="org.labkey.api.study.assay.AbstractAssayProvider"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.nab.DilutionSummary" %>
<%@ page import="org.labkey.nab.Luc5Assay" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.nab.NabAssayProvider" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Map" %>
<%@page extends="org.labkey.api.jsp.JspBase"%>

<%
    JspView<NabAssayController.GraphSelectedBean> me = (JspView<NabAssayController.GraphSelectedBean>) HttpView.currentView();
    NabAssayController.GraphSelectedBean bean = me.getModelBean();

    String errs = PageFlowUtil.getStrutsError(request, "main");
    if (null != StringUtils.trimToNull(errs))
    {
        out.write("<span class=\"labkey-error\">");
        out.write(errs);
        out.write("</span>");
    }

    StringBuilder chartURL = new StringBuilder("multiGraph.view?");
    for (int dataId : bean.getGraphableObjectIds())
    {
        chartURL.append("id=").append(dataId);
        chartURL.append("&");
    }
    chartURL.append("protocolId=").append(bean.getProtocol().getRowId());
%>
<% me.include(bean.getQueryView(), out); %>
<br>
<img src="<%= chartURL %>">