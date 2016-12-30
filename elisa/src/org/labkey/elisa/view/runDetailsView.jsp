<%
/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.data.PropertyManager" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.reports.permissions.ShareReportPermission" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.util.ExtUtil" %>
<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.elisa.ElisaController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext4");
        dependencies.add("vischart");
        dependencies.add("/elisa/runDetailsPanel.js");
        dependencies.add("/elisa/runDataPanel.js");
    }
%>
<%
    JspView<ElisaController.GenericReportForm> me = (JspView<ElisaController.GenericReportForm>) HttpView.currentView();
    ViewContext ctx = getViewContext();
    Container c = getContainer();
    User user = getUser();
    ElisaController.GenericReportForm form = me.getModelBean();
    String numberFormat = PropertyManager.getProperties(c, "DefaultStudyFormatStrings").get("NumberFormatString");
    String numberFormatFn;
    if(numberFormat == null)
    {
        numberFormat = Formats.f1.toPattern();
    }
    numberFormatFn = ExtUtil.toExtNumberFormatFn(numberFormat);

    String renderId = "chart-wizard-report-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());

    ActionURL filterUrl = ctx.cloneActionURL().deleteParameters();
    filterUrl.addFilter(QueryView.DATAREGIONNAME_DEFAULT, FieldKey.fromParts("Run", "RowId"), CompareType.EQUAL, form.getRunId());
    ActionURL baseUrl = ctx.cloneActionURL().addParameter("filterUrl", filterUrl.getLocalURIString());
    ObjectMapper jsonMapper = new ObjectMapper();
%>
<div id="<%=h(renderId)%>" style="width:100%;"></div>
<script type="text/javascript">
    Ext4.QuickTips.init();

    Ext4.onReady(function(){

        Ext4.create('LABKEY.elisa.RunDetailsPanel', {
            renderTo        : <%=q(renderId)%>,
            schemaName      : <%=q(form.getSchemaName())%>,
            queryName       : <%=q(form.getQueryName())%>,
            runTableName    : <%=q(form.getRunTableName())%>,
            runId           : <%=form.getRunId()%>,
            dataRegionName  : <%=q(form.getDataRegionName())%>,
            baseUrl         : <%=q(baseUrl.getLocalURIString())%>
        });

        Ext4.create('LABKEY.ext4.GenericChartPanel', {
            renderTo        : <%=q(renderId)%>,
            height          : 500,
            padding         : '20px 0',
            schemaName      : <%=q(form.getSchemaName() != null ? form.getSchemaName() : null) %>,
            queryName       : <%=q(form.getQueryName() != null ? form.getQueryName() : null) %>,
            dataRegionName  : <%=q(form.getDataRegionName())%>,
            renderType      : <%=q(form.getRenderType())%>,
            baseUrl         : <%=q(baseUrl.getLocalURIString())%>,
            allowShare      : <%=c.hasPermission(user, ShareReportPermission.class)%>,
            isDeveloper     : <%=user.isDeveloper()%>,
            hideSave        : <%=user.isGuest()%>,
            hideViewData    : true,
            autoColumnYName  : <%=q(form.getAutoColumnYName() != null ? form.getAutoColumnYName() : null)%>,
            autoColumnXName  : <%=q(form.getAutoColumnXName() != null ? form.getAutoColumnXName() : null)%>,
            defaultNumberFormat: eval(<%=q(numberFormatFn)%>),
            allowEditMode   : <%=!user.isGuest() && c.hasPermission(user, UpdatePermission.class)%>,
            curveFit        : {type : 'linear', min: 0, max: 100, points: 5, params : <%=text(jsonMapper.writeValueAsString(form.getFitParams()))%>},
            defaultTitleFn  : function(){ return 'Calibration Curve '; }
        });

        Ext4.create('LABKEY.elisa.RunDataPanel', {
            renderTo        : <%=q(renderId)%>,
            schemaName      : <%=q(form.getSchemaName())%>,
            queryName       : <%=q(form.getQueryName())%>,
            sampleColumns   : <%=text(jsonMapper.writeValueAsString(form.getSampleColumns()))%>
        });
    });

    function customizeGenericReport(elementId) {

        function initPanel() {
            var panel = Ext4.getCmp(elementId);

            if (panel) { panel.customize(); }
        }
        Ext4.onReady(initPanel);
    }

</script>

