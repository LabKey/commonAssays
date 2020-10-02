/*
 * Copyright (c) 2012-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('LABKEY.elisa.RunDetailsPanel', {

    extend : 'Ext.panel.Panel',

    constructor : function(config) {

        Ext4.applyIf(config, {
            layout  : 'auto',
            border  : false,
            frame   : false,
            autoScroll : true,
            cls     : 'iScroll'
        });

        this.callParent([config]);
    },

    initComponent : function() {
        this.items = [];

        this.callParent([arguments]);

        this.queryRunDetails();
    },

    queryRunDetails : function() {
        LABKEY.Query.selectRows({
            schemaName: this.schemaName,
            queryName: this.runTableName,
            columns: 'Name,Created,RSquared,CurveFitParams',
            filterArray: [LABKEY.Filter.create('RowId', this.runId)],
            scope: this,
            success: function(response) {
                if (response.rows.length === 1) {
                    this.createRunDetailView(response.rows[0]);
                }
            }
        });
    },

    createRunDetailView : function(data) {

        Ext4.define('Elisa.model.RunDetail', {
            extend : 'Ext.data.Model',
            fields : [
                {name : 'Name'},
                {name : 'Created'},
                {name : 'RSquared'},
                {name : 'CurveFitParams'}
            ]
        });

        this.elisaDetailStore = Ext4.create('Ext.data.Store', {
            model: 'Elisa.model.RunDetail',
            data: data
        });

        var tpl = new Ext4.XTemplate(
                '<div>',
                '<table>',
                '<tpl for=".">',
                '<tr><td style="padding-right: 10px; font-weight: bold;">Name</td><td>{Name}</td></tr>',
                '<tr><td style="padding-right: 10px; font-weight: bold;">Curve Fit Type</td><td>Linear</td></tr>',
                '<tr><td style="padding-right: 10px; font-weight: bold;">Curve Fit Parameters</td><td>{[this.formatFitParams(values)]}</td></tr>',
                '<tr><td style="padding-right: 10px; font-weight: bold;">Coefficient of Determination</td><td>{[Ext.util.Format.number(values.RSquared, "0.00000")]}</td></tr>',
                '<tr><td style="padding-right: 10px; font-weight: bold;">Created</td><td>{Created}</td></tr>',
                '</tpl></table>',
                '</div>',
                {
                    formatFitParams : function(data) {
                        if (data.CurveFitParams)
                        {
                            var parts = data.CurveFitParams.split('&');
                            return 'slope : ' + Ext4.util.Format.number(parts[0], "0.00") + ', intercept : ' + Ext4.util.Format.number(parts[1], "0.00");
                        }
                        else
                            return 'error';
                    }
                }
        );

        this.add(Ext4.create('Ext.view.View', {
            store   : this.elisaDetailStore,
            tpl     : tpl,
            ui      : 'custom',
            flex    : 1,
            scope   : this
        }));
    }
});
