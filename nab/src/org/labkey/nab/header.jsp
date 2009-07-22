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
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.NabController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.HeaderBean> me = (JspView<NabController.HeaderBean>) HttpView.currentView();
    NabController.HeaderBean bean = me.getModelBean();
%>
<%
    if (bean.showNewRunLink())
    {
%>
<%= textLink("New Run", "begin.view?.lastFilter=1") %>
<%
    }
%>
<%= textLink("Previous Runs", "runs.view?.lastFilter=1") %>
<%= textLink("Previous Runs By Sample", "sampleList.view?.lastFilter=1") %>
<%
    if (bean.showPrintView())
    {
%>
[<a href="<%=h(bean.getPrintURL())%>" target="_blank">Print View</a>]
<%
    }
    if (bean.getDatafileURL() != null)
    {
%>
[<a href="<%=h(bean.getDatafileURL())%>">Download Datafile</a>]
<%
    }
    if (bean.getCustomizeURL() != null)
    {
%>
[<a href="<%=h(bean.getCustomizeURL())%>">Customize View</a>]
<%
    }
%>
<br>
<span class="labkey-error"><%=PageFlowUtil.getStrutsError(request, "main")%></span>
