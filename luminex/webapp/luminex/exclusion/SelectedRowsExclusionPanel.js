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
                    assayId: assayId,
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
 * @params removeMode = true to remove the selected rows' exclusions, false to exclude them
 * @params runs = the proposed exclusions grouped by run, from luminex-getSelectedRowsExclusions.api
 * @params skippedRowCount = the number of selected rows that will not change
 */
LABKEY.Exclusions.SelectedRowsPanel = Ext.extend(LABKEY.Exclusions.BasePanel, {

    singleRun: false,

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
            : 'The analytes of the selected rows will be excluded for these wells. A row in a replicate group with summary rows excludes the whole group.';
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

        this.add(new Ext.BoxComponent({
            id: 'selectedrowsexclusions',
            autoEl: {
                tag: 'div',
                style: 'max-height: 250px; overflow-y: auto; padding: 1px;',
                html: this.getTableHtml()
            }
        }));

        this.addCommentPanel();
        this.addStandardButtons();
    },

    getTableHtml : function()
    {
        var headers = ['Run', 'Data File', 'Description', 'Type', 'Well(s)', this.removeMode ? 'Analytes to Remove' : 'Analytes to Exclude', 'Exclusion Type'];
        var html = '<table class="labkey-data-region-legacy labkey-show-borders" style="width: 100%;"><tr>';
        Ext.each(headers, function(header)
        {
            html += '<td class="labkey-column-header">' + Ext.util.Format.htmlEncode(header) + '</td>';
        });
        html += '</tr>';

        Ext.each(this.getTableRecords(), function(record, index)
        {
            html += '<tr class="' + (index % 2 === 0 ? 'labkey-alternate-row' : 'labkey-row') + '">';
            Ext.each([record.runName, record.dataFileName, record.description, record.type, record.wells, record.analyteNames, record.exclusionType], function(value)
            {
                html += '<td style="vertical-align: top;">' + this.getCellHtml(value) + '</td>';
            }, this);
            html += '</tr>';
        }, this);

        return html + '</table>';
    },

    // An array value, such as the analyte names, gets one line per item
    getCellHtml : function(value)
    {
        if (value == null)
            return '';
        if (Ext.isArray(value))
            return value.map(function(item) { return Ext.util.Format.htmlEncode(item); }).join('<br>');
        return Ext.util.Format.htmlEncode(value);
    },

    getTableRecords : function()
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
                    wells: exclusion.well != null ? exclusion.well : exclusion.groupWells.join(','),
                    analyteNames: exclusion.changedAnalyteNames,
                    exclusionType: this.getExclusionTypeText(exclusion.command)
                });
            }, this);
        }, this);
        return records;
    },

    getExclusionTypeText : function(command)
    {
        if (command === 'insert')
            return 'Add';
        if (command === 'delete')
            return 'Remove';
        return 'Update';
    },

    // A blank comment keeps an updated exclusion's own comment, since an update rewrites it
    getCommandComment : function(exclusion, comment)
    {
        if (comment)
            return comment;
        if (exclusion.command === 'delete')
            return 'Deleting exclusion';
        if (exclusion.command === 'update')
            return exclusion.existingComment;
        return comment;
    },

    insertUpdateExclusions: function()
    {
        this.mask(this.removeMode ? 'Removing exclusions...' : 'Saving exclusions...');

        var comment = this.findById('comment').getValue();
        var runs = this.runs.map(function(run)
        {
            return {
                runId: run.runId,
                commands: run.exclusions.map(function(exclusion)
                {
                    return {
                        command: exclusion.command,
                        key: exclusion.key != null ? exclusion.key : undefined, // the save form can't bind a null key
                        dataId: exclusion.dataId,
                        description: exclusion.description,
                        type: exclusion.type,
                        well: exclusion.well,
                        analyteRowIds: exclusion.analyteRowIds,
                        analyteNames: exclusion.analyteNames, // for logging purposes only
                        comment: this.getCommandComment(exclusion, comment)
                    };
                }, this)
            };
        }, this);

        this.saveExclusions({
            assayId: this.assayId,
            tableName: 'WellExclusion',
            runs: runs
        }, 'well');
    }
});
