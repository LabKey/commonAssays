/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
        if (!config.assayId || config.assayId == "null")
            throw "You must specify a assayId!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            padding: 10,
            items: [],
            header: false,
            bodyStyle: 'background-color:#EEEEEE',
            labelWidth: 150,
            width: 865,
            border: true,
            cls: 'extContainer',
            disabled: true,
            userCanUpdate: LABKEY.user.canUpdate
        });

        this.assayName = config.assayName;

        this.addEvents('currentGuideSetUpdated', 'exportPdfBtnClicked', 'guideSetMetricsUpdated');

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
            hideLabel: true,
            width: 583,
            border: true
        });

        // add a button to edit a current guide set
        this.editGuideSetButton = new Ext.Button({
            disabled: true,
            text: "Edit",
            tooltip: "Edit current guide set",
            handler: function() {
                this.manageGuideSetClicked(false);
            },
            scope: this
        });

        // add a button to create a new current guide set
        this.newGuideSetButton = new Ext.Button({
            disabled: true,
            text: "New",
            tooltip: "Create new guide set",
            handler: this.newGuideSetClicked,
            scope: this
        });

        this.guideSetDetailsButton = new Ext.Button({
            hidden: true,
            text: "Details",
            tooltip: "View guide set parameters and metric details",
            handler: this.viewParameterDetails,
            scope: this
        });

        this.guideSetLabelPanel = new Ext.Panel({
            width: 150,
            html: this.getRelatedGuideSetLabel()
        });

        // if the user has permissions to update in this container, show them the Guide Set Edit/New buttons
        var buttonItems = [];
        if (this.userCanUpdate)
        {
            buttonItems.push({
                xtype: 'compositefield',
                items:[this.editGuideSetButton, this.newGuideSetButton]
            });
        }
        buttonItems.push({
            padding: '5px 0 0 0',
            items: [this.guideSetDetailsButton]
        });

        // add the guide set elements as a composite field for layout reasons
        this.guideSetCompositeField = new Ext.form.CompositeField({
            hideLabel: true,
            defaults: {
                border: false,
                bodyStyle: 'background-color:#EEEEEE;'
            },
            items: [
                this.guideSetLabelPanel,
                this.guideSetDisplayField,
                {
                    xtype: 'panel',
                    height: 60,
                    layout:  'vbox',
                    defaults: {
                        xtype: 'panel',
                        border: false,
                        layout: 'hbox',
                        bodyStyle: 'background-color:#EEEEEE;',
                        padding: 2,
                        width: 200
                    },
                    items: buttonItems
                }
            ]
        });

        items.push(this.guideSetCompositeField);

        this.items = items;

        LABKEY.LeveyJenningsGuideSetPanel.superclass.initComponent.call(this);
    },

    getRelatedGuideSetLabel: function() {
        var html = '<div style="padding-top: 4px;">Current Guide Set:</div>';

        if (Ext.isDefined(this.currentGuideSetId))
        {
            var props = {
                rowId: this.assayId,
                'GuideSet.ControlType~eq': this.controlType,
                'GuideSet.ControlName~eq': this.controlName,
                'GuideSet.AnalyteName~eq': this.analyte
            };

            if (Ext.isDefined(this.isotype) && this.isotype == '') {
                props['GuideSet.Isotype~isblank'] = null;
            }
            else {
                props['GuideSet.Isotype~eq'] = this.isotype;
            }

            if (Ext.isDefined(this.conjugate) && this.conjugate == '') {
                props['GuideSet.Conjugate~isblank'] = null;
            }
            else {
                props['GuideSet.Conjugate~eq'] = this.conjugate;
            }

            html += '<div style="padding-top: 8px;" class="related-guidesets">'
                + LABKEY.Utils.textLink({text: 'All Related Guide Sets', href: LABKEY.ActionURL.buildURL('luminex', 'manageGuideSet', null, props)})
                + '</div>';
        }

        return html;
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        this.enable();

        // update the display field to show the selected params
        this.paramsDisplayField.setValue($h(this.analyte)
               + ' - ' + $h(this.isotype == '' ? "[None]" : this.isotype)
               + ' ' + $h(this.conjugate == '' ? "[None]" : this.conjugate));

        // update the guide set display field to say loading...
        this.guideSetDisplayField.setValue("Loading...");

        this.queryCurrentGuideSetInfo();
    },

    queryCurrentGuideSetInfo: function() {
        // query the server for the current guide set for the selected graph params
        LABKEY.Query.selectRows({
            schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
            queryName:  'GuideSet',
            filterArray: [
                    LABKEY.Filter.create('ControlType', this.controlType),
                    LABKEY.Filter.create('ControlName', this.controlName),
                    LABKEY.Filter.create('AnalyteName', this.analyte),
                    LABKEY.Filter.create('Isotype', this.isotype, (this.isotype == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                    LABKEY.Filter.create('Conjugate', this.conjugate, (this.conjugate == '' ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                    LABKEY.Filter.create('CurrentGuideSet', true)],
            columns: 'RowId, Comment, Created, ValueBased',
            success: this.updateGuideSetDisplayField,
            failure: function(response){
                this.guideSetDisplayField.setValue("Error: " + response.exception);
            },
            scope: this
        });
    },

    updateGuideSetDisplayField: function(data) {
        if (data.rows.length == 0)
        {
            this.guideSetDisplayField.setValue("<div class='guideset-no'>No current guide set for the selected graph parameters.</div>");

            // remove any reference to a current guide set and enable/disable buttons
            this.currentGuideSetId = undefined;
            this.editGuideSetButton.disable();
            this.newGuideSetButton.enable();
            this.guideSetDetailsButton.hide();
        }
        else
        {
            // there can only be one current guide set for any given set of graph params
            var row = data.rows[0];

            var html = '<table class="guideset-tbl" guide-set-id="' + row["RowId"] + '">'
                    + '<tr><td class="guideset-hdr">Created:</td><td width="200">' + this.formatDate(row["Created"]) + '</td>'
                    + '<td class="guideset-hdr">Type: </td><td>' + (row["ValueBased"] ? 'Value-based' : 'Run-based') + '</td></tr>'
                    + '<tr><td class="guideset-hdr">Comment:</td><td colspan="3">' + (row["Comment"] == null ? "&nbsp;" : $h(row["Comment"])) + '</td></tr>'
                    + '</table>';

            this.guideSetDisplayField.setValue(html);

            // store a reference to the current guide set and enable buttons
            this.currentGuideSetId = row["RowId"];
            this.editGuideSetButton.enable();
            this.newGuideSetButton.enable();
            this.guideSetDetailsButton.setVisible(true);
        }

        this.guideSetCompositeField.doLayout();

        // update the "all related guide sets" link
        this.guideSetLabelPanel.update(this.getRelatedGuideSetLabel());
    },

    formatDate: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    manageGuideSetClicked: function(createNewGuideSet) {
        // create a pop-up window to display the manage guide set UI
        var win = new Ext.Window({
            layout:'fit',
            width:1115,
            height:650,
            closeAction:'close',
            modal: true,
            cls : 'leveljenningsreport',
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
                            this.queryCurrentGuideSetInfo();
                            this.fireEvent('currentGuideSetUpdated');
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
                msg: 'Creating a new guide set will cause the current guide set to be uneditable. Would you like to proceed?',
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
    },

    viewParameterDetails: function() {
        if (this.currentGuideSetId)
        {
            Ext4.create('LABKEY.luminex.GuideSetWindow', {
                assayName: this.assayName,
                currentGuideSetId: this.currentGuideSetId,
                canEdit: this.userCanUpdate,
                listeners: {
                    scope: this,
                    aftersave: function() { this.fireEvent('guideSetMetricsUpdated'); }
                }
            });
        }
    }
});