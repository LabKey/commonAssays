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
            buttons: []
        });

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
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            schemaName: 'assay',
            sql: 'SELECT x.Analyte, '
                + 'x.Titration, '
                + 'x.Titration.Run.Name AS AssayId, '
                + 'x.Titration.Run.Folder.Name AS Folder, '
                + 'x.Titration.Run.Folder.EntityId AS EntityId, '
                + 'x.Titration.Run.Isotype AS Isotype, '
                + 'x.Titration.Run.Conjugate AS Conjugate,  '
                + 'x.Titration.Run.Batch.Network AS Network, ' // user defined field
                + 'x.Titration.Run.NotebookNo AS NotebookNo, ' // user defined field
                + 'x.Titration.Run.AssayType AS AssayType, ' // user defined field
                + 'x.Titration.Run.ExpPerformer AS ExpPerformer, ' // user defined field
                + 'd.AcquisitionDate AS Date, '
                + 'x.GuideSet AS GuideSet, '
                + 'x.IncludeInGuideSetCalculation AS IncludeInGuideSetCalculation, '
                + 'cf1.EC50 AS EC50, '
                + 'cf1.MaxFI AS HighMFI, '
                + 'cf2.AUC AS AUC '
                + 'FROM "' + this.assayName + ' AnalyteTitration" AS x '
                // join to get the acquisition date from the data table
                + 'LEFT JOIN (SELECT DISTINCT y.Analyte.RowId AS Analyte, y.Titration.RowId AS Titration, y.Data.AcquisitionDate AS AcquisitionDate'
                + '     FROM "' + this.assayName + ' Data" AS y) AS d '
                + '     ON x.Analyte = d.Analyte AND x.Titration = d.Titration '
                // join to CurveFit table for EC50 and MaxFI
                + 'LEFT JOIN "' + this.assayName + ' CurveFit" AS cf1 '
                + '    ON x.Titration = cf1.TitrationId AND x.Analyte = cf1.AnalyteId '
                + '    AND cf1.CurveType = \'Four Parameter\' '
                // join to CurveFit table for AUC
                + 'LEFT JOIN "' + this.assayName + ' CurveFit" AS cf2 '
                + '    ON x.Titration = cf2.TitrationId AND x.Analyte = cf2.AnalyteId '
                + '    AND cf2.CurveType = \'Trapezoidal\' '
                + 'WHERE x.Analyte in (' + analyteIds.join(",") + ') '
                + '     AND x.Titration in (' + titrationIds.join(",") + ') '
                + 'ORDER BY d.AcquisitionDate DESC, x.Titration.Run.Created DESC ',
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
                {header:'Assay Id', dataIndex:'AssayId', renderer: this.tooltipRenderer, width:200},
                {header:'Network', dataIndex:'Network', width:75},
                {header:'Folder', dataIndex:'Folder', width:75},
                {header:'Notebook No.', dataIndex:'NotebookNo', width:100},
                {header:'Assay Type', dataIndex:'AssayType', width:100},
                {header:'Exp Performer', dataIndex:'ExpPerformer', width:100},
                {header:'Run Date', dataIndex:'Date', renderer: this.dateRenderer, width:100},
                {header:'EC50', dataIndex:'EC50', width:75},
                {header:'High MFI', dataIndex:'HighMFI', width:75},
                {header:'AUC', dataIndex:'AUC', width:75}
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
            viewConfig: {forceFit: true}
        });

        // add a grid with the list of possible guide sets for the given criteria
        var guideSetsStore = new LABKEY.ext.Store({
            storeId: 'guideSetsStore',
            schemaName: 'assay',
            sql: 'SELECT x.RowId AS RowId, '
                + 'x.Created AS Created, '
                + 'x.CreatedBy.DisplayName AS CreatedBy, '
                + 'x.Comment AS Comment, '
                + 'x.CurrentGuideSet AS CurrentGuideSet, '
                + 'cf1.EC50Average AS EC50Average, '
                + 'cf2.AUCAverage AS AUCAverage '
                + 'FROM "' + this.assayName + ' GuideSet" AS x '
                // join to CurveFit table for average EC50
                + 'LEFT JOIN "' + this.assayName + ' GuideSetCurveFit" AS cf1 '
                + '    ON x.RowId = cf1.GuideSetId AND cf1.CurveType = \'Four Parameter\' '
                // join to CurveFit table for average AUC
                + 'LEFT JOIN "' + this.assayName + ' GuideSetCurveFit" AS cf2 '
                + '    ON x.RowId = cf2.GuideSetId AND cf2.CurveType = \'Trapezoidal\' '
                + 'WHERE x.AnalyteName = \'' + this.analyte.replace(/'/g, "''") + '\' '
                + '     AND x.TitrationName = \'' + this.titration.replace(/'/g, "''") + '\' '
                + '     AND x.Isotype = \'' + this.isotype.replace(/'/g, "''") + '\' '
                + '     AND x.Conjugate = \'' + this.conjugate.replace(/'/g, "''") + '\' '
                + 'ORDER BY x.Created DESC ',
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
                {header:'Created By', dataIndex:'CreatedBy', width:100},
                {header:'Created', dataIndex:'Created', renderer: this.dateRenderer},
                {header:'Current', dataIndex:'CurrentGuideSet'},
                {header:'Comment', dataIndex:'Comment', width:200},
                {header:'Avg EC50', dataIndex:'EC50Average'},
                {header:'Avg AUC', dataIndex:'AUCAverage'}
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
                        LABKEY.Query.updateRows({
                            schemaName: 'assay',
                            queryName: this.assayName + ' AnalyteTitration',
                            rows: nonMemberUpdateRows,
                            success: function(data) {
                                // TODO: once this is a dialog pop-up, this can just close the window
                                this.selectedRunsGrid.getStore().reload();
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
                    // TODO: once this is a dialog pop-up, this can just close the window
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