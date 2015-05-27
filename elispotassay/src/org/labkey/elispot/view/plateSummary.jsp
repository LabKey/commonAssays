<%
/*
 * Copyright (c) 2008-2015 LabKey Corporation
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
<%@ page import="org.labkey.api.util.UniqueID"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.elispot.ElispotController" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        resources.add(ClientDependency.fromPath("elispot/PlateSummary.js"));
        resources.add(ClientDependency.fromPath("elispot/PlateSummary.css"));
        return resources;
    }
%>
<%
    JspView<ElispotController.PlateSummaryBean> me = (JspView<ElispotController.PlateSummaryBean>)HttpView.currentView();
    ElispotController.PlateSummaryBean bean = me.getModelBean();

    String renderId = "plate-summary-div-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>

<script type="text/javascript">

    Ext4.onReady(function(){
        var panel = Ext4.create('LABKEY.ext4.PlateSummary', {
            runId       : <%=bean.getRun()%>,
            width       : 1500,
            renderTo    : '<%= renderId %>',
            isFluorospot: <%=bean.isFluorospot()%>
        });
    });
</script>

<div id='<%= renderId%>'></div>
