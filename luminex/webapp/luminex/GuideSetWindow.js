// TODO: it was a bad move to go with XTemplate here. This should be replaced with a full ExtJS implementation whenever possible.
// issues include styling, dirtying the form, and a few other things

Ext4.define('Luminex.window.GuideSetWindow', {

    extend: 'Ext.window.Window',
    title: 'Guide Set Parameter Details',
    modal: true,
    border: false,
    height: 325,
    width: 550,
    autoScroll: true,

    // NOTE: these are required fields (any way to enforce?)
    assayName: null,
    currentGuideSetId: null,

    statics: {
        viewTpl: new Ext4.XTemplate(
            '<style>table.gsDetails th {background-color:#d3d3d3;font-weight:normal;text-align:left;}</style>',
            '<tpl for=".">',
            '<form id="GuideSetForm">',
            '<table width="300px" style="float: left;" class="gsDetails">',
                '<tr><th>Guide Set Id:</th><td>{RowId}</td></tr>',
                '<tr><th>Created:</th><td>{Created}</td></tr>',
                '<tr><th>Titration:</th><td>{ControlName}</td></tr>',
                '<tr><th>Analyte:</th><td>{AnalyteName}</td></tr>',
                '<tr><th>Comment:</th><td>{Comment}</td></tr>',
            '</table>',
            '<table width="200px" style="display: inline-block;" class="gsDetails">',
                '<tr>',
                    '<th>Type:</th>',
                    '<tpl if="ValueBased &gt; 0">',
                    '<td>Value-based</td>',
                    '<tpl else>',
                    '<td>Run-based</td>',
                    '</tpl>',
                '</tr>',
                '<tr><th>Created By:</th><td>{CreatedBy}</td></tr>',
                '<tr><th>Isotype:</th><td>{Isotype}</td></tr>',
                '<tr><th>Conjugate:</th><td>{Conjugate}</td></tr>',
            '</table>',
            // TODO: come back to styling here...
            '<table width="100%" class="gsDetails" style="border:1px solid black;margin-top:30px;">',
            '<caption style="background-color:#d3d3d3;text-align:left;font-weight:bold;font-size:16px;padding:2px;">Guide Set Metrics</caption>',
            '<tr>',
                '<th>Metric</th>',
                '<th>Std Dev</th>',
                '<th>Mean</th>',
                '<tpl if="ValueBased &lt; 1">',
                '<th>Num Runs</th>',
                '<th>Use for QC</th>',
                '</tpl>',
            '</tr><tr>',
                '<td>EC50 4PL</td>',
                '<td align="right">{[this.formatNumber(values.EC504PLStdDev)]}</td>',
                '<td align="right">{[this.formatNumber(values.EC504PLAverage)]}</td>',
                '<tpl if="ValueBased &lt; 1">',
                '<td align="center">{EC504PLRunCounts}</td>',
                '<td><input type="checkbox" name="EC504PLCheckBox" onchange="checkGuideSetWindowDirty();"></td>',
                '</tpl>',
            '</tr><tr>',
                '<td>EC50 5PL</td>',
                '<td align="right">{[this.formatNumber(values.EC505PLStdDev)]}</td>',
                '<td align="right">{[this.formatNumber(values.EC505PLAverage)]}</td>',
                '<tpl if="ValueBased &lt; 1">',
                '<td align="center">{EC505PLRunCounts}</td>',
                '<td><input type="checkbox" name="EC505PLCheckBox" onchange="checkGuideSetWindowDirty();"></td>',
                '</tpl>',
            '</tr><tr>',
                '<td>MFI</td>',
                '<td align="right">{[this.formatNumber(values.MaxFIStdDev)]}</td>',
                '<td align="right">{[this.formatNumber(values.MaxFIAverage)]}</td>',
                '<tpl if="ValueBased &lt; 1">',
                '<td align="center">{maxFIRunCounts}</td>',
                '<td><input type="checkbox" name="MFICheckBox" onchange="checkGuideSetWindowDirty();"></td>',
                '</tpl>',
            '</tr><tr>',
                '<td>AUC</td>',
                '<td align="right">{[this.formatNumber(values.AUCStdDev)]}</td>',
                '<td align="right">{[this.formatNumber(values.AUCAverage)]}</td>',
                '<tpl if="ValueBased &lt; 1">',
                '<td align="center">{AUCRunCounts}</td>',
                '<td><input type="checkbox" name="AUCCheckBox" onchange="checkGuideSetWindowDirty();"></td>',
                '</tpl>',
            '</tr>',
            '</table>',
            '</form>',
            '</tpl>',
            {
                formatNumber: function(value) {
                    return value != 0 ? value.toFixed(3) : "N/A";
                }
            }
        )
    },

    initComponent: function ()
    {
        this.items = [{
            xtype: 'dataview',
            tpl: Luminex.window.GuideSetWindow.viewTpl,
            padding: 10,
            store: this.getGuideSetStore(),
        }];

        this.buttons = [{
            id: 'GuideSetSaveButton',
            disabled: true,
            text: 'Save',
            scope: this,
            handler: function(btn) {
                // NOTE: could check if dirty...
                var form = document.forms['GuideSetForm'];

                // NOTE: need to check that these fields are set (e.g. check that not value-based)
                LABKEY.Query.updateRows({
                    schemaName: 'assay.Luminex.'+LABKEY.QueryKey.encodePart(this.assayName),
                    queryName: 'GuideSet',
                    rows: [{
                        rowId: this.currentGuideSetId,
                        ec504plEnabled: form.elements['EC504PLCheckBox'].checked,
                        ec505plEnabled: form.elements['EC505PLCheckBox'].checked,
                        MaxFIEnabled: form.elements['MFICheckBox'].checked,
                        aucEnabled: form.elements['AUCCheckBox'].checked
                    }],
                    scope: this,
                    success: function() { this.close(); }
                });
            }
        },{
            text: 'Close',
            scope: this,
            handler: function() { this.close(); }
        }];

        this.callParent();
    },

    constructor: function(config) {
        this.currentGuideSetId = config['currentGuideSetId'];
        this.assayName = config['assayName'];
        this.callParent([config]);
        // wait till after constructed so that currentGuideSetId is set and assayName
        this.guideSetStore.load();
        this.show();
    },

    // NOTE: consider putting store/model into seperate file...
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
                    {name: 'CreatedBy'},
                    {name: 'Created'},
                    {name: 'ValueBased', type: 'boolean'},
                    {name: 'EC504PLEnabled', type: 'boolean'},
                    {name: 'EC505PLEnabled', type: 'boolean'},
                    {name: 'AUCEnabled', type: 'boolean'},
                    {name: 'MaxFIEnabled', type: 'boolean'},
                    {name: 'EC504PLAverage', type: 'float'},
                    {name: 'EC504PLStdDev', type: 'float'},
                    {name: 'EC505PLAverage', type: 'float'},
                    {name: 'EC505PLStdDev', type: 'float'},
                    {name: 'MaxFIAverage', type: 'float'},
                    {name: 'MaxFIStdDev', type: 'float'},
                    {name: 'AUCAverage', type: 'float'},
                    {name: 'AUCStdDev', type: 'float'},
                    {name: 'maxFIRunCounts', type: 'int'},
                    {name: 'EC504PLRunCounts', type: 'int'},
                    {name: 'EC505PLRunCounts', type: 'int'},
                    {name: 'AUCRunCounts', type: 'int'}
                ]
            });

            var assayName = this.assayName;
            var currentGuideSetId = this.currentGuideSetId;

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
                        success: this.handleCounts, scope: this,
                        sql: 'SELECT RowId, AnalyteName, Conjugate, Isotype, Comment, CreatedBy, Created, ValueBased, ' +
                             'ControlName, EC504PLEnabled, EC505PLEnabled, AUCEnabled, MaxFIEnabled, ' +
                             'maxFIRunCounts, EC504PLRunCounts, EC505PLRunCounts, AUCRunCounts, ' +
                             // handle value-based vs run-based
                             'CASE ValueBased WHEN true THEN EC504PLAverage ELSE "Four ParameterCurveFit".EC50Average END "EC504PLAverage", ' +
                             'CASE ValueBased WHEN true THEN EC504PLStdDev ELSE "Four ParameterCurveFit".EC50StdDev END "EC504PLStdDev", ' +
                             'CASE ValueBased WHEN true THEN EC505PLAverage ELSE "Five ParameterCurveFit".EC50Average END "EC505PLAverage", ' +
                             'CASE ValueBased WHEN true THEN EC505PLStdDev ELSE "Five ParameterCurveFit".EC50StdDev END "EC505PLStdDev", ' +
                             'CASE ValueBased WHEN true THEN MaxFIAverage ELSE TitrationMaxFIAverage END "MaxFIAverage", ' +
                             'CASE ValueBased WHEN true THEN MaxFIStdDev ELSE TitrationMaxFIStdDev END "MaxFIStdDev", ' +
                             'CASE ValueBased WHEN true THEN AUCAverage ELSE TrapezoidalCurveFit.AUCAverage  END "AUCAverage", ' +
                             'CASE ValueBased WHEN true THEN AUCStdDev ELSE TrapezoidalCurveFit.AUCStdDev END "AUCStdDev" ' +
                             'FROM GuideSet WHERE RowId = ' + currentGuideSetId

                    });
                },
                handleCounts: function(response) {
                    var responseRows = response.rows;
                    var rows = [];
                    for (var i = 0; i < responseRows.length; i++)
                        rows.push(Ext4.create('Luminex.model.GuideSet', responseRows[i]));

                    this.removeAll();
                    this.add(rows);
                    // Now that store is loaded, set combos
                    var form = document.forms['GuideSetForm'];
                    var record = this.getAt(0);
                    // note: this will sometimes create a javascript error... (when value-based)
                    form.elements['EC504PLCheckBox'].checked = record.get("EC504PLEnabled");
                    form.elements['EC505PLCheckBox'].checked = record.get("EC505PLEnabled");
                    form.elements['MFICheckBox'].checked = record.get("MaxFIEnabled");
                    form.elements['AUCCheckBox'].checked = record.get("AUCEnabled");

                    // NOTE: using this for dirty bit logic.
                    form.elements['EC504PLCheckBox'].initial = record.get("EC504PLEnabled");
                    form.elements['EC505PLCheckBox'].initial = record.get("EC505PLEnabled");
                    form.elements['MFICheckBox'].initial = record.get("MaxFIEnabled");
                    form.elements['AUCCheckBox'].initial = record.get("AUCEnabled");
                }
            });

            this.guideSetStore = Ext4.create('Luminex.store.GuideSet', {});
        }
        return this.guideSetStore;
    }
});

// helper used in display column
function createGuideSetWindow(protocolId, currentGuideSetId) {
    LABKEY.Assay.getById({
        id: protocolId,
        success: function(assay){
            if (Ext.isArray(assay) && assay.length == 1)
            {
                // could use either full name or base name here...
                Ext4.create('Luminex.window.GuideSetWindow', {
                    assayName: assay[0].name,
                    currentGuideSetId: currentGuideSetId
                });
            }
        }
    });
}

function checkGuideSetWindowDirty(name) {
    var fields = ['EC504PLCheckBox', 'EC505PLCheckBox', 'MFICheckBox', 'AUCCheckBox']
    var form = document.forms['GuideSetForm'];
    console.log(form);
    var guideSetWindowDirtyBit = false;
    // Uncaught TypeError: Cannot read property 'checked' of undefined (when unchecking all the boxes... who knows why)
    try {
        for (var name in fields) {
            if (form.elements[name].checked != form.elements[name].initial)
            {
                guideSetWindowDirtyBit = true;
                break;
            }
        }
    }
    catch (err) {}
    Ext4.getCmp('GuideSetSaveButton').setDisabled(!guideSetWindowDirtyBit);
}