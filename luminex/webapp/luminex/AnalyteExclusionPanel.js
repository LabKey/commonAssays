/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of 'Exclude Analytes' button to open the run exclusion window 
function analyteExclusionWindow(assayName, runId)
{
    var win = new Ext.Window({
        cls: 'extContainer',
        title: 'Exclude Analytes from Analysis',
        layout:'fit',
        width: Ext.getBody().getViewSize().width < 490 ? Ext.getBody().getViewSize().width * .9 : 440,
        height: Ext.getBody().getViewSize().height > 500 ? 460 : Ext.getBody().getViewSize().height * .75,
        padding: 15,
        modal: true,
        closeAction:'close',
        items: new LABKEY.AnalyteExclusionPanel({
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
LABKEY.AnalyteExclusionPanel = Ext.extend(Ext.Panel, {
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
        LABKEY.AnalyteExclusionPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // query the RunExclusion table to see if there are any existing exclusions for this run
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

        LABKEY.AnalyteExclusionPanel.superclass.initComponent.call(this);
    },

    setupWindowPanelItems: function()
    {
        // panel header information for replicate group
        this.add(new Ext.form.FormPanel({
            style: 'padding-bottom: 10px; background: #ffffff',
            html: this.getExclusionPanelHeader(),
            timeout: Ext.Ajax.timeout,
            border: false
        }));

        // text to describe how run exclusions are handled
        this.add(new Ext.form.DisplayField({
            hideLabel: true,
            style: 'font-style: italic; font-size: 90%',
            value: 'Analytes excluded for a replicate group will not be re-included by changes in assay level exclusions'
        }));

        // checkbox selection model for selecting which analytes to exclude
        var selMod = new Ext.grid.CheckboxSelectionModel();
        selMod.on('selectionchange', function(sm){
            // enable the save button when changes are made to the selection or is exclusions exist
            if (sm.getCount() > 0 || this.exclusionsExist)
                this.getFooterToolbar().findById('saveBtn').enable();
            
            // disable the save button if no exclusions exist and no selection is made
            if(sm.getCount() == 0 && !this.exclusionsExist)
                this.getFooterToolbar().findById('saveBtn').disable();
        }, this, {buffer: 250});

        // set the title for the grid panel based on previous exclusions
        var title = "Select the checkbox next to the analytes to be excluded";
        if (this.exclusionsExist)
        {
            title += "<BR/><span style='color:red;font-style:italic;'>Uncheck analytes to remove exclusions</span>";
        }

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
                listeners: {
                    scope: this,
                    'load': function(store, records, options){
                        if (this.analytes)
                        {
                            // preselect any previously excluded analytes
                            availableAnalytesGrid.getSelectionModel().suspendEvents(false);
                            Ext.each(this.analytes, function(analyte){
                                var index = store.find('RowId', analyte);
                                availableAnalytesGrid.getSelectionModel().selectRow(index, true);
                            });
                            availableAnalytesGrid.getSelectionModel().resumeEvents();
                        }
                    }
                },
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
            loadMask: true
        });
        this.add(availableAnalytesGrid);

        // comment textfield
        this.add(new Ext.form.FormPanel({
            height: 75,
            style: 'padding-top: 20px; background: #ffffff',
            timeout: Ext.Ajax.timeout,
            labelAlign: 'top',
            items: [
                new Ext.form.TextField({
                    id: 'comment',
                    fieldLabel: 'Comment',
                    value: this.comment ? this.comment : null,
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
        }));

        // add save and cancel buttons
        this.addButton({
            id: 'saveBtn',
            text: 'Save Changes',
            disabled: true,
            handler: this.insertUpdateAnalyteExclusions,
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

    insertUpdateAnalyteExclusions: function(){
        // generage a comma delim string of the analyte Ids to exclude
        var analytesForExclusion = this.findById('availableanalytes').getSelectionModel().getSelections();
        var analytesForExclusionStr = "";
        Ext.each(analytesForExclusion, function(record){
            analytesForExclusionStr += (analytesForExclusionStr != "" ? "," : "") + record.get("RowId");
        });

        // config of data to save for the given replicate group exclusion
        var config = {
            schemaName: 'assay',
            queryName: this.queryName + ' RunExclusion',
            rows: [{
                runId: this.runId,
                comment: this.findById('comment').getValue(),
                "analyteId/RowId": (analytesForExclusionStr != "" ? analytesForExclusionStr : null)
            }],
            success: function(){
                this.fireEvent('closeWindow');
                window.location.reload();
            },
            scope: this
        };

        // insert, update, or delete to/from the RunExclusion table with config information
        if (this.exclusionsExist)
        {
            if (analytesForExclusionStr != "")
            {
                LABKEY.Query.updateRows(config);
            }
            else
            {
                // ask the user if they are sure they want to remove the exclusions before deleting
                Ext.Msg.show({
                    title:'Warning',
                    msg: 'Are you sure you want to remove all analyte exlusions for run Id ' + this.runId + '?',
                    buttons: Ext.Msg.YESNO,
                    fn: function(btnId, text, opt){
                        if (btnId == 'yes')
                        {
                            LABKEY.Query.deleteRows(config);
                        }
                    },
                    icon: Ext.MessageBox.WARNING,
                    scope: this
                });
            }
        }
        else
        {
            if (analytesForExclusionStr != "")
                LABKEY.Query.insertRows(config);
        }
    }
});