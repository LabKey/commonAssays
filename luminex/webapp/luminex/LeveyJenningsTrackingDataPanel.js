/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
            loadMask: {msg: "loading tracking data..."},
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

    initComponent: function ()
    {
        this.store = this.getTrackingDataStore();
        this.selModel = this.getTrackingDataSelModel();
        this.colModel = this.getTrackingDataColModel();

        // initialize an export button for the toolbar
        this.exportMenuButton = new Ext.Button({
            text: 'Export',
            menu: [
                {
                    text: 'Excel',
                    tooltip: 'Click to Export the data to Excel',
                    handler: function ()
                    {
                        this.exportData('excel');
                    },
                    scope: this
                },
                {
                    text: 'TSV',
                    tooltip: 'Click to Export the data to TSV',
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
            this.tbar = this.userCanUpdate ? [this.exportMenuButton, '-', this.applyGuideSetButton, '-', this.viewCurvesButton] : [this.exportMenuButton, '-', this.viewCurvesButton];
        }
        else
        {
            // if the user has permissions to update in this container, show them the Apply Guide Set button
            this.tbar = this.userCanUpdate ? [this.exportMenuButton, '-', this.applyGuideSetButton ] : [this.exportMenuButton];
        }

        this.fbar = [
            {xtype: 'label', text: 'Bold values in the "Guide Set Date" column indicate runs that are members of a guide set.'}
        ];

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.initComponent.call(this);
    },

    getTrackingDataStore: function (startDate, endDate, network, networkAny, protocol, protocolAny)
    {
        // build the array of filters to be applied to the store
        var filterArray = this.getFilterArray();
        if (startDate)
        {
            filterArray.push(LABKEY.Filter.create('Analyte/Data/AcquisitionDate', startDate, LABKEY.Filter.Types.DATE_GREATER_THAN_OR_EQUAL));
        }
        if (endDate)
        {
            filterArray.push(LABKEY.Filter.create('Analyte/Data/AcquisitionDate', endDate, LABKEY.Filter.Types.DATE_LESS_THAN_OR_EQUAL));
        }
        if (!networkAny)
        {
            filterArray.push(LABKEY.Filter.create((this.controlType == "Titration" ? "Titration" : "SinglePointControl") + '/Run/Batch/Network', network));
        }
        if (!protocolAny)
        {
            filterArray.push(LABKEY.Filter.create((this.controlType == "Titration" ? "Titration" : "SinglePointControl") + '/Run/Batch/CustomProtocol', protocol));
        }

        if (this.controlType == "Titration")
        {
            return new LABKEY.ext.Store({
                autoLoad: false,
                schemaName: 'assay.Luminex.' + this.assayName,
                queryName: 'AnalyteTitration',
                columns: 'Titration, Analyte, Titration/Run/Isotype, Titration/Run/Conjugate, Titration/Run/RowId, '
                        + 'Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Folder/EntityId, '
                        + 'Titration/Run/Batch/Network, Titration/Run/Batch/CustomProtocol, Titration/Run/NotebookNo, Titration/Run/AssayType, '
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
        }
        else if (this.controlType == "SinglePoint")
        {
            return new LABKEY.ext.Store({
                autoLoad: false,
                schemaName: 'assay.Luminex.' + this.assayName,
                queryName: 'AnalyteSinglePointControl',
                columns: 'SinglePointControl, Analyte, SinglePointControl/Run/Isotype, SinglePointControl/Run/Conjugate, SinglePointControl/Run/RowId, '
                        + 'SinglePointControl/Run/Name, SinglePointControl/Run/Folder/Name, SinglePointControl/Run/Folder/EntityId, '
                        + 'SinglePointControl/Run/Batch/Network, SinglePointControl/Run/Batch/CustomProtocol, SinglePointControl/Run/NotebookNo, SinglePointControl/Run/AssayType, '
                        + 'SinglePointControl/Run/ExpPerformer, Analyte/Data/AcquisitionDate, Analyte/Properties/LotNumber, '
                        + 'GuideSet/Created, IncludeInGuideSetCalculation, '
                        + 'AverageFiBkgd, AverageFiBkgdQCFlagsEnabled',
                filterArray: filterArray,
                sort: '-Analyte/Data/AcquisitionDate, -SinglePointControl/Run/Created',
                maxRows: (startDate && endDate ? undefined : this.defaultRowSize),
                containerFilter: LABKEY.Query.containerFilter.allFolders,
                listeners: {
                    scope: this,
                    // add a listener to the store to load the QC Flags for the given runs/analyte/titration
                    'load': this.loadQCFlags
                },
                scope: this
            });
        }
    },

    getFilterArray: function ()
    {
        if (this.controlType == "Titration")
        {
            return [
                LABKEY.Filter.create('Titration/Name', this.controlName),
                LABKEY.Filter.create('Titration/IncludeInQcReport', true),
                LABKEY.Filter.create('Analyte/Name', this.analyte),
                LABKEY.Filter.create('Titration/Run/Isotype', this.isotype, (this.isotype == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                LABKEY.Filter.create('Titration/Run/Conjugate', this.conjugate, (this.conjugate == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL))
            ];
        }
        else if (this.controlType == "SinglePoint")
        {
            return [
                LABKEY.Filter.create('SinglePointControl/Name', this.controlName),
                LABKEY.Filter.create('Analyte/Name', this.analyte),
                LABKEY.Filter.create('SinglePointControl/Run/Isotype', this.isotype, (this.isotype == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                LABKEY.Filter.create('SinglePointControl/Run/Conjugate', this.conjugate, (this.conjugate == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL))
            ];
        }
        else
        {
            return null;
        }
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
        if (this.controlType == "Titration")
        {
            return [
                this.selModel,
                {header: 'Analyte', dataIndex: 'Analyte', hidden: true, renderer: this.encodingRenderer},
                {header: 'Titration', dataIndex: 'Titration', hidden: true, renderer: this.encodingRenderer},
                {header: 'Isotype', dataIndex: 'Titration/Run/Isotype', hidden: true, renderer: this.encodingRenderer},
                {header: 'Conjugate', dataIndex: 'Titration/Run/Conjugate', hidden: true, renderer: this.encodingRenderer},
                {header: 'QC Flags', dataIndex: 'QCFlags', width: 75},
                {header: 'Assay Id', dataIndex: 'Titration/Run/Name', renderer: this.assayIdHrefRendererTitration, width: 200},
                {header: 'Network', dataIndex: 'Titration/Run/Batch/Network', width: 75, renderer: this.encodingRenderer, hidden: !this.networkExists},
                {header: 'Protocol', dataIndex: 'Titration/Run/Batch/CustomProtocol', width: 75, renderer: this.encodingRenderer, hidden: !this.protocolExists},
                {header: 'Folder', dataIndex: 'Titration/Run/Folder/Name', width: 75, renderer: this.encodingRenderer},
                {header: 'Notebook No.', dataIndex: 'Titration/Run/NotebookNo', width: 100, renderer: this.encodingRenderer},
                {header: 'Assay Type', dataIndex: 'Titration/Run/AssayType', width: 100, renderer: this.encodingRenderer},
                {header: 'Experiment Performer', dataIndex: 'Titration/Run/ExpPerformer', width: 100, renderer: this.encodingRenderer},
                {header: 'Acquisition Date', dataIndex: 'Analyte/Data/AcquisitionDate', renderer: this.dateRenderer, width: 100},
                {header: 'Analyte Lot No.', dataIndex: 'Analyte/Properties/LotNumber', width: 100, renderer: this.encodingRenderer},
                {header: 'Guide Set Start Date', dataIndex: 'GuideSet/Created', renderer: this.formatGuideSetMembers, scope: this, width: 100},
                {header: 'GS Member', dataIndex: 'IncludeInGuideSetCalculation', hidden: true},
                {header: 'EC50 4PL', dataIndex: 'Four ParameterCurveFit/EC50', width: 75, renderer: this.outOfRangeRenderer("Four ParameterCurveFit/EC50QCFlagsEnabled"), scope: this, align: 'right'},
                {header: 'EC50 4PL QC Flags Enabled', dataIndex: 'Four ParameterCurveFit/EC50QCFlagsEnabled', hidden: true},
                {header: 'EC50 5PL', dataIndex: 'Five ParameterCurveFit/EC50', width: 75, renderer: this.outOfRangeRenderer("Five ParameterCurveFit/EC50QCFlagsEnabled"), scope: this, align: 'right'},
                {header: 'EC50 5PL QC Flags Enabled', dataIndex: 'Five ParameterCurveFit/EC50QCFlagsEnabled', hidden: true},
                {header: 'AUC', dataIndex: 'TrapezoidalCurveFit/AUC', width: 75, renderer: this.outOfRangeRenderer("TrapezoidalCurveFit/AUCQCFlagsEnabled"), scope: this, align: 'right'},
                {header: 'AUC  QC Flags Enabled', dataIndex: 'TrapezoidalCurveFit/AUCQCFlagsEnabled', hidden: true},
                {header: 'High MFI', dataIndex: 'MaxFI', width: 75, renderer: this.outOfRangeRenderer("MaxFIQCFlagsEnabled"), scope: this, align: 'right'},
                {header: 'High  QC Flags Enabled', dataIndex: 'MaxFIQCFlagsEnabled', hidden: true}
            ];
        }
        else if (this.controlType == "SinglePoint")
        {
            return [
                this.selModel,
                {header: 'Analyte', dataIndex: 'Analyte', hidden: true, renderer: this.encodingRenderer},
                {header: 'SinglePointControl', dataIndex: 'SinglePointControl', hidden: true, renderer: this.encodingRenderer},
                {header: 'Isotype', dataIndex: 'SinglePointControl/Run/Isotype', hidden: true, renderer: this.encodingRenderer},
                {header: 'Conjugate', dataIndex: 'SinglePointControl/Run/Conjugate', hidden: true, renderer: this.encodingRenderer},
                {header: 'QC Flags', dataIndex: 'QCFlags', width: 75},
                {header: 'Assay Id', dataIndex: 'SinglePointControl/Run/Name', renderer: this.assayIdHrefRendererSinglePointControl, width: 200},
                {header: 'Network', dataIndex: 'SinglePointControl/Run/Batch/Network', width: 75, renderer: this.encodingRenderer},
                {header: 'Protocol', dataIndex: 'SinglePointControl/Run/Batch/CustomProtocol', width: 75, renderer: this.encodingRenderer},
                {header: 'Folder', dataIndex: 'SinglePointControl/Run/Folder/Name', width: 75, renderer: this.encodingRenderer},
                {header: 'Notebook No.', dataIndex: 'SinglePointControl/Run/NotebookNo', width: 100, renderer: this.encodingRenderer},
                {header: 'Assay Type', dataIndex: 'SinglePointControl/Run/AssayType', width: 100, renderer: this.encodingRenderer},
                {header: 'Experiment Performer', dataIndex: 'SinglePointControl/Run/ExpPerformer', width: 100, renderer: this.encodingRenderer},
                {header: 'Acquisition Date', dataIndex: 'Analyte/Data/AcquisitionDate', renderer: this.dateRenderer, width: 100},
                {header: 'Analyte Lot No.', dataIndex: 'Analyte/Properties/LotNumber', width: 100, renderer: this.encodingRenderer},
                {header: 'Guide Set Start Date', dataIndex: 'GuideSet/Created', renderer: this.formatGuideSetMembers, scope: this, width: 100},
                {header: 'GS Member', dataIndex: 'IncludeInGuideSetCalculation', hidden: true},
                {header: 'MFI', dataIndex: 'AverageFiBkgd', width: 75, renderer: this.outOfRangeRenderer("AverageFiBkgdQCFlagsEnabled"), scope: this, align: 'right'},
                {header: 'MFI QC Flags Enabled', dataIndex: 'AverageFiBkgdQCFlagsEnabled', hidden: true}
            ];
        }
        else
        {
            return [];
        }
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

        // create a new store now that the graph params are selected and bind it to the grid
        var newStore = this.getTrackingDataStore(startDate, endDate, network, networkAny, protocol, protocolAny);
        var newColModel = this.getTrackingDataColModel();
        this.reconfigure(newStore, newColModel);
        newStore.load();

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
            width: 1140,
            height: 500,
            closeAction: 'close',
            modal: true,
            padding: 15,
            cls: 'extContainer',
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
            width: 750,
            autoEl: {tag: 'div'}
        });
        var pdfDiv = new Ext.Container({
            hidden: true,
            autoEl: {tag: 'div'}
        });
        var win = new Ext.Window({
            layout: 'fit',
            width: 750,
            minWidth: 400,
            height: 660,
            minHeight: 300,
            closeAction: 'hide',
            modal: true,
            cls: 'extContainer',
            bodyStyle: 'background-color: white;',
            title: 'Curve Comparison',
            items: [plotDiv, pdfDiv],
            logComparisonPlot: false,
            buttons: [
                {
                    text: 'Export to PDF',
                    handler: function (btn)
                    {
                        this.updateCurvesPlot(win, pdfDiv.getId(), false, true);
                    },
                    scope: this
                },
                {
                    text: 'View Log Y-Axis',
                    handler: function (btn)
                    {
                        win.logComparisonPlot = !win.logComparisonPlot;
                        this.updateCurvesPlot(win, plotDiv.getId(), win.logComparisonPlot, false);
                        btn.setText(win.logComparisonPlot ? "View Linear Y-Axis" : "View Log Y-Axis");
                    },
                    scope: this
                },
                {
                    text: 'Close',
                    handler: function ()
                    {
                        win.hide();
                    }
                }
            ],
            listeners: {
                scope: this,
                'resize': function (w, width, height)
                {
                    // update the curve plot to the new size of the window
                    this.updateCurvesPlot(win, plotDiv.getId(), win.logComparisonPlot, false);
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

        this.updateCurvesPlot(win, plotDiv.getId(), false, false);
    },

    updateCurvesPlot: function (win, divId, logYaxis, outputPdf)
    {
        win.getEl().mask("loading curves...", "x-mask-loading");

        // get the selected record list from the grid
        var selection = this.selModel.getSelections();
        var runIds = [];
        Ext.each(selection, function (record)
        {
            runIds.push(record.get("Titration/Run/RowId"));
        });

        // build the config object of the properties that will be needed by the R report
        var config = {reportId: 'module:luminex/CurveComparisonPlot.r', showSection: 'Curve Comparison Plot'};
        config['RunIds'] = runIds.join(";");
        config['Protocol'] = this.assayName;
        config['Titration'] = this.controlName;
        config['Analyte'] = this.analyte;
        config['AsLog'] = logYaxis;
        config['MainTitle'] = $h(this.controlName) + ' 4PL for ' + $h(this.analyte)
                + ' - ' + $h(this.isotype == '' ? '[None]' : this.isotype)
                + ' ' + $h(this.conjugate == '' ? '[None]' : this.conjugate);
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
                        var pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('&amp;attachment=true'));
                        window.location = pdfHref + "&attachment=true&deleteFile=false";
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
                if (col.dataIndex == "Titration/Run/Name")
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
            schemaName: "assay.Luminex." + this.assayName,
            sql: 'SELECT DISTINCT x.Run, x.FlagType, x.Enabled, FROM Analyte' + prefix + 'QCFlags AS x '
                    + 'WHERE x.Analyte.Name=\'' + this.analyte + '\' AND x.' + prefix + '.Name=\'' + this.controlName + '\' '
                    + (this.isotype == '' ? '  AND x.' + prefix + '.Run.Isotype IS NULL ' : '  AND x.' + prefix + '.Run.Isotype=\'' + this.isotype + '\' ')
                    + (this.conjugate == '' ? '  AND x.' + prefix + '.Run.Conjugate IS NULL ' : '  AND x.' + prefix + '.Run.Conjugate=\'' + this.conjugate + '\' ')
                    + 'ORDER BY x.Run, x.FlagType, x.Enabled LIMIT 1000 ',
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
                this.store.each(function (record)
                {
                    var runFlag = runFlagList[record.get(prefix + "/Run/RowId")];
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
                schemaName: "assay.Luminex." + this.assayName,
                queryName: "Analyte" + prefix + "QCFlags",
                runId: record.get(prefix + "/Run/RowId"),
                analyte: this.analyte,
                controlName: this.controlName,
                controlType: this.controlType,
                listeners: {
                    scope: this,
                    'saveSuccess': function ()
                    {
                        this.store.reload();
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

    assayIdHrefRendererTitration: function (val, p, record)
    {
        var msg = Ext.util.Format.htmlEncode(val);
        var url = LABKEY.ActionURL.buildURL('assay', 'assayDetailRedirect', LABKEY.container.path, {runId: record.get('Titration/Run/RowId')});
        return "<a href='" + url + "'>" + msg + "</a>";
    },

    assayIdHrefRendererSinglePointControl: function (val, p, record)
    {
        var msg = Ext.util.Format.htmlEncode(val);
        var url = LABKEY.ActionURL.buildURL('assay', 'assayDetailRedirect', LABKEY.container.path, {runId: record.get('SinglePointControl/Run/RowId')});
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
