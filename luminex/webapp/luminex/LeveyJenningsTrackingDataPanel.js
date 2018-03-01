/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
 * Class to create a labkey editorGridPanel to display the tracking data for the selected graph parameters
 *
 * @params controlName
 * @params assayName
 */
LABKEY.LeveyJenningsTrackingDataPanel = Ext.extend(Ext.grid.GridPanel, {
    constructor: function (config)
    {
        // check that the config properties needed are present
        if (!config.controlName || config.controlName == "null")
            throw "You must specify a controlName!";
        if (!config.assayName || config.assayName == "null")
            throw "You must specify a assayName!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            width: 1375,
            autoHeight: true,
            title: $h(config.controlName) + ' Tracking Data',
            loadMask: {msg: "Loading data..."},
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

        this.addEvents('appliedGuideSetUpdated', 'trackingDataLoaded');

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.constructor.call(this, config);
    },

    initComponent: function ()
    {
        this.store = new Ext.data.ArrayStore();
        this.selModel = this.getTrackingDataSelModel();
        this.colModel = this.getTrackingDataColModel();

        // initialize an export button for the toolbar
        this.exportMenuButton = new Ext.Button({
            text: 'Export',
            menu: [
                {
                    text: 'Excel',
                    handler: function ()
                    {
                        this.exportData('excel');
                    },
                    scope: this
                },
                {
                    text: 'TSV',
                    handler: function ()
                    {
                        this.exportData('tsv');
                    },
                    scope: this
                }
            ]
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

        // if the controlType is Titration, show the viewCurves 'View 4PL Curves' button, for Single Point Controls do not
        if (this.controlType == "Titration")
        {
            // if the user has permissions to update in this container, show them the Apply Guide Set button
            this.tbar = this.userCanUpdate ? [this.exportMenuButton, this.applyGuideSetButton, this.viewCurvesButton] : [this.exportMenuButton, this.viewCurvesButton];
        }
        else
        {
            // if the user has permissions to update in this container, show them the Apply Guide Set button
            this.tbar = this.userCanUpdate ? [this.exportMenuButton, this.applyGuideSetButton ] : [this.exportMenuButton];
        }

        this.fbar = [
            {xtype: 'label', text: 'Bold values in the "Guide Set Date" column indicate runs that are members of a guide set.'}
        ];

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.initComponent.call(this);
    },

    getNeworkProtocolFilter: function(name, value) {
        var fieldName = (this.controlType == "Titration" ? "Titration" : "SinglePointControl") + '.Run.Batch.' + name;
        if (value != null) {
            return " AND " + fieldName + " = '" + value.replace(/'/g, "''") + "'";
        }
        else {
            return " AND " + fieldName + " IS NULL";
        }
    },

    storeLoaded: function(store, records, options) {
        this.fireEvent('trackingDataLoaded', store);
        this.loadQCFlags(store, records, options);
    },

    getTrackingDataSelModel: function ()
    {
        return new Ext.grid.CheckboxSelectionModel({
            listeners: {
                scope: this,
                'selectionchange': function (selectionModel)
                {
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

    getTrackingDataColModel: function ()
    {
        return new Ext.grid.ColumnModel({
            defaults: {sortable: true},
            columns: this.getTrackingDataColumns(),
            scope: this
        });
    },

    getTrackingDataColumns: function ()
    {
        var cols = [
            this.selModel,
            {header: 'Analyte', dataIndex: 'Analyte', hidden: true, renderer: this.encodingRenderer},
            {header: 'Isotype', dataIndex: 'Isotype', hidden: true, renderer: this.encodingRenderer},
            {header: 'Conjugate', dataIndex: 'Conjugate', hidden: true, renderer: this.encodingRenderer},
            {header: 'QC Flags', dataIndex: 'QCFlags', width: 75},
            {header: 'Assay Id', dataIndex: 'RunName', renderer: this.assayIdHrefRenderer, width: 200},
            {header: 'Network', dataIndex: 'Network', width: 75, renderer: this.encodingRenderer, hidden: !this.networkExists},
            {header: 'Protocol', dataIndex: 'CustomProtocol', width: 75, renderer: this.encodingRenderer, hidden: !this.protocolExists},
            {header: 'Folder', dataIndex: 'FolderName', width: 75, renderer: this.encodingRenderer},
            {header: 'Notebook No.', dataIndex: 'NotebookNo', width: 100, renderer: this.encodingRenderer},
            {header: 'Assay Type', dataIndex: 'AssayType', width: 100, renderer: this.encodingRenderer},
            {header: 'Experiment Performer', dataIndex: 'ExpPerformer', width: 100, renderer: this.encodingRenderer},
            {header: 'Acquisition Date', dataIndex: 'AcquisitionDate', renderer: this.dateRenderer, width: 100},
            {header: 'Analyte Lot No.', dataIndex: 'LotNumber', width: 100, renderer: this.encodingRenderer},
            {header: 'Guide Set Start Date', dataIndex: 'GuideSetCreated', renderer: this.formatGuideSetMembers, scope: this, width: 100},
            {header: 'GS Member', dataIndex: 'IncludeInGuideSetCalculation', hidden: true}
        ];

        if (this.controlType == "Titration")
        {
            cols.splice(2, 0, {header: 'Titration', dataIndex: 'Titration', hidden: true, renderer: this.encodingRenderer});
            cols.push({header: 'EC50 4PL', dataIndex: 'EC504PL', width: 75, renderer: this.outOfRangeRenderer("EC504PLQCFlagsEnabled"), scope: this, align: 'right'});
            cols.push({header: 'EC50 4PL QC Flags Enabled', dataIndex: 'EC504PLQCFlagsEnabled', hidden: true});
            cols.push({header: 'EC50 5PL', dataIndex: 'EC505PL', width: 75, renderer: this.outOfRangeRenderer("EC505PLQCFlagsEnabled"), scope: this, align: 'right'});
            cols.push({header: 'EC50 5PL QC Flags Enabled', dataIndex: 'EC505PLQCFlagsEnabled', hidden: true});
            cols.push({header: 'AUC', dataIndex: 'AUC', width: 75, renderer: this.outOfRangeRenderer("AUCQCFlagsEnabled"), scope: this, align: 'right'});
            cols.push({header: 'AUC  QC Flags Enabled', dataIndex: 'AUCQCFlagsEnabled', hidden: true});
            cols.push({header: 'High MFI', dataIndex: 'HighMFI', width: 75, renderer: this.outOfRangeRenderer("HighMFIQCFlagsEnabled"), scope: this, align: 'right'});
            cols.push({header: 'High  QC Flags Enabled', dataIndex: 'HighMFIQCFlagsEnabled', hidden: true});
        }
        else if (this.controlType == "SinglePoint")
        {
            cols.splice(2, 0, {header: 'SinglePointControl', dataIndex: 'SinglePointControl', hidden: true, renderer: this.encodingRenderer});
            cols.push({header: 'MFI', dataIndex: 'MFI', width: 75, renderer: this.outOfRangeRenderer("MFIQCFlagsEnabled"), scope: this, align: 'right'});
            cols.push({header: 'MFI QC Flags Enabled', dataIndex: 'MFIQCFlagsEnabled', hidden: true});
        }

        return cols;
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function (analyte, isotype, conjugate, startDate, endDate, network, networkAny, protocol, protocolAny)
    {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // set the grid title based on the selected graph params
        this.setTitle($h(this.controlName) + ' Tracking Data for ' + $h(this.analyte)
                + ' - ' + $h(this.isotype == '' ? '[None]' : this.isotype)
                + ' ' + $h(this.conjugate == '' ? '[None]' : this.conjugate));

        var whereClause = "";
        var hasReportFilter = false;
        if (startDate)
        {
            hasReportFilter = true;
            whereClause += " AND CAST(Analyte.Data.AcquisitionDate AS DATE) >= '" + startDate + "'";
        }
        if (endDate)
        {
            hasReportFilter = true;
            whereClause += " AND CAST(Analyte.Data.AcquisitionDate AS DATE) <= '" + endDate + "'";
        }
        if (Ext.isDefined(network) && !networkAny)
        {
            hasReportFilter = true;
            whereClause += this.getNeworkProtocolFilter("Network", network);
        }
        if (Ext.isDefined(protocol) && !protocolAny)
        {
            hasReportFilter = true;
            whereClause += this.getNeworkProtocolFilter("CustomProtocol", protocol);
        }

        // create a new store now that the graph params are selected and bind it to the grid
        var controlTypeColName = this.controlType == "SinglePoint" ? "SinglePointControl" : this.controlType;
        var orderByClause = " ORDER BY Analyte.Data.AcquisitionDate DESC, " + controlTypeColName + ".Run.Created DESC"
                            + " LIMIT " + (hasReportFilter ? "10000" : this.defaultRowSize);

        var storeConfig = {
            assayName: this.assayName,
            controlName: this.controlName,
            controlType: this.controlType,
            analyte: this.analyte,
            isotype: this.isotype,
            conjugate: this.conjugate,
            scope: this,
            loadListener: this.storeLoaded,
            orderBy: orderByClause
        };

        if (whereClause != "")
        {
            storeConfig['whereClause'] = whereClause;
        }

        this.store = LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore(storeConfig);

        this.store.on('exception', function(store, type, action, options, response){
            var errorJson = Ext.util.JSON.decode(response.responseText);
            if (errorJson.exception) {
                Ext.get('EC504PLTrendPlotDiv').update("<span class='labkey-error'>" + errorJson.exception + "</span>");
            }
        });

        var newColModel = this.getTrackingDataColModel();
        this.reconfigure(this.store, newColModel);
        this.store.load();

        // enable the trending data grid
        this.enable();
    },

    applyGuideSetClicked: function ()
    {
        // get the selected record list from the grid
        var selection = this.selModel.getSelections();
        var selectedRecords = [];
        // Copy so that it's available in the scope for the callback function
        var controlType = this.controlType;
        Ext.each(selection, function (record)
        {
            var newItem = {Analyte: record.get("Analyte")};
            if (controlType == 'Titration')
            {
                newItem.ControlId = record.get("Titration");
            }
            else
            {
                newItem.ControlId = record.get("SinglePointControl");
            }
            selectedRecords.push(newItem);
        });

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
                assayName: this.assayName,
                controlName: this.controlName,
                controlType: this.controlType,
                analyte: this.analyte,
                isotype: this.isotype,
                conjugate: this.conjugate,
                selectedRecords: selectedRecords,
                networkExists: this.networkExists,
                protocolExists: this.protocolExists,
                listeners: {
                    scope: this,
                    'closeApplyGuideSetPanel': function (hasUpdated)
                    {
                        if (hasUpdated)
                            this.fireEvent('appliedGuideSetUpdated');
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

        win.show(this);
    },

    viewCurvesClicked: function ()
    {
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
                    scope: this,
                    select: function(cmp, record) {
                        win.yAxisCol = record.data.value;
                        win.yAxisDisplay = record.data.name;
                        this.updateCurvesPlot(win, plotDiv.getId(), false);
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
                    scope: this,
                    select: function(cmp, record) {
                        win.yAxisScale = record.data.value;
                        this.updateCurvesPlot(win, plotDiv.getId(), false);
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
                    scope: this,
                    select: function(cmp, record) {
                        win.legendCol = record.data.value;
                        this.updateCurvesPlot(win, plotDiv.getId(), false);
                    }
                }
            },
            '->',
            {
                xtype: 'button',
                text: 'Export to PDF',
                handler: function (btn)
                {
                    this.updateCurvesPlot(win, pdfDiv.getId(), true);
                },
                scope: this
            },
            {
                xtype: 'button',
                text: 'Close',
                handler: function ()
                {
                    win.hide();
                }
            }],
            listeners: {
                scope: this,
                'resize': function (w, width, height)
                {
                    // update the curve plot to the new size of the window
                    this.updateCurvesPlot(win, plotDiv.getId(), false);
                }
            }
        });

        // for testing, narrow window puts left aligned buttons off of the page
        win.on('show', function(cmp) {
            var posArr = cmp.getPosition();
            if (posArr[0] < 0)
                cmp.setPosition(0, posArr[1]);
        });

        win.show(this);

        this.updateCurvesPlot(win, plotDiv.getId(), false);
    },

    updateCurvesPlot: function (win, divId, outputPdf)
    {
        win.getEl().mask("loading curves...", "x-mask-loading");

        // get the selected record list from the grid
        var selection = this.selModel.getSelections();
        var runIds = [];
        Ext.each(selection, function (record)
        {
            runIds.push(record.get("RunRowId"));
        });

        // build the config object of the properties that will be needed by the R report
        var config = {reportId: 'module:luminex/CurveComparisonPlot.r', showSection: 'Curve Comparison Plot'};
        config['RunIds'] = runIds.join(";");
        config['Protocol'] = this.assayName;
        config['Titration'] = this.controlName;
        config['Analyte'] = this.analyte;
        config['YAxisScale'] = !outputPdf ? win.yAxisScale  : 'Linear';
        config['YAxisCol'] = win.yAxisCol,
        config['YAxisDisplay'] = win.yAxisDisplay,
        config['LegendCol'] = win.legendCol,
        config['MainTitle'] = $h(this.controlName) + ' 4PL for ' + $h(this.analyte)
                + ' - ' + $h(this.isotype === '' ? '[None]' : this.isotype)
                + ' ' + $h(this.conjugate === '' ? '[None]' : this.conjugate);
        config['PlotHeight'] = win.getHeight();
        config['PlotWidth'] = win.getWidth();
        if (outputPdf)
            config['PdfOut'] = true;

        // call and display the Report webpart
        new LABKEY.WebPart({
            partName: 'Report',
            renderTo: divId,
            frame: 'none',
            partConfig: config,
            success: function ()
            {
                this.getEl().unmask();

                if (outputPdf)
                {
                    // ugly way of getting the href for the pdf file and open it
                    if (Ext.getDom(divId))
                    {
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
            failure: function (response)
            {
                Ext.get(plotDiv.getId()).update("Error: " + response.statusText);
                this.getEl().unmask();
            },
            scope: win
        }).render();
    },

    exportData: function (type)
    {
        // build up the JSON to pass to the export util
        var exportJson = {
            fileName: this.title + ".xls",
            sheets: [
                {
                    name: 'data',
                    // add a header section to the export with the graph parameter information
                    data: [
                        [this.controlType == 'Titration' ? 'Titration' : 'SinglePointControl', this.controlName],
                        ['Analyte:', this.analyte],
                        ['Isotype:', this.isotype],
                        ['Conjugate:', this.conjugate],
                        ['Export Date:', this.dateRenderer(new Date())],
                        []
                    ]
                }
            ]
        };

        // get all of the columns that are currently being shown in the grid (except for the checkbox column)
        var columns = this.getColumnModel().getColumnsBy(function (c)
        {
            return !c.hidden && c.dataIndex != "";
        });

        // add the column header row to the export JSON object
        var rowIndex = exportJson.sheets[0].data.length;
        exportJson.sheets[0].data.push([]);
        Ext.each(columns, function (col)
        {
            exportJson.sheets[0].data[rowIndex].push(col.header);
        });

        // loop through the grid store to put the data into the export JSON object
        Ext.each(this.getStore().getRange(), function (row)
        {
            var rowIndex = exportJson.sheets[0].data.length;
            exportJson.sheets[0].data[rowIndex] = [];

            // loop through the column list to get the data for each column
            var colIndex = 0;
            Ext.each(columns, function (col)
            {
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

                // Issue 19019: specify that this value should be displayed as a string and not converted to a date
                if (col.dataIndex == "RunName")
                {
                    value = {value: value, forceString: true};
                }

                exportJson.sheets[0].data[rowIndex][colIndex] = value;
                colIndex++;
            }, this);
        }, this);

        if (type == 'excel')
        {
            LABKEY.Utils.convertToExcel(exportJson);
        }
        else
        {
            LABKEY.Utils.convertToTable({
                fileNamePrefix: this.title,
                delim: 'TAB',
                rows: exportJson.sheets[0].data
            });
        }
    },

    outOfRangeRenderer: function (enabledDataIndex)
    {
        return function (val, metaData, record)
        {
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

    getPrecision: function (val)
    {
        return (null != val && val > 0 && val < 1) ? 6 : 2;
    },

    formatGuideSetMembers: function (val, metaData, record)
    {
        if (record.get("IncludeInGuideSetCalculation"))
        {
            metaData.attr = "style='font-weight:bold'";
        }
        return this.dateRenderer(val);
    },

    loadQCFlags: function (store, records, options)
    {
        // query the server for the QC Flags that match the selected Titration and Analyte and update the grid store accordingly
        this.getEl().mask("loading QC Flags...", "x-mask-loading");
        var prefix = this.controlType == 'Titration' ? 'Titration' : 'SinglePointControl';
        LABKEY.Query.executeSql({
            schemaName: "assay.Luminex." + LABKEY.QueryKey.encodePart(this.assayName),
            sql: 'SELECT DISTINCT x.Run, x.FlagType, x.Enabled, FROM Analyte' + prefix + 'QCFlags AS x '
                    + 'WHERE x.Analyte.Name=\'' + this.analyte.replace(/'/g, "''") + '\' AND x.' + prefix + '.Name=\'' + this.controlName.replace(/'/g, "''") + '\' '
                    + (this.isotype == '' ? '  AND x.' + prefix + '.Run.Isotype IS NULL ' : '  AND x.' + prefix + '.Run.Isotype=\'' + this.isotype.replace(/'/g, "''") + '\' ')
                    + (this.conjugate == '' ? '  AND x.' + prefix + '.Run.Conjugate IS NULL ' : '  AND x.' + prefix + '.Run.Conjugate=\'' + this.conjugate.replace(/'/g, "''") + '\' ')
                    + 'ORDER BY x.Run, x.FlagType, x.Enabled LIMIT 10000 ',
            sort: "Run,FlagType,Enabled",
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            success: function (data)
            {
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
                store.each(function (record)
                {
                    var runFlag = runFlagList[record.get("RunRowId")];
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
            failure: function (info, response, options)
            {
                if (this.getEl().isMasked())
                {
                    this.getEl().unmask();
                }

                LABKEY.Utils.displayAjaxErrorResponse(response, options);
            },
            scope: this
        })
    },

    showQCFlagToggleWindow: function (grid, rowIndex, colIndex, evnt)
    {
        var record = grid.getStore().getAt(rowIndex);
        var fieldName = grid.getColumnModel().getDataIndex(colIndex);
        var value = record.get(fieldName);
        var prefix = this.controlType == 'Titration' ? 'Titration' : 'SinglePointControl';

        if (fieldName == "QCFlags" && value != null)
        {
            var win = new LABKEY.QCFlagToggleWindow({
                schemaName: "assay.Luminex." + LABKEY.QueryKey.encodePart(this.assayName),
                queryName: "Analyte" + prefix + "QCFlags",
                runId: record.get("RunRowId"),
                analyte: this.analyte,
                controlName: this.controlName,
                controlType: this.controlType,
                editable: true,
                listeners: {
                    scope: this,
                    'saveSuccess': function ()
                    {
                        grid.getStore().reload();
                        win.close();
                    }
                }
            });
            win.show();
        }
    },

    dateRenderer: function (val)
    {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    numberRenderer: function (val)
    {
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

    assayIdHrefRenderer: function (val, p, record)
    {
        var msg = Ext.util.Format.htmlEncode(val);
        var url = LABKEY.ActionURL.buildURL('assay', 'assayDetailRedirect', LABKEY.container.path, {runId: record.get('RunRowId')});
        return "<a href='" + url + "'>" + msg + "</a>";
    },

    encodingRenderer: function (value, p, record)
    {
        return $h(value);
    },

    flagsExcelRenderer: function (value)
    {
        if (value != null)
        {
            value = value.replace(/<a>/gi, "").replace(/<\/a>/gi, "");
            value = value.replace(/<span style="text-decoration: line-through;">/gi, "-").replace(/<\/span>/gi, "-");
        }
        return value;
    }
});
