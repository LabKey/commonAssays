/*
 * Copyright (c) 2011-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of 'Exclude Analytes' button to open the run exclusion window
function openExclusionsAnalyteWindow(assayId, runId)
{
    // lookup the assay design information based on the Assay RowId
    LABKEY.Assay.getById({
        id: assayId,
        success: function(assay)
        {
            if (Ext.isArray(assay) && assay.length == 1)
            {
                var win = new LABKEY.Exclusions.BaseWindow({
                    title: 'Exclude Analytes from Analysis',
                    width: Ext.getBody().getViewSize().width < 500 ? Ext.getBody().getViewSize().width * .9 : 450,
                    height: Ext.getBody().getViewSize().height > 500 ? 460 : Ext.getBody().getViewSize().height * .75,
                    items: new LABKEY.Exclusions.AnalytePanel({
                        protocolSchemaName: assay[0].protocolSchemaName,
                        assayId: assayId,
                        runId: runId,
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
 * Class to display panel for selecting which analytes to exclude from a Luminex run
 * @params protocolSchemaName = the encoded protocol schema name to use (based on the assay design name)
 * @params assayId = the assay design RowId
 * @params runId = runId for the selected replicate group
 */
LABKEY.Exclusions.AnalytePanel = Ext.extend(LABKEY.Exclusions.BasePanel, {

    initComponent : function()
    {
        // query the RunExclusion table to see if there are any existing exclusions for this run
        this.queryExistingExclusions('RunExclusion', [LABKEY.Filter.create('runId', this.runId)], 'RunId,Comment,Analytes/RowId');

        LABKEY.Exclusions.AnalytePanel.superclass.initComponent.call(this);
    },

    setupWindowPanelItems: function()
    {
        this.addHeaderPanel('Analytes excluded for a well, replicate group, singlepoint unknown, or titration will not be re-included by changes in assay level exclusions.');

        var selMod = this.getGridCheckboxSelectionModel();

        // set the title for the grid panel based on previous exclusions
        var title = "Select the checkbox next to the analyte(s) to be excluded:";
        if (this.exclusionsExist)
        {
            title += "<BR/><span style='color:red;'>To remove an exclusion, uncheck the analyte(s).</span>";
        }

        // grid of avaialble/excluded analytes
        var availableAnalytesGrid = new Ext.grid.GridPanel({
            id: 'availableanalytes',
            style: 'padding-top: 10px;',
            title: title,
            headerStyle: 'font-weight: normal;',
            store:  new LABKEY.ext.Store({
                sql: "SELECT DISTINCT x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                    + " FROM Data AS x WHERE x.Data.Run.RowId = " + this.runId
                    + " ORDER BY x.Analyte.Name",
                schemaName: this.protocolSchemaName,
                autoLoad: true,
                listeners: {
                    scope: this,
                    load: function(store, records, options)
                    {
                        if (this.analytes)
                        {
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

        this.addCommentPanel();

        this.addStandardButtons();

        this.doLayout();

        this.queryForRunAssayId();
    },

    insertUpdateExclusions: function()
    {
        this.mask("Saving analyte exclusions...");

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

        // config of data to save for the given analyte exclusion
        var config = {
            assayId: this.assayId,
            tableName: 'RunExclusion',
            runId: this.runId,
            commands: [{
                key: this.runId,
                analyteRowIds: (analyteRowIds != "" ? analyteRowIds : null),
                analyteNames: (analyteNames != "" ? analyteNames : null), // for logging purposes only
                comment: this.findById('comment').getValue()
            }]
        };

        // if we don't have an exclusion to delete or anything to insert/update, do nothing
        if (!this.exclusionsExist && config.commands[0].analyteRowIds == null)
        {
            this.unmask();
            return;
        }

        if (config.commands[0].analyteRowIds == null)
        {
            // ask the user if they are sure they want to remove the exclusions before deleting
            config.commands[0].command = 'delete';
            this.confirmExclusionDeletion(config, 'Are you sure you want to remove all analyte exlusions for run Id ' + this.runId + '?', 'analyte');
        }
        else
        {
            config.commands[0].command = this.exclusionsExist ? 'update' : 'insert';
            this.saveExclusions(config, 'analyte');
        }
    }
});