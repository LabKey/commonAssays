<%
/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

/**
* User: cnathe
* Date: Sept 19, 2011
*/

%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.luminex.LeveyJenningsForm" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("luminexLeveyJennings");
        dependencies.add("experiment/QCFlagToggleWindow.js");
        dependencies.add("fileAddRemoveIcon.css");
    }
%>
<%
    JspView<LeveyJenningsForm> me = (JspView<LeveyJenningsForm>) HttpView.currentView();
    LeveyJenningsForm bean = me.getModelBean();
%>

<div class="leveljenningsreport">
<table>
    <tr>
        <td rowspan="2" valign="top"><div id="graphParamsPanel"></div></td>
        <td><div id="guideSetOverviewPanel" style="padding-left: 15px;"></div></td>
    </tr>
    <tr>
        <td><div id="ljPlotPanel" style="padding: 15px 0 0 15px;"></div></td>
    </tr>
</table>
<div id="trackingDataPanel" style="padding-top: 15px;"></div>
</div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">

        var $h = Ext.util.Format.htmlEncode;

        // the default number of records to return for the report when no start and end date are provided
        var defaultRowSize = 30;

        // local variables for storing the selected graph parameters
        var _protocolId, _protocolName, _controlName, _controlType, _analyte, _isotype, _conjugate,
                _protocolExists = false, _networkExists = false,
                _has4PLCurveFit = false, _has5PLCurveFit = false;

        function init()
        {
            _controlName = <%=q(bean.getControlName())%>;
            _controlType = <%=q(bean.getControlType().toString())%>;
            _protocolName = <%=q(bean.getProtocol().getName())%>;
            _protocolId = <%=bean.getProtocol().getRowId()%>;

            if ("" === _controlType || "" === _controlName)
            {
                Ext.get('graphParamsPanel').update("<span class='labkey-error'>Error: no control name specified.</span>");
                return;
            }
            if ('SinglePoint' !== _controlType && 'Titration' !== _controlType)
            {
                Ext.get('graphParamsPanel').update("<span class='labkey-error'>Error: unsupported control type: '" + _controlType + "'</span>");
                return;
            }
            if ("" === _protocolName)
            {
                Ext.get('graphParamsPanel').update("<span class='labkey-error'>Error: no protocol specified.</span>");
                return;
            }

            var getByNameQueryComplete = false, executeSqlQueryComplete = false, getCurveFitTypes = false;
            var loader = function() {
                if (getByNameQueryComplete && executeSqlQueryComplete) {
                    initializeReportPanels();
                }
            };

            // Query to see if 4PL and/or 5PL curve fit data exists for this assay
            LABKEY.Query.selectDistinctRows({
                schemaName: 'assay.Luminex.' + _protocolName,
                queryName: 'CurveFit',
                column: 'CurveType',
                scope: this,
                success: function(results){
                    var curveTypes = results.values;
                    _has4PLCurveFit = curveTypes.indexOf('Four Parameter') > -1;
                    _has5PLCurveFit = curveTypes.indexOf('Five Parameter') > -1;
                    getCurveFitTypes = true;
                    loader();
                }
            });

            // Query the assay design to check for the required columns for the L-J report and the existance of Network and Protocol columns
            LABKEY.Assay.getByName({
                name: _protocolName,
                success: function(data) {

                    var missingColumns = ['isotype', 'conjugate', 'acquisitiondate'];
                    var runFields = data[0].domains[_protocolName + ' Run Fields'];
                    runFields = runFields.concat(data[0].domains[_protocolName + ' Excel File Run Properties']);
                    for (var i=0; i<runFields.length; i++)
                    {
                        var index = missingColumns.indexOf(runFields[i].name.toLowerCase());
                        if (index !== -1) {
                            missingColumns.splice(index, 1);
                        }
                    }
                    if (missingColumns.length > 0)
                    {
                        Ext.get('graphParamsPanel').update("<span class='labkey-error'>Error: one or more of the required properties ("
                            + missingColumns.join(',') + ") for the report do not exist in '" + $h(_protocolName) + "'.<span>");
                        return;
                    }

                    var batchFields = data[0].domains[_protocolName + ' Batch Fields'];
                    for (var i=0; i<batchFields.length; i++) {
                        if (batchFields[i].fieldKey.toLowerCase() === "network") {
                            _networkExists = true;
                        }
                        if (batchFields[i].fieldKey.toLowerCase() === "customprotocol") {
                            _protocolExists = true;
                        }
                    }

                    getByNameQueryComplete = true;
                    loader();
                }
            });

            // verify that the given titration/singlepointcontrol exists and has run's associated with it as a Standard or QC Control
            var sql;
            if ('Titration' === _controlType) {
                sql = "SELECT COUNT(*) AS RunCount FROM Titration WHERE Name=CONTROL_NAME AND IncludeInQcReport=true";
            }
            else {
                sql = "SELECT COUNT(*) AS RunCount FROM SinglePointControl WHERE Name=CONTROL_NAME";
            }
            LABKEY.Query.executeSql({
                containerFilter: LABKEY.Query.containerFilter.allFolders,
                schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(_protocolName),
                sql: 'PARAMETERS(CONTROL_NAME VARCHAR) ' + sql,
                parameters: {CONTROL_NAME: _controlName},
                success: function(data) {
                    if (data.rows.length === 0 || data.rows[0]['RunCount'] === 0)
                    {
                        Ext.get('graphParamsPanel').update("<span class='labkey-error'>Error: there were no records found in '"
                            + $h(_protocolName) + "' for '" + $h(_controlName) + "'.</span>");
                    }
                    else
                    {
                        executeSqlQueryComplete = true;
                        loader();
                    }
                },
                failure: function(response) {
                    Ext.get('graphParamsPanel').update("<span class='labkey-error'>" + response.exception + "</span>");
                }
            });
        }

        function initializeReportPanels()
        {
            // initialize the graph parameters selection panel
            var graphParamsPanel = new LABKEY.LeveyJenningsGraphParamsPanel({
                renderTo: 'graphParamsPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                listeners: {
                    'applyGraphBtnClicked': function(analyte, isotype, conjugate){
                        _analyte = analyte;
                        _isotype = isotype;
                        _conjugate = conjugate;

                        guideSetPanel.graphParamsSelected(analyte, isotype, conjugate);
                        trendPlotPanel.graphParamsSelected(analyte, isotype, conjugate);
                        trackingDataPanel.graphParamsSelected(analyte, isotype, conjugate, null, null);
                    },
                    'graphParamsChanged': function(){
                        guideSetPanel.disable();
                        trendPlotPanel.disable();
                        trackingDataPanel.disable();
                    }
                }
            });

            var resizer = new Ext.Resizable('graphParamsPanel', {
                handles: 'e',
                minWidth: 225
            });
            resizer.on('resize', function(rez, width, height){
                graphParamsPanel.setWidth(width);
                graphParamsPanel.doLayout();
            });

            // initialize the panel for user to interact with the current guide set (edit and create new)
            var guideSetPanel = new LABKEY.LeveyJenningsGuideSetPanel({
                renderTo: 'guideSetOverviewPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayId: _protocolId,
                assayName: _protocolName,
                networkExists: _networkExists,
                protocolExists: _protocolExists,
                has4PLCurveFit: _has4PLCurveFit,
                has5PLCurveFit: _has5PLCurveFit,
                listeners: {
                    'currentGuideSetUpdated': function() {
                        guideSetPanel.toggleExportBtn(false);
                        trendPlotPanel.setTrendPlotLoading();
                        trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, trendPlotPanel.getStartDate(), trendPlotPanel.getEndDate());
                    },
                    'exportPdfBtnClicked': function() {
                        trendPlotPanel.exportToPdf();
                    },
                    'guideSetMetricsUpdated': function() {
                        trackingDataPanel.fireEvent('appliedGuideSetUpdated');
                    }
                }
            });

            // initialize the panel that displays the R plot for the trend plotting of EC50, AUC, and High MFI
            var trendPlotPanel = new LABKEY.LeveyJenningsTrendPlotPanel({
                renderTo: 'ljPlotPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                defaultRowSize: defaultRowSize,
                networkExists: _networkExists,
                protocolExists: _protocolExists,
                has4PLCurveFit: _has4PLCurveFit,
                has5PLCurveFit: _has5PLCurveFit,
                listeners: {
                    'reportFilterApplied': function(startDate, endDate, network, networkAny, protocol, protocolAny) {
                        trendPlotPanel.setTrendPlotLoading();
                        trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, startDate, endDate, network, networkAny, protocol, protocolAny);
                    },
                    'togglePdfBtn': function(toEnable) {
                        guideSetPanel.toggleExportBtn(toEnable);
                    }
                }
            });

            // initialize the grid panel to display the tracking data
            var trackingDataPanel = new LABKEY.LeveyJenningsTrackingDataPanel({
                renderTo: 'trackingDataPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                defaultRowSize: defaultRowSize,
                networkExists: _networkExists,
                protocolExists: _protocolExists,
                has4PLCurveFit: _has4PLCurveFit,
                has5PLCurveFit: _has5PLCurveFit,
                listeners: {
                    'appliedGuideSetUpdated': function() {
                        guideSetPanel.toggleExportBtn(false);
                        trendPlotPanel.setTrendPlotLoading();
                        trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, trendPlotPanel.getStartDate(), trendPlotPanel.getEndDate(),
                                trendPlotPanel.network, trendPlotPanel.networkAny, trendPlotPanel.protocol, trendPlotPanel.protocolAny);
                    },
                    'trackingDataLoaded': function(store) {
                        trendPlotPanel.trackingDataLoaded(store);
                    }
                }
            });
        }

        Ext.onReady(init);
</script>
