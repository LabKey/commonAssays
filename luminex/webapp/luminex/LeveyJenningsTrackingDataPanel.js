/*
 * Copyright (c) 2011-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

Ext.QuickTips.init();

/**
 * Class to create a QueryWebPart to display the tracking data for the selected graph parameters
 *
 * @params controlName
 * @params assayName
 */
LABKEY.LeveyJenningsTrackingDataPanel = Ext.extend(Ext.Component, {
    constructor: function (config)
    {
        // check that the config properties needed are present
        if (!config.controlName || config.controlName == "null")
            throw "You must specify a controlName!";
        if (!config.assayName || config.assayName == "null")
            throw "You must specify a assayName!";

        config.html = '<div id="trackingDataPanel_title" class="lj-report-title"></div>'
                + '<div id="trackingDataPanel_QWP"></div>';

        this.addEvents('appliedGuideSetUpdated', 'plotDataLoading', 'plotDataLoaded');

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.constructor.call(this, config);
    },

    initComponent: function ()
    {
        this.store = new Ext.data.ArrayStore();

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.initComponent.call(this);
    },

    hasReportFilter: function() {
        var userFilters = this.qwp.getUserFilterArray();
        return userFilters.length > 0;
    },

    configurePlotDataStore: function(baseFilters) {
        this.storeLoading();
        this.store = LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore({
            assayName: this.assayName,
            controlName: this.controlName,
            controlType: this.controlType,
            analyte: this.analyte,
            isotype: this.isotype,
            conjugate: this.conjugate,
            filters: baseFilters.concat(this.qwp.getUserFilterArray()),
            maxRows: !this.hasReportFilter() ? this.defaultRowSize : undefined,
            scope: this,
            loadListener: this.storeLoaded,
        });
        this.store.on('exception', function(store, type, action, options, response){
            var errorJson = Ext.util.JSON.decode(response.responseText);
            if (errorJson.exception) {
                Ext.get(document.querySelector('.ljTrendPlot').id).update("<span class='labkey-error'>" + errorJson.exception + "</span>");
            }
        });
    },

    storeLoading: function() {
        this.fireEvent('plotDataLoading', this.store, this.hasGuideSetUpdate);

        // reset hasGuideSetUpdate so that other grid filter changes won't trigger this as well
        this.hasGuideSetUpdate = false;
    },

    storeLoaded: function() {
        this.fireEvent('plotDataLoaded', this.store, this.hasReportFilter());
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function (analyte, isotype, conjugate, hasGuideSetUpdate)
    {
        var shouldClearSelections = true;
        this.hasGuideSetUpdate = hasGuideSetUpdate;

        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // set the grid title based on the selected graph params
        document.getElementById('trackingDataPanel_title').innerHTML =
            $h(this.controlName) + ' Tracking Data for ' + $h(this.analyte)
            + ' - ' + $h(this.isotype === '' ? '[None]' : this.isotype)
            + ' ' + $h(this.conjugate === '' ? '[None]' : this.conjugate);

        var controlTypeColName = this.controlType === 'SinglePoint' ? 'SinglePointControl' : this.controlType;
        const filters = [
            LABKEY.Filter.create('Analyte/Name', this.analyte),
            LABKEY.Filter.create(controlTypeColName + '/Name', this.controlName),
            LABKEY.Filter.create(controlTypeColName + '/Run/Isotype', this.isotype),
            LABKEY.Filter.create(controlTypeColName + '/Run/Conjugate', this.conjugate),
        ];
        if (this.controlType === 'Titration') {
            filters.push(LABKEY.Filter.create('Titration/IncludeInQcReport', true));
        }

        var buttonBarItems = [
            LABKEY.QueryWebPart.standardButtons.views,
            LABKEY.QueryWebPart.standardButtons.exportRows,
            LABKEY.QueryWebPart.standardButtons.print,
        ];
        if (LABKEY.user.canUpdate) {
            buttonBarItems.push({
                text: 'Apply Guide Set',
                requiresSelection: true,
                handler: this.applyGuideSetClicked,
            });
        }
        if (this.controlType === 'Titration') {
            buttonBarItems.push({
                text: 'View 4PL Curves',
                requiresSelection: true,
                handler: this.viewCurvesClicked,
            });
        }

        this.qwp = new LABKEY.QueryWebPart({
            renderTo: 'trackingDataPanel_QWP',
            schemaName: "assay.Luminex." + LABKEY.QueryKey.encodePart(_protocolName),
            queryName: this.controlType === 'SinglePoint' ? 'AnalyteSinglePointControl' : 'AnalyteTitration',
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            maxRows: this.defaultRowSize,
            showUpdateColumn : false,
            showImportDataButton : false,
            showInsertNewButton : false,
            showDeleteButton : false,
            showReports : false,
            frame: 'none',
            filters: filters,
            sort: '-Analyte/Data/AcquisitionDate, -' + controlTypeColName + '/Run/Created',
            scope: this,
            buttonBar: {
                includeStandardButtons: false,
                items: buttonBarItems,
            },
            listeners: {
                scope: this,
                success: function(dataRegion) {
                    // clear selections on each graph param change so we don't inadvertently get selections
                    // row selections from a different analyte/isotype/conjugate
                    if (shouldClearSelections) {
                        dataRegion.clearSelected();
                        shouldClearSelections = false;
                    }

                    this.configurePlotDataStore(filters);
                }
            }
        });

        // enable the trending data grid
        this.enable();
    },

    parseGridSelectionKeys: function(selected, getAnalyteIds) {
        var isSinglePoint = this.controlType === 'SinglePoint';
        var selectedRecords = [], analyteIds = [];
        for (var i = 0; i < selected.length; i++) {
            var keys = selected[i].split(',');
            if (keys.length === 3) {
                // AnalyteTitration selection keys = [Analyte RowId, Titration RowId, Titration RowId]
                // AnalyteSinglePointControl selection keys = [SinglePointControl RowId, Analyte RowId, SinglePointControl RowId]
                var analyteId = isSinglePoint ? keys[1] : keys[0];
                var controlId = isSinglePoint ? keys[0] : keys[1];
                selectedRecords.push({ Analyte: analyteId, ControlId: controlId });
                analyteIds.push(analyteId);
            }
        }
        return getAnalyteIds ? analyteIds : selectedRecords;
    },

    applyGuideSetClicked: function(dataRegion) {
        var scope = dataRegion.scope;

        // get the selected record list from the grid
        dataRegion.getSelected({
            success: function(response) {
                var selectedRecords = scope.parseGridSelectionKeys(response.selected, false);

                // create a pop-up window to display the apply guide set UI
                var win = new Ext.Window({
                    layout: 'fit',
                    width: 1115,
                    height: 500,
                    closeAction: 'close',
                    modal: true,
                    padding: 15,
                    cls: 'extContainer leveljenningsreport',
                    bodyStyle: 'background-color: white;',
                    title: 'Apply Guide Set...',
                    items: [new LABKEY.ApplyGuideSetPanel({
                        selectedRecords: selectedRecords,
                        assayName: scope.assayName,
                        controlName: scope.controlName,
                        controlType: scope.controlType,
                        analyte: scope.analyte,
                        isotype: scope.isotype,
                        conjugate: scope.conjugate,
                        networkExists: scope.networkExists,
                        protocolExists: scope.protocolExists,
                        listeners: {
                            'closeApplyGuideSetPanel': function (hasUpdated)
                            {
                                if (hasUpdated)
                                    scope.fireEvent('appliedGuideSetUpdated');
                                win.close();
                            }
                        }
                    })]
                });

                // for testing, narrow window puts left aligned buttons off of the page
                win.on('show', function(cmp) {
                    var posArr = cmp.getPosition();
                    if (posArr[0] < 0)
                        cmp.setPosition(0, posArr[1]);
                });

                win.show(scope);
            },
            failure: function() {
                LABKEY.Utils.alert('Error', 'Unable to get the tracking data region row selections.');
            }
        });
    },

    viewCurvesClicked: function(dataRegion) {
        var scope = dataRegion.scope;

        // get the selected record list from the grid
        dataRegion.getSelected({
            success: function(response) {
                var analyteIds = scope.parseGridSelectionKeys(response.selected, true);

                LABKEY.Query.selectRows({
                    schemaName: "assay.Luminex." + LABKEY.QueryKey.encodePart(_protocolName),
                    queryName: this.controlType === 'SinglePoint' ? 'AnalyteSinglePointControl' : 'AnalyteTitration',
                    columns: 'Analyte/Data/Run/RowId',
                    filterArray: [LABKEY.Filter.create('Analyte', analyteIds, LABKEY.Filter.Types.IN)],
                    success: function(data) {
                        var runIds = [];
                        for (var i = 0; i < data.rows.length; i++) {
                            runIds.push(data.rows[i]['Analyte/Data/Run/RowId']);
                        }

                        // create a pop-up window to display the plot
                        var plotDiv = new Ext.Container({
                            height: 600,
                            width: 900,
                            autoEl: {tag: 'div'}
                        });
                        var pdfDiv = new Ext.Container({
                            hidden: true,
                            autoEl: {tag: 'div'}
                        });

                        var yAxisScaleDefault = 'Linear';
                        var yAxisScaleStore = new Ext.data.ArrayStore({
                            fields: ['value'],
                            data: [['Linear'], ['Log']]
                        });

                        var yAxisColDefault = 'FIBackground', yAxisColDisplayDefault = 'FI-Bkgd';
                        var yAxisColStore = new Ext.data.ArrayStore({
                            fields: ['name', 'value'],
                            data: [['FI', 'FI'], ['FI-Bkgd', 'FIBackground'], ['FI-Bkgd-Neg', 'FIBackgroundNegative']]
                        });

                        var legendColDefault = 'Name';
                        var legendColStore = new Ext.data.ArrayStore({
                            fields: ['name', 'value'],
                            data: [['Assay Type', 'AssayType'], ['Experiment Performer', 'ExpPerformer'], ['Assay Id', 'Name'], ['Notebook No.', 'NotebookNo']]
                        });

                        var win = new Ext.Window({
                            layout: 'fit',
                            width: 900,
                            minWidth: 600,
                            height: 660,
                            minHeight: 500,
                            closeAction: 'hide',
                            modal: true,
                            cls: 'extContainer',
                            title: 'Curve Comparison',
                            items: [plotDiv, pdfDiv],
                            // stash the default values for the plot options on the win component
                            runIds: runIds,
                            yAxisScale: yAxisScaleDefault,
                            yAxisCol: yAxisColDefault,
                            yAxisDisplay: yAxisColDisplayDefault,
                            legendCol: legendColDefault,
                            buttonAlign: 'left',
                            buttons: [{
                                xtype: 'label',
                                text: 'Y-Axis:'
                            },{
                                xtype: 'combo',
                                width: 80,
                                id: 'curvecomparison-yaxis-combo',
                                store: yAxisColStore ,
                                displayField: 'name',
                                valueField: 'value',
                                mode: 'local',
                                editable: false,
                                forceSelection: true,
                                triggerAction: 'all',
                                value: yAxisColDefault,
                                listeners: {
                                    scope: scope,
                                    select: function(cmp, record) {
                                        win.yAxisCol = record.data.value;
                                        win.yAxisDisplay = record.data.name;
                                        scope.updateCurvesPlot(win, plotDiv.getId(), false);
                                    }
                                }
                            },{
                                xtype: 'label',
                                text: 'Scale:'
                            },{
                                xtype: 'combo',
                                width: 75,
                                id: 'curvecomparison-scale-combo',
                                store: yAxisScaleStore ,
                                displayField: 'value',
                                valueField: 'value',
                                mode: 'local',
                                editable: false,
                                forceSelection: true,
                                triggerAction: 'all',
                                value: yAxisScaleDefault,
                                listeners: {
                                    scope: scope,
                                    select: function(cmp, record) {
                                        win.yAxisScale = record.data.value;
                                        scope.updateCurvesPlot(win, plotDiv.getId(), false);
                                    }
                                }
                            },{
                                xtype: 'label',
                                text: 'Legend:'
                            },{
                                xtype: 'combo',
                                width: 140,
                                id: 'curvecomparison-legend-combo',
                                store: legendColStore ,
                                displayField: 'name',
                                valueField: 'value',
                                mode: 'local',
                                editable: false,
                                forceSelection: true,
                                triggerAction: 'all',
                                value: legendColDefault,
                                listeners: {
                                    scope: scope,
                                    select: function(cmp, record) {
                                        win.legendCol = record.data.value;
                                        scope.updateCurvesPlot(win, plotDiv.getId(), false);
                                    }
                                }
                            },
                                '->',
                                {
                                    xtype: 'button',
                                    text: 'Export to PDF',
                                    handler: function (btn) {
                                        scope.updateCurvesPlot(win, pdfDiv.getId(), true);
                                    },
                                    scope: scope
                                }, {
                                    xtype: 'button',
                                    text: 'Close',
                                    handler: function () {
                                        win.hide();
                                    }
                                }],
                            listeners: {
                                scope: scope,
                                'resize': function (w, width, height) {
                                    // update the curve plot to the new size of the window
                                    scope.updateCurvesPlot(win, plotDiv.getId(), false);
                                }
                            }
                        });

                        // for testing, narrow window puts left aligned buttons off of the page
                        win.on('show', function(cmp) {
                            var posArr = cmp.getPosition();
                            if (posArr[0] < 0)
                                cmp.setPosition(0, posArr[1]);
                        });

                        win.show(scope);
                    },
                    failure: function() {
                        LABKEY.Utils.alert('Error', 'Unable to get the tracking data run RowIds from the selections.');
                    }
                });
            },
            failure: function() {
                LABKEY.Utils.alert('Error', 'Unable to get the tracking data region row selections.');
            }
        });
    },

    updateCurvesPlot: function (win, divId, outputPdf)
    {
        win.getEl().mask("loading curves...", "x-mask-loading");

        // build the config object of the properties that will be needed by the R report
        var config = {reportId: 'module:luminex/CurveComparisonPlot.r', showSection: 'Curve Comparison Plot'};
        config['RunIds'] = win.runIds.join(";");
        config['Protocol'] = this.assayName;
        config['Titration'] = this.controlName;
        config['Analyte'] = this.analyte;
        config['YAxisScale'] = !outputPdf ? win.yAxisScale  : 'Linear';
        config['YAxisCol'] = win.yAxisCol;
        config['YAxisDisplay'] = win.yAxisDisplay;
        config['LegendCol'] = win.legendCol;
        config['MainTitle'] = $h(this.controlName) + ' 4PL for ' + $h(this.analyte)
                + ' - ' + $h(this.isotype === '' ? '[None]' : this.isotype)
                + ' ' + $h(this.conjugate === '' ? '[None]' : this.conjugate);
        config['PlotHeight'] = win.getHeight();
        config['PlotWidth'] = win.getWidth();
        if (outputPdf) config['PdfOut'] = true;

        // call and display the Report webpart
        new LABKEY.WebPart({
            partName: 'Report',
            renderTo: divId,
            frame: 'none',
            partConfig: config,
            success: function () {
                this.getEl().unmask();

                if (outputPdf) {
                    // ugly way of getting the href for the pdf file and open it
                    if (Ext.getDom(divId)) {
                        var html = Ext.getDom(divId).innerHTML;
                        html = html.replace(/&amp;/g, "&");
                        var pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('&attachment=true'));
                        if (pdfHref.indexOf("deleteFile") == -1) {
                            pdfHref = pdfHref + "&deleteFile=false";
                        }
                        window.location = pdfHref + "&attachment=true";
                    }
                }
            },
            failure: function (response) {
                Ext.get(plotDiv.getId()).update("Error: " + response.statusText);
                this.getEl().unmask();
            },
            scope: win
        }).render();
    },
});
