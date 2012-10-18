/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('LABKEY.elisa.RunDetailsPanel', {

    extend : 'Ext.panel.Panel',

    constructor : function(config) {

        Ext4.applyIf(config, {
            layout  : 'fit',
            border  : false,
            frame   : false,
            autoScroll : true,
            cls     : 'iScroll'
        });

        this.callParent([config]);
    },

    initComponent : function() {

        this.items = [];

        Ext4.define('Elisa.model.RunDetail', {
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
            model   : 'Elisa.model.RunDetail',
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
                    '<tr class="{[xindex % 2 === 0 ? "labkey-alternate-row" : "labkey-row"]}"><td>{WellLocation}</td><td>{WellgroupLocation}</td><td>{Absorption}</td><td>{Concentration}</td></tr>',
                '</tpl></table>',
            '</div>'
        );
        var dataView = Ext4.create('Ext.view.View', {
            store   : this.elisaDetailStore,
            loadMask: true,
            tpl     : tpl,
            ui      : 'custom',
            flex    : 1,
            padding : '20, 8',
            scope   : this
        });

        this.items.push(dataView);

        this.callParent([arguments]);
    }

});
