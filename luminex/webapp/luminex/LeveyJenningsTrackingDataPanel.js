/*
 * Copyright (c) 2011-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
* User: cnathe
* Date: Sept 21, 2011
*/

LABKEY.requiresCss("luminex/LeveyJenningsReport.css");
Ext.QuickTips.init();

/**
 * Class to create a labkey editorGridPanel to display the tracking data for the selected graph parameters
 *
 * @params titration
 * @params assayName
 */
LABKEY.LeveyJenningsTrackingDataPanel = Ext.extend(Ext.grid.GridPanel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.titration || config.titration == "null")
            throw "You must specify a titration!";
        if (!config.assayName || config.assayName == "null")
            throw "You must specify a assayName!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            width: 1375,
            autoHeight: true,
            title: $h(config.titration) + ' Tracking Data',
            loadMask:{msg:"loading tracking data..."},
            columnLines: true,
            stripeRows: true,
            viewConfig: {
                forceFit: true,
                scrollOffset: 0
            },
            disabled: true,
            analyte: null,
            isotype: null,
            conjugate: null,
            userCanUpdate: LABKEY.user.canUpdate
        });

        this.addEvents('appliedGuideSetUpdated');

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        this.store = this.getTrackingDataStore();
        this.selModel = this.getTrackingDataSelModel();
        this.colModel = this.getTrackingDataColModel();

        // initialize an export button for the toolbar
        this.exportButton = new Ext.Button({
            text: 'Export',
            tooltip: 'Click to Export the data to Excel',
            handler: this.exportExcelData,
            scope: this
        });

        // initialize the apply guide set button to the toolbar
        this.applyGuideSetButton = new Ext.Button({
            disabled: true,
            text: 'Apply Guide Set',
            handler: this.applyGuideSetClicked,
            scope: this
        });

        // initialize the view curves button to the toolbar
        this.viewCurvesButton = new Ext.Button({
            disabled: true,
            text: 'View 4PL Curves',
            tooltip: 'Click to view overlapping curves for the selected runs.',
            handler: this.viewCurvesClicked,
            scope: this
        });

        // if the user has permissions to update in this container, show them the Apply Guide Set button
        this.tbar = this.userCanUpdate ? [this.exportButton, '-', this.applyGuideSetButton, '-', this.viewCurvesButton] : [this.exportButton, '-', this.viewCurvesButton];

        this.fbar = [{xtype:'label', text:'Bold values in the "Guide Set Date" column indicate assays that are members of a guide set.'}];

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.initComponent.call(this);
    },

    getTrackingDataStore: function(startDate, endDate) {
        // build the array of filters to be applied to the store
        var filterArray = [
            LABKEY.Filter.create('Titration/Name', this.titration),
            LABKEY.Filter.create('Analyte/Name', this.analyte),
            LABKEY.Filter.create('Titration/Run/Isotype', this.isotype, (this.isotype == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
            LABKEY.Filter.create('Titration/Run/Conjugate', this.conjugate, (this.conjugate == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL))
        ];
        if (startDate && endDate)
        {
            filterArray.push(LABKEY.Filter.create('Analyte/Data/AcquisitionDate', startDate, LABKEY.Filter.Types.DATE_GREATER_THAN_OR_EQUAL));
            filterArray.push(LABKEY.Filter.create('Analyte/Data/AcquisitionDate', endDate, LABKEY.Filter.Types.DATE_LESS_THAN_OR_EQUAL));
        }

        return new LABKEY.ext.Store({
            autoLoad: false,
            schemaName: 'assay',
            queryName: this.assayName + ' AnalyteTitration',
            columns: 'Titration, Analyte, Titration/Run/Isotype, Titration/Run/Conjugate, Titration/Run/RowId, '
                    + 'Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Folder/EntityId, '
                    + 'Titration/Run/Batch/Network, Titration/Run/NotebookNo, Titration/Run/AssayType, '
                    + 'Titration/Run/ExpPerformer, Analyte/Data/AcquisitionDate, Analyte/Properties/LotNumber, '
                    + 'GuideSet/Created, IncludeInGuideSetCalculation, '
                    + 'Four ParameterCurveFit/EC50, Four ParameterCurveFit/EC50QCFlagsEnabled, '
                    + 'Five ParameterCurveFit/EC50, Five ParameterCurveFit/EC50QCFlagsEnabled, '
                    + 'TrapezoidalCurveFit/AUC, TrapezoidalCurveFit/AUCQCFlagsEnabled, '
                    + 'MaxFI, MaxFIQCFlagsEnabled',
            filterArray: filterArray,
            sort: '-Analyte/Data/AcquisitionDate, -Titration/Run/Created',
            maxRows: (startDate && endDate ? undefined : this.defaultRowSize),
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            listeners: {
                scope: this,
                // add a listener to the store to load the QC Flags for the given runs/analyte/titration
                'load': this.loadQCFlags
            },
            scope: this
        });
    },

    getTrackingDataSelModel: function() {
        return new Ext.grid.CheckboxSelectionModel({
            listeners: {
                scope: this,
                'selectionchange': function(selectionModel){
                    if (selectionModel.hasSelection())
                    {
                        this.applyGuideSetButton.enable();
                        this.viewCurvesButton.enable();
                    }
                    else
                    {
                        this.applyGuideSetButton.disable();
                        this.viewCurvesButton.disable();
                    }
                }
            }
        });
    },

    getTrackingDataColModel: function() {
        return new Ext.grid.ColumnModel({
            defaults: {sortable: true},
            columns: [
                this.selModel,
                {header:'Analyte', dataIndex:'Analyte', hidden: true, renderer: this.encodingRenderer},
                {header:'Titration', dataIndex:'Titration', hidden: true, renderer: this.encodingRenderer},
                {header:'Isotype', dataIndex:'Titration/Run/Isotype', hidden: true, renderer: this.encodingRenderer},
                {header:'Conjugate', dataIndex:'Titration/Run/Conjugate', hidden: true, renderer: this.encodingRenderer},
                {header:'QC Flags', dataIndex:'QCFlags', width: 75}, 
                {header:'Assay Id', dataIndex:'Titration/Run/Name', renderer: this.assayIdHrefRenderer, width:200},
                {header:'Network', dataIndex:'Titration/Run/Batch/Network', width:75, renderer: this.encodingRenderer},
                {header:'Folder', dataIndex:'Titration/Run/Folder/Name', width:75, renderer: this.encodingRenderer},
                {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo', width:100, renderer: this.encodingRenderer},
                {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:100, renderer: this.encodingRenderer},
                {header:'Experiment Performer', dataIndex:'Titration/Run/ExpPerformer', width:100, renderer: this.encodingRenderer},
                {header:'Acquisition Date', dataIndex:'Analyte/Data/AcquisitionDate', renderer: this.dateRenderer, width:100},
                {header:'Analyte Lot No.', dataIndex:'Analyte/Properties/LotNumber', width:100, renderer: this.encodingRenderer},
                {header:'Guide Set Start Date', dataIndex:'GuideSet/Created', renderer: this.formatGuideSetMembers, scope: this, width:100},
                {header:'GS Member', dataIndex:'IncludeInGuideSetCalculation', hidden: true},
                {header:'EC50 4PL', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: this.outOfRangeRenderer("Four ParameterCurveFit/EC50QCFlagsEnabled"), scope: this, align: 'right'},
                {header:'EC50 4PL QC Flags Enabled', dataIndex:'Four ParameterCurveFit/EC50QCFlagsEnabled', hidden: true},
                {header:'EC50 5PL', dataIndex:'Five ParameterCurveFit/EC50', width:75, renderer: this.outOfRangeRenderer("Five ParameterCurveFit/EC50QCFlagsEnabled"), scope: this, align: 'right'},
                {header:'EC50 5PL QC Flags Enabled', dataIndex:'Five ParameterCurveFit/EC50QCFlagsEnabled', hidden: true},
                {header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: this.outOfRangeRenderer("TrapezoidalCurveFit/AUCQCFlagsEnabled"), scope: this, align: 'right'},
                {header:'AUC  QC Flags Enabled', dataIndex:'TrapezoidalCurveFit/AUCQCFlagsEnabled', hidden: true},
                {header:'High MFI', dataIndex:'MaxFI', width:75, renderer: this.outOfRangeRenderer("MaxFIQCFlagsEnabled"), scope: this, align: 'right'},
                {header:'High  QC Flags Enabled', dataIndex:'MaxFIQCFlagsEnabled', hidden: true}
            ],
            scope: this
        });
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate, startDate, endDate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // set the grid title based on the selected graph params
        this.setTitle($h(this.titration) + ' Tracking Data for ' + $h(this.analyte)
                + ' - ' + $h(this.isotype == '' ? '[None]' : this.isotype)
                + ' ' + $h(this.conjugate == '' ? '[None]' : this.conjugate));

        // create a new store now that the graph params are selected and bind it to the grid
        var newStore = this.getTrackingDataStore(startDate, endDate);
        var newColModel = this.getTrackingDataColModel();
        this.reconfigure(newStore, newColModel);
        newStore.load();
        
        // enable the trending data grid
        this.enable();
    },

    applyGuideSetClicked: function() {
        // get the selected record list from the grid
        var selection = this.selModel.getSelections();
        var selectedRecords = [];
        Ext.each(selection, function(record){
            selectedRecords.push({Analyte: record.get("Analyte"), Titration: record.get("Titration")});
        });

        // create a pop-up window to display the apply guide set UI
        var win = new Ext.Window({
            layout:'fit',
            width:1120,
            height:500,
            closeAction:'close',
            modal: true,
            padding: 15,
            cls: 'extContainer',
            bodyStyle: 'background-color: white;',
            title: 'Apply Guide Run Set...',
            items: [new LABKEY.ApplyGuideSetPanel({
                assayName: this.assayName,
                titration: this.titration,
                analyte: this.analyte,
                isotype: this.isotype,
                conjugate: this.conjugate,
                selectedRecords: selectedRecords,
                listeners: {
                    scope: this,
                    'closeApplyGuideSetPanel': function(hasUpdated) {
                        if (hasUpdated)
                            this.fireEvent('appliedGuideSetUpdated');
                        win.close();
                    }
                }
            })]
        });
        win.show(this);
    },

    viewCurvesClicked: function() {
        // create a pop-up window to display the plot
        var plotDiv = new Ext.Container({
            height: 600,
            width: 750,
            autoEl: {tag: 'div'}
        });
        var pdfDiv = new Ext.Container({
            hidden: true,
            autoEl: {tag: 'div'}
        });
        var win = new Ext.Window({
            layout:'fit',
            width:750,
            height:660,
            closeAction:'hide',
            modal: true,
            cls: 'extContainer',
            bodyStyle: 'background-color: white;',
            title: 'Curve Comparison',
            items: [plotDiv, pdfDiv],
            logComparisonPlot: false,
            buttons: [
                {
                    text: 'Export to PDF',
                    handler: function(btn){
                        this.updateCurvesPLot(win, pdfDiv.getId(), false, true);
                    },
                    scope: this
                },
                {
                    text: 'View Log Y-Axis',
                    handler: function(btn){
                        win.logComparisonPlot = !win.logComparisonPlot;
                        this.updateCurvesPLot(win, plotDiv.getId(), win.logComparisonPlot, false);
                        btn.setText(win.logComparisonPlot ? "View Linear Y-Axis" : "View Log Y-Axis");
                    },
                    scope: this
                },
                {
                    text: 'Close',
                    handler: function(){win.hide();}
                }
            ]
        });
        win.show(this);

        this.updateCurvesPLot(win, plotDiv.getId(), false, false)
    },

    updateCurvesPLot: function(win, divId, logYaxis, outputPdf) {
        win.getEl().mask("loading curves...", "x-mask-loading");

        // get the selected record list from the grid
        var selection = this.selModel.getSelections();
        var runIds = [];
        Ext.each(selection, function(record){
            runIds.push(record.get("Titration/Run/RowId"));
        });        

        // build the config object of the properties that will be needed by the R report
        var config = {reportId: 'module:luminex/CurveComparisonPlot.r', showSection: 'Curve Comparison Plot'};
        config['RunIds'] = runIds.join(";");
        config['Protocol'] = this.assayName;
        config['Titration'] = this.titration;
        config['Analyte'] = this.analyte;
        config['AsLog'] = logYaxis;
        config['MainTitle'] = $h(this.titration) + ' 4PL for ' + $h(this.analyte)
                + ' - ' + $h(this.isotype == '' ? '[None]' : this.isotype)
                + ' ' + $h(this.conjugate == '' ? '[None]' : this.conjugate);
        if (outputPdf)
            config['PdfOut'] = true;

        // call and display the Report webpart
        new LABKEY.WebPart({
               partName: 'Report',
               renderTo: divId,
               frame: 'none',
               partConfig: config,
               success: function() {
                   this.getEl().unmask();

                   if (outputPdf)
                   {
                       // ugly way of getting the href for the pdf file and open it
                       if (Ext.getDom(divId))
                       {
                           var html = Ext.getDom(divId).innerHTML;
                           var pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('&amp;attachment=true'));
                           window.location = pdfHref + "&attachment=true&deleteFile=false";
                       }

                   }
               },
               failure: function(response) {
                   Ext.get(plotDiv.getId()).update("Error: " + response.statusText);
                   this.getEl().unmask();
               },
               scope: win
        }).render();
    },

    exportExcelData: function() {
        // build up the JSON to pass to the export util
        var exportJson = {
            fileName: this.title + ".xls",
            sheets: [{
                name: 'data',
                // add a header section to the export with the graph parameter information
                data: [['Titration:', this.titration],
                    ['Analyte:', this.analyte],
                    ['Isotype:', this.isotype],
                    ['Conjugate:', this.conjugate],
                    ['Export Date:', this.dateRenderer(new Date())],
                    []]
            }]
        };

        // get all of the columns that are currently being shown in the grid (except for the checkbox column)
        var columns = this.getColumnModel().getColumnsBy(function (c) {
            return !c.hidden && c.dataIndex != "";
        });

        // add the column header row to the export JSON object
        var rowIndex = exportJson.sheets[0].data.length;
        exportJson.sheets[0].data.push([]);
        Ext.each(columns, function(col) {
            exportJson.sheets[0].data[rowIndex].push(col.header);
        });

        // loop through the grid store to put the data into the export JSON object
        Ext.each(this.getStore().getRange(), function(row) {
            var rowIndex = exportJson.sheets[0].data.length;
            exportJson.sheets[0].data[rowIndex] = [];

            // loop through the column list to get the data for each column
            var colIndex = 0;
            Ext.each(columns, function(col) {
                // some of the columns may not be defined in the assay design, so set to null
                var value = null;
                if (null != row.get(col.dataIndex))
                {
                    value = row.get(col.dataIndex);
                }

                // render dates with the proper renderer
                if (value instanceof Date)
                {
                    value = this.dateRenderer(value);
                }
                // render numbers with the proper rounding and format
                if (typeof(value) == 'number')
                {
                    value = this.numberRenderer(value);
                }
                // render out of range values with an asterisk
                var enabledStates = row.get(col.dataIndex + "QCFlagsEnabled");
                if (enabledStates != null && (enabledStates.indexOf('t') > -1 || enabledStates.indexOf('1') > -1))
                {
                    value = "*" + value;
                }

                // render the flags in an excel friendly format
                if (col.dataIndex == "QCFlags")
                {
                    value = this.flagsExcelRenderer(value);
                }

                exportJson.sheets[0].data[rowIndex][colIndex] = value;
                colIndex++;
            }, this);
        }, this);

        LABKEY.Utils.convertToExcel(exportJson);
    },

    outOfRangeRenderer: function(enabledDataIndex) {
        return function(val, metaData, record) {
            if (null == val)
            {
                return null;
            }

            // if the record has an enabled QC flag, highlight it in red
            var enabledStates = record.get(enabledDataIndex);
            if (enabledStates != null && (enabledStates.indexOf('t') > -1 || enabledStates.indexOf('1') > -1))
            {
                metaData.attr = "style='color:red'";
            }

            // if this is a very small number, display more decimal places
            var precision = this.getPrecision(val);
            return Ext.util.Format.number(Ext.util.Format.round(val, precision), (precision == 6 ? '0.000000' : '0.00'));
        }
    },

    getPrecision: function(val) {
        return (null != val && val > 0 && val < 1) ? 6 : 2;
    },

    formatGuideSetMembers: function(val, metaData, record) {
        if (record.get("IncludeInGuideSetCalculation"))
        {
            metaData.attr = "style='font-weight:bold'";
        }
        return this.dateRenderer(val); 
    },

    loadQCFlags: function(store, records, options) {
        // query the server for the QC Flags that match the selected Titration and Analyte and update the grid store accordingly
        this.getEl().mask("loading QC Flags...", "x-mask-loading");
        LABKEY.Query.executeSql({
            schemaName: "assay",
            sql: 'SELECT DISTINCT x.Run, x.FlagType, x.Enabled, FROM "' + this.assayName + ' AnalyteTitrationQCFlags" AS x '
                 + 'WHERE x.Analyte.Name=\'' + this.analyte + '\' AND x.Titration.Name=\'' + this.titration + '\' '
                 + (this.isotype == '' ? '  AND x.Titration.Run.Isotype IS NULL ' : '  AND x.Titration.Run.Isotype=\'' + this.isotype + '\' ')
                 + (this.conjugate == '' ? '  AND x.Titration.Run.Conjugate IS NULL ' : '  AND x.Titration.Run.Conjugate=\'' + this.conjugate + '\' '),
            sort: "Run,FlagType,Enabled",
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            success: function(data) {
                // put together the flag display for each runId
                var runFlagList = {};
                for (var i = 0; i < data.rows.length; i++)
                {
                    var row = data.rows[i];
                    if (runFlagList[row.Run] == undefined)
                    {
                        runFlagList[row.Run] = {id: row.Run, count: 0, value: ""};
                    }

                    // add a comma separator
                    if (runFlagList[row.Run].count > 0)
                    {
                        runFlagList[row.Run].value += ", ";
                    }

                    // add strike-thru for disabled flags
                    if (row.Enabled)
                    {
                        runFlagList[row.Run].value += row.FlagType;
                    }
                    else
                    {
                        runFlagList[row.Run].value += '<span style="text-decoration: line-through;">' + row.FlagType + '</span>';
                    }

                    runFlagList[row.Run].count++;
                }

                // update the store records with the QC Flag values
                this.store.each(function(record) {
                    var runFlag = runFlagList[record.get("Titration/Run/RowId")];
                    if (runFlag)
                    {
                        record.set("QCFlags", "<a>" + runFlag.value + "</a>");
                    }
                }, this);

                // add cellclick event to the grid to trigger the QCFlagToggleWindow
                this.on('cellclick', this.showQCFlagToggleWindow, this);

                if (this.getEl().isMasked())
                {
                    this.getEl().unmask();
                }
            },
            failure: function(info, response, options){
                if (this.getEl().isMasked())
                {
                    this.getEl().unmask();
                }

                LABKEY.Utils.displayAjaxErrorResponse(response, options);
            },
            scope: this
        })
    },

    showQCFlagToggleWindow: function(grid, rowIndex, colIndex, evnt) {
        var record = grid.getStore().getAt(rowIndex);
        var fieldName = grid.getColumnModel().getDataIndex(colIndex);
        var value = record.get(fieldName);

        if (fieldName == "QCFlags" && value != null)
        {
            var win = new LABKEY.QCFlagToggleWindow({
                schemaName: "assay",
                queryName: this.assayName + " AnalyteTitrationQCFlags",
                runId: record.get("Titration/Run/RowId"),
                analyte: this.analyte,
                titration: this.titration,
                listeners: {
                    scope: this,
                    'saveSuccess': function(){
                        this.store.reload();
                        win.close();
                    }
                }
            });
            win.show();
        }
    },

    dateRenderer: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    numberRenderer: function(val) {
        // if this is a very small number, display more decimal places
        if (null == val)
        {
            return null;
        }
        else
        {
            if (val > 0 && val < 1)
            {
                return Ext.util.Format.number(Ext.util.Format.round(val, 6), '0.000000');
            }
            else
            {
                return Ext.util.Format.number(Ext.util.Format.round(val, 2), '0.00');
            }
        }
    },

    assayIdHrefRenderer: function(val, p, record) {
        var msg = Ext.util.Format.htmlEncode(val);
        var url = LABKEY.ActionURL.buildURL('assay', 'assayDetailRedirect', LABKEY.container.path,  {runId: record.get('Titration/Run/RowId')});
        return "<a href='" + url + "'>" + msg + "</a>";
    },

    encodingRenderer: function(value, p, record) {
        return $h(value);
    },

    flagsExcelRenderer: function(value) {
        if (value != null)
        {
            value = value.replace(/<a>/gi, "").replace(/<\/a>/gi, "");
            value = value.replace(/<span style="text-decoration: line-through;">/gi, "-").replace(/<\/span>/gi, "-");
        }
        return value;
    }
});
