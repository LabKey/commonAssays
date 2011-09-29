/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
 * User: cnathe
* Date: Sept 7, 2011
*/

LABKEY.requiresCss("fileAddRemoveIcon.css");
Ext.QuickTips.init();

/**
 * Class to display panel for selecting which runs are part of the current guide set for the given
 * titration, analyte, isotype, and conjugate combination
 *
 * @params guideSetId
 * @params assayName
 */
LABKEY.ManageGuideSetPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.guideSetId)
            throw "You must specify a guideSetId!";
        if (!config.assayName)
            throw "You must specify a assayName!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            border: false,
            items: [],
            buttonAlign: 'left',
            buttons: [],
            cls: 'extContainer',
            autoScroll: true
        });

        this.addEvents('closeManageGuideSetPanel');

        LABKEY.ManageGuideSetPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // query the server for the current guide set information
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.assayName + ' GuideSet',
            filterArray: [LABKEY.Filter.create('RowId', this.guideSetId)],
            columns: 'RowId, TitrationName, AnalyteName, Isotype, Conjugate, CurrentGuideSet, Comment, Created',
            success: this.addGuideSetInfoLabels,
            scope: this
        });

        LABKEY.ManageGuideSetPanel.superclass.initComponent.call(this);
    },

    addGuideSetInfoLabels: function(data) {
        if (data.rows.length != 1)
        {
            Ext.Msg.alert("Error", "No guide set found for id " + this.guideSetId);
        }
        else
        {
            // store the guide set info from the data row
            this.titration = data.rows[0]["TitrationName"];
            this.analyte = data.rows[0]["AnalyteName"];
            this.isotype = data.rows[0]["Isotype"];
            this.conjugate = data.rows[0]["Conjugate"];
            this.currentGuideSet = data.rows[0]["CurrentGuideSet"];
            this.created = data.rows[0]["Created"];
            this.comment = data.rows[0]["Comment"];

            // add labels for the guide set information to the top of the panel
            this.add(new Ext.Panel({
                width: 800,
                border: false,
                items: [{
                    border: false,
                    layout: 'column',
                    defaults:{
                        columnWidth: 0.5,
                        layout: 'form',
                        border: false
                    },
                    items: [{
                        defaults:{xtype: 'label', labelStyle: 'background-color:#EEEEEE; padding:3px; font-weight:bold'},
                        items: [
                            {fieldLabel: 'Guide Set ID', text: this.guideSetId, id: 'guideSetIdLabel'},
                            {fieldLabel: 'Titration', text: this.titration},
                            {fieldLabel: 'Analyte', text: this.analyte, id: 'analyteLabel'}
                        ]
                    },{
                        defaults:{xtype: 'label', labelStyle: 'background-color:#EEEEEE; padding:3px; font-weight:bold'},
                        items: [
                            {fieldLabel: 'Created', text: this.dateRenderer(this.created)},
                            {fieldLabel: 'Isotype', text: this.isotype == null ? '[None]' : this.isotype},
                            {fieldLabel: 'Conjugate', text: this.conjugate == null ? '[None]' : this.conjugate}
                        ]
                    }]
                }]
            }));
            this.add(new Ext.Spacer({height: 20}));

            // make sure that this guide set is a "current" guide set (only current sets are editable)
            if (!this.currentGuideSet)
            {
                this.add({
                    xtype: 'displayfield',
                    hideLabel: true,
                    value: 'The selected guide set is not a currently active guide set. Only current guide sets are editable at this time.'
                });
            }
            else
            {
                // add a grid for all of the runs that match the guide set criteria
                var allRunsStore = new Ext.data.JsonStore({
                    storeId: 'allRunsStore',
                    root: 'rows',
                    fields: ['Analyte', 'GuideSet', 'IncludeInGuideSetCalculation', 'Titration', 'Titration/Run/Conjugate', 'Titration/Run/Batch/Network',
                        'Titration/Run/NotebookNo', 'Titration/Run/AssayType', 'Titration/Run/ExpPerformer', 'Titration/Run/TestDate', 'Titration/Run/Folder/Name',
                        'Titration/Run/Isotype', 'Titration/Run/Name', 'Four ParameterCurveFit/EC50', 'MaxFI', 'TrapezoidalCurveFit/AUC']
                });

                // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
                var allRunsColModel = new Ext.grid.ColumnModel({
                    defaults: {sortable: true},
                    columns: [
                        {header:'', dataIndex:'RowId', renderer:this.renderAddRunIcon, scope: this, width:25},
                        {header:'Assay Id', dataIndex:'Titration/Run/Name', renderer: this.tooltipRenderer, width:200},
                        {header:'Network', dataIndex:'Titration/Run/Batch/Network', width:75},
                        {header:'Folder', dataIndex:'Titration/Run/Folder/Name', renderer: this.tooltipRenderer, width:75},
                        {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo', width:100},
                        {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:100},
                        {header:'Exp Performer', dataIndex:'Titration/Run/ExpPerformer', width:100},
                        {header:'Test Date', dataIndex:'Titration/Run/TestDate', renderer: this.dateRenderer, width:100},
                        {header:'EC50', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: this.numberRenderer, align: 'right'},
                        {header:'High MFI', dataIndex:'MaxFI', width:75, renderer: this.numberRenderer, align: 'right'},
                        {header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: this.numberRenderer, align: 'right'}
                    ],
                    scope: this
                });

                // create the grid for the full list of runs that match the given guide set criteria
                this.allRunsGrid = new Ext.grid.GridPanel({
                    autoScroll:true,
                    height:200,
                    width:1000,
                    loadMask:{msg:"Loading runs..."},
                    store: allRunsStore,
                    colModel: allRunsColModel,
                    disableSelection: true,
                    viewConfig: {forceFit: true},
                    stripeRows: true
                });
                this.allRunsGrid.on('cellclick', function(grid, rowIndex, colIndex, event){
                    if (colIndex == 0)
                        this.addRunToGuideSet(grid.getStore().getAt(rowIndex));
                }, this);

                this.add(new Ext.Panel(
                {
                    title: 'All Runs',
                    width:1000,
                    items: [
                        {
                            xtype: 'displayfield',
                            value: 'List of all of the runs from the "' + this.assayName + '" assay that contain '
                                + this.titration + ' ' + this.analyte + ' '
                                + (this.isotype == null ? '[None]' : this.isotype) + ' '
                                + (this.conjugate == null ? '[None]' : this.conjugate) + '.'
                                + ' Note that runs that are already members of a different guide set will not be displayed.'
                        },
                        this.allRunsGrid
                    ]
                }));
            }
            this.add(new Ext.Spacer({height: 20}));

            // add a grid for the list of runs currently in the selected guide set
            var guideRunSetStore = new Ext.data.JsonStore({
                storeId: 'guideRunSetStore',
                root: 'rows',
                fields: ['Analyte', 'GuideSet', 'IncludeInGuideSetCalculation', 'Titration', 'Titration/Run/Conjugate', 'Titration/Run/Batch/Network',
                    'Titration/Run/NotebookNo', 'Titration/Run/AssayType', 'Titration/Run/ExpPerformer', 'Titration/Run/TestDate', 'Titration/Run/Folder/Name',
                    'Titration/Run/Isotype', 'Titration/Run/Name', 'Four ParameterCurveFit/EC50', 'MaxFI', 'TrapezoidalCurveFit/AUC']
            });

            // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
            var guideRunSetColModel = new Ext.grid.ColumnModel({
                defaults: {sortable: true},
                columns: [
                    {header:'', dataIndex:'RowId', renderer:this.renderRemoveIcon, scope: this, hidden: !this.currentGuideSet, width:25},
                    {header:'Assay Id', dataIndex:'Titration/Run/Name', renderer: this.tooltipRenderer, width:200},
                    {header:'Network', dataIndex:'Titration/Run/Batch/Network', width:75},
                    {header:'Folder', dataIndex:'Titration/Run/Folder/Name', renderer: this.tooltipRenderer, width:75},
                    {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo', width:100},
                    {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:100},
                    {header:'Exp Performer', dataIndex:'Titration/Run/ExpPerformer', width:100},
                    {header:'Test Date', dataIndex:'Titration/Run/TestDate', renderer: this.dateRenderer, width:100},
                    {header:'EC50', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: this.numberRenderer, align: 'right'},
                    {header:'High MFI', dataIndex:'MaxFI', width:75, renderer: this.numberRenderer, align: 'right'},
                    {header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: this.numberRenderer, align: 'right'}
                ],
                scope: this
            });

            // create the grid for the runs that are a part of the given guide set
            this.guideRunSetGrid = new Ext.grid.GridPanel({
                autoHeight:true,
                width:1000,
                loadMask:{msg:"Loading runs assigned to guide set..."},
                store: guideRunSetStore,
                colModel: guideRunSetColModel,
                disableSelection: true,
                viewConfig: {forceFit: true},
                stripeRows: true
            });
            this.guideRunSetGrid.on('cellclick', function(grid, rowIndex, colIndex, event){
                if (colIndex == 0)
                    this.removeRunFromGuideSet(grid.getStore().getAt(rowIndex));
            }, this);

            this.add(new Ext.Panel({
                title: 'Runs Assigned to This Guide Set',
                width:1000,
                items: [
                    {xtype: 'displayfield', value: 'List of all of the runs included in the guide set calculations for the selected guide set.'},
                    this.guideRunSetGrid
                ]
            }));
            this.add(new Ext.Spacer({height: 20}));

            // add a comment text field for the guide set
            this.commentTextField = new Ext.form.TextField({
                id: 'commentTextField',
                labelStyle: 'background-color:#EEEEEE; padding:3px; font-weight:bold',
                fieldLabel: 'Comment',
                value: this.comment,
                width: 890,
                enableKeyEvents: true,
                listeners: {
                    scope: this,
                    'keydown': function(){
                        // enable the save button
                        Ext.getCmp('saveButton').enable();
                    },
                    'change': function(){
                        // enable the save button
                        Ext.getCmp('saveButton').enable();
                    }
                }
            });
            
            this.add(this.commentTextField);
            this.add(new Ext.Spacer({height: 10}));

            // add save and cancel buttons to the toolbar
            if (this.currentGuideSet)
            {
                this.addButton({
                    id: 'saveButton',
                    text: 'Save',
                    disabled: true,
                    handler: this.saveGuideSetData,
                    scope: this
                });
                this.addButton({
                    id: 'cancelButton',
                    text: 'Cancel',
                    handler: function(){
                        this.fireEvent('closeManageGuideSetPanel');
                    },
                    scope: this
                });
            }

            this.doLayout();

            this.queryAllRunsForCriteria();
        }
    },

    saveGuideSetData: function() {
        var commands = [{
            schemaName: 'assay',
            queryName: this.assayName + ' GuideSet',
            command: 'update',
            rows: [{
                RowId: this.guideSetId,
                Comment: this.commentTextField.getValue()
            }]
        }];

        // get the list of modified records and set up the save rows array
        var modRecords = this.allRunsGrid.getStore().getModifiedRecords();
        var analyteTitrationRows = [];
        Ext.each(modRecords, function(record){
            analyteTitrationRows.push({
                Analyte: record.get("Analyte"),
                Titration: record.get("Titration"),
                IncludeInGuideSetCalculation: record.get("IncludeInGuideSetCalculation"),
                GuideSetId: record.get("IncludeInGuideSetCalculation") ? this.guideSetId : record.get("GuideSet")
            });
        }, this);

        if (analyteTitrationRows.length > 0)
        {
            commands.push({
                schemaName: 'assay',
                queryName: this.assayName + ' AnalyteTitration',
                command: 'update',
                rows: analyteTitrationRows
            });
        }

        this.getEl().mask('Saving guide set information...');
        var that = this; // work-around for 'too much recursion' error
        LABKEY.Query.saveRows({
            commands: commands,
            success: function(data) {
                if (that.getEl().isMasked())
                    that.getEl().unmask();
                that.fireEvent('closeManageGuideSetPanel', data["result"]);
            }
        });
    },

    queryAllRunsForCriteria: function() {
        // query the server for the list of runs that meet the given criteria
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.assayName + ' AnalyteTitration',
            columns: 'Analyte, Titration, Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Folder/EntityId, '
                    + 'Titration/Run/Isotype, Titration/Run/Conjugate, Titration/Run/Batch/Network, Titration/Run/NotebookNo, '
                    + 'Titration/Run/AssayType, Titration/Run/ExpPerformer, Titration/Run/TestDate, GuideSet, IncludeInGuideSetCalculation, '
                    + 'Four ParameterCurveFit/EC50, MaxFI, TrapezoidalCurveFit/AUC ',
            filterArray: [
                LABKEY.Filter.create('Titration/Name', this.titration),
                LABKEY.Filter.create('Analyte/Name', this.analyte),
                LABKEY.Filter.create('Titration/Run/Isotype', this.isotype, (this.isotype == null ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                LABKEY.Filter.create('Titration/Run/Conjugate', this.conjugate, (this.conjugate == null ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL))
            ],
            sort: '-Titration/Run/TestDate, -Titration/Run/Created',
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            success: this.populateRunGridStores,
            scope: this
        });
    },

    populateRunGridStores: function(data) {
        // loop through the list of runs and determin which ones are candidates for inclusion in the current guide set and which ones are already included
        var allRunsStoreData = {rows: []};
        var guideRunSetStoreData = {rows: []};
        for (var i = 0; i < data.rows.length; i++)
        {
            var row = data.rows[i];

            // include in all runs list if not already a member of a different guide set
            if (!(row["GuideSet"] != this.guideSetId && row["IncludeInGuideSetCalculation"]))
                allRunsStoreData.rows.push(row);

            // include in guide run set if that is the case
            if (row["GuideSet"] == this.guideSetId && row["IncludeInGuideSetCalculation"])
                guideRunSetStoreData.rows.push(row);    
        }

        if (this.allRunsGrid)
            this.allRunsGrid.getStore().loadData(allRunsStoreData);
        this.guideRunSetGrid.getStore().loadData(guideRunSetStoreData);
    },

    renderRemoveIcon: function(value, metaData, record, rowIndex, colIndex, store) {
        return "<span class='labkey-file-remove-icon labkey-file-remove-icon-enabled' id='guideRunSetRow_" + rowIndex + "'>&nbsp;</span>";
    },

    removeRunFromGuideSet: function(record) {
        // remove the record from the guide run set store
        this.guideRunSetGrid.getStore().remove(record);

        // enable the add icon in the all runs store by setting the IncludeInGuideSetCalculation value 
        var index = this.allRunsGrid.getStore().findBy(function(rec, id){
            return (record.get("Analyte") == rec.get("Analyte") && record.get("Titration") == rec.get("Titration"));
        });
        if (index > -1)
            this.allRunsGrid.getStore().getAt(index).set("IncludeInGuideSetCalculation", false);

        // enable the save button
        Ext.getCmp('saveButton').enable();
    },

    renderAddRunIcon: function(value, metaData, record, rowIndex, colIndex, store) {
        if (record.get("IncludeInGuideSetCalculation"))
            return "<span class='labkey-file-add-icon labkey-file-add-icon-disabled' id='allRunsRow_" + rowIndex + "'>&nbsp;</span>";
        else
            return "<span class='labkey-file-add-icon labkey-file-add-icon-enabled' id='allRunsRow_" + rowIndex + "'>&nbsp;</span>";
    },

    addRunToGuideSet: function(record) {
        if (!record.get("IncludeInGuideSetCalculation"))
        {
            // disable the add icon in the all runs store by setting the IncludeInGuideSetCalculation value
            record.set("IncludeInGuideSetCalculation", true);

            // add the record to the guide run set store
            this.guideRunSetGrid.getStore().insert(0, record.copy());

            // enable the save button
            Ext.getCmp('saveButton').enable();
        }
    },

    dateRenderer: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    tooltipRenderer: function(value, p, record) {
        var msg = Ext.util.Format.htmlEncode(value);
        p.attr = 'ext:qtip="' + msg + '"';
        return msg;
    },

    numberRenderer: function(val) {
        // if this is a very small number, display more decimal places
        if (val && val > 0 && val < 1)
            return Ext.util.Format.number(val, '0.000000');
        else
            return Ext.util.Format.number(val, '0.00');        
    }
});