/*
 * Copyright (c) 2011-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
 * User: cnathe
 * Date: Sept 21, 2011
 */

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

        // initialize an export button for the toolbar
        // this.exportMenuButton = new Ext.Button({
        //     text: 'Export',
        //     menu: [
        //         {
        //             text: 'Excel',
        //             handler: function ()
        //             {
        //                 this.exportData('excel');
        //             },
        //             scope: this
        //         },
        //         {
        //             text: 'TSV',
        //             handler: function ()
        //             {
        //                 this.exportData('tsv');
        //             },
        //             scope: this
        //         }
        //     ]
        // });

        // initialize the apply guide set button to the toolbar
        // this.applyGuideSetButton = new Ext.Button({
        //     disabled: true,
        //     text: 'Apply Guide Set',
        //     handler: this.applyGuideSetClicked,
        //     scope: this
        // });

        // initialize the view curves button to the toolbar
        // this.viewCurvesButton = new Ext.Button({
        //     disabled: true,
        //     text: 'View 4PL Curves',
        //     tooltip: 'Click to view overlapping curves for the selected runs.',
        //     handler: this.viewCurvesClicked,
        //     scope: this
        // });

        // if the controlType is Titration, show the viewCurves 'View 4PL Curves' button, for Single Point Controls do not
        // if (this.controlType == "Titration")
        // {
        //     // if the user has permissions to update in this container, show them the Apply Guide Set button
        //     this.tbar = LABKEY.user.canUpdate ? [this.exportMenuButton, this.applyGuideSetButton, this.viewCurvesButton] : [this.exportMenuButton, this.viewCurvesButton];
        // }
        // else
        // {
        //     // if the user has permissions to update in this container, show them the Apply Guide Set button
        //     this.tbar = LABKEY.user.canUpdate ? [this.exportMenuButton, this.applyGuideSetButton ] : [this.exportMenuButton];
        // }
        //
        // this.fbar = [
        //     {xtype: 'label', text: 'Bold values in the "Guide Set Date" column indicate runs that are members of a guide set.'}
        // ];

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.initComponent.call(this);
    },

    hasReportFilter: function() {
        var userFilters = this.qwp.getUserFilterArray();
        return userFilters.length > 0;
    },

    configurePlotDataStore: function(baseFilters) {
        // create a new store now that the graph params are selected
        var storeConfig = {
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
        };

        this.store = LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore(storeConfig);

        this.store.on('exception', function(store, type, action, options, response){
            var errorJson = Ext.util.JSON.decode(response.responseText);
            if (errorJson.exception) {
                Ext.get(document.querySelector('.ljTrendPlot').id).update("<span class='labkey-error'>" + errorJson.exception + "</span>");
            }
        });

        this.storeLoading();
        this.store.load();
    },

    storeLoading: function() {
        this.fireEvent('plotDataLoading', this.store);
    },

    storeLoaded: function() {
        this.fireEvent('plotDataLoaded', this.store, this.hasReportFilter());
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function (analyte, isotype, conjugate)
    {
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
            listeners: {
                scope: this,
                success: function(qwp) {
                    this.configurePlotDataStore(filters);
                }
            }
        });

        this.configurePlotDataStore(filters);

        // enable the trending data grid
        this.enable();
    },

    // applyGuideSetClicked: function ()
    // {
    //     // get the selected record list from the grid
    //     var selection = this.selModel.getSelections();
    //     var selectedRecords = [];
    //     // Copy so that it's available in the scope for the callback function
    //     var controlType = this.controlType;
    //     Ext.each(selection, function (record)
    //     {
    //         var newItem = {Analyte: record.get("Analyte")};
    //         if (controlType == 'Titration')
    //         {
    //             newItem.ControlId = record.get("Titration");
    //         }
    //         else
    //         {
    //             newItem.ControlId = record.get("SinglePointControl");
    //         }
    //         selectedRecords.push(newItem);
    //     });
    //
    //     // create a pop-up window to display the apply guide set UI
    //     var win = new Ext.Window({
    //         layout: 'fit',
    //         width: 1115,
    //         height: 500,
    //         closeAction: 'close',
    //         modal: true,
    //         padding: 15,
    //         cls: 'extContainer leveljenningsreport',
    //         bodyStyle: 'background-color: white;',
    //         title: 'Apply Guide Set...',
    //         items: [new LABKEY.ApplyGuideSetPanel({
    //             assayName: this.assayName,
    //             controlName: this.controlName,
    //             controlType: this.controlType,
    //             analyte: this.analyte,
    //             isotype: this.isotype,
    //             conjugate: this.conjugate,
    //             selectedRecords: selectedRecords,
    //             networkExists: this.networkExists,
    //             protocolExists: this.protocolExists,
    //             listeners: {
    //                 scope: this,
    //                 'closeApplyGuideSetPanel': function (hasUpdated)
    //                 {
    //                     if (hasUpdated)
    //                         this.fireEvent('appliedGuideSetUpdated');
    //                     win.close();
    //                 }
    //             }
    //         })]
    //     });
    //
    //     // for testing, narrow window puts left aligned buttons off of the page
    //     win.on('show', function(cmp) {
    //         var posArr = cmp.getPosition();
    //         if (posArr[0] < 0)
    //             cmp.setPosition(0, posArr[1]);
    //     });
    //
    //     win.show(this);
    // },
    //
    // viewCurvesClicked: function ()
    // {
    //     // create a pop-up window to display the plot
    //     var plotDiv = new Ext.Container({
    //         height: 600,
    //         width: 900,
    //         autoEl: {tag: 'div'}
    //     });
    //     var pdfDiv = new Ext.Container({
    //         hidden: true,
    //         autoEl: {tag: 'div'}
    //     });
    //
    //     var yAxisScaleDefault = 'Linear';
    //     var yAxisScaleStore = new Ext.data.ArrayStore({
    //         fields: ['value'],
    //         data: [['Linear'], ['Log']]
    //     });
    //
    //     var yAxisColDefault = 'FIBackground', yAxisColDisplayDefault = 'FI-Bkgd';
    //     var yAxisColStore = new Ext.data.ArrayStore({
    //         fields: ['name', 'value'],
    //         data: [['FI', 'FI'], ['FI-Bkgd', 'FIBackground'], ['FI-Bkgd-Neg', 'FIBackgroundNegative']]
    //     });
    //
    //     var legendColDefault = 'Name';
    //     var legendColStore = new Ext.data.ArrayStore({
    //         fields: ['name', 'value'],
    //         data: [['Assay Type', 'AssayType'], ['Experiment Performer', 'ExpPerformer'], ['Assay Id', 'Name'], ['Notebook No.', 'NotebookNo']]
    //     });
    //
    //     var win = new Ext.Window({
    //         layout: 'fit',
    //         width: 900,
    //         minWidth: 600,
    //         height: 660,
    //         minHeight: 500,
    //         closeAction: 'hide',
    //         modal: true,
    //         cls: 'extContainer',
    //         title: 'Curve Comparison',
    //         items: [plotDiv, pdfDiv],
    //         // stash the default values for the plot options on the win component
    //         yAxisScale: yAxisScaleDefault,
    //         yAxisCol: yAxisColDefault,
    //         yAxisDisplay: yAxisColDisplayDefault,
    //         legendCol: legendColDefault,
    //         buttonAlign: 'left',
    //         buttons: [{
    //             xtype: 'label',
    //             text: 'Y-Axis:'
    //         },{
    //             xtype: 'combo',
    //             width: 80,
    //             id: 'curvecomparison-yaxis-combo',
    //             store: yAxisColStore ,
    //             displayField: 'name',
    //             valueField: 'value',
    //             mode: 'local',
    //             editable: false,
    //             forceSelection: true,
    //             triggerAction: 'all',
    //             value: yAxisColDefault,
    //             listeners: {
    //                 scope: this,
    //                 select: function(cmp, record) {
    //                     win.yAxisCol = record.data.value;
    //                     win.yAxisDisplay = record.data.name;
    //                     this.updateCurvesPlot(win, plotDiv.getId(), false);
    //                 }
    //             }
    //         },{
    //             xtype: 'label',
    //             text: 'Scale:'
    //         },{
    //             xtype: 'combo',
    //             width: 75,
    //             id: 'curvecomparison-scale-combo',
    //             store: yAxisScaleStore ,
    //             displayField: 'value',
    //             valueField: 'value',
    //             mode: 'local',
    //             editable: false,
    //             forceSelection: true,
    //             triggerAction: 'all',
    //             value: yAxisScaleDefault,
    //             listeners: {
    //                 scope: this,
    //                 select: function(cmp, record) {
    //                     win.yAxisScale = record.data.value;
    //                     this.updateCurvesPlot(win, plotDiv.getId(), false);
    //                 }
    //             }
    //         },{
    //             xtype: 'label',
    //             text: 'Legend:'
    //         },{
    //             xtype: 'combo',
    //             width: 140,
    //             id: 'curvecomparison-legend-combo',
    //             store: legendColStore ,
    //             displayField: 'name',
    //             valueField: 'value',
    //             mode: 'local',
    //             editable: false,
    //             forceSelection: true,
    //             triggerAction: 'all',
    //             value: legendColDefault,
    //             listeners: {
    //                 scope: this,
    //                 select: function(cmp, record) {
    //                     win.legendCol = record.data.value;
    //                     this.updateCurvesPlot(win, plotDiv.getId(), false);
    //                 }
    //             }
    //         },
    //         '->',
    //         {
    //             xtype: 'button',
    //             text: 'Export to PDF',
    //             handler: function (btn)
    //             {
    //                 this.updateCurvesPlot(win, pdfDiv.getId(), true);
    //             },
    //             scope: this
    //         },
    //         {
    //             xtype: 'button',
    //             text: 'Close',
    //             handler: function ()
    //             {
    //                 win.hide();
    //             }
    //         }],
    //         listeners: {
    //             scope: this,
    //             'resize': function (w, width, height)
    //             {
    //                 // update the curve plot to the new size of the window
    //                 this.updateCurvesPlot(win, plotDiv.getId(), false);
    //             }
    //         }
    //     });
    //
    //     // for testing, narrow window puts left aligned buttons off of the page
    //     win.on('show', function(cmp) {
    //         var posArr = cmp.getPosition();
    //         if (posArr[0] < 0)
    //             cmp.setPosition(0, posArr[1]);
    //     });
    //
    //     win.show(this);
    //
    //     this.updateCurvesPlot(win, plotDiv.getId(), false);
    // },
    //
    // updateCurvesPlot: function (win, divId, outputPdf)
    // {
    //     win.getEl().mask("loading curves...", "x-mask-loading");
    //
    //     // get the selected record list from the grid
    //     var selection = this.selModel.getSelections();
    //     var runIds = [];
    //     Ext.each(selection, function (record)
    //     {
    //         runIds.push(record.get("RunRowId"));
    //     });
    //
    //     // build the config object of the properties that will be needed by the R report
    //     var config = {reportId: 'module:luminex/CurveComparisonPlot.r', showSection: 'Curve Comparison Plot'};
    //     config['RunIds'] = runIds.join(";");
    //     config['Protocol'] = this.assayName;
    //     config['Titration'] = this.controlName;
    //     config['Analyte'] = this.analyte;
    //     config['YAxisScale'] = !outputPdf ? win.yAxisScale  : 'Linear';
    //     config['YAxisCol'] = win.yAxisCol,
    //     config['YAxisDisplay'] = win.yAxisDisplay,
    //     config['LegendCol'] = win.legendCol,
    //     config['MainTitle'] = $h(this.controlName) + ' 4PL for ' + $h(this.analyte)
    //             + ' - ' + $h(this.isotype === '' ? '[None]' : this.isotype)
    //             + ' ' + $h(this.conjugate === '' ? '[None]' : this.conjugate);
    //     config['PlotHeight'] = win.getHeight();
    //     config['PlotWidth'] = win.getWidth();
    //     if (outputPdf)
    //         config['PdfOut'] = true;
    //
    //     // call and display the Report webpart
    //     new LABKEY.WebPart({
    //         partName: 'Report',
    //         renderTo: divId,
    //         frame: 'none',
    //         partConfig: config,
    //         success: function ()
    //         {
    //             this.getEl().unmask();
    //
    //             if (outputPdf)
    //             {
    //                 // ugly way of getting the href for the pdf file and open it
    //                 if (Ext.getDom(divId))
    //                 {
    //                     var html = Ext.getDom(divId).innerHTML;
    //                     html = html.replace(/&amp;/g, "&");
    //                     var pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('&attachment=true'));
    //                     if (pdfHref.indexOf("deleteFile") == -1) {
    //                         pdfHref = pdfHref + "&deleteFile=false";
    //                     }
    //                     window.location = pdfHref + "&attachment=true";
    //                 }
    //
    //             }
    //         },
    //         failure: function (response)
    //         {
    //             Ext.get(plotDiv.getId()).update("Error: " + response.statusText);
    //             this.getEl().unmask();
    //         },
    //         scope: win
    //     }).render();
    // },

    // exportData: function (type)
    // {
    //     // build up the JSON to pass to the export util
    //     var exportJson = {
    //         fileName: this.title + ".xls",
    //         sheets: [
    //             {
    //                 name: 'data',
    //                 // add a header section to the export with the graph parameter information
    //                 data: [
    //                     [this.controlType == 'Titration' ? 'Titration' : 'SinglePointControl', this.controlName],
    //                     ['Analyte:', this.analyte],
    //                     ['Isotype:', this.isotype],
    //                     ['Conjugate:', this.conjugate],
    //                     ['Export Date:', this.dateRenderer(new Date())],
    //                     []
    //                 ]
    //             }
    //         ]
    //     };
    //
    //     // get all of the columns that are currently being shown in the grid (except for the checkbox column)
    //     var columns = this.getColumnModel().getColumnsBy(function (c)
    //     {
    //         return !c.hidden && c.dataIndex != "";
    //     });
    //
    //     // add the column header row to the export JSON object
    //     var rowIndex = exportJson.sheets[0].data.length;
    //     exportJson.sheets[0].data.push([]);
    //     Ext.each(columns, function (col)
    //     {
    //         exportJson.sheets[0].data[rowIndex].push(col.header);
    //     });
    //
    //     // loop through the grid store to put the data into the export JSON object
    //     Ext.each(this.getStore().getRange(), function (row)
    //     {
    //         var rowIndex = exportJson.sheets[0].data.length;
    //         exportJson.sheets[0].data[rowIndex] = [];
    //
    //         // loop through the column list to get the data for each column
    //         var colIndex = 0;
    //         Ext.each(columns, function (col)
    //         {
    //             // some of the columns may not be defined in the assay design, so set to null
    //             var value = null;
    //             if (null != row.get(col.dataIndex))
    //             {
    //                 value = row.get(col.dataIndex);
    //             }
    //
    //             // render dates with the proper renderer
    //             if (value instanceof Date)
    //             {
    //                 value = this.dateRenderer(value);
    //             }
    //             // render numbers with the proper rounding and format
    //             if (typeof(value) == 'number')
    //             {
    //                 value = this.numberRenderer(value);
    //             }
    //             // render out of range values with an asterisk
    //             var enabledStates = row.get(col.dataIndex + "QCFlagsEnabled");
    //             if (enabledStates != null && (enabledStates.indexOf('t') > -1 || enabledStates.indexOf('1') > -1))
    //             {
    //                 value = "*" + value;
    //             }
    //
    //             // render the flags in an excel friendly format
    //             if (col.dataIndex == "QCFlags")
    //             {
    //                 value = this.flagsExcelRenderer(value);
    //             }
    //
    //             // Issue 19019: specify that this value should be displayed as a string and not converted to a date
    //             if (col.dataIndex == "RunName")
    //             {
    //                 value = {value: value, forceString: true};
    //             }
    //
    //             exportJson.sheets[0].data[rowIndex][colIndex] = value;
    //             colIndex++;
    //         }, this);
    //     }, this);
    //
    //     if (type == 'excel')
    //     {
    //         LABKEY.Utils.convertToExcel(exportJson);
    //     }
    //     else
    //     {
    //         LABKEY.Utils.convertToTable({
    //             fileNamePrefix: this.title,
    //             delim: 'TAB',
    //             rows: exportJson.sheets[0].data
    //         });
    //     }
    // },
    //
    // dateRenderer: function (val)
    // {
    //     return val ? new Date(val).format("Y-m-d") : null;
    // },
    //
    // numberRenderer: function (val)
    // {
    //     // if this is a very small number, display more decimal places
    //     if (null == val)
    //     {
    //         return null;
    //     }
    //     else
    //     {
    //         if (val > 0 && val < 1)
    //         {
    //             return Ext.util.Format.number(Ext.util.Format.round(val, 6), '0.000000');
    //         }
    //         else
    //         {
    //             return Ext.util.Format.number(Ext.util.Format.round(val, 2), '0.00');
    //         }
    //     }
    // },
    //
    // flagsExcelRenderer: function (value)
    // {
    //     if (value != null)
    //     {
    //         value = value.replace(/<a>/gi, "").replace(/<\/a>/gi, "");
    //         value = value.replace(/<span style="text-decoration: line-through;">/gi, "-").replace(/<\/span>/gi, "-");
    //     }
    //     return value;
    // }
});
