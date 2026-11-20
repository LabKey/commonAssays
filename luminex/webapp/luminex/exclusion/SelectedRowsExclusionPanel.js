/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of the 'Exclude Selected Rows' and 'Remove Selected Exclusions' menu items
function openExclusionsSelectedRowsWindow(assayId, dataRegionName, remove)
{
    var dataRegion = LABKEY.DataRegions[dataRegionName];
    if (!dataRegion)
    {
        Ext.Msg.alert('ERROR', 'Unable to find the results grid: ' + dataRegionName);
        return;
    }

    var title = remove ? 'Remove Selected Exclusions' : 'Exclude Selected Rows from Analysis';
    var loadingMsg = Ext.Msg.wait('Loading the selected rows...', title);
    LABKEY.Ajax.request({
        url: LABKEY.ActionURL.buildURL('luminex', 'getSelectedRowsExclusions.api', null, {
            assayId: assayId,
            dataRegionSelectionKey: dataRegion.selectionKey,
            remove: remove
        }),
        method: 'GET',
        success: LABKEY.Utils.getCallbackWrapper(function(response)
        {
            loadingMsg.hide();
            var win = new LABKEY.Exclusions.BaseWindow({
                title: title,
                width: Math.max(800, Ext.getBody().getViewSize().width * .6),
                height: Math.max(500, Ext.getBody().getViewSize().height * .6),
                items: new LABKEY.Exclusions.SelectedRowsPanel({
                    // the base panel requires these; the selection's runs come from the response
                    protocolSchemaName: 'n/a',
                    runId: response.runs[0].runId,
                    assayId: assayId,
                    dataRegionName: dataRegionName,
                    removeMode: remove,
                    runs: response.runs,
                    skippedRowCount: response.skippedRowCount,
                    listeners: {
                        closeWindow: function()
                        {
                            win.close();
                        }
                    }
                })
            });
            win.show();
        }),
        failure: LABKEY.Utils.getCallbackWrapper(function(response)
        {
            loadingMsg.hide();
            Ext.Msg.show({
                title: title,
                msg: Ext.util.Format.htmlEncode(response.exception),
                buttons: Ext.Msg.OK,
                minWidth: 300
            });
        }, this, true)
    });
}

/**
 * Class to display and save the well and replicate group exclusions proposed for the rows selected in a results grid
 * @params assayId = the assay design RowId
 * @params dataRegionName = the results grid whose selection is used
 * @params removeMode = true to remove the selected rows' exclusions, false to exclude them
 * @params runs = the proposed exclusions grouped by run, from luminex-getSelectedRowsExclusions.api
 * @params skippedRowCount = the number of selected rows that will not change
 */
LABKEY.Exclusions.SelectedRowsPanel = Ext.extend(LABKEY.Exclusions.BasePanel, {

    initComponent : function()
    {
        LABKEY.Exclusions.SelectedRowsPanel.superclass.initComponent.call(this);

        this.setupWindowPanelItems();
        this.on('afterrender', this.enableSaveBtn, this);
    },

    setupWindowPanelItems: function()
    {
        var descText = this.removeMode
            ? 'The analytes of the selected rows will be removed from these well and replicate group exclusions. An exclusion with no analytes left is removed.'
            : 'The analytes of the selected rows will be excluded for these wells. A selected summary row excludes its whole replicate group.';
        this.add(new Ext.form.DisplayField({
            hideLabel: true,
            style: 'padding-bottom: 10px;',
            value: descText
        }));

        this.add(new Ext.form.DisplayField({
            hideLabel: true,
            style: 'font-style: italic; font-size: 90%; padding-bottom: 10px;',
            value: 'Analytes excluded for a singlepoint unknown, titration, or at the assay level will not be re-included by changes in well or replicate group exclusions.'
        }));

        if (this.skippedRowCount > 0)
        {
            this.add(new Ext.form.DisplayField({
                hideLabel: true,
                style: 'font-size: 90%; padding-bottom: 10px;',
                value: Ext.util.Format.htmlEncode(this.skippedRowCount + ' of the selected rows '
                    + (this.removeMode ? 'have no well or replicate group exclusion to remove.' : 'are not part of a replicate group and will be skipped.'))
            }));
        }

        this.add(new Ext.grid.GridPanel({
            id: 'selectedrowsexclusions',
            store: new Ext.data.JsonStore({
                fields: ['runName', 'dataFileName', 'description', 'type', 'wells', 'analyteNames', 'action'],
                data: this.getGridRecords()
            }),
            colModel: new Ext.grid.ColumnModel({
                defaults: {sortable: false, menuDisabled: true, renderer: Ext.util.Format.htmlEncode},
                columns: [
                    {header: 'Run', dataIndex: 'runName'},
                    {header: 'Data File', dataIndex: 'dataFileName'},
                    {header: 'Description', dataIndex: 'description'},
                    {header: 'Type', dataIndex: 'type', width: 50},
                    {header: 'Well(s)', dataIndex: 'wells', width: 70},
                    {header: this.removeMode ? 'Analytes to Remove' : 'Analytes to Exclude', dataIndex: 'analyteNames'},
                    {header: 'Action', dataIndex: 'action', width: 60}
                ]
            }),
            viewConfig: {
                forceFit: true
            },
            anchor: '100%',
            height: 250,
            frame: false
        }));

        this.addCommentPanel();
        this.addStandardButtons();
    },

    getGridRecords : function()
    {
        var records = [];
        Ext.each(this.runs, function(run)
        {
            Ext.each(run.exclusions, function(exclusion)
            {
                records.push({
                    runName: run.runName,
                    dataFileName: exclusion.dataFileName,
                    description: exclusion.description,
                    type: exclusion.type,
                    wells: exclusion.well != null ? exclusion.well : 'Replicate Group',
                    analyteNames: exclusion.changedAnalyteNames.split(',').join(', '),
                    action: this.getActionText(exclusion.command)
                });
            }, this);
        }, this);
        return records;
    },

    getActionText : function(command)
    {
        if (command === 'insert')
            return 'Insert';
        if (command === 'delete')
            return 'Remove';
        return 'Update';
    },

    insertUpdateExclusions: function()
    {
        this.mask(this.removeMode ? 'Removing exclusions...' : 'Saving exclusions...');

        var comment = this.findById('comment').getValue();
        var run = this.runs[0];
        var commands = [];
        Ext.each(run.exclusions, function(exclusion)
        {
            commands.push({
                command: exclusion.command,
                key: exclusion.key != null ? exclusion.key : undefined, // the save form can't bind a null key
                dataId: exclusion.dataId,
                description: exclusion.description,
                type: exclusion.type,
                well: exclusion.well,
                analyteRowIds: exclusion.analyteRowIds,
                analyteNames: exclusion.analyteNames, // for logging purposes only
                comment: !comment && exclusion.command === 'delete' ? 'Deleting exclusion' : comment
            });
        });

        this.saveExclusions({
            assayId: this.assayId,
            tableName: 'WellExclusion',
            runId: run.runId,
            commands: commands
        }, 'well');
    },

    showJobQueuedSuccess : function(type, url)
    {
        LABKEY.DataRegions[this.dataRegionName].clearSelected();
        LABKEY.Exclusions.SelectedRowsPanel.superclass.showJobQueuedSuccess.call(this, type, url);
    }
});
