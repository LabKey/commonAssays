/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');
var $h = Ext.util.Format.htmlEncode;

/**
* User: cnathe
* Date: Sept 20, 2011
*/

/**
 * Class to create a small panel for displaying the current guide set info for the selected graph parameters
 *   and to give the user access to the edit guide set and create new guide set buttons
 *
 * @params titration
 * @params assayName
 */
LABKEY.LeveyJenningsGuideSetPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.titration || config.titration == "null")
            throw "You must specify a titration!";
        if (!config.assayName || config.assayName == "null")
            throw "You must specify a assayName!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            padding: 10,
            items: [],
            header: false,
            bodyStyle: 'background-color:#EEEEEE',
            labelWidth: 150,
            width: 850,
            height: 70,
            border: true,
            cls: 'extContainer',
            disabled: true
        });

        this.addEvents('currentGuideSetUpdated');

        LABKEY.LeveyJenningsGuideSetPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        var items = [];

        // add a display field listing the selected graph params
        this.paramsDisplayField = new Ext.form.DisplayField({
            hideLabel: true,
            value: "",
            style: "background-color:white; font-weight:bold",
            width: 725,
            height: 20,
            border: true
        });
        items.push(this.paramsDisplayField);

        // add a display field listing the current guide set for the graph params
        this.guideSetDisplayField = new Ext.form.DisplayField({
            fieldLabel: "Current Guide Run Set",
            value: "",
            width: 570,
            height: 20,
            border: true
        });

        // add a button to edit a current guide set
        this.editGuideSetButton = new Ext.Button({
            disabled: true,
            text: "Edit",
            handler: this.editCurrentGuideSetClicked,
            scope: this
        });

        // add a button to create a new current guide set
        this.newGuideSetButton = new Ext.Button({
            disabled: true,
            text: "New",
            handler: this.newGuideSetClicked,
            scope: this
        });

        // add the guide set elements as a composite field for layout reasons
        this.guideSetCompositeField = new Ext.form.CompositeField({
            labelStyle: "font-weight:bold",
            items: [this.guideSetDisplayField, this.editGuideSetButton, this.newGuideSetButton]
        });
        items.push(this.guideSetCompositeField);

        this.items = items;

        LABKEY.LeveyJenningsGuideSetPanel.superclass.initComponent.call(this);
    },

    // function called by the JSP when the graph params are selected and the "Reset Graph" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        this.enable();

        // update the display field to show the selected params
        this.paramsDisplayField.setValue($h(this.analyte) + ' - ' + $h(this.isotype) + ' ' + $h(this.conjugate));

        // update the guide set display field to say loading...
        this.guideSetDisplayField.setValue("Loading...");

        this.queryCurrentGuideSetInfo(false);
    },

    queryCurrentGuideSetInfo: function(clickEditButton) {
        // query the server for the current guide set for the selected graph params
        LABKEY.Query.selectRows({
            schemaName: 'assay',
            queryName: this.assayName + ' GuideSet',
            filterArray: [LABKEY.Filter.create('TitrationName', this.titration),
                    LABKEY.Filter.create('AnalyteName', this.analyte),
                    LABKEY.Filter.create('Isotype', this.isotype),
                    LABKEY.Filter.create('Conjugate', this.conjugate),
                    LABKEY.Filter.create('CurrentGuideSet', true)],
            columns: 'RowId, Comment, Created',
            success: this.updateGuideSetDisplayField(clickEditButton),
            scope: this
        });
    },

    updateGuideSetDisplayField: function(clickEditButton) {
        return function(data) {
            if (data.rows.length == 0)
            {
                this.guideSetDisplayField.setValue("No current guide set for the selected graph parameters");

                // remove any reference to a current guide set and enable/disable buttons
                this.currentGuideSetId = undefined;
                this.editGuideSetButton.disable();
                this.newGuideSetButton.enable();
            }
            else
            {
                // there can only be one current guide set for any given set of graph params
                var row = data.rows[0];
                this.guideSetDisplayField.setValue('Created: ' + this.formatDate(row["Created"])
                        + '; Comment: ' + (row["Comment"] == null ? "&nbsp;" : $h(row["Comment"])));

                // store a reference to the current guide set and enable buttons
                this.currentGuideSetId = row["RowId"];
                this.editGuideSetButton.enable();
                this.newGuideSetButton.enable();

                // if this function is being called after a new current guide set is created we need to
                // click the edit button to open the manage guide set window
                if (clickEditButton)
                    this.editCurrentGuideSetClicked();
            }
        }
    },

    formatDate: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    editCurrentGuideSetClicked: function() {
        // create a pop-up window to display the manage guide set UI
        var win = new Ext.Window({
            layout:'fit',
            width:1075,
            height:650,
            closeAction:'close',
            modal: true,
            padding: 15,
            
            title: 'Manage Guide Set',
            items: [new LABKEY.ManageGuideSetPanel({
                cls: 'extContainer',
                guideSetId: this.currentGuideSetId,
                assayName: this.assayName,
                listeners: {
                    scope: this,
                    'closeManageGuideSetPanel': function(saveResults) {
                        // if the panel was closed because of a successful save, we need to reload some stuff
                        if (saveResults)
                        {
                            for (var i = 0; i < saveResults.length; i++)
                            {
                                // if a change was made to the GuideSet table, it was the comment
                                if (saveResults[i].queryName == this.assayName + " GuideSet")
                                {
                                    this.queryCurrentGuideSetInfo(false);
                                }
                                // if a change was made to the list of runs in the current guide set, update accordingly 
                                else if (saveResults[i].queryName == this.assayName + " AnalyteTitration")
                                {
                                    this.fireEvent('currentGuideSetUpdated');
                                }
                            }
                        }

                        win.close();
                    }
                }
            })]
        });
        win.show(this);
    },

    newGuideSetClicked: function() {
        // confirm with the user that they want to de-activate the current guide set in favor of a new one
        if (this.currentGuideSetId)
        {
           Ext.Msg.show({
                title:'Confirmation',
                msg: 'Creating a new guide set will set the current guide set to be inactive. Would you like to proceed?',
                buttons: Ext.Msg.YESNO,
                fn: function(btnId, text, opt){
                    if(btnId == 'yes'){
                        this.disableCurrentGuideSet();
                    }
                },
                icon: Ext.MessageBox.WARNING,
                scope: this
            });
        }
        else
        {
            this.createNewGuideSet();
        }
    },

    disableCurrentGuideSet: function() {
        LABKEY.Query.updateRows({
            schemaName: 'assay',
            queryName: this.assayName + ' GuideSet',
            rows: [{RowId: this.currentGuideSetId, CurrentGuideSet: false}],
            success: this.createNewGuideSet,
            scope: this
        });
    },

    createNewGuideSet: function() {
        LABKEY.Query.insertRows({
            schemaName: 'assay',
            queryName: this.assayName + ' GuideSet',
            rows: [{
                TitrationName: this.titration,
                AnalyteName: this.analyte,
                Isotype: this.isotype,
                Conjugate: this.conjugate,
                CurrentGuideSet: true
            }],
            success: function(data) {
                this.queryCurrentGuideSetInfo(true);
            },
            scope: this
        });
    }
});