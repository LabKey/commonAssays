/*
 * Copyright (c) 2013-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.onReady(function(){
    Ext4.define('TBDCC.CriteriaStore', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'name', type: 'string'},
            {name: 'text', type: 'string'},
            {name: 'values'},
            {name: 'type', type: 'string'}
        ]
    });

    Ext4.define('TBDCC.CriteriaPanel', {
        extend: 'Ext.panel.Panel',

        constructor: function(config){
            Ext4.apply(config, {
                border: false,
                frame: false,
                width: 650,
                height: 500,
                layout: {
                    type: 'vbox',
                    pack: 'center'
                }
            });

            this.callParent([config]);
        },

        initComponent: function(config){
            this.addEvents('storechanged');
            var gridConfig = {
                flex: 1,
                width: 650,
                border: false,
                frame: false,
                columns: [
                    {text: 'Field', dataIndex: 'text', flex: 1},
                    {text: 'Values', dataIndex: 'values', flex: 1, renderer: function(value){return value.join(', ')}},
                    {text: 'Type', dataIndex: 'type', width: 100},
                    {
                        xtype:'actioncolumn',
                        width:40,
                        items: [{
                            icon: LABKEY.contextPath + '/_images/delete.png',
                            tooltip: 'Delete',
                            handler: function(grid, rowIndex) {
                                grid.getStore().removeAt(rowIndex);
                            }
                        }]
                    }
                ]
            };

            var storeConfig = {
                model: 'TBDCC.CriteriaStore',
                proxy: {type: 'memory', reader: {type:'json'}},
                autoLoad: false,
                listeners: {
                    scope: this,
                    remove: function(){
                        this.fireEvent('storechanged');
                    }
                }
            };

            this.baselineStore = Ext4.create('Ext.data.Store', storeConfig);
            this.baselineGrid = Ext4.create('Ext.grid.Panel', Ext4.apply({title: 'Baseline Criteria', store: this.baselineStore}, gridConfig));
            this.comparisonStore = Ext4.create('Ext.data.Store', storeConfig);
            this.comparisonGrid = Ext4.create('Ext.grid.Panel', Ext4.apply({title: 'Comparison Criteria', store: this.comparisonStore}, gridConfig));
            this.items = [this.baselineGrid, this.comparisonGrid]
            this.callParent();
        }
    });
});
