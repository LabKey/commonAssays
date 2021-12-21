<%
/*
 * Copyright (c) 2012-2018 LabKey Corporation
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
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.elisa.ElisaController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("vis/vis");
        dependencies.add("gen/runDetails");
        //dependencies.add("http://localhost:3001/runDetails.js");
    }
%>
<%
    JspView<ElisaController.RunDetailsForm> me = (JspView<ElisaController.RunDetailsForm>) HttpView.currentView();
    ElisaController.RunDetailsForm form = me.getModelBean();
    String appId = "run-details-" + getRequestScopedUID();
%>

<div id="<%=h(appId)%>"></div>

<script type="application/javascript">
    (function() {
        LABKEY.App.loadApp('elisaRunDetails', <%=q(appId)%>, {
            protocolId: <%=form.getProtocolId()%>,
            runId: <%=form.getRunId()%>,
            schemaName: <%=q(form.getSchemaName())%>
        });
    })(jQuery);
</script>

