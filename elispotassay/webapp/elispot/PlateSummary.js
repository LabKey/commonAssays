/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
LABKEY.requiresExt4Sandbox(true);

Ext4.define('LABKEY.ext4.PlateSummary', {

    extend : 'Ext.panel.Panel',

    constructor : function(config) {

        Ext4.QuickTips.init();

        Ext4.apply(this, config, {
            layout : 'border',
            frame  : false,
            border : false
        });

        this.callParent([config]);
    },

    initComponent : function() {

        this.items = [];

        this.platePanel = this.getPlatePanel();
        this.centerPanel = Ext4.create('Ext.panel.Panel', {
            border  : false,
            frame   : false,
            bodyPadding : 20,
            region   : 'center',
            items    : [this.platePanel]
        });
        this.items.push(this.centerPanel);

        this.eastPanel = Ext4.create('Ext.panel.Panel', {
            border  : false,
            frame   : false,
            bodyPadding : 20,
            region  : 'east',
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
                {name : 'title'},
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

        this.plateStore.on('load', this.renderPlateSummary, this);
        this.callParent([arguments]);
    },

    getPlatePanel : function() {

        var template = [
            '<table>',
                '<tr><td><div style="width:53px; height:40px; text-align:center;"></div></td>',
                '<tpl for="columnLabel">',
                    '<td><div style="width:53px; height:40px; text-align:center;">{.}</div></td>',
                '</tpl>',
                '</tr>',
            '<tpl for="rows">',
                '<tr><td><div style="width:45px; height:40px; text-align:center;"><br>{label}</div></td>' +
                    '<tpl for="cols">',
                        '<td><div class="plate-well-td-div {aCls} {sCls}" position="{position}" style="border:1px solid gray;width:49px; height:35px; vertical-align:middle; text-align:center; background-color:#AAAAAA;">',
                            '<a style="color: white;" href="javascript:void(0);">{name}</a></div>',
                        '</td>',
                    '</tpl>',
                '</tr>',
            '</tpl>',
            '</table>'
        ];

        this.platePanel = Ext4.create('Ext.panel.Panel', {
            border  : false,
            frame   : false,
            height  : 400,
            tpl     : template.join('')
        });

        return this.platePanel;
    },

    renderPlateSummary : function() {

        console.log('render plate summary');
        var rows = [];

        var template = [
            '<table>',
                '<tpl for=".">',
                    '<tr><td>{name}</td><td>&nbsp;</td><td>{value}</td></tr>',
                '</tpl>',
            '</table>'
        ];

        var tip = Ext4.create('Ext.tip.ToolTip', {
            target      : this.platePanel.el,
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
                    var pos = tip.triggerElement.getAttribute('position');
                    var rec = this.plateStore.findRecord('position', pos);

                    var tipData = [];
                    if (rec) {
                        for (var d in rec.data.wellProperties) {
                            if (rec.data.wellProperties.hasOwnProperty(d))
                            {
                                tipData.push({name: d, value : Ext4.htmlEncode(rec.data.wellProperties[d])});
                            }
                        }
                        tip.update(tipData);
                        return true;
                    }
                    return false;
                },
                scope : this
            }
        });

        this.sampleGroups = {};
        this.antigenGroups = {};

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
                        var antigenCls = 'labkey-antigenGroup-';
                        if (antigenGroupName && antigenGroupName.length > 0)
                            antigenCls = antigenCls.concat(antigenGroupName.replace(/\s/g, '-'));

                        this.antigenGroups[antigenGroupName] = {
                            label : antigenLabel,
                            cls : antigenCls
                        }

                        cols.push({
                            name        : rec.data.title,
                            position    : rec.data.position,
                            sCls        : sampleCls,
                            aCls        : antigenCls
                        });
                    }
                }
            }
            rows.push({label:label, cols:cols});
        }

        var data = {
            columnLabel : this.columnLabel,
            rows : rows
        };

        this.platePanel.update({columnLabel : this.columnLabel, rows : rows});
        this.eastPanel.add(this.getEastPanel());
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

        var form = Ext4.create('Ext.form.Panel', {
            border: false,
            fieldDefaults : {
                labelAlign : 'left',
                labelWidth : 130,
                labelSeparator : ''
            },
            items: [sampleGroup, antigenGroup]
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

    applyStyleToClass : function(cls, style) {

        var sample = Ext.select('.' + cls, true);
        if (sample) {
            sample.applyStyles(style);
            sample.repaint();
        }
    }
});
