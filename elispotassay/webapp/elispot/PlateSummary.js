/*
 * Copyright (c) 2012-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
LABKEY.requiresExt4Sandbox();

Ext4.define('LABKEY.ext4.PlateSummary', {

    extend  : 'Ext.panel.Panel',

    layout  : 'hbox',

    frame   : false,

    border  : false,

    rowLabel    : ['A','B','C','D','E','F','G','H'],

    columnLabel : [1,2,3,4,5,6,7,8,9,10,11,12],

    constructor : function(config) {

        Ext4.QuickTips.init();

        this.callParent([config]);
    },

    initComponent : function() {

        this.items = [];

        this.centerPanel = Ext4.create('Ext.panel.Panel', {
            border  : false,
            frame   : false,
            bodyPadding : 20,
            flex     : 1.2
        });
        this.items.push(this.centerPanel);

        this.eastPanel = Ext4.create('Ext.panel.Panel', {
            border  : false,
            frame   : false,
            bodyPadding : 20,
            flex    : 1,
            items   : [
                {html:'<span>Click on a button to highlight the wells in a particular well group.<br>Hover over an individual well ' +
                        'to display a tooltip with additional details.</span>', border: false},
                {html:'&nbsp;', border:false}
            ]
        });

        this.items.push(this.eastPanel);

        // initialize the plate summary store
        Ext4.define('LABKEY.data.PlateSummary', {
            extend : 'Ext.data.Model',
            fields : [
                {name : 'position'},
                {name : 'spotCount'},
                {name : 'spotSize'},
                {name : 'activity'},
                {name : 'intensity'},
                {name : 'analyte'},
                {name : 'cytokine'},
                {name : 'wellProperties'}
            ]
        });

        var config = {
            model   : 'LABKEY.data.PlateSummary',
            autoLoad: true,
            proxy   : {
                type   : 'ajax',
                url : LABKEY.ActionURL.buildURL('elispot-assay', 'getPlateSummary.api', null, {rowId : this.runId}),
                reader : {
                    type : 'json',
                    root : 'summary'
                }
            }
        };

        this.plateStore = Ext4.create('Ext.data.Store', config);

        this.plateStore.on('load', this.createPlateSummary, this);
        this.callParent([arguments]);
    },

    createPlateSummary : function() {
        var grids = [];
        this.analytes = [''];
        this.analyteMap = {};
        this.sampleGroups = {};
        this.antigenGroups = {};

        var reader = this.plateStore.getProxy().getReader();
        if (reader && reader.rawData.analytes){
            this.analytes = reader.rawData.analytes;
        }

        if (reader && reader.rawData.analyteMap){
            this.analyteMap = reader.rawData.analyteMap;
        }

        for (var idx=0; idx < this.analytes.length; idx++){

            var analyte = this.analytes[idx];
            var rows = [];

            this.plateStore.clearFilter();
            if (analyte){
                this.plateStore.filter({
                    property : 'analyte',
                    value    : analyte,
                    exactMatch : true
                });
            }

            // create the row map to populate the template data
            for (var row=0, i=0; row < this.rowLabel.length; row++, i++) {

                var label = this.rowLabel[row];
                var cols = [];
                for (var col=0, j=0; col < this.columnLabel.length; col++, j++) {

                    var position = '(' + i + ', ' + j + ')';

                    var rec = this.plateStore.findRecord('position', position);

                    if (rec) {
                        // sample group map
                        var sampleGroupName = rec.data.wellProperties.WellgroupName;

                        if (sampleGroupName) {

                            var sampleCls = 'labkey-sampleGroup-' + sampleGroupName.replace(/\s/g, '-');

                            this.sampleGroups[sampleGroupName] = {
                                label : sampleGroupName,
                                cls : sampleCls
                            };

                            // antigen group map
                            var antigenGroupName = rec.data.wellProperties.AntigenWellgroupName;
                            var antigenName = rec.data.wellProperties.AntigenName;
                            var antigenLabel = antigenGroupName;

                            if (antigenName && antigenName.length > 0)
                                antigenLabel = antigenGroupName + ' (' + antigenName + ')';
                            var antigenCls = '';
                            if (antigenGroupName && antigenGroupName.length > 0)
                                antigenCls = 'labkey-antigenGroup-' + antigenGroupName.replace(/\s/g, '-');

                            this.antigenGroups[antigenGroupName] = {
                                label : antigenLabel,
                                cls : antigenCls
                            }

                            cols.push({
                                spotCount   : rec.data.spotCount,
                                spotSize    : rec.data.spotSize,
                                activity    : rec.data.activity,
                                intensity   : rec.data.intensity,
                                dataIndex   : rec.index,
                                sCls        : sampleCls,
                                aCls        : antigenCls
                            });
                        }
                        else {
                            cols.push({
                                spotCount   : 'N/A',
                                spotSize    : 'N/A',
                                activity    : 'N/A',
                                intensity   : 'N/A',
                                position    : rec.data.position,
                                dataIndex   : rec.index,
                                sCls        : '',
                                aCls        : ''
                            });
                        }
                    }
                }
                rows.push({label:label, cols:cols});
            }

            grids.push({
                xtype       : 'lk_platepanel',
                height      : 450,
                plateStore  : this.plateStore,
                isFluorospot: this.isFluorospot,
                data        : {
                    columnLabel : this.columnLabel,
                    analyte     : analyte,
                    cytokine    : this.analyteMap[analyte],
                    rows        : rows
                }
            });
        }
        this.centerPanel.add(grids);
        this.eastPanel.add(this.getEastPanel());
        this.plateStore.clearFilter();
    },

    getEastPanel : function() {
        var sampleItems = new Array();

        for (var s in this.sampleGroups) {
            if (this.sampleGroups.hasOwnProperty(s)) {
                sampleItems.push({
                    boxLabel    : this.sampleGroups[s].label,
                    wellCls     : this.sampleGroups[s].cls,
                    name        : 'sampleGroup',
                    handler     : function(cmp, checked){this.showSample(cmp.initialConfig.wellCls, checked);},
                    scope       : this
                });
            }
        }
        sampleItems = Ext4.Array.sort(sampleItems, function(a, b) {
            return a.boxLabel > b.boxLabel;
        });

        var sampleGroup = Ext4.create('Ext.form.RadioGroup', {
            fieldLabel  : 'Sample Well Groups',
            columns     : 1,
            items       : sampleItems
        });

        var antigenItems = new Array();
        for (var a in this.antigenGroups) {
            if (this.antigenGroups.hasOwnProperty(a)) {
                antigenItems.push({
                    boxLabel    : this.antigenGroups[a].label,
                    wellCls     : this.antigenGroups[a].cls,
                    name        : 'sampleGroup',
                    handler     : function(cmp, checked){this.showSample(cmp.initialConfig.wellCls, checked);},
                    scope       : this
                });
            }
        }
        antigenItems = Ext4.Array.sort(antigenItems, function(a, b) {
            return a.boxLabel > b.boxLabel;
        });

        var antigenGroup = Ext4.create('Ext.form.RadioGroup', {
            fieldLabel  : 'Antigen Well Groups',
            columns     : 1,
            items       : antigenItems
        });

        var items = [sampleGroup, antigenGroup];

        if (this.isFluorospot){

            items.push(Ext4.create('Ext.form.RadioGroup', {
                fieldLabel  : 'Measurement',
                columns     : 1,
                items       : [
                    {boxLabel : 'Spot Count', name : 'measurement', inputValue : 'labkey-cls-spotcount', checked : true},
                    {boxLabel : 'Spot Size (microns)', name : 'measurement', inputValue : 'labkey-cls-spotsize'},
                    {boxLabel : 'Activity', name : 'measurement', inputValue : 'labkey-cls-activity'},
                    {boxLabel : 'Intensity (fluorescence)', name : 'measurement', inputValue : 'labkey-cls-intensity'}],
                listeners   : {
                    change  : {fn : function(cmp, newVal, oldVal){
                        this.showMeasurement(newVal, oldVal);
                    }},
                    scope   : this
                }
            }));
        }

        var form = Ext4.create('Ext.form.Panel', {
            border: false,
            fieldDefaults : {
                labelAlign : 'left',
                labelWidth : 130,
                labelSeparator : ''
            },
            items: items
        });

        return form;
    },

    showSample : function(cls, hilight) {

        if (hilight) {

            // clear the current
            if (this.currentSelection)
                this.applyStyleToClass(this.currentSelection, {backgroundColor: '#AAAAAA'});

            this.applyStyleToClass(cls, {backgroundColor: '#126495'});
            this.currentSelection = cls;
        }
    },

    showMeasurement : function(newVal, oldVal) {

        if (oldVal && oldVal.measurement) {
            // clear the current
            this.applyStyleToClass(oldVal.measurement, {display: 'none'});
        }

        if (newVal && newVal.measurement){
            this.applyStyleToClass(newVal.measurement, {display: ''});
        }
    },

    applyStyleToClass : function(cls, style) {

        var sample = Ext4.select('.' + cls, true);
        if (sample) {
            sample.applyStyles(style);
            sample.repaint();
        }
    }
});

Ext4.define('LABKEY.ext4.PlatePanel', {

    extend  : 'Ext.panel.Panel',

    alias   : 'widget.lk_platepanel',

    border  : false,

    frame   : false,

    constructor: function (config)
    {
        Ext4.QuickTips.init();

        this.callParent([config]);
        var plateTemplate = [];

        if (this.isFluorospot){
            plateTemplate.push(
                '<div><span>Analyte: {analyte}</span></div>',
                '<div><span>Cytokine: {cytokine}</span></div>');
        }

        plateTemplate.push(
            '<table class="plate-summary-grid">',
            '<tr><td><div class="plate-columnlabel"></div></td>',
            '<tpl for="columnLabel">',
            '<td><div class="plate-columnlabel">{.}</div></td>',
            '</tpl>',
            '</tr>',
            '<tpl for="rows">',
            '<tr><td><div class="plate-rowlabel"><br>{label}</div></td>' +
            '<tpl for="cols">',
            '<td><div class="plate-well-td-div {aCls} {sCls}" dataIndex="{dataIndex}">',
            '<a class="labkey-cls-spotcount" style="display:" href="javascript:void(0);">{spotCount}</a>',
            '<a class="labkey-cls-spotsize" style="display: none" href="javascript:void(0);">{spotSize}</a>',
            '<a class="labkey-cls-activity" style="display: none" href="javascript:void(0);">{activity}</a>',
            '<a class="labkey-cls-intensity" style="display: none" href="javascript:void(0);">{intensity}</a>',
            '</div>',
            '</td>',
            '</tpl>',
            '</tr>',
            '</tpl>',
            '</table>'
        );
        this.tpl = plateTemplate.join('');
    },

    initComponent: function ()
    {
        this.listeners = {

            render: function(cmp) {
                var template = [
                    '<table>',
                    '<tpl for=".">',
                    '<tr><td>{name}</td><td>&nbsp;</td><td>{value}</td></tr>',
                    '</tpl>',
                    '</table>'
                ];

                var tip = Ext4.create('Ext.tip.ToolTip', {
                    target      : cmp.el,
                    delegate    : '.plate-well-td-div',
                    title       : 'Well Detail',
                    anchor      : 'left',
                    tpl         : template.join(''),
                    bodyPadding : 10,
                    showDelay   : 500,
                    anchorOffset : 100,
                    dismissDelay : 10000,
                    autoHide    : true,
                    scope       : this,
                    anchorToTarget : true,
                    listeners: {
                        // Change content dynamically depending on which element triggered the show.
                        beforeshow: function(tip) {
                            var element = tip.triggerElement;
                            var index = tip.triggerElement.getAttribute('dataIndex');
                            var rec = this.plateStore.getAt(index);

                            var tipData = [];
                            if (rec) {
                                for (var d in rec.data.wellProperties) {
                                    if (rec.data.wellProperties.hasOwnProperty(d)) {
                                        tipData.push({name: d, value : Ext4.htmlEncode(rec.data.wellProperties[d])});
                                    }
                                }
                                if (0 == tipData.length) {
                                    tipData.push({name: 'SpotCount', value: 'N/A'});
                                    tipData.push({name: 'WellgroupLocation', value: Ext4.htmlEncode(pos)});
                                }
                                tip.update(tipData);
                                return true;
                            }
                            return false;
                        },
                        scope : this
                    }
                });
            },
            scope : this
        };

        this.callParent();
    }
});
