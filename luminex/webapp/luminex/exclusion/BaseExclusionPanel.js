/*
 * Copyright (c) 2014-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

if (!LABKEY.Exclusions)
    LABKEY.Exclusions = {};

LABKEY.Exclusions.BasePanel = Ext.extend(Ext.Panel, {

    constructor : function(config)
    {
        // check that the config properties needed are present
        if (!config.protocolSchemaName)
            throw "You must specify a protocolSchemaName!";
        if (!config.assayId)
            throw "You must specify a assayId!";
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
        LABKEY.Exclusions.BasePanel.superclass.constructor.call(this, config);
    },

    queryExistingExclusions : function(queryName, filterArray, columns)
    {
        LABKEY.Query.selectRows({
            schemaName: this.protocolSchemaName,
            queryName: queryName,
            filterArray: filterArray,
            columns: columns,
            success: function(data)
            {
                this.exclusionsExist = false;
                if (data.rows.length >= 1)
                {
                    this.exclusionsExist = true;
                    this.handleExistingExclusions(data.rows);
                }
                this.setupWindowPanelItems();
            },
            scope: this
        });
    },

    // success handler for queryExistingExclusions, may be overridden if applications need to access
    // different data
    handleExistingExclusions : function(rows){

        // even if there are multiple exclusions for the data point, there should be a single
        // comment and a single set of analytes.

        var row = rows[0];
        if (row.hasOwnProperty("Comment"))
            this.comment = row["Comment"];
        if (row.hasOwnProperty("Analytes/RowId"))
            this.analytes = row["Analytes/RowId"];
    },

    setupWindowPanelItems: function()
    {
        // TO BE OVERRIDDEN
    },

    addHeaderPanel: function(descText)
    {
        this.add(new Ext.form.FormPanel({
            style: 'padding-bottom: 10px;',
            html: this.getExclusionPanelHeader(),
            timeout: Ext.Ajax.timeout,
            border: false
        }));

        // text to describe how exclusion interactions per exclusion type
        this.add(new Ext.form.DisplayField({
            hideLabel: true,
            style: 'font-style: italic; font-size: 90%',
            value: descText
        }));

    },

    addCommentPanel: function()
    {
        this.add(new Ext.form.FormPanel({
            height: 75,
            style: 'padding-top: 20px;',
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
                        keydown: function()
                        {
                            // enable the save changes button when the comment is edited by the user, if exclusions exist
                            if (this.exclusionsExist)
                                this.getFooterToolbar().findById('saveBtn').enable();
                        }
                    }
                })
            ],
            border: false
        }));
    },

    addStandardButtons: function()
    {
        this.addButton({
            id: 'saveBtn',
            text: 'Save',
            disabled: true,
            handler: this.insertUpdateExclusions,
            scope: this
        });
        this.addButton({
            text: 'Cancel',
            scope: this,
            handler: function()
            {
                this.fireEvent('closeWindow');
            }
        });
    },

    toggleSaveBtn : function(sm, grid)
    {
        // enable the save button when changes are made to the selection or is exclusions exist
        if (sm.getCount() > 0 || grid.exclusionsExist)
            grid.getFooterToolbar().findById('saveBtn').enable();

        // disable the save button if no exclusions exist and no selection is made
        if (sm.getCount() == 0 && !grid.exclusionsExist)
            grid.getFooterToolbar().findById('saveBtn').disable();
    },

    enableSaveBtn : function(){
        this.getFooterToolbar().findById('saveBtn').enable();
    },

    getGridCheckboxSelectionModel : function()
    {
        // checkbox selection model for selecting which analytes to exclude
        var selMod = new Ext.grid.CheckboxSelectionModel();
        selMod.on('selectionchange', function(sm)
        {
            this.toggleSaveBtn(sm, this);
        }, this, {buffer: 250});

        // Issue 17974: make rowselect behave like checkbox select, i.e. keep existing other selections in the grid
        selMod.on('beforerowselect', function(sm, rowIndex, keepExisting, record)
        {
            sm.suspendEvents();
            if (sm.isSelected(rowIndex))
                sm.deselectRow(rowIndex);
            else
                sm.selectRow(rowIndex, true);
            sm.resumeEvents();

            this.toggleSaveBtn(sm, this);

            return false;
        }, this);

        return selMod;
    },

    getExclusionPanelHeader: function()
    {
        // return an HTML table with the run Id and a place holder div for the assay Id
        return "<table cellspacing='0' width='100%' style='border-collapse: collapse'>"
                + "<tr><td class='labkey-exclusion-td-label'>Run ID:</td><td class='labkey-exclusion-td-cell'>" + this.runId + "</td></tr>"
                + "<tr><td class='labkey-exclusion-td-label'>Assay ID:</td><td class='labkey-exclusion-td-cell'><div id='run_assay_id'>...</div></td></tr>"
                + "</table>";
    },

    queryForRunAssayId: function()
    {
        // query to get the assay Id for the given run and put it into the panel header div
        LABKEY.Query.selectRows({
            schemaName: this.protocolSchemaName,
            queryName: 'Runs',
            filterArray: [LABKEY.Filter.create('RowId', this.runId)],
            columns: 'Name',
            success: function(data)
            {
                if (data.rows.length == 1)
                    Ext.get('run_assay_id').update(data.rows[0].Name);
            },
            scope: this
        });
    },

    confirmExclusionDeletion : function(config, msg, type)
    {
        Ext.Msg.show({
            width: 400,
            title:'Warning',
            msg: msg,
            icon: Ext.MessageBox.WARNING,
            buttons: Ext.Msg.YESNO,
            fn: function(btnId, text, opt)
            {
                if (btnId == 'yes')
                    this.saveExclusions(config, type);
                else
                    this.unmask();
            },
            scope: this
        });
    },

    saveExclusions : function(config, type)
    {
        if (!config.commands || !Ext.isArray(config.commands))
            Ext.Msg.alert('Error', 'SaveExclusion API expects an array on commands.');

        config.scope = this;

        config.success = function(response)
        {
            this.fireEvent('closeWindow');
            this.showJobQueuedSuccess(type, response.returnUrl);
        };

        config.failure = function(response)
        {
            Ext.Msg.alert('ERROR', response.exception);
            this.unmask();
        };

        LABKEY.Luminex.saveExclusion(config);
    },

    showJobQueuedSuccess : function(type, url)
    {
        Ext.Msg.show({
            width: 400,
            title:'Success',
            msg: 'The ' + type + ' exclusion job(s) has been added to the pipeline.'
                    + ' Would you like to go to the pipeline job status page now to review this exclusion?',
            icon: Ext.MessageBox.INFO,
            buttons: Ext.Msg.YESNO,
            fn: function(btnId)
            {
                if (btnId == 'yes')
                    window.location = url;
            },
            scope: this
        });
    },

    mask : function(message)
    {
        this.ownerCt.getEl().mask(message, "x-mask-loading");
    },

    unmask : function()
    {
        var windowEl = this.ownerCt.getEl();
        if (windowEl.isMasked())
            windowEl.unmask();
    }
});

LABKEY.Exclusions.BaseWindow = Ext.extend(Ext.Window, {
    cls: 'extContainer',
    layout:'fit',
    padding: 10,
    modal: true,
    closeAction:'close',
    bodyStyle: 'background-color: white;',
    draggable: false,
    resizable: false
});

LABKEY.Luminex = new function()
{
    return {
        /*
         * Save (insert/update/delete) a single Luminex exclusion of type replicate group (well), analyte, or titration.
         * @param {Object} config An object which contains the following configuration properties.
         * @param {String} [config.assayId] Required. Assay design RowId of the Luminex assay design
         * @param {String} [config.tableName] Required. Either WellExclusion (i.e. replicate group), TitrationExclusion, or RunExclusion (i.e. anaalyte)
         * @param {String} [config.runId] Required. RowId for the assay run
         * @param {Array}  [config.commands] Required. Array of exclusion commeands to be executed
         * @param {String} [config.commands[].command] Required. Either insert, update, or delete
         * @param {String} [config.commands[].key] If update or delete, the RowId key for the record to be modified
         * @param {String} [config.commands[].dataId] For well group or titration exclusions, the dataId for the run file
         * @param {String} [config.commands[].description] The "Description" column value (i.e. standard or sample name)
         * @param {String} [config.commands[].type] For well group exclusion, the "Type" column value (i.e. X1, S3)
         * @param {String} [config.commands[].analyteRowIds] A comma separated list of analyte RowIds
         * @param {String} [config.commands[].analyteNames] A comma separated list of analyte Names
         * @param {String} [config.commands[].comment] The comment to save with the exclusion
         * @param {Function} config.success Function called when the "saveExclusion" function executes successfully.
         * @param {Function} [config.failure] Function called if execution of the "saveExclusion" function fails.
         * a save.
         */
        saveExclusion : function(config)
        {
            var dataObject = {
                assayId: config.assayId,
                tableName: config.tableName,
                runId: config.runId,
                commands: config.commands
            };

            var requestConfig = {
                url : LABKEY.ActionURL.buildURL('luminex', 'saveExclusion.api'),
                method : 'POST',
                jsonData : dataObject,
                headers : { 'Content-Type' : 'application/json' },
                success: LABKEY.Utils.getCallbackWrapper(LABKEY.Utils.getOnSuccess(config), config.scope, false),
                failure: LABKEY.Utils.getCallbackWrapper(LABKEY.Utils.getOnFailure(config), config.scope, true)
            };

            return LABKEY.Ajax.request(requestConfig);
        }
    }
};