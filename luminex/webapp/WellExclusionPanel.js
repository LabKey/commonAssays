/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of Exclusion Toggle to open the well exclusion window 
function wellExclusionWindow(assayName, runId, dataId, description, dilution)
{
    var win = new Ext.Window({
        cls: 'extContainer',
        title: 'Exclude Replicate Group from Analysis',
        layout:'fit',
        width:440,
        height:555,
        padding: 15,
        modal: true,
        closeAction:'close',
        items: new LABKEY.WellExclusionPanel({
            schemaName: 'assay',
            queryName: assayName,
            runId: runId,
            dataId: dataId,
            description: description,
            dilution: dilution,
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
 * @params dataId = dataId for the selected replicate group
 * @params description = description for the selected replicate group
 * @params dilution = dilution for the selected replicate group
 */
LABKEY.WellExclusionPanel = Ext.extend(Ext.Panel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.schemaName)
            throw "You must specify a schemaName!";
        if (!config.queryName)
            throw "You must specify a queryName!";
        if (!config.runId || !config.dataId || !config.dilution)
            throw "You must specify the following: runId, dataId, and dilution!";

        Ext.apply(config, {
            cls: 'extContainer',
            autoScroll: true,
            border: false,
            items: [],
            buttonAlign: 'center',
            buttons: []
        });

        this.addEvents('closeWindow');
        LABKEY.WellExclusionPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // query the WellExclusion table to see if there are any existing exclusions for this replicate Group
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.queryName + ' WellExclusion',
            filterArray: [
                LABKEY.Filter.create('description', this.description),
                LABKEY.Filter.create('dilution', this.dilution),
                LABKEY.Filter.create('dataId', this.dataId)
            ],
            columns: 'RowId,Comment,Analytes/RowId',
            success: function(data){
                // if there are well exclusions for the replicate group, add the info to this
                this.exclusionsExist = false;
                if (data.rows.length == 1)
                {
                    this.exclusionsExist = true;
                    this.rowId = data.rows[0].RowId;
                    this.comment = data.rows[0].Comment;
                    this.analytes = data.rows[0]["Analytes/RowId"];
                }

                this.setupWindowPanelItems();
            },
            scope: this
        });

        LABKEY.WellExclusionPanel.superclass.initComponent.call(this);
    },

    setupWindowPanelItems: function()
    {
        // panel header information for replicate group
        this.add(new Ext.form.FormPanel({
            style: 'padding-bottom: 10px; background: #ffffff',
            html: this.getExclusionPanelHeader(),
            border: false
        }));

        // text to describe how run exclusions are handled
        this.add(new Ext.form.DisplayField({
            hideLabel: true,
            style: 'font-style: italic; font-size: 90%',
            value: 'Analytes excluded at the assay level will not be re-included by changes in replicate group exclusions'
        }));

        // radio group for selecting "exclude all" or "exclude selected"
        this.add(new Ext.form.FormPanel({
            style: 'padding-top: 10px; padding-bottom: 10px; background: #ffffff',
            border: true,
            padding: 3,
            items: new Ext.form.RadioGroup({
                id: 'excluderadiogroup',
                allowBlank: false,
                anchor: '100%',
                hideLabel: true,
                columns: 2,
                items: [{
                    id: 'excludeall',
                    name: 'excluderadio',
                    boxLabel: 'Exclude all analytes',
                    inputValue: 1,
                    listeners: {
                        scope: this,
                        'check': function(radio, checked)
                        {
                            if (checked)
                            {
                                this.findById('availableanalytes').getSelectionModel().selectAll();
                                this.findById('availableanalytes').disable();
                            }
                        }
                    }
                },{
                    id: 'excludeselected',
                    name: 'excluderadio',
                    boxLabel: 'Exclude selected analytes',
                    inputValue: 2,
                    listeners: {
                        scope: this,
                        'check': function(radio, checked)
                        {
                            if (checked)
                            {
                                this.findById('availableanalytes').getSelectionModel().clearSelections();
                                this.findById('availableanalytes').enable();
                            }
                        }
                    }
                }]
            })
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
            cls: 'extContainer',
            title: title,
            headerStyle: 'font-weight: normal; background-color: #ffffff',
            store:  new LABKEY.ext.Store({
                sql: "SELECT DISTINCT x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                    + " FROM \"" + this.queryName + " Data\" AS x "
                    + " WHERE x.Data.Run.RowId = " + this.runId, // todo: check if this works for 2 files in one run that have different sets of analytes
                schemaName: this.schemaName,
                autoLoad: true,
                listeners: {
                    scope: this,
                    'load': function(store, records, options){
                        if (this.analytes)
                        {
                            // determine if all of the analytes are excluded for this replicate group
                            if (this.analytes.length == store.getTotalCount())
                            {
                                this.findById('excluderadiogroup').onSetValue('excludeall', 1);
                            }
                            else
                            {
                                this.findById('excluderadiogroup').onSetValue('excludeselected', 2);

                                // preselect any previously excluded analytes
                                availableAnalytesGrid.getSelectionModel().suspendEvents(false);
                                Ext.each(this.analytes, function(analyte){
                                    var index = store.find('RowId', analyte);
                                    availableAnalytesGrid.getSelectionModel().selectRow(index, true);
                                });
                                availableAnalytesGrid.getSelectionModel().resumeEvents();
                            }
                        }
                        else
                        {
                            this.findById('excluderadiogroup').onSetValue('excludeall', 1);
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
            viewConfig: {
                forceFit: true
            },
            autoExpandColumn: 'Name',
            sm: selMod,
            anchor: '100%',
            height: 165,
            frame: false
        });
        this.add(availableAnalytesGrid);

        // add a div for listing the run exclusions (if necessary)
        this.add({
            html: "<div id='run_analyte_exclusions'></div>",
            style: "font-style: italic; font-size: 90%",
            border: false
        });

        // comment textfield
        this.add(new Ext.form.FormPanel({
            height: 75,
            style: 'padding-top: 20px; background: #ffffff',
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
            handler: this.insertUpdateWellExclusions,
            scope: this
        });
        this.addButton({
            text: 'Cancel',
            handler: function(){this.fireEvent('closeWindow');},
            scope: this
        });

        this.doLayout();

        this.queryForReplicateGroupWellsAndFileName();
        this.queryForRunLevelExclusions();
    },

    queryForReplicateGroupWellsAndFileName: function()
    {
        var sql = "SELECT DISTINCT x.Well, x.Data.Name AS Name "
                + "FROM \"" + this.queryName + " Data\" AS x WHERE ";
        sql += (this.description != null ? " x.Description = '" + this.description + "'" : " x.Description IS NULL ");
        sql += " AND x.Dilution = " + this.dilution + " AND x.Data.RowId = " + this.dataId;

        // query to get the wells and data id (file name) for the given replicate group
        LABKEY.Query.executeSql({
            schemaName: 'assay',
            sql: sql,
            success: function(data){
                var wells = "";
                var filename = "";
                for (var i = 0; i < data.rows.length; i++)
                {
                    wells += (wells.length > 0 ? ", " : "") + data.rows[i].Well;
                    filename = data.rows[i].Name;
                }
                Ext.get('replicate_group_wells').update(wells);
                Ext.get('replicate_group_filename').update(filename);
            },
            scope: this
        });
    },

    queryForRunLevelExclusions: function()
    {
        // query to see if there are any run level exclusions for this RunId
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.queryName + ' RunExclusion',
            filterArray: [LABKEY.Filter.create('RunId', this.runId)],
            columns: 'Analytes/Name',
            success: function(data){
                if (data.rows.length == 1)
                {
                    Ext.get('run_analyte_exclusions').update("The following analytes have been excluded at the assay level: <b>" + data.rows[0]['Analytes/Name'] + "</b>");
                }
            },
            scope: this
        });
    },

    getExclusionPanelHeader: function()
    {
        // return an HTML table with the description and dilution and place holder divs for the file name and wells
        return "<table cellspacing='0' width='100%' style='border-collapse: collapse'>"
                    + "<tr><td class='labkey-exclusion-td-label'>File Name:</td><td class='labkey-exclusion-td-cell' colspan='3'><div id='replicate_group_filename'>...</div></td></tr>"
                    + "<tr><td class='labkey-exclusion-td-label'>Sample:</td><td class='labkey-exclusion-td-cell' colspan='3'>" + (this.description != null ? this.description : "") + "</td></tr>"
                    + "<tr><td class='labkey-exclusion-td-label'>Dilution:</td><td class='labkey-exclusion-td-cell'>" + this.dilution + "</td>"
                    + "<td class='labkey-exclusion-td-label'>Wells:</td><td class='labkey-exclusion-td-cell'><div id='replicate_group_wells'>...</div></td></tr>"
                    + "</table>";
    },

    insertUpdateWellExclusions: function(){
        // generage a comma delim string of the analyte Ids to exclude
        var analytesForExclusion = this.findById('availableanalytes').getSelectionModel().getSelections();
        var analytesForExclusionStr = "";
        Ext.each(analytesForExclusion, function(record){
            analytesForExclusionStr += (analytesForExclusionStr != "" ? "," : "") + record.get("RowId");
        });

        // config of data to save for the given replicate group exclusion
        var config = {
            schemaName: 'assay',
            queryName: this.queryName + ' WellExclusion',
            rows: [{
                description: this.description,
                dilution: this.dilution,
                dataId: this.dataId,
                comment: this.findById('comment').getValue(),
                "analyteId/RowId": (analytesForExclusionStr != "" ? analytesForExclusionStr : null)
            }],
            success: function(){
                this.fireEvent('closeWindow');
                window.location.reload();
            },
            scope: this
        };

        // insert, update, or delete to/from the WellExclusions table with config information
        if (this.rowId)
        {
            config.rows[0].rowId = this.rowId;
            if (analytesForExclusionStr != "")
            {
                LABKEY.Query.updateRows(config);
            }
            else
            {
                // ask the user if they are sure they want to remove the exclusions before deleting
                Ext.Msg.show({
                    title:'Warning',
                    msg: 'Are you sure you want to remove all analyte exlusions for the selected replicate group?',
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