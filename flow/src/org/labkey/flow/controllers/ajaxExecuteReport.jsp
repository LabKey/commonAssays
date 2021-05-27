<%
/*
 * Copyright (c) 2020 LabKey Corporation
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
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.flow.controllers.ReportsController.ExecuteForm" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    Pair<ExecuteForm, FlowReport> bean = (Pair<ExecuteForm, FlowReport>)getModelBean();
    ExecuteForm form = bean.first;
%>
<labkey:errors/>

<labkey:panel title="Report" type="portal">
    <div id="report-view">
        <i aria-hidden="true" class="fa fa-spinner fa-pulse" ></i> Loading...
    </div>
</labkey:panel>
<script type="text/javascript">
    let reportView = document.getElementById('report-view');
    LABKEY.Ajax.request({
        url: <%=q(form.url(ReportsController.ExecuteReportAction.class))%>,
        method: 'POST',
        success: function (resp) {
            console.info("Success executing report", resp);
            LABKEY.Utils.loadAjaxContent(resp, 'report-view');
        },
        failure: function (resp) {
            console.error("Failed to retrieve report resutls", resp);
            reportView.innerHTML = '<span class="labkey-error">Failed to retrieve report results</span>';
        }
    })
</script>