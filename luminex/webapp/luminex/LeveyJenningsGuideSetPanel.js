/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
        if (!config.controlName || config.controlName == "null")
            throw "You must specify a controlName!";
        if (!config.controlType || config.controlType == "null")
            throw "You must specify a controlType!";
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
            border: true,
            cls: 'extContainer',
            disabled: true,
            userCanUpdate: LABKEY.user.canUpdate
        });

        this.addEvents('currentGuideSetUpdated', 'exportPdfBtnClicked');

        LABKEY.LeveyJenningsGuideSetPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        var items = [];

        // add a display field listing the selected graph params
        this.paramsDisplayField = new Ext.form.DisplayField({
            hideLabel: true,
            value: "",
            style: "font-size:110%; font-weight:bold",
            width: 738,
            border: true
        });

        // add a button for exporting the PDF
        this.exportPdftButton = new Ext.Button({
            disabled: true,
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: "Export PDF of plots",
            handler: function() {
                this.fireEvent('exportPdfBtnClicked');
            },
            scope: this
        });

        items.push(new Ext.form.CompositeField({
            hideLabel: true,
            items: [this.paramsDisplayField, this.exportPdftButton]
        }));

        // add a display field listing the current guide set for the graph params
        this.guideSetDisplayField = new Ext.form.DisplayField({
            fieldLabel: "Current Guide Run Set",
            value: "",
            style: "background-color:#CCCCCC; padding:3px",
            width: 583,
            border: true
        });

        // add a button to edit a current guide set
        this.editGuideSetButton = new Ext.Button({
            disabled: true,
            text: "Edit",
            tooltip: "Edit current guide run set",
            handler: function() {
                this.manageGuideSetClicked(false);
            },
            scope: this
        });

        // add a button to create a new current guide set
        this.newGuideSetButton = new Ext.Button({
            disabled: true,
            text: "New",
            tooltip: "Create new guide run set",
            handler: this.newGuideSetClicked,
            scope: this
        });

        // add the guide set elements as a composite field for layout reasons
        this.guideSetCompositeField = new Ext.form.CompositeField({
            items: [this.guideSetDisplayField, this.editGuideSetButton, this.newGuideSetButton]
        });

        // if the user has permissions to update in this container, show them the Guide Set Edit/New buttons
        this.userCanUpdate ? items.push(this.guideSetCompositeField) : items.push(this.guideSetDisplayField);

        this.items = items;

        LABKEY.LeveyJenningsGuideSetPanel.superclass.initComponent.call(this);
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        this.enable();
        this.exportPdftButton.enable();

        // update the display field to show the selected params
        this.paramsDisplayField.setValue($h(this.analyte)
               + ' - ' + $h(this.isotype == '' ? "[None]" : this.isotype)
               + ' ' + $h(this.conjugate == '' ? "[None]" : this.conjugate));

        // update the guide set display field to say loading...
        this.guideSetDisplayField.setValue("Loading...");

        this.queryCurrentGuideSetInfo(false);
    },

    queryCurrentGuideSetInfo: function() {
        // query the server for the current guide set for the selected graph params
        LABKEY.Query.selectRows({
            schemaName: 'assay.Luminex.' + this.assayName,
            queryName:  'GuideSet',
            filterArray: [LABKEY.Filter.create('ControlName', this.controlName),
                    LABKEY.Filter.create('AnalyteName', this.analyte),
                    LABKEY.Filter.create('Isotype', this.isotype, (this.isotype == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                    LABKEY.Filter.create('Conjugate', this.conjugate, (this.conjugate == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                    LABKEY.Filter.create('CurrentGuideSet', true)],
            columns: 'RowId, Comment, Created',
            success: this.updateGuideSetDisplayField(),
            failure: function(response){
                this.guideSetDisplayField.setValue("Error: " + response.exception);
            },
            scope: this
        });
    },

    updateGuideSetDisplayField: function() {
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
            }

            this.guideSetCompositeField.doLayout();
        }
    },

    formatDate: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    manageGuideSetClicked: function(createNewGuideSet) {
        // create a pop-up window to display the manage guide set UI
        var win = new Ext.Window({
            layout:'fit',
            width:1150,
            height:650,
            closeAction:'close',
            modal: true,
            padding: 15,
            bodyStyle: 'background-color: white;',
            title: (createNewGuideSet ? 'Create' : 'Manage') + ' Guide Set...',
            items: [new LABKEY.ManageGuideSetPanel({
                cls: 'extContainer',
                disableId: createNewGuideSet ? this.currentGuideSetId : null,
                guideSetId: createNewGuideSet ? null : this.currentGuideSetId,
                assayName: this.assayName,
                controlName: this.controlName,
                controlType: this.controlType,
                analyte: this.analyte,
                isotype: this.isotype,
                conjugate: this.conjugate,
                networkExists: this.networkExists,
                protocolExists: this.protocolExists,
                listeners: {
                    scope: this,
                    'closeManageGuideSetPanel': function(saveResults) {
                        // if the panel was closed because of a successful save, we need to reload some stuff
                        if (saveResults)
                        {
                            for (var i = 0; i < saveResults.length; i++)
                            {
                                // if a change was made to the GuideSet table, it was the comment
                                if (saveResults[i].queryName == "GuideSet")
                                {
                                    this.queryCurrentGuideSetInfo(false);
                                }
                                // if a change was made to the list of runs in the current guide set, update accordingly 
                                else if (saveResults[i].queryName == "AnalyteTitration" || saveResults[i].queryName == "AnalyteSinglePointControl")
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
                title:'Confirmation...',
                msg: 'Creating a new guide set will set the current guide set to be inactive. Would you like to proceed?',
                buttons: Ext.Msg.YESNO,
                fn: function(btnId, text, opt){
                    if(btnId == 'yes'){
                        this.manageGuideSetClicked(true);
                    }
                },
                icon: Ext.MessageBox.QUESTION,
                scope: this
            });
        }
        else
        {
            this.manageGuideSetClicked(true);
        }
    },

    toggleExportBtn: function(toEnable) {
        this.exportPdftButton.setDisabled(!toEnable);
    }
});