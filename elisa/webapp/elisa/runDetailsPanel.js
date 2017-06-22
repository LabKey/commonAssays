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

        this.items = [
            this.createRunDetailView()
        ];

        this.callParent([arguments]);
    },

    createRunDetailView : function() {

        Ext4.define('Elisa.model.RunDetail', {
            extend : 'Ext.data.Model',
            fields : [
                {name : 'Name'},
                {name : 'Created'},
                {name : 'RSquared'},
                {name : 'CurveFitParams'}
            ]
        });

        var config = {
            model   : 'Elisa.model.RunDetail',
            autoLoad: true,
            proxy   : {
                type   : 'ajax',
                url    : LABKEY.ActionURL.buildURL('query', 'selectRows.api'),
                extraParams : {
                    schemaName : this.schemaName,
                    queryName  : this.runTableName,
                    'query.columns' : 'Name,Created,RSquared,CurveFitParams'
                },
                reader : {
                    type : 'json',
                    root : 'rows'
                }
            },
            filters : [{property:'RowId', value : this.runId}]
        };

        this.elisaDetailStore = Ext4.create('Ext.data.Store', config);

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
                        return 'slope : ' + Ext.util.Format.number(parts[0], "0.00") + ' intercept : ' + Ext.util.Format.number(parts[1], "0.00");
                    }
                    else
                        return 'error';
                }
            }
        );

        return Ext4.create('Ext.view.View', {
            store   : this.elisaDetailStore,
            loadMask: true,
            tpl     : tpl,
            ui      : 'custom',
            flex    : 1,
            scope   : this
        });
    },

    createRunDataView : function() {

        Ext4.define('Elisa.model.RunData', {
            extend : 'Ext.data.Model',
            fields : [
                {name : 'WellgroupLocation'},
                {name : 'WellLocation'},
                {name : 'Absorption'},
                {name : 'Concentration'},
                {name : 'RSquared', mapping : 'Run/RSquared'}
            ]
        });

        var urlParams = LABKEY.ActionURL.getParameters(this.baseUrl);
        var filterUrl = urlParams['filterUrl'];

        // lastly check if there is a filter on the url
        var filters = LABKEY.Filter.getFiltersFromUrl(filterUrl, this.dataRegionName);

        var config = {
            model   : 'Elisa.model.RunData',
            autoLoad: true,
            pageSize: 92,
            proxy   : {
                type   : 'ajax',
                url    : LABKEY.ActionURL.buildURL('query', 'selectRows.api'),
                extraParams : {
                    schemaName : this.schemaName,
                    queryName  : this.queryName,
                    filters    : filters
                },
                reader : {
                    type : 'json',
                    root : 'rows'
                }
            }
        };

        this.elisaDetailStore = Ext4.create('Ext.data.Store', config);

        var tpl = new Ext4.XTemplate(
            '<div>',
                '<table width="100%">',
                    '<tr><td>Well Location</td><td>Well Group</td><td>Absorption</td><td>Concentration</td></tr>',
                    '<tpl for=".">',
                    '<tr class="{[xindex % 2 === 0 ? "labkey-alternate-row" : "labkey-row"]}"><td>{WellLocation}</td><td>{WellgroupLocation}</td><td>{Absorption}</td><td>{[Ext.util.Format.number(values.Concentration, "0.000")]}</td></tr>',
                '</tpl></table>',
            '</div>'
        );

        return Ext4.create('Ext.view.View', {
            store   : this.elisaDetailStore,
            loadMask: true,
            tpl     : tpl,
            ui      : 'custom',
            flex    : 1,
            padding : '20, 8',
            scope   : this
        });
    }
});
