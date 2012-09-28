<%
/*
 * Copyright (c) 2008-2012 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.microarray.MicroarrayStatisticsView" %>
<%@ page import="org.labkey.api.view.JspView" %><%
    JspView<MicroarrayStatisticsView.MicroarraySummaryBean> view = (JspView<MicroarrayStatisticsView.MicroarraySummaryBean>) HttpView.currentView();
    MicroarrayStatisticsView.MicroarraySummaryBean bean = view.getModelBean();
%>
There
<%
if (bean.getRunCount() == 1)
{ %>
    is <%= bean.getRunCount() %> hyb <%
}
else
{ %>
    are <%= bean.getRunCount() %> hybs <%
} %>
in this folder.
