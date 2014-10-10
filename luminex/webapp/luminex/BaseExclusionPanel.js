
Ext.namespace('LABKEY');

LABKEY.BaseExclusionPanel = Ext.extend(Ext.Panel, {

    constructor : function(config){
        // check that the config properties needed are present
        if (!config.assayName)
            throw "You must specify a assayName!";
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
        LABKEY.BaseExclusionPanel.superclass.constructor.call(this, config);
    },

    queryExistingExclusions : function(queryName, filterArray, columns) {
        LABKEY.Query.selectRows({
            schemaName: 'assay.Luminex.' + this.assayName,
            queryName: queryName,
            filterArray: filterArray,
            columns: columns,
            success: function(data){
                this.exclusionsExist = false;
                if (data.rows.length == 1)
                {
                    this.exclusionsExist = true;

                    var row = data.rows[0];
                    if (row.hasOwnProperty("RowId"))
                        this.rowId = row["RowId"];
                    if (row.hasOwnProperty("Comment"))
                        this.comment = row["Comment"];
                    if (row.hasOwnProperty("Analytes/RowId"))
                        this.analytes = row["Analytes/RowId"];
                }

                this.setupWindowPanelItems();
            },
            scope: this
        });
    },

    setupWindowPanelItems: function() {
        // TO BE OVERRIDDEN
    },

    addHeaderPanel: function(descText) {
        this.add(new Ext.form.FormPanel({
            style: 'padding-bottom: 10px; background: #ffffff',
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

    addCommentPanel: function() {
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
    },

    addStandardButtons: function() {
        this.addButton({
            id: 'saveBtn',
            text: 'Save',
            disabled: true,
            handler: this.insertUpdateExclusions,
            scope: this
        });
        this.addButton({
            text: 'Cancel',
            handler: function(){this.fireEvent('closeWindow');},
            scope: this
        });
    },

    toggleSaveBtn : function(sm, grid){
        // enable the save button when changes are made to the selection or is exclusions exist
        if (sm.getCount() > 0 || grid.exclusionsExist)
            grid.getFooterToolbar().findById('saveBtn').enable();

        // disable the save button if no exclusions exist and no selection is made
        if(sm.getCount() == 0 && !grid.exclusionsExist)
            grid.getFooterToolbar().findById('saveBtn').disable();
    },

    getGridCheckboxSelectionModel : function() {
        // checkbox selection model for selecting which analytes to exclude
        var selMod = new Ext.grid.CheckboxSelectionModel();
        selMod.on('selectionchange', function(sm){
            this.toggleSaveBtn(sm, this);
        }, this, {buffer: 250});

        // Issue 17974: make rowselect behave like checkbox select, i.e. keep existing other selections in the grid
        selMod.on('beforerowselect', function(sm, rowIndex, keepExisting, record) {
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
            schemaName: 'assay.Luminex.' + this.assayName,
            queryName: 'Runs',
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
    }
});