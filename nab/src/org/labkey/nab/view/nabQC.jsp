<%
    /*
     * Copyright (c) 2016-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.assay.dilution.DilutionAssayRun" %>
<%@ page import="org.labkey.api.assay.nab.Luc5Assay" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("nabqc");
    }
%>
<%
    JspView<NabAssayController.NabQCForm> me = (JspView<NabAssayController.NabQCForm>) HttpView.currentView();
    NabAssayController.NabQCForm bean = me.getModelBean();
    DilutionAssayRun assay = bean.getAssay();
    ObjectMapper jsonMapper = new ObjectMapper();

    // run properties array
    List<Map<String, String>> runProperties = new ArrayList<>();
    for (Map.Entry<String, Object> entry : bean.getRunDisplayProperties().entrySet())
    {
        runProperties.add(PageFlowUtil.map(
                "name", String.valueOf(entry.getKey()),
                "value", PageFlowUtil.filter(String.valueOf(entry.getValue()))
        ));
    }

    // control properties
    List<Map<String, String>> controlProperties = new ArrayList<>();
    for (Plate plate : assay.getPlates())
    {
        controlProperties.add(PageFlowUtil.map(
                "controlRange", Luc5Assay.intString(assay.getControlRange(plate, null)),
                "virusControlMean", Luc5Assay.intString(assay.getVirusControlMean(plate, null)),
                "virusControlPlusMinus", Luc5Assay.percentString(assay.getVirusControlPlusMinus(plate, null)),
                "cellControlMean", Luc5Assay.intString(assay.getCellControlMean(plate, null)),
                "cellControlPlusMinus", Luc5Assay.percentString(assay.getCellControlPlusMinus(plate, null))));
    }
%>
<div id="nabQCDiv"></div>
<script type="text/javascript">
    Ext4.onReady(function() {
        Ext4.create('LABKEY.ext4.NabQCPanel', {
            renderTo    : 'nabQCDiv',
            edit        : <%=bean.isEdit()%>,
            runId       : <%=bean.getRunId()%>,
            returnUrl   : <%=q(bean.getReturnUrl(getContainer()).getLocalURIString())%>,
            runName     : <%=q(assay.getRunName())%>,
            runProperties : <%=text(jsonMapper.writeValueAsString(runProperties))%>,
            controlProperties : <%=text(jsonMapper.writeValueAsString(controlProperties))%>
        });
    });
</script>