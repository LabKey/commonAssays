/*
 * Copyright (c) 2013 LabKey Corporation
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
            schemaName: 'assay',
            queryName: assayName,
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
 * @params schameName = the name of the schema used to get the run's unique analyte names
 * @params queryName = the name of the query used to get the run's unique analyte names
 * @params runId = runId for the selected replicate group
 */
LABKEY.TitrationExclusionPanel = Ext.extend(Ext.Panel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.schemaName)
            throw "You must specify a schemaName!";
        if (!config.queryName)
            throw "You must specify a queryName!";
        if (!config.runId)
            throw "You must specify a runId!";

        Ext.apply(config, {
            autoScroll: true,
            border: false,
            items: [],
            buttonAlign: 'center',
            buttons: []
        });

        this.addEvents('closeWindow');
        LABKEY.TitrationExclusionPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // query the RunExclusion table to see if there are any existing exclusions for this run
        console.log(this.runId);
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.queryName + ' RunExclusion',
            filterArray: [LABKEY.Filter.create('runId', this.runId)],
            columns: 'RunId,Comment,Analytes/RowId',
            success: function(data){
                // if there are exclusions for this run, add the info to this
                this.exclusionsExist = false;
                if (data.rows.length == 1)
                {
                    this.exclusionsExist = true;
                    this.comment = data.rows[0].Comment;
                    this.analytes = data.rows[0]["Analytes/RowId"];
                }

                this.setupWindowPanelItems();
            },
            scope: this
        });

        LABKEY.TitrationExclusionPanel.superclass.initComponent.call(this);
    },

    setupWindowPanelItems: function()
    {
        this.excluded = [];
        this.comments = [];
        this.excludedDataIds = [];
        this.present = [];
        this.preExcludedIds = [];
        // panel header information for replicate group
        this.add(new Ext.form.FormPanel({
            style: 'padding-bottom: 10px; background: #ffffff',
            html: this.getExclusionPanelHeader(),
            timeout: Ext.Ajax.timeout,
            border: false
        }));

        var titrationExclusionStore = new LABKEY.ext.Store({
            schemaName: 'assay.Luminex.' + this.queryName,
            queryName : 'TitrationExclusion',
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
                        this.preExcludedIds[id] = records[i].get('Analytes/RowId').split(",");
                        this.comments[id] = records[i].get('Comment');
                    }
                },
                scope : this
            }
        });

        // text to describe how run exclusions are handled
        this.add(new Ext.form.DisplayField({
            hideLabel: true,
            style: 'font-style: italic; font-size: 90%',
            value: 'Analytes excluded for a replicate group will not be re-included by changes in assay level exclusions'
        }));

        var updateSaveBtn = function(sm, grid){
            // enable the save button when changes are made to the selection or is exclusions exist
            if (sm.getCount() > 0 || grid.exclusionsExist)
                grid.getFooterToolbar().findById('saveBtn').enable();
        };

        // checkbox selection model for selecting which analytes to exclude
        var selMod = new Ext.grid.CheckboxSelectionModel();
        selMod.on('selectionchange', function(sm){
            updateSaveBtn(sm, this);
        }, this, {buffer: 250});

        // Issue 17974: make rowselect behave like checkbox select, i.e. keep existing other selections in the grid
        selMod.on('beforerowselect', function(sm, rowIndex, keepExisting, record) {
            sm.suspendEvents();
            if (sm.isSelected(rowIndex))
                sm.deselectRow(rowIndex);
            else
                sm.selectRow(rowIndex, true);
            sm.resumeEvents();

            updateSaveBtn(sm, this);

            return false;
        }, this);

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
                    console.log(record);
                },
                rowselect : function(tsl, rowId, record)
                {
                    availableAnalytesGrid.setDisabled(false);
                    if(typeof this.preExcludedIds[rowId] === 'object')
                    {
                        selMod.clearSelections();
                        Ext.each(this.preExcludedIds[rowId], function(analyte){
                            var index = availableAnalytesGrid.getStore().find('RowId', analyte);
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

        // set the title for the grid panel based on previous exclusions
        var titrationTitle = "Select a titration to view a list of available analytes";

        var title = "Select the checkbox next to the analytes within the selected titration to be excluded";

        var gridData = [];
        // grid of avaialble/excluded titrations
        var titrationsGridStore = new LABKEY.ext.Store({
            schemaName: 'assay.Luminex.' + this.queryName,
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
                    console.log(gridData);
                    combinedStore.loadData(gridData);
                    titrationExclusionStore.fireEvent('titrationgridloaded');

                },
                scope : this
            }
        });

        var combinedStore = new Ext.data.ArrayStore({
            fields : ['Name', 'DataId', 'RunId', 'Present'],
            listeners : {
                load : function(store)
                {
                    console.log(store);
                }
            }
        });

        var me = this;
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
            title: titrationTitle,
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
            title: title,
            headerStyle: 'font-weight: normal; background-color: #ffffff',
            store:  new LABKEY.ext.Store({
                sql: "SELECT DISTINCT x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                        + " FROM \"" + this.queryName + " Data\" AS x "
                        + " WHERE x.Data.Run.RowId = " + this.runId,
                schemaName: this.schemaName,
                autoLoad: true,
                sortInfo: {
                    field: 'Name',
                    direction: 'ASC'
                }
            }),
            colModel: new Ext.grid.ColumnModel({
                columns: [
                    selMod,
                    {header: 'Available Analytes', sortable: false, dataIndex: 'Name', menuDisabled: true}
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

        // comment textfield
        var commentPanel = new Ext.form.FormPanel({
            height: 75,
            style: 'padding-top: 20px; background: #ffffff',
            timeout: Ext.Ajax.timeout,
            labelAlign: 'top',
            items: [
                new Ext.form.TextField({
                    id: 'comment',
                    fieldLabel: 'Comment',
                    labelStyle: 'font-weight: bold',
                    anchor: '100%',
                    enableKeyEvents: true,
                    listeners: {
                        scope: this,
                        'keydown': function(){
                            // enable the save changes button when the comment is edited by the user, if exclusions exist
                            if (this.exclusionsExist)
                                this.getFooterToolbar().findById('saveBtn').enable();
                        }
                    }
                })
            ],
            border: false
        });
        this.add(commentPanel);

        // add save and cancel buttons
        this.addButton({
            id: 'saveBtn',
            text: 'Save',
            disabled: true,
            handler: function()
            {
                this.titrationSelMod.fireEvent('rowdeselect', this.titrationSelMod, this.availableTitrationsGrid.getStore().indexOf(this.titrationSelMod.getSelected()), this.titrationSelMod.getSelected());
                this.openConfirmWindow();
            },
            scope: this
        });
        this.addButton({
            text: 'Cancel',
            handler: function(){this.fireEvent('closeWindow');},
            scope: this
        });

        this.doLayout();

        this.queryForRunAssayId();
    },

    queryForRunAssayId: function()
    {
        // query to get the assay Id for the given run and put it into the panel header div
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.queryName + ' Runs',
            filterArray: [LABKEY.Filter.create('RowId', this.runId)],
            columns: 'Name',
            success: function(data){
                if (data.rows.length == 1)
                {
                    Ext.get('run_assay_id').update(data.rows[0].Name);
                }
            },
            scope: this
        });
    },

    getExclusionPanelHeader: function()
    {
        // return an HTML table with the run Id and a place holder div for the assay Id
        return "<table cellspacing='0' width='100%' style='border-collapse: collapse'>"
                + "<tr><td class='labkey-exclusion-td-label'>Run ID:</td><td class='labkey-exclusion-td-cell'>" + this.runId + "</td></tr>"
                + "<tr><td class='labkey-exclusion-td-label'>Assay ID:</td><td class='labkey-exclusion-td-cell'><div id='run_assay_id'>...</div></td></tr>"
                + "</table>";
    },

    getExcludedString : function()
    {
        var retString = '';
        console.log(this.present);
        for(var thing in this.present)
        {
            if(thing != 'remove' && !(this.preExcludedIds[thing] == undefined && this.present[thing] == 0))
            {
                if(this.present[thing] != 1)
                    retString += this.availableTitrationsGrid.getStore().getAt(thing).get('Name') + ': ' + this.present[thing] + ' analytes excluded.<br>';
                else
                    retString += this.availableTitrationsGrid.getStore().getAt(thing).get('Name') + ': ' + this.present[thing] + ' analyte excluded.<br>';
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
                msg: 'Would you really like to exclude analytes on the following Titrations?<br><br> ' + this.getExcludedString(),
                buttons: Ext.Msg.YESNO,
                fn: function(button){
                    if(button == 'yes'){
                        this.insertUpdateWellExclusions();
                    }
                },
                icon: Ext.MessageBox.QUESTION,
                scope : this
            });
        }
    },

    insertUpdateWellExclusions: function(){
        // mask the window until the insert/update is complete (or if something goes wrong)
//        var message = "Saving replicate group exclusion...";
//        if (this.findById('reCalcDisplay').isVisible())
//            message = "Saving replicate group exclusion and re-calculating curve...";
//        this.findParentByType('window').getEl().mask(message, "x-mask-loading");

        // generage a comma delim string of the analyte Ids to exclude
        for(var key in this.excluded)
        {
            var dataId = this.excludedDataIds[key];
            var analytesForExclusion = this.excluded[key];
            if(key==='remove')
                continue;
            var analytesForExclusionStr = "";
            Ext.each(analytesForExclusion, function(record){
                analytesForExclusionStr += (analytesForExclusionStr != "" ? "," : "") + record.data.RowId;
            });
            // config of data to save for the given replicate group exclusion
            var config = {
                schemaName: "assay.Luminex." + this.queryName,
                queryName: 'WellExclusion',
                rows: [{
                    description: analytesForExclusion.name,
                    dataId: dataId,
                    comment: this.comments[key],
                    "analyteId/RowId": (analytesForExclusionStr != "" ? analytesForExclusionStr : null)
                }],
                success: function(){
                    this.fireEvent('closeWindow');
                    window.location.reload();
                },
                failure: function(info, response, options){
                    if (this.findParentByType('window').getEl().isMasked())
                        this.findParentByType('window').getEl().unmask();

                    LABKEY.Utils.displayAjaxErrorResponse(response, options);
                },
                scope: this
            };

            // insert, update, or delete to/from the WellExclusions table with config information

            if (typeof this.preExcludedIds[key] === 'number')
            {
                console.log(this.preExcludedIds);

                config.rows[0].rowId = this.preExcludedIds[key];
                if (analytesForExclusionStr != "")
                {
                    LABKEY.Query.updateRows(config);
                }
                else
                {
                   LABKEY.Query.deleteRows(config);
                }
            }
            else
            {
                if (analytesForExclusionStr != "")
                {
                    LABKEY.Query.insertRows(config);
                }
                else
                {
                    this.findParentByType('window').getEl().unmask();
                }
            }
        }
    }
});