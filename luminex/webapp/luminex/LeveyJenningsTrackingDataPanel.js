/*
 * Copyright (c) 2011 LabKey Corporation
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
            width: 1200,
            autoHeight: true,
            editable: false,
            pageSize: 0,
            title: 'Tracking Data',
            loadMask:{msg:"Loading tracking data..."},
            disabled: true,
            analyte: null,
            isotype: null,
            conjugate: null
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
            handler: function(){
                if (this.store)
                    this.store.exportData("excel");
            },
            scope: this
        });

        // initialize and add the apply guide set button to the toolbar
        this.applyGuideSetButton = new Ext.Button({
            disabled: true,
            text: 'Apply Guide Set',
            handler: this.applyGuideSetClicked,
            scope: this
        });

        this.tbar = [this.exportButton, '-', this.applyGuideSetButton];

        LABKEY.LeveyJenningsTrackingDataPanel.superclass.initComponent.call(this);
    },

    getTrackingDataStore: function(startDate, endDate) {
        // build the array of filters to be applied to the store
        var filterArray = [
            LABKEY.Filter.create('Titration/Name', this.titration),
            LABKEY.Filter.create('Analyte/Name', this.analyte),
            LABKEY.Filter.create('Titration/Run/Isotype', this.isotype),
            LABKEY.Filter.create('Titration/Run/Conjugate', this.conjugate)
        ];
        if (startDate && endDate)
        {
            filterArray.push(LABKEY.Filter.create('Titration/Run/TestDate', startDate, LABKEY.Filter.Types.GREATER_THAN_OR_EQUAL));
            filterArray.push(LABKEY.Filter.create('Titration/Run/TestDate', endDate, LABKEY.Filter.Types.LESS_THAN_OR_EQUAL));
        }

        return new LABKEY.ext.Store({
            autoLoad: false,
            schemaName: 'assay',
            queryName: this.assayName + ' AnalyteTitration',
            columns: 'Analyte, Titration, Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Folder/EntityId, '
                    + 'Analyte/Properties/LotNumber, Titration/Run/Batch/Network, Titration/Run/NotebookNo, '
                    + 'Titration/Run/AssayType, Titration/Run/ExpPerformer, Titration/Run/TestDate, GuideSet/Created, '
                    + 'Four ParameterCurveFit/EC50, MaxFI, TrapezoidalCurveFit/AUC, '
                    + 'GuideSet/Four ParameterCurveFit/EC50Average, GuideSet/Four ParameterCurveFit/EC50StdDev ',
            filterArray: filterArray,
            sort: '-Titration/Run/TestDate, -Titration/Run/Created',
            maxRows: (startDate && endDate ? undefined : this.defaultRowSize),
            containerFilter: LABKEY.Query.containerFilter.allFolders
        });
    },

    getTrackingDataSelModel: function() {
        return new Ext.grid.CheckboxSelectionModel({
            listeners: {
                scope: this,
                'selectionchange': function(selectionModel){
                    if (selectionModel.hasSelection())
                        this.applyGuideSetButton.enable();
                    else
                        this.applyGuideSetButton.disable();
                }
            }
        });
    },

    getTrackingDataColModel: function() {
        return new Ext.grid.ColumnModel({
            defaults: {sortable: true},
            columns: [
                this.selModel,
                {header:'', dataIndex:'Analtye', hidden: true},
                {header:'', dataIndex:'Titration', hidden: true},
                {header:'Assay Id', dataIndex:'Titration/Run/Name', renderer: this.tooltipRenderer, width:200},
                {header:'Network', dataIndex:'Titration/Run/Batch/Network', width:75},
                {header:'Folder', dataIndex:'Titration/Run/Folder/Name', renderer: this.tooltipRenderer, width:75},
                {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo', width:100},
                {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:100},
                {header:'Exp Performer', dataIndex:'Titration/Run/ExpPerformer', width:100},
                {header:'Test Date', dataIndex:'Titration/Run/TestDate', renderer: this.dateRenderer, width:100},
                {header:'Analyte Lot No.', dataIndex:'Analyte/Properties/LotNumber', width:100},
                {header:'Guide Set Date', dataIndex:'GuideSet/Created', renderer: this.dateRenderer, width:100},
                {header:'EC50', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: this.outOfRangeRenderer, align: 'right'},
                {header:'High MFI', dataIndex:'MaxFI', width:75, renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'},
                {header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'},
                {header:'EC50 Average', dataIndex:'GuideSet/Four ParameterCurveFit/EC50Average', hidden: true},
                {header:'EC50 StdDev', dataIndex:'GuideSet/Four ParameterCurveFit/EC50StdDev', hidden: true}
            ],
            scope: this
        });
    },

    // function called by the JSP when the graph params are selected and the "Reset Graph" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // set the grid title based on the selected graph params
        this.setTitle('Tracking Data for ' + $h(_analyte) + ' - ' + $h(_isotype) + ' ' + $h(_conjugate));

        // create a new store now that the graph params are selected and bind it to the grid
        this.updateTrackingDataGrid();

        // enable the trending data grid
        this.enable();
    },

    updateTrackingDataGrid: function(startDate, endDate) {
        var newStore = this.getTrackingDataStore(startDate, endDate);
        var newColModel = this.getTrackingDataColModel();
        this.reconfigure(newStore, newColModel);
        newStore.load();
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
            width:1100,
            height:500,
            closeAction:'close',
            modal: true,
            padding: 15,
            cls: 'extContainer',
            title: 'Apply Guide Run Set',
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

    outOfRangeRenderer: function(val, metaData, record) {
        // if this record has a guide set average and stdDev, check if the value is outside of the +/- 3 stdDev range
        var avg = record.get('GuideSet/Four ParameterCurveFit/EC50Average');
        var stdDev = record.get('GuideSet/Four ParameterCurveFit/EC50StdDev');
        if (val && avg)
        {
            // if there is not stdDev (i.e. only one run in the guide set) and the value != avg (compared to two decimals)
            // OR if the value is outside of the +/- 3 stdDev range from the avg
            if (!stdDev && Ext.util.Format.number(val, '0.00') != Ext.util.Format.number(avg, '0.00'))
                metaData.attr = "style='color:red;'";
            else if(stdDev && (val > (avg + (3 * stdDev)) || val < (avg - (3 * stdDev))))
                metaData.attr = "style='color:red;'";
        }

        return Ext.util.Format.number(val, '0.00');
    },

    dateRenderer: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    tooltipRenderer: function(value, p, record) {
        var msg = Ext.util.Format.htmlEncode(value);
        p.attr = 'ext:qtip="' + msg + '"';
        return msg;
    }
});