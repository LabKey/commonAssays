<%
/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.data.DataRegion" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.HeaderBean> me = (JspView<NabController.HeaderBean>) HttpView.currentView();
    NabController.HeaderBean bean = me.getModelBean();
    Map<String, String> print = new HashMap<String, String>();
    print.put("target", "_blank");
%>
<%
    if (bean.showNewRunLink())
    {
%>
<%= textLink("New Run", "begin.view?" + DataRegion.LAST_FILTER_PARAM + "=1") %>
<%
    }
%>
<%= textLink("Previous Runs", "runs.view?" + DataRegion.LAST_FILTER_PARAM + "=1") %>
<%= textLink("Previous Runs By Sample", "sampleList.view?" + DataRegion.LAST_FILTER_PARAM + "=1") %>
<%
    if (bean.showPrintView())
    {
%>
<%=textLink("Print View", bean.getPrintURL().getLocalURIString(), "", "", print)%>
<%
    }
    if (bean.getDatafileURL() != null)
    {
%>
<%=textLink("Download Datafile", bean.getDatafileURL())%>
<% } %>
<br>
<labkey:errors/>
