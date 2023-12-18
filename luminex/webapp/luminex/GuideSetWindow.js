/*
 * Copyright (c) 2014-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// TODO: it was a bad move to go with XTemplate here. This should be replaced with a full ExtJS implementation whenever possible.
// issues include styling, dirtying the form, and a few other things

Ext4.define('LABKEY.luminex.GuideSetWindow', {

    extend: 'Ext.window.Window',
    title: 'Guide Set Parameter Details',
    modal: true,
    border: false,
    width: 550,
    minHeight: 200,
    autoScroll: true,
    resizable: false,

    // NOTE: these are required fields (any way to enforce?)
    assayName: null,
    currentGuideSetId: null,
    canEdit: false,

    // These values will be set in the constructor via the LABKEY.Query.selectDistinctRows call
    has4PLCurveFit: false,
    has5PLCurveFit: false,

    statics: {
        viewTpl: new Ext4.XTemplate(
            '<style>table.gsDetails th {font-weight:bold;padding:3px;vertical-align: top;}</style>',
            '<tpl for=".">',
            '<form id="GuideSetForm">',
            '<table width="100%" class="gsDetails" style="margin-bottom:10px;">',
                '<tr><th width="120px">Guide Set Id:</th><td>{RowId}</td>',
                    '<th width="120px">Type:</th>',
                    '<tpl if="ValueBased &gt; 0">',
                    '<td>Value-based</td>',
                    '<tpl else>',
                    '<td>Run-based</td>',
                    '</tpl>',
                '</tr>',
                '<tr><th>Control Name:</th><td>{ControlName:htmlEncode}</td>',
                    '<th>Created:</th><td>{[this.dateRenderer(values.Created)]}</td></tr>',
                '<tr><th>Control Type:</th><td>{ControlType:htmlEncode}</td>',
                    '<th>Isotype:</th><td>{[this.formatNone(values.Isotype)]}</td></tr>',
                '<tr><th>Analyte:</th><td>{AnalyteName:htmlEncode}</td>',
                    '<th>Conjugate:</th><td>{[this.formatNone(values.Conjugate)]}</td></tr>',
                '<tr><th>Comment:</th><td colspan="3">{Comment:htmlEncode}</td></tr>',
            '</table>',
            '<div style="font-weight:bold;font-size:14px;padding:3px;">Guide Set Metrics</div>',
            '<table width="100%" class="labkey-data-region-legacy labkey-show-borders gsMetricDetails">',
            '<tr>',
                '<td class="labkey-column-header">Metric</td>',
                '<td class="labkey-column-header">Mean</td>',
                '<td class="labkey-column-header">Std Dev</td>',
                '<tpl if="ValueBased &lt; 1">',
                    '<td class="labkey-column-header"># Runs</td>',
                '<td class="labkey-column-header">Use for QC</td>',
                '</tpl>',
            '</tr>',
            '<tpl if="ControlType ==\'Titration\'">',
                '<tpl if="has4PLCurveFit == true">',
                    '<tr class="labkey-alternate-row">',
                        '<td>EC50 4PL</td>',
                        '<td align="right">{[this.formatNumber(values.EC504PLAverage)]}</td>',
                        '<td align="right">{[this.formatNumber(values.EC504PLStdDev)]}</td>',
                        '<tpl if="ValueBased &lt; 1">',
                        '<td align="right">{EC504PLRunCount}</td>',
                        '<td align="center"><input type="checkbox" name="EC504PLCheckBox" class="lk-check-dirty" {[this.initCheckbox(values.EC504PLEnabled, values.UserCanEdit)]}></td>',
                        '</tpl>',
                    '</tr>',
                '</tpl>',
                '<tpl if="has5PLCurveFit == true">',
                    '<tr class="labkey-row">',
                        '<td>EC50 5PL</td>',
                        '<td align="right">{[this.formatNumber(values.EC505PLAverage)]}</td>',
                        '<td align="right">{[this.formatNumber(values.EC505PLStdDev)]}</td>',
                        '<tpl if="ValueBased &lt; 1">',
                        '<td align="right">{EC505PLRunCount}</td>',
                        '<td align="center"><input type="checkbox" name="EC505PLCheckBox" class="lk-check-dirty" {[this.initCheckbox(values.EC505PLEnabled, values.UserCanEdit)]}></td>',
                    '</tpl>',
                    '</tr>',
                '</tpl>',
            '</tpl>',
            '<tr class="labkey-alternate-row">',
                '<td>MFI</td>',
                '<td align="right">{[this.formatNumber(values.MaxFIAverage)]}</td>',
                '<td align="right">{[this.formatNumber(values.MaxFIStdDev)]}</td>',
                '<tpl if="ValueBased &lt; 1">',
                '<td align="right">{MaxFIRunCount}</td>',
                '<td align="center"><input type="checkbox" name="MFICheckBox" class="lk-check-dirty" {[this.initCheckbox(values.MaxFIEnabled, values.UserCanEdit)]}></td>',
                '</tpl>',
            '</tr>',
            '<tpl if="ControlType ==\'Titration\'">',
                '<tr class="labkey-row">',
                    '<td>AUC</td>',
                    '<td align="right">{[this.formatNumber(values.AUCAverage)]}</td>',
                    '<td align="right">{[this.formatNumber(values.AUCStdDev)]}</td>',
                    '<tpl if="ValueBased &lt; 1">',
                    '<td align="right">{AUCRunCount}</td>',
                    '<td align="center"><input type="checkbox" name="AUCCheckBox" class="lk-check-dirty" {[this.initCheckbox(values.AUCEnabled, values.UserCanEdit)]}></td>',
                    '</tpl>',
                '</tr>',
            '</tpl>',
            '</table>',
            '</form>',
            '</tpl>',
            {
                formatNumber: function(value) { return value != null ? value.toFixed(3) : "N/A"; },
                formatNone: function(value) { return value ? Ext4.util.Format.htmlEncode(value) : '[None]'; },
                initCheckbox: function(check, editable)
                {
                    // set the checked and disabled state of the checkbox
                    var props = '';
                    if (check) {
                        props += 'checked';
                    }
                    if (!editable) {
                        props += ' disabled';
                    }
                    return props;
                },
                dateRenderer: function(val)
                {
                    return val ? Ext4.Date.format(new Date(val), LABKEY.extDefaultDateFormat) : null;
                }
            }
        )
    },

    initComponent: function ()
    {
        this.items = [{
            xtype: 'dataview',
            tpl: LABKEY.luminex.GuideSetWindow.viewTpl,
            padding: 10,
            store: this.getGuideSetStore(),
            listeners: {
                // "viewready" event fired too early; inputs were not found in the DOM at that point. So use "refresh".
                // This event is invoked multiple times, but it's fine if handlers get attached more than once.
                refresh: function() {
                    LABKEY.Utils.attachEventHandlerForQuerySelector("INPUT.lk-check-dirty", "change", function()
                    {
                        checkGuideSetWindowDirty();
                    });
                }
            }
        }];

        this.buttons = [{
            text: this.canEdit ? 'Cancel' : 'Close',
            scope: this,
            handler: function() { this.close(); }
        },{
            id: 'GuideSetSaveButton',
            disabled: true,
            text: 'Save',
            hidden: !this.canEdit,
            scope: this,
            handler: function(btn) {
                this.getEl().mask("Saving QC flag changes...");

                var form = document.forms['GuideSetForm'];
                LABKEY.Query.updateRows({
                    schemaName: 'assay.Luminex.'+LABKEY.QueryKey.encodePart(this.assayName),
                    queryName: 'GuideSet',
                    rows: [{
                        rowId: this.currentGuideSetId,
                        ec504plEnabled: form.elements['EC504PLCheckBox'] ? form.elements['EC504PLCheckBox'].checked : false,
                        ec505plEnabled: form.elements['EC505PLCheckBox'] ? form.elements['EC505PLCheckBox'].checked : false,
                        MaxFIEnabled: form.elements['MFICheckBox'].checked,
                        aucEnabled: form.elements['AUCCheckBox'] ? form.elements['AUCCheckBox'].checked : false
                    }],
                    scope: this,
                    success: function() {
                        this.getEl().unmask();
                        this.fireEvent('aftersave');
                        this.close();
                    },
                    failure: function(response) {
                        Ext4.Msg.alert("Error", response.exception);
                        this.getEl().unmask();
                    }
                });
            }
        }];

        this.callParent();
    },

    constructor: function(config) {
        this.currentGuideSetId = config['currentGuideSetId'];
        this.assayName = config['assayName'];
        this.addEvents('aftersave');
        this.callParent([config]);

        // Query to see if 4PL and/or 5PL curve fit data exists for this assay
        LABKEY.Query.selectDistinctRows({
            schemaName: 'assay.Luminex.'+LABKEY.QueryKey.encodePart(this.assayName),
            queryName: 'CurveFit',
            column: 'CurveType',
            scope: this,
            success: function(results){
                var curveTypes = results.values;
                this.has4PLCurveFit = curveTypes.indexOf('Four Parameter') > -1;
                this.has5PLCurveFit = curveTypes.indexOf('Five Parameter') > -1;

                // wait till after constructed so that currentGuideSetId is set and assayName
                this.getGuideSetStore().load();
                this.show();
            }
        });
    },

    // NOTE: consider putting store/model into separate file...
    getGuideSetStore: function() {
        if(!this.guideSetStore)
        {
            Ext4.define('Luminex.model.GuideSet', {

                extend : 'Ext.data.Model',

                fields : [
                    {name: 'RowId', type: 'int'},
                    {name: 'ControlName'},
                    {name: 'AnalyteName'},
                    {name: 'Conjugate'},
                    {name: 'Isotype'},
                    {name: 'Comment'},
                    {name: 'Created'},
                    {name: 'ValueBased', type: 'boolean'},
                    {name: 'EC504PLEnabled', type: 'boolean'},
                    {name: 'EC505PLEnabled', type: 'boolean'},
                    {name: 'AUCEnabled', type: 'boolean'},
                    {name: 'MaxFIEnabled', type: 'boolean'},
                    {name: 'EC504PLAverage'},
                    {name: 'EC504PLStdDev'},
                    {name: 'EC505PLAverage'},
                    {name: 'EC505PLStdDev'},
                    {name: 'MaxFIAverage'},
                    {name: 'MaxFIStdDev'},
                    {name: 'AUCAverage'},
                    {name: 'AUCStdDev'},
                    {name: 'MaxFIRunCount', type: 'int'},
                    {name: 'EC504PLRunCount', type: 'int'},
                    {name: 'EC505PLRunCount', type: 'int'},
                    {name: 'AUCRunCount', type: 'int'},
                    {name: 'ControlType'},
                    {name: 'has4PLCurveFit', type: 'boolean'},
                    {name: 'has5PLCurveFit', type: 'boolean'},
                    {name: 'UserCanEdit', type: 'boolean', defaultValue: this.canEdit}
                ]
            });

            var assayName = this.assayName;
            var currentGuideSetId = this.currentGuideSetId;
            var gsWindow = this;

            Ext4.define('Luminex.store.GuideSet', {
                extend: 'Ext.data.Store',
                model: 'Luminex.model.GuideSet',
                constructor : function (config) {
                    this.callParent([config]);
                },
                load: function() {
                    // NOTE: need to error here if assayName not set...
                    LABKEY.Query.executeSql({
                        schemaName: 'assay.Luminex.'+LABKEY.QueryKey.encodePart(assayName),
                        success: this.handleResponse, scope: this,
                        sql: 'SELECT RowId, AnalyteName, Conjugate, Isotype, Comment, Created, ValueBased, ' +
                             'ControlName, EC504PLEnabled, EC505PLEnabled, AUCEnabled, MaxFIEnabled, ' +
                             'MaxFIRunCount, EC504PLRunCount, EC505PLRunCount, AUCRunCount, ControlType, ' +
                             // handle value-based vs run-based
                             'CASE ValueBased WHEN true THEN EC504PLAverage ELSE "Four ParameterCurveFit".EC50Average END "EC504PLAverage", ' +
                             'CASE ValueBased WHEN true THEN EC504PLStdDev ELSE "Four ParameterCurveFit".EC50StdDev END "EC504PLStdDev", ' +
                             'CASE ValueBased WHEN true THEN EC505PLAverage ELSE "Five ParameterCurveFit".EC50Average END "EC505PLAverage", ' +
                             'CASE ValueBased WHEN true THEN EC505PLStdDev ELSE "Five ParameterCurveFit".EC50StdDev END "EC505PLStdDev", ' +

                             'CASE ValueBased WHEN true THEN MaxFIAverage ELSE ' +
                                'CASE ControlType WHEN \'Titration\' THEN TitrationMaxFIAverage ELSE SinglePointControlFIAverage END ' +
                             'END "MaxFIAverage", ' +

                             'CASE ValueBased WHEN true THEN MaxFIStdDev ELSE ' +
                                'CASE ControlType WHEN \'Titration\' THEN TitrationMaxFIStdDev ELSE SinglePointControlFIStdDev END ' +
                             'END "MaxFIStdDev", ' +

                             'CASE ValueBased WHEN true THEN AUCAverage ELSE TrapezoidalCurveFit.AUCAverage  END "AUCAverage", ' +
                             'CASE ValueBased WHEN true THEN AUCStdDev ELSE TrapezoidalCurveFit.AUCStdDev END "AUCStdDev" ' +
                             'FROM GuideSet WHERE RowId = ' + currentGuideSetId

                    });
                },
                handleResponse: function(response) {
                    if (response.rows.length > 1)
                    {
                        Ext4.Msg.alert("Error", "There is an issue with the request as the returned rows should be length 1 and is " + response.rows.length);
                        return;
                    }

                    // add in the has4PLCurveFit and has5PLCurveFit info to the record so that it can be used in the tpl
                    response.rows[0]['has4PLCurveFit'] = gsWindow.has4PLCurveFit;
                    response.rows[0]['has5PLCurveFit'] = gsWindow.has5PLCurveFit;

                    this.removeAll();
                    var record = Ext4.create('Luminex.model.GuideSet', response.rows[0]);
                    this.add(record);

                    // Now that store is loaded, set combos (if not value based)
                    var form = document.forms['GuideSetForm'];
                    if (!record.get("ValueBased"))
                    {
                        // NOTE: using this for dirty bit logic.
                        form.elements['MFICheckBox'].initial = record.get("MaxFIEnabled");
                        if(record.get("ControlType") === "Titration")
                        {
                            if (gsWindow.has4PLCurveFit) form.elements['EC504PLCheckBox'].initial = record.get("EC504PLEnabled");
                            if (gsWindow.has5PLCurveFit) form.elements['EC505PLCheckBox'].initial = record.get("EC505PLEnabled");
                            form.elements['AUCCheckBox'].initial = record.get("AUCEnabled");
                        }
                    }
                }
            });

            this.guideSetStore = Ext4.create('Luminex.store.GuideSet', {});
        }
        return this.guideSetStore;
    }
});

// helper used in display column... see GuideSetTable and guidSetConfirmDelete.jsp
// noinspection JSUnusedGlobalSymbols
function createGuideSetWindow(protocolId, currentGuideSetId, allowEdit) {
    LABKEY.Assay.getById({
        id: protocolId,
        success: function(assay){
            if (Ext4.isArray(assay) && assay.length === 1)
            {
                // could use either full name or base name here...
                Ext4.create('LABKEY.luminex.GuideSetWindow', {
                    assayName: assay[0].name,
                    currentGuideSetId: currentGuideSetId,
                    canEdit: allowEdit && LABKEY.user.canUpdate
                });
            }
        }
    });
}

function checkGuideSetWindowDirty(name) {
    const fields = ['EC504PLCheckBox', 'EC505PLCheckBox', 'MFICheckBox', 'AUCCheckBox']
    const form = document.forms['GuideSetForm'];
    let guideSetWindowDirtyBit = false;
    fields.forEach(function(name) {
        const element = form.elements[name];
        if (element && (element.checked !== element.initial))
        {
            guideSetWindowDirtyBit = true;
        }
    });
    Ext4.getCmp('GuideSetSaveButton').setDisabled(!guideSetWindowDirtyBit);
}
