/*
 * Copyright (c) 2013-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of 'Exclude Analytes' button to open the run exclusion window 
function titrationExclusionWindow(assayName, runId)
{
    var win = new Ext.Window({
        cls: 'extContainer',
        title: 'Exclude Titrations from Analysis',
        layout:'fit',
        width: Ext.getBody().getViewSize().width < 490 ? Ext.getBody().getViewSize().width * .9 : 440,
        height: Ext.getBody().getViewSize().height > 700 ? 600 : Ext.getBody().getViewSize().height * .75,
        padding: 15,
        modal: true,
        closeAction:'close',
        bodyStyle: 'background-color: white;',
        items: new LABKEY.TitrationExclusionPanel({
            assayName: assayName,
            runId: runId,
            listeners: {
                scope: this,
                'closeWindow': function(){
                    win.close();
                }
            }
        })
    });
    win.show(this);
}

/**
 * Class to display panel for selecting which analytes for a given replicate group to exlude from a Luminex run
 * @params assayName = the assay design name
 * @params runId = runId for the selected replicate group
 */
LABKEY.TitrationExclusionPanel = Ext.extend(LABKEY.BaseExclusionPanel, {

    initComponent : function() {
        this.excluded = [];
        this.comments = [];
        this.excludedDataIds = [];
        this.present = [];
        this.preExcludedIds = [];
        this.preAnalyteRowIds = [];

        LABKEY.TitrationExclusionPanel.superclass.initComponent.call(this);

        this.setupWindowPanelItems();
    },

    setupWindowPanelItems: function()
    {
        this.addHeaderPanel('Analytes excluded for a replicate group or at the assay level will not be re-included by changes in titration exclusions');

        var titrationExclusionStore = new LABKEY.ext.Store({
            schemaName: 'assay.Luminex.' + this.assayName,
            queryName: 'TitrationExclusion',
            columns: 'Description,Analytes/RowId,RowId,Comment, DataId/Run',
            filterArray : [
                    LABKEY.Filter.create('DataId/Run', this.runId, LABKEY.Filter.Types.EQUALS)
            ],
            autoLoad : true,
            listeners : {
                load : function(store, records){
                    titrationsGridStore.load();
                },
                titrationgridloaded : function()
                {
                    var records = titrationExclusionStore.data.items;
                    var id;
                    for(var i = 0; i < records.length; i++)
                    {
                        id = combinedStore.findExact('Name', records[i].data.Description);
                        this.preAnalyteRowIds[id] = records[i].get('Analytes/RowId');
                        this.preExcludedIds[id] = records[i].get('Analytes/RowId').split(",");
                        this.comments[id] = records[i].get('Comment');
                    }
                },
                scope : this
            }
        });

        var selMod = this.getGridCheckboxSelectionModel();

        this.titrationSelMod = new Ext.grid.RowSelectionModel({
            singleSelect : true,
            header : 'Available Titrations',
            listeners : {
                rowdeselect : function(tsl, rowId, record)
                {
                    this.excluded[rowId] = selMod.getSelections();
                    this.excluded[rowId].name = record.data.Name;
                    this.comments[rowId] = Ext.getCmp('comment').getValue();
                    this.present[rowId] = selMod.getSelections().length;
                    record.set('Present', this.present[rowId]);
                    this.excludedDataIds[rowId] = record.data.DataId;
                },
                rowselect : function(tsl, rowId, record)
                {
                    availableAnalytesGrid.getStore().clearFilter();
                    availableAnalytesGrid.getStore().filter({property: 'Titration', value: record.data.Name, exactMatch: true});
                    availableAnalytesGrid.setDisabled(false);

                    selMod.suspendEvents(false);
                    if(typeof this.preExcludedIds[rowId] === 'object')
                    {
                        selMod.clearSelections();
                        Ext.each(this.preExcludedIds[rowId], function(analyte){
                            var index = availableAnalytesGrid.getStore().findBy(function(rec, id){
                                return rec.get('Titration') == record.data.Name && rec.get('RowId') == analyte;
                            });
                            availableAnalytesGrid.getSelectionModel().selectRow(index, true);
                        });
                        var id = titrationExclusionStore.findExact('Description', record.data.Name);
                        this.preExcludedIds[rowId] = titrationExclusionStore.getAt(id).data.RowId;
                    }
                    else if(this.excluded[rowId])
                    {
                        selMod.selectRecords(this.excluded[rowId], false);
                    }
                    else
                    {
                        selMod.clearSelections();
                    }
                    selMod.resumeEvents();

                    if(this.comments[rowId])
                    {
                        Ext.getCmp('comment').setValue(this.comments[rowId]);
                    }
                    else
                    {
                        Ext.getCmp('comment').setValue('');
                    }
                },
                scope : this
            }

        });

        // grid of avaialble/excluded titrations
        var gridData = [];
        var titrationsGridStore = new LABKEY.ext.Store({
            schemaName: 'assay.Luminex.' + this.assayName,
            sql : 'SELECT DISTINCT x.Titration.Name, x.Data.RowId AS DataId, x.Data.Run.RowId AS RunId ' +
                    'FROM Data AS x '+
                    'WHERE x.Titration IS NOT NULL AND x.Titration.Standard != true AND x.Data.Run.RowId = ' + this.runId,
            sortInfo: {
                field: 'Name',
                direction: 'ASC'
            },
            listeners : {
                load : function(store, records)
                {
                    var id;
                    for(var i = 0; i < titrationExclusionStore.getCount(); i++)
                    {
                        id = store.findExact('Name', titrationExclusionStore.getAt(i).data.Description);
                        if(id >= 0)
                        {
                            this.present[id] = titrationExclusionStore.getAt(i).get("Analytes/RowId").split(",").length;
                            titrationExclusionStore.getAt(i).set('present', this.present[id]);
                            titrationExclusionStore.getAt(i).commit();
                        }
                    }
                    for(i = 0; i < records.length; i++)
                    {
                        gridData[i] = [];
                        for(var index in records[i].data)
                        {
                            gridData[i].push(records[i].get(index));
                        }
                        gridData[i].push(this.present[i]);
                    }

                    combinedStore.loadData(gridData);
                    titrationExclusionStore.fireEvent('titrationgridloaded');

                },
                scope : this
            }
        });

        var combinedStore = new Ext.data.ArrayStore({
            fields : ['Name', 'DataId', 'RunId', 'Present']
        });

        var _tpl = new Ext.XTemplate(
                '<span>{[this.getPresentValue(values.Present)]}</span>',
                {
                    getPresentValue : function(x) {
                        if(x != ''){
                            var val = x + ' analytes excluded';
                        }
                        else
                            var val = '';
                        return val;
                    }
                }
        );
        _tpl.compile();

        this.availableTitrationsGrid = new Ext.grid.GridPanel({
            id: 'titrationGrid',
            style: 'padding-top: 10px;',
            title: "Select a titration to view a list of available analytes",
            headerStyle: 'font-weight: normal; background-color: #ffffff',
            store: combinedStore,
            colModel: new Ext.grid.ColumnModel({
                columns: [
                    this.titrationSelMod,
                    {
                        xtype : 'templatecolumn',
                        header : 'Exclusions',
                        tpl : _tpl
                    }
                ]
            }),
            autoExpandColumn: 'Name',
            viewConfig: {
                forceFit: true
            },
            sm: this.titrationSelMod,
            anchor: '100%',
            height: 165,
            frame: false,
            loadMask: true
        });

        this.add(this.availableTitrationsGrid);

        // grid of avaialble/excluded analytes
        var availableAnalytesGrid = new Ext.grid.GridPanel({
            id: 'availableanalytes',
            style: 'padding-top: 10px;',
            title: "Select the checkbox next to the analytes within the selected titration to be excluded",
            headerStyle: 'font-weight: normal; background-color: #ffffff',
            store:  new LABKEY.ext.Store({
                sql: "SELECT DISTINCT x.Titration.Name AS Titration, x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                        + " FROM Data AS x WHERE x.Titration IS NOT NULL AND x.Data.Run.RowId = " + this.runId,
                schemaName: 'assay.Luminex.' + this.assayName,
                autoLoad: true,
                sortInfo: {
                    field: 'Name',
                    direction: 'ASC'
                }
            }),
            colModel: new Ext.grid.ColumnModel({
                defaults: {
                    sortable: false,
                    menuDisabled: true
                },
                columns: [
                    selMod,
                    {header: 'Available Analytes', dataIndex: 'Name'},
                    {header: 'Titration', dataIndex: 'Titration', hidden: true}
                ]
            }),
            autoExpandColumn: 'Name',
            viewConfig: {
                forceFit: true
            },
            sm: selMod,
            anchor: '100%',
            height: 165,
            frame: false,
            disabled : true,
            loadMask: true
        });
        this.add(availableAnalytesGrid);

        this.addCommentPanel();

        this.addStandardButtons();

        this.doLayout();

        this.queryForRunAssayId();
    },

    toggleSaveBtn : function(sm, grid){
        grid.getFooterToolbar().findById('saveBtn').enable();
    },

    insertUpdateExclusions: function() {
        var index = this.availableTitrationsGrid.getStore().indexOf(this.titrationSelMod.getSelected());
        this.titrationSelMod.fireEvent('rowdeselect', this.titrationSelMod, index, this.titrationSelMod.getSelected());
        this.openConfirmWindow();
    },

    getExcludedString : function()
    {
        var retString = '';

        for (var i = 0; i < this.present.length; i++)
        {
            // issue 21431
            if (this.present[i] == undefined)
                continue;

            if(!(this.preExcludedIds[i] == undefined && this.present[i] == 0))
            {
                if(this.present[i] != 1)
                    retString += this.availableTitrationsGrid.getStore().getAt(i).get('Name') + ': ' + this.present[i] + ' analytes excluded.<br>';
                else
                    retString += this.availableTitrationsGrid.getStore().getAt(i).get('Name') + ': ' + this.present[i] + ' analyte excluded.<br>';
            }
        }
        return retString;
    },

    openConfirmWindow : function(){
        var excludedMessage = this.getExcludedString();
        if(excludedMessage == '')
        {
            this.fireEvent('closeWindow');
        }
        else
        {
            Ext.Msg.show({
                title:'Confirm Exclusions',
                msg: 'Please verify the excluded analytes for the following titrations. Continue?<br><br> ' + excludedMessage,
                buttons: Ext.Msg.YESNO,
                fn: function(button){
                    if(button == 'yes'){
                        this.insertUpdateTitrationExclusions();
                    }
                },
                icon: Ext.MessageBox.QUESTION,
                scope : this
            });
        }
    },

    insertUpdateTitrationExclusions: function(){

        this.mask("Saving titration exclusions...");

        var commands = [];
        for (var index = 0; index < this.excluded.length; index++)
        {
            var dataId = this.excludedDataIds[index];
            var analytesForExclusion = this.excluded[index];
            if (analytesForExclusion == undefined)
                continue;

            // generage a comma delim string of the analyte Ids to exclude
            var analyteRowIds = "";
            var analyteNames = "";
            var sep = "";
            Ext.each(analytesForExclusion, function(record){
                analyteRowIds += sep.trim() + record.data.RowId;
                analyteNames += sep + record.data.Name;
                sep = ", ";
            });

            // determine if this is an insert, update, or delete
            var command = "insert";
            if (this.preExcludedIds[index] != undefined)
                command = analyteRowIds != "" ? "update" : "delete";

            // issue 21551: don't insert an exclusion w/out any analytes
            if (command == "insert" && analyteRowIds == "")
                continue;

            // don't call update if no change to analyte selection for a given titration
            if (this.preAnalyteRowIds[index] == analyteRowIds)
                continue;

            // config of data to save for a single titration exclusion
            var commandConfig = {
                command: command,
                key: this.preExcludedIds[index], // this will be undefined for the insert case
                dataId: dataId,
                description: analytesForExclusion.name,
                analyteRowIds: (analyteRowIds != "" ? analyteRowIds : null),
                analyteNames: (analyteNames != "" ? analyteNames : null), // for logging purposes only
                comment: this.comments[index]
            };

            commands.push(commandConfig);
        }

        if (commands.length > 0)
        {
            var config = {
                assayName: this.assayName,
                tableName: 'TitrationExclusion',
                runId: this.runId,
                commands: commands
            };

            this.saveExclusions(config, 'titration');
        }
        else
        {
            this.unmask();
        }
    }
});