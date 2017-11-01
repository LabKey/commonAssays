/*
 * Copyright (c) 2011-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of Exclusion Toggle to open the well exclusion window
function openExclusionsWellWindow(assayId, runId, dataId, description, type)
{
    // lookup the assay design information based on the Assay RowId
    LABKEY.Assay.getById({
        id: assayId,
        success: function(assay)
        {
            if (Ext.isArray(assay) && assay.length == 1)
            {
                var win = new LABKEY.Exclusions.BaseWindow({
                    title: 'Exclude Replicate Group from Analysis',
                    width: Ext.getBody().getViewSize().width < 500 ? Ext.getBody().getViewSize().width * .9 : 450,
                    height: Ext.getBody().getViewSize().height > 595 ? 560 : Ext.getBody().getViewSize().height * .75,
                    items: new LABKEY.Exclusions.WellPanel({
                        protocolSchemaName: assay[0].protocolSchemaName,
                        assayId: assayId,
                        runId: runId,
                        dataId: dataId,
                        description: description,
                        type: type,
                        listeners: {
                            scope: this,
                            closeWindow: function()
                            {
                                win.close();
                            }
                        }
                    })
                });
                win.show(this);
            }
            else {
                Ext.Msg.alert('ERROR', 'Unable to find assay design information for id ' + assayId);
            }
        }
    });
}

/**
 * Class to display panel for selecting which analytes for a given replicate group to exclude from a Luminex run
 * @params protocolSchemaName = the encoded protocol schema name to use (based on the assay design name)
 * @params assayId = the assay design RowId
 * @params runId = runId for the selected replicate group
 * @params dataId = dataId for the selected replicate group
 * @params description = description for the selected replicate group
 * @params type = type for the selected replicate group (i.e. S1, C2, X3, etc.)
 */
LABKEY.Exclusions.WellPanel = Ext.extend(LABKEY.Exclusions.BasePanel, {
    constructor : function(config)
    {
        // check that the config properties needed are present
        if (!config.dataId || !config.type)
            throw "You must specify the following: dataId, and type!";

        LABKEY.Exclusions.WellPanel.superclass.constructor.call(this, config);
    },

    initComponent : function()
    {
        // query the WellExclusion table to see if there are any existing exclusions for this replicate Group
        var filterArray = [
            LABKEY.Filter.create('description', this.description),
            LABKEY.Filter.create('type', this.type),
            LABKEY.Filter.create('dataId', this.dataId)
        ];
        this.queryExistingExclusions('WellExclusion', filterArray, 'RowId,Comment,Analytes/RowId');

        LABKEY.Exclusions.WellPanel.superclass.initComponent.call(this);
    },

    setupWindowPanelItems: function()
    {
        this.addHeaderPanel('Analytes excluded for a singlepoint unknown, titration, or at the assay level will not be re-included by changes in replicate group exclusions.');

        // radio group for selecting "exclude all" or "exclude selected"
        this.add(new Ext.form.FormPanel({
            style: 'padding: 10px 0 5px 0;',
            border: false,
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
                        check: function(radio, checked)
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
                        check: function(radio, checked)
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

        var selMod = this.getGridCheckboxSelectionModel();

        // set the title for the grid panel based on previous exclusions
        var title = "Select the checkbox next to the analyte(s) to be excluded:";
        if (this.exclusionsExist)
        {
            title += "<BR/><span style='color:red;font-style:italic;'>Uncheck all analytes to remove exclusions</span>";
        }

        // grid of available/excluded analytes
        var availableAnalytesGrid = new Ext.grid.GridPanel({
            id: 'availableanalytes',
            cls: 'extContainer',
            title: title,
            headerStyle: 'font-weight: normal;',
            store:  new LABKEY.ext.Store({
                sql: "SELECT DISTINCT x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                    + " FROM Data AS x "
                    + " WHERE x.Data.Run.RowId = " + this.runId + " AND x.Type = '" + this.type + "' "
                    + " AND x.Description " + (this.description ? " = '" + this.description + "'" : " IS NULL")
                    + " ORDER BY x.Analyte.Name",
                schemaName: this.protocolSchemaName,
                autoLoad: true,
                listeners: {
                    scope: this,
                    load: function(store, records, options)
                    {
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
                                Ext.each(this.analytes, function(analyte)
                                {
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
                    {header: 'Analyte Name', sortable: false, dataIndex: 'Name', menuDisabled: true}
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

        this.addCommentPanel();

        // add a warning message for when exclusions of titrations will result in assay re-run
        this.add(new Ext.form.DisplayField({
            id: 'reCalcDisplay',
            hidden: true,
            value: 'With this exclusion, your titration curve will be out-of-date and will be re-calculated.',
            style: {color: 'red'}
        }));

        this.addStandardButtons();

        this.doLayout();

        this.queryForReplicateGroupWellsAndFileName();
        this.queryForRunLevelExclusions();
    },

    queryForReplicateGroupWellsAndFileName: function()
    {
        var sql = "SELECT DISTINCT x.Well, x.Data.Name AS Name, "
                + "CASE WHEN (x.Titration IS NOT NULL) THEN TRUE ELSE FALSE END AS IsTitration "
                + "FROM Data AS x WHERE ";
        sql += (this.description != null ? " x.Description = '" + this.description + "'" : " x.Description IS NULL ");
        sql += " AND x.Type = '" + this.type + "' AND x.Data.RowId = " + this.dataId;

        // query to get the wells and data id (file name) for the given replicate group
        LABKEY.Query.executeSql({
            schemaName: this.protocolSchemaName,
            sql: sql,
            sort: 'Well',
            success: function(data)
            {
                var wells = [];
                var filename = "";
                var isTitration = false;
                for (var i = 0; i < data.rows.length; i++)
                {
                    // summary rows will have > 1 well (separated by a comma)
                    Ext.each(data.rows[i].Well.split(","), function(well)
                    {
                        if (wells.indexOf(well) == -1)
                            wells.push(well);
                    });

                    filename = data.rows[i].Name;
                    isTitration = data.rows[i].IsTitration;
                }
                
                Ext.get('replicate_group_wells').update(wells.join(", "));
                Ext.get('replicate_group_filename').update(filename);
                this.findById('reCalcDisplay').setVisible(isTitration);
            },
            scope: this
        });
    },

    queryForRunLevelExclusions: function()
    {
        // query to see if there are any run level exclusions for this RunId
        LABKEY.Query.selectRows({
            schemaName: this.protocolSchemaName,
            queryName: 'RunExclusion',
            filterArray: [LABKEY.Filter.create('RunId', this.runId)],
            columns: 'Analytes/Name',
            success: function(data)
            {
                if (data.rows.length == 1)
                    Ext.get('run_analyte_exclusions').update("The following analytes have been excluded at the assay level: <b>" + data.rows[0]['Analytes/Name'] + "</b>");
            },
            scope: this
        });
    },

    getExclusionPanelHeader: function()
    {
        // return an HTML table with the description and type and place holder divs for the file name and wells
        return "<table cellspacing='0' width='100%' style='border-collapse: collapse'>"
                    + "<tr><td class='labkey-exclusion-td-label'>File Name:</td><td class='labkey-exclusion-td-cell'><div id='replicate_group_filename'>...</div></td></tr>"
                    + "<tr><td class='labkey-exclusion-td-label'>Sample:</td><td class='labkey-exclusion-td-cell'>" + (this.description != null ? this.description : "") + "</td></tr>"
                    + "<tr><td class='labkey-exclusion-td-label'>Type:</td><td class='labkey-exclusion-td-cell'>" + this.type + "</td></tr>"
                    + "<tr><td class='labkey-exclusion-td-label'>Wells:</td><td class='labkey-exclusion-td-cell'><div id='replicate_group_wells'>...</div></td></tr>"
                    + "</table>";
    },

    insertUpdateExclusions: function()
    {
        // mask the window until the insert/update is complete (or if something goes wrong)
        var message = "Saving replicate group exclusion...";
        if (this.findById('reCalcDisplay').isVisible())
            message = "Saving replicate group exclusion and re-calculating curve...";
        this.mask(message);

        // generate a comma delim string of the analyte Ids to exclude
        var analytesForExclusion = this.findById('availableanalytes').getSelectionModel().getSelections();
        var analyteRowIds = "";
        var analyteNames = "";
        var sep = "";
        Ext.each(analytesForExclusion, function(record)
        {
            analyteRowIds += sep.trim() + record.data.RowId;
            analyteNames += sep + record.data.Name;
            sep = ", ";
        });

        // config of data to save for the given replicate group exclusion
        var config = {
            assayId: this.assayId,
            tableName: 'WellExclusion',
            runId: this.runId,
            commands: [{
                key: this.rowId ? this.rowId : undefined,
                dataId: this.dataId,
                description: this.description,
                type: this.type,
                analyteRowIds: (analyteRowIds != "" ? analyteRowIds : null),
                analyteNames: (analyteNames != "" ? analyteNames : null), // for logging purposes only
                comment: this.findById('comment').getValue()
            }]
        };

        // if we don't have an exclusion to delete or anything to insert/update, do nothing
        if (!config.commands[0].key && config.commands[0].analyteRowIds == null)
        {
            this.unmask();
            return;
        }

        if (config.commands[0].analyteRowIds == null)
        {
            // ask the user if they are sure they want to remove the exclusions before deleting
            config.commands[0].command = 'delete';
            this.confirmExclusionDeletion(config, 'Are you sure you want to remove all analyte exlusions for the selected replicate group?', 'replicate group');
        }
        else
        {
            config.commands[0].command = config.commands[0].key ? 'update' : 'insert';
            this.saveExclusions(config, 'replicate group');
        }
    }
});
