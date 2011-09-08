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
            padding: 20,
            border: false,
            items: [],
            buttonAlign: 'left',
            buttons: []
        });

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
            this.criteria = this.titration + ' ' + this.analyte + ' ' + this.isotype + ' ' + this.conjugate;

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
                            {fieldLabel: 'Guide Set ID', text: this.guideSetId},
                            {fieldLabel: 'Titration', text: this.titration},
                            {fieldLabel: 'Analyte', text: this.analyte}
                        ]
                    },{
                        defaults:{xtype: 'label', labelStyle: 'background-color:#EEEEEE; padding:3px; font-weight:bold'},
                        items: [
                            {fieldLabel: 'Created', text: this.created},
                            {fieldLabel: 'Isotype', text: this.isotype},
                            {fieldLabel: 'Conjugate', text: this.conjugate}
                        ]
                    }]
                }]
            }));
            this.add(new Ext.Spacer({height: 20}));

            // make sure that this guide set is a "current" guide set (only current sets are editable)
            if (!this.currentGuideSet)
            {
                Ext.Msg.alert("Error", "The selected guide set is not a currently active guide set. Only current guide sets are editable at this time.");
            }
            else
            {
                // add a grid for all of the runs that match the guide set criteria
                var allRunsStore = new LABKEY.ext.Store({
                    storeId: 'allRunsStore',
                    schemaName: 'assay',
                    queryName: this.assayName + ' AnalyteTitration',
                    containerFilter: LABKEY.Query.containerFilter.allFolders,
                    filterArray: [LABKEY.Filter.create('Analyte/Name', this.analyte),
                            LABKEY.Filter.create('Titration/Name', this.titration),
                            LABKEY.Filter.create('Titration/Run/Isotype', this.isotype),
                            LABKEY.Filter.create('Titration/Run/Conjugate', this.conjugate)],
                    columns: 'Analyte, Titration, Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Isotype, Titration/Run/Conjugate, '
                            + 'Titration/Run/Batch/Network, Titration/Run/NotebookNo, Titration/Run/AssayType, Titration/Run/ExperimentPerformer, Titration/Run/Created, '
                            + 'GuideSet, IncludeInGuideSetCalculation',
                    updatable: false
                });
                allRunsStore.load({params:{start:0, limit:5}});

                // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
                var allRunsColModel = new Ext.grid.ColumnModel({
                    defaults: {
                        width: 150,
                        sortable: true
                    },
                    columns: [
                        {header:'', dataIndex:'RowId', renderer:this.renderAddRunIcon, scope: this, width:25},
                        //{header:'Analyte', dataIndex:'Analyte'},
                        //{header:'Titration', dataIndex:'Titration'},
                        {header:'Assay Id', dataIndex:'Titration/Run/Name', width:225},
                        {header:'Network', dataIndex:'Network', width:75},
                        {header:'Folder', dataIndex:'Titration/Run/Folder/Name', width:75},
                        //{header:'Isotype', dataIndex:'Titration/Run/Isotype'},
                        //{header:'Conjugate', dataIndex:'Titration/Run/Conjugate'},
                        //{header:'Guide Set', dataIndex:'GuideSet'},
                        //{header:'Included', dataIndex:'IncludeInGuideSetCalculation'}
                        {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo'},
                        {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:75},
                        {header:'Experiment Performer', dataIndex:'Titration/Run/ExperimentPerformer'},
                        {header:'Created', dataIndex:'Titration/Run/Created', renderer:function(val){return new Date(val).format("Y-m-d");}, width:75}
                    ],
                    scope: this
                });

                // create the grid for the full list of runs that match the given guide set criteria
                this.allRunsGrid = new Ext.grid.GridPanel({
                    autoHeight:true,
                    width:1000,
                    loadMask:{msg:"Loading runs..."},
                    store: allRunsStore,
                    colModel: allRunsColModel,
                    disableSelection: true,
                    viewConfig: {forceFit: true},
                    bbar: new Ext.PagingToolbar({
                        pageSize: 5,
                        store: allRunsStore,
                        displayInfo: true,
                        displayMsg: 'Displaying runs {0} - {1} of {2}',
                        emptyMsg: "No runs to display"
                    })
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
                        {xtype: 'displayfield', value: 'List of all of the runs for the "' + this.assayName + '" assay that contain ' + this.criteria + '.'},
                        this.allRunsGrid
                    ]
                }));
                this.add(new Ext.Spacer({height: 20}));
            }

            // add a grid for the list of runs currently in the selected guide set
            var guideRunSetStore = new LABKEY.ext.Store({
                storeId: 'guideRunSetStore',
                schemaName: 'assay',
                queryName: this.assayName + ' AnalyteTitration',
                containerFilter: LABKEY.Query.containerFilter.allFolders, 
                filterArray: [LABKEY.Filter.create('GuideSet', this.guideSetId),
                        LABKEY.Filter.create('IncludeInGuideSetCalculation', true)],
                columns: 'Analyte, Titration, Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Isotype, Titration/Run/Conjugate, '
                        + 'Titration/Run/Batch/Network, Titration/Run/NotebookNo, Titration/Run/AssayType, Titration/Run/ExperimentPerformer, Titration/Run/Created, '
                        + 'GuideSet, IncludeInGuideSetCalculation',
                updatable: false,
                autoLoad: true
            });

            // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
            var guideRunSetColModel = new Ext.grid.ColumnModel({
                defaults: {
                    width: 150,
                    sortable: true
                },
                columns: [
                    {header:'', dataIndex:'RowId', renderer:this.renderRemoveIcon, scope: this, width:25},
                    //{header:'Analyte', dataIndex:'Analyte'},
                    //{header:'Titration', dataIndex:'Titration'},
                    {header:'Assay Id', dataIndex:'Titration/Run/Name', width:225},
                    {header:'Network', dataIndex:'Network', width:75},
                    {header:'Folder', dataIndex:'Titration/Run/Folder/Name', width:75},
                    //{header:'Isotype', dataIndex:'Titration/Run/Isotype'},
                    //{header:'Conjugate', dataIndex:'Titration/Run/Conjugate'},
                    //{header:'Guide Set', dataIndex:'GuideSet'},
                    //{header:'Included', dataIndex:'IncludeInGuideSetCalculation'}
                    {header:'Notebook No.', dataIndex:'Titration/Run/NotebookNo'},
                    {header:'Assay Type', dataIndex:'Titration/Run/AssayType', width:75},
                    {header:'Experiment Performer', dataIndex:'Titration/Run/ExperimentPerformer'},
                    {header:'Created', dataIndex:'Titration/Run/Created', renderer:function(val){return new Date(val).format("Y-m-d");}, width:75}
                ],
                scope: this
            });

            // create the grid for the runs that are a part of the given guide set
            this.guideRunSetGrid = new Ext.grid.GridPanel({
                autoHeight:true,
                width:1000,
                loadMask:{msg:"Loading guide run set..."},
                store: guideRunSetStore,
                colModel: guideRunSetColModel,
                disableSelection: true,
                viewConfig: {forceFit: true}
            });
            this.guideRunSetGrid.on('cellclick', function(grid, rowIndex, colIndex, event){
                if (colIndex == 0)
                    this.removeRunFromGuideSet(grid.getStore().getAt(rowIndex));
            }, this);

            this.add(new Ext.Panel({
                title: 'Guide Run Set',
                width:1000,
                items: [
                    {xtype: 'displayfield', value: 'List of all of the runs included in the guide set calculations for the selected guide set.'},
                    this.guideRunSetGrid
                ]
            }));
            this.add(new Ext.Spacer({height: 20}));

            // TODO: do we want to display the guide set calculated thresholds?

            // add a comment text field for the guide set
            this.commentTextField = new Ext.form.TextField({
                labelStyle: 'background-color:#EEEEEE; padding:3px; font-weight:bold', 
                fieldLabel: 'Comment',
                value: this.comment,
                width: 900
            });
            this.add(this.commentTextField);
            this.add(new Ext.Spacer({height: 10}));

            // add save and cancel buttons to the toolbar
            if (this.currentGuideSet)
            {
                this.add({
                    xtype: 'button',
                    text: 'Save',
                    handler: function(){
                        this.getEl().mask('Saving comment...');
                        LABKEY.Query.updateRows({
                            schemaName: 'assay',
                            queryName: this.assayName + ' GuideSet',
                            rows: [{
                                RowId: this.guideSetId,
                                Comment: this.commentTextField.getValue()
                            }],
                            success: function(data){
                                this.getEl().unmask();
                            },
                            scope: this
                        });
                    },
                    scope: this
                });
//                this.add({
//                    xtype: 'button',
//                    text: 'Cancel',
//                    handler: function(){
//                        alert("Cancel something");
//                    },
//                    scope: this
//                });
            }

            this.doLayout();
        }
    },

    renderRemoveIcon: function() {
        return (!this.currentGuideSet ? "&nbsp;" : "<span class='labkey-file-remove-icon labkey-file-remove-icon-enabled'>&nbsp;</span>");
    },

    removeRunFromGuideSet: function(record) {
        // ask the user if they are sure they want to remove the selected run from the guide set
        Ext.Msg.show({
            title:'Confirmation',
            msg: 'You are about the remove the following run from the Guide Run Set:<br/><br/>'
                + record.get("Titration/Run/Name") + '<br/><br/>'
                + 'Proceed?',
            buttons: Ext.Msg.YESNO,
            fn: function(btnId, text, opt){
                if (btnId == 'yes')
                {
                    LABKEY.Query.updateRows({
                        schemaName: 'assay',
                        queryName: this.assayName + ' AnalyteTitration',
                        rows: [{
                            Analyte: record.get("Analyte"),
                            Titration: record.get("Titration"),
                            IncludeInGuideSetCalculation: false
                        }],
                        success: function(data){
                            // reload the grids to get the updated run list (and set icons accordingly)
                            if (this.allRunsGrid)
                                this.allRunsGrid.getStore().reload();
                            this.guideRunSetGrid.getStore().reload();
                        },
                        scope: this
                    });
                }
            },
            icon: Ext.MessageBox.QUESTION,
            scope: this
        });
    },

    renderAddRunIcon: function(value, metaData, record, rowIndex, colIndex, store) {
        if (record.get("GuideSet") == this.guideSetId && record.get("IncludeInGuideSetCalculation"))
            return "<span class='labkey-file-add-icon labkey-file-add-icon-disabled'>&nbsp;</span>";
        else
            return "<span class='labkey-file-add-icon labkey-file-add-icon-enabled'>&nbsp;</span>";
    },

    addRunToGuideSet: function(record) {
        if (!(record.get("GuideSet") == this.guideSetId && record.get("IncludeInGuideSetCalculation")))
        {
            // TODO: should we warn the user if they are also about to change the associated guide set for the analyte/titration?
            console.log("Run currently in guide set: " + record.get("GuideSet"));
            console.log("Run included in guide set calculation: " + record.get("IncludeInGuideSetCalculation"));

            // ask the user if they are sure they want to add the selected run to the guide set
            Ext.Msg.show({
                title:'Confirmation',
                msg: 'You are about the add the following run to the Guide Run Set for inclusion in the guide set calculations:<br/><br/>'
                    + record.get("Titration/Run/Name") + '<br/><br/>'
                    + 'Proceed?',
                buttons: Ext.Msg.YESNO,
                fn: function(btnId, text, opt){
                    if (btnId == 'yes')
                    {
                        LABKEY.Query.updateRows({
                            schemaName: 'assay',
                            queryName: this.assayName + ' AnalyteTitration',
                            rows: [{
                                Analyte: record.get("Analyte"),
                                Titration: record.get("Titration"),
                                IncludeInGuideSetCalculation: true,
                                GuideSetId: this.guideSetId
                            }],
                            success: function(data){
                                // reload the grids to get the updated run list (and set icons accordingly)
                                if (this.allRunsGrid)
                                    this.allRunsGrid.getStore().reload();
                                this.guideRunSetGrid.getStore().reload();
                            },
                            scope: this
                        });
                    }
                },
                icon: Ext.MessageBox.QUESTION,
                scope: this
            });
        }
    }
});