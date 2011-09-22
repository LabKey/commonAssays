/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
 * User: cnathe
* Date: Sept 8, 2011
*/

LABKEY.requiresCss("GuideSet.css");
LABKEY.requiresCss("LeveyJenningsReport.css");
Ext.QuickTips.init();

/**
 * Class to display panel for selecting which runs are part of the current guide set for the given
 * titration, analyte, isotype, and conjugate combination
 *
 * @params guideSetId
 * @params assayName
 */
LABKEY.ApplyGuideSetPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.assayName)
            throw "You must specify a assayName!";
        if (!config.titration)
            throw "You must specify a titration!";
        if (!config.analyte)
            throw "You must specify a analyte!";
        if (!config.isotype)
            throw "You must specify a isotype!";
        if (!config.conjugate)
            throw "You must specify a conjugate!";
        if (!config.selectedRecords)
            throw "You must specify selectedRecords!";
        
        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            border: false,
            items: [],
            buttonAlign: 'left',
            buttons: [],
            autoScroll: true
        });

        this.addEvents('closeApplyGuideSetPanel');

        LABKEY.ApplyGuideSetPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // build up two lists of analytes and titrations for use in the query filter
        var analyteIds = [];
        var titrationIds = [];
        Ext.each(this.selectedRecords, function(record){
            analyteIds.push(record["Analyte"]);
            titrationIds.push(record["Titration"]);
        });

        // add a grid of all of the "selected" runs for the given criteria
        var selectedRunsStore = new LABKEY.ext.Store({
            storeId: 'selectedRunsStore',
            schemaName: 'assay',
            queryName: this.assayName + ' AnalyteTitration',
            columns: 'Analyte, Titration, Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Folder/EntityId, ' 
                    + 'Titration/Run/Isotype, Titration/Run/Conjugate, Titration/Run/Batch/Network, Titration/Run/NotebookNo, '
                    + 'Titration/Run/AssayType, Titration/Run/ExpPerformer, Titration/Run/TestDate, GuideSet, IncludeInGuideSetCalculation, '
                    + 'Four ParameterCurveFit/EC50, MaxFI, TrapezoidalCurveFit/AUC ',
            filterArray: [
                LABKEY.Filter.create('Titration', titrationIds.join(";"), LABKEY.Filter.Types.EQUALS_ONE_OF),
                LABKEY.Filter.create('Analyte', analyteIds.join(";"), LABKEY.Filter.Types.EQUALS_ONE_OF)
            ],
            sort: '-Titration/Run/TestDate, -Titration/Run/Created',
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            updatable: false,
            autoLoad: true,
            listeners: {
                scope: this,
                'load': function(store, records, options) {
                    // highlight any rows that are members of guide run sets
                    var gridView = this.selectedRunsGrid.getView();
                    for (var i = 0; i < records.length; i++)
                    {
                        if (records[i].get("IncludeInGuideSetCalculation"))
                        {
                            Ext.fly(gridView.getRow(i)).addClass("highlight");
                            Ext.getCmp('highlightedRowDisplayField').show();
                        }
                    }
                }
            }
        });

        // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
        var selectedRunsColModel = new Ext.grid.ColumnModel({
            defaults: {sortable: true},
            columns: [
                {header:'Assay Id', dataIndex:'Titration/Run/Name', renderer: this.tooltipRenderer, width:200},
                {header:'Network', dataIndex:'Titration/Run/Batch/Network', width:75},
                {header:'Folder', dataIndex:'Titration/Run/Folder/Name', width:75},
                {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo', width:100},
                {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:100},
                {header:'Exp Performer', dataIndex:'Titration/Run/ExpPerformer', width:100},
                {header:'Test Date', dataIndex:'Titration/Run/TestDate', renderer: this.dateRenderer, width:100},
                {header:'EC50', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'},
                {header:'High MFI', dataIndex:'MaxFI', width:75, renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'},
                {header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'}
            ],
            scope: this
        });

        // create the grid for the full list of runs that match the given guide set criteria
        this.selectedRunsGrid = new Ext.grid.GridPanel({
            autoScroll:true,
            width:1000,
            height:150,
            loadMask:{msg:"Loading selected runs..."},
            store: selectedRunsStore,
            colModel: selectedRunsColModel,
            disableSelection: true,
            viewConfig: {forceFit: true},
            trackMouseOver: false
        });

        // add a grid with the list of possible guide sets for the given criteria
        var guideSetsStore = new LABKEY.ext.Store({
            storeId: 'guideSetsStore',
            schemaName: 'assay',
            queryName: this.assayName + ' GuideSet',
            columns: 'RowId, Created, CreatedBy/DisplayName, Comment, CurrentGuideSet, '
                    + 'Four ParameterCurveFit/EC50Average, MaxFIAverage, TrapezoidalCurveFit/AUCAverage',
            filterArray: [
                LABKEY.Filter.create('TitrationName', this.titration),
                LABKEY.Filter.create('AnalyteName', this.analyte),
                LABKEY.Filter.create('Isotype', this.isotype),
                LABKEY.Filter.create('Conjugate', this.conjugate)
            ],
            sort: '-Created',
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            updatable: false,
            autoLoad: true,
            listeners: {
                scope: this,
                'load': function(store, records, options){
                    // by default, check the current guide set
                    var index = store.find("CurrentGuideSet", true);
                    if (index > -1)
                        this.guideSetsGrid.getSelectionModel().selectRow(index);
                }
            }
        });

        var guideSetsSelModel = new Ext.grid.CheckboxSelectionModel({
            id: 'guideSetsSelModel',
            header: '',
            singleSelect: true
        });

        // column model for the list of guide sets that can be "applied" to the runs
        var guideSetsColModel = new Ext.grid.ColumnModel({
            defaults: {width: 75, sortable: true},
            columns: [
                guideSetsSelModel,
                {header:'Created By', dataIndex:'CreatedBy/DisplayName', width:100},
                {header:'Created', dataIndex:'Created', renderer: this.dateRenderer},
                {header:'Current', dataIndex:'CurrentGuideSet'},
                {header:'Comment', dataIndex:'Comment', width:200},
                {header:'Avg EC50', dataIndex:'Four ParameterCurveFit/EC50Average', renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'},
                {header:'Avg High MFI', dataIndex:'MaxFIAverage', renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'},
                {header:'Avg AUC', dataIndex:'TrapezoidalCurveFit/AUCAverage', renderer: Ext.util.Format.numberRenderer('0.00'), align: 'right'}
            ],
            scope: this
        });

        // create the grid for the full list of runs that match the given guide set criteria
        this.guideSetsGrid = new Ext.grid.GridPanel({
            autoHeight:true,
            width:1000,
            loadMask:{msg:"Loading guide sets..."},
            store: guideSetsStore,
            colModel: guideSetsColModel,
            selModel: guideSetsSelModel,
            viewConfig: {forceFit: true}
        });

        // add all of the built up items to the panel items array
        this.items = [
            new Ext.Panel({
                title: 'Selected Runs',
                width:1000,
                items: [
                    {
                        xtype: 'displayfield',
                        value: 'List of all of the selected runs for which the guide set below will be applied.'
                    },
                    this.selectedRunsGrid,
                    {
                        id: 'highlightedRowDisplayField',
                        xtype: 'displayfield',
                        value: 'NOTICE: Highlighted rows indicate that the selected assay is a member of a guide run set. '
                            + 'You are currently not allowed to apply guide sets to these assays, so they will be ignored '
                            + 'with the \'Apply Thresholds\' action.',
                        hidden: true
                    }
                ]
            }),
            new Ext.Spacer({height: 20}),
            new Ext.Panel({
                title: 'Guide Run Sets for ' + this.titration + ' : ' + this.analyte + ' ' + this.isotype + ' ' + this.conjugate,
                width:1000,
                items: [
                    {xtype: 'displayfield', value: 'Choose the guide set that you would like to apply to the selected runs in the list above.'},
                    this.guideSetsGrid    
                ]
            }),
            new Ext.Spacer({height: 20})
        ];

        // buttons to apply the selected guide set to the selected runs and cancel
        this.buttons = [
            {
                id: 'applyThresholdsButton',
                text: 'Apply Thresholds',
                handler: function(){
                    // get the selected guide set to be applied, return if none selected
                    var selectedGuideSet = this.guideSetsGrid.getSelectionModel().getSelected();
                    if (!selectedGuideSet)
                    {
                        Ext.Msg.alert("Error", "Please select a guide set to be applied to the selected records.");
                        return;
                    }

                    // get the list of runs from the top grid and apply the selected guide set to those that are
                    // not member runs (i.e. not members of a guide run set)
                    var allSelectedRecords = this.selectedRunsGrid.getStore().getRange();
                    var nonMemberUpdateRows = [];
                    Ext.each(allSelectedRecords, function(record){
                        if (!record.get("IncludeInGuideSetCalculation"))
                            nonMemberUpdateRows.push({
                                Analyte: record.get("Analyte"),
                                Titration: record.get("Titration"),
                                GuideSetId: selectedGuideSet.get("RowId")
                            });
                    });

                    // persist the applied guide set changes to the server
                    if (nonMemberUpdateRows.length > 0)
                    {
                        this.getEl().mask('Applying guide run set...');
                        LABKEY.Query.updateRows({
                            schemaName: 'assay',
                            queryName: this.assayName + ' AnalyteTitration',
                            rows: nonMemberUpdateRows,
                            success: function(data) {
                                if (this.getEl().isMasked())
                                    this.getEl().unmask();
                                this.fireEvent('closeApplyGuideSetPanel', data.rows.length > 0);
                            },
                            scope: this
                        });
                    }
                    else
                    {
                        Ext.Msg.alert("Error", "There are no non-member runs in the selected runs set.");
                    }
                },
                scope: this
            },
            {
                id: 'cancelButton',
                text: 'Cancel',
                handler: function(){
                    this.fireEvent('closeApplyGuideSetPanel');
                },
                scope: this
            }                
        ];

        LABKEY.ApplyGuideSetPanel.superclass.initComponent.call(this);
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