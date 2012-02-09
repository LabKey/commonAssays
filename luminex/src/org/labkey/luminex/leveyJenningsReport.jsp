<%
/*
 * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.TitrationForm" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>

<%
    JspView<TitrationForm> me = (JspView<TitrationForm>) HttpView.currentView();
    TitrationForm bean = me.getModelBean();
%>

<table cellpadding="0" cellspacing="15">
    <tr>
        <td rowspan="2"><div id="graphParamsPanel"></div></td>
        <td><div id="guideSetOverviewPanel"></div></td>
    </tr>
    <tr>
        <td><div id="rPlotPanel"></div></td>
    </tr>
</table>
<div id="trackingDataPanelTitle" style="margin-left:15px"></div>
<div id="trackingDataPanel" style="margin-left:15px"></div>

<script type="text/javascript">
    LABKEY.requiresScript("luminex/LeveyJenningsGraphParamsPanel.js");
    LABKEY.requiresScript("luminex/LeveyJenningsGuideSetPanel.js");
    LABKEY.requiresScript("luminex/LeveyJenningsTrendPlotPanel.js");
    LABKEY.requiresScript("luminex/LeveyJenningsTrackingDataPanel.js");
    LABKEY.requiresScript("luminex/ManageGuideSetPanel.js");
    LABKEY.requiresScript("luminex/ApplyGuideSetPanel.js");
    LABKEY.requiresScript("Experiment/QCFlagToggleWindow.js");
    LABKEY.requiresCss("luminex/LeveyJenningsReport.css");

    var $h = Ext.util.Format.htmlEncode;

    // the default number of records to return for the report when no start and end date are provided
    var defaultRowSize = 30;

    // local variables for storing the selected graph parameters
    var _protocolName, _titration, _analyte, _isotype, _conjugate;

    Ext.onReady(init);
    function init()
    {
        _titration = <%=PageFlowUtil.jsString(bean.getTitration())%>;
        _protocolName = <%=PageFlowUtil.jsString(bean.getProtocol().getName())%>;

        if ("" == _titration)
        {
            Ext.get('graphParamsPanel').update("Error: no titration specified.");
            return;
        }
        if ("" == _protocolName)
        {
            Ext.get('graphParamsPanel').update("Error: no protocol specified.");
            return;
        }

        // verify that the given titration and protocol exist, and that the required report properties exist in the protocol
        var reqColumns = ['Titration/Name', 'Titration/Run/Isotype', 'Titration/Run/Conjugate', 'Analyte/Data/AcquisitionDate'];
        LABKEY.Query.selectRows({
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            schemaName: 'assay',
            queryName: _protocolName + ' AnalyteTitration',
            filterArray: [LABKEY.Filter.create('Titration/Name', _titration)],
            columns: reqColumns.join(','),
            maxRows: 1,
            success: function(data) {
                if (data.rows.length == 0)
                    Ext.get('graphParamsPanel').update("Error: there were no records found in '" + $h(_protocolName) + "' for '" + $h(_titration) + "'.");
                else
                {
                    var missingColumns = '';
                    var separator = '';
                    // check that all of the required properties for the report exist
                    for (var i = 0; i < reqColumns.length; i++)
                    {
                        if (!(reqColumns[i] in data.rows[0]))
                        {
                            missingColumns += separator + reqColumns[i];
                            separator = ", ";
                        }
                    }
                    if (missingColumns.length > 0)
                    {
                        Ext.get('graphParamsPanel').update("Error: one or more of the required properties (" + missingColumns + ") for the report do not exist in '" + $h(_protocolName) + "'.");
                        return;
                    }

                    initializeReportPanels();
                }
            },
            failure: function(response) {
                Ext.get('graphParamsPanel').update("Error: " + response.exception);
            }
        });
    }

    function initializeReportPanels()
    {
        // initialize the graph parameters selection panel
        var graphParamsPanel = new LABKEY.LeveyJenningsGraphParamsPanel({
            renderTo: 'graphParamsPanel',
            cls: 'extContainer',
            titration: _titration,
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
            titration: _titration,
            assayName: _protocolName,
            listeners: {
                'currentGuideSetUpdated': function() {
                    trendPlotPanel.setTabsToRender();
                    trendPlotPanel.displayTrendPlot();
                    trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, trendPlotPanel.getStartDate(), trendPlotPanel.getEndDate());
                },
                'exportPdfBtnClicked': function() {
                    if (trendPlotPanel.getPdfHref())
                    {
                        window.location = trendPlotPanel.getPdfHref() + "&attachment=true&deleteFile=false";
                    }
                }
            }
        });

        // initialize the panel that displays the R plot for the trend plotting of EC50, AUC, and High MFI
        var trendPlotPanel = new LABKEY.LeveyJenningsTrendPlotPanel({
            renderTo: 'rPlotPanel',
            cls: 'extContainer',
            titration: _titration,
            assayName: _protocolName,
            defaultRowSize: defaultRowSize,
            listeners: {
                'reportDateRangeApplied': function(startDate, endDate) {
                    trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, startDate, endDate);
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
            titration: _titration,
            assayName: _protocolName,
            defaultRowSize: defaultRowSize,
            listeners: {
                'appliedGuideSetUpdated': function() {
                    trendPlotPanel.setTabsToRender();
                    trendPlotPanel.displayTrendPlot();
                    trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, trendPlotPanel.getStartDate(), trendPlotPanel.getEndDate());
                }
            }
        });
    }
</script>
