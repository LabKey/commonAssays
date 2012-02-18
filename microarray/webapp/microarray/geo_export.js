/*
 * Copyright (c) 2010-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
LABKEY.requiresScript("/extWidgets/GroupTabPanel.js");

Ext4.define('Microarray.GeoExportPanel', {
    extend: 'Ext.tab.Panel',
    DEFAULT_FIELD_WIDTH: 800,
    initComponent: function(){

        var items = [];
        items.push(this.getSeriesTab());
        items.push(this.getSamplesTab());
        items.push(this.getProtocolsTab());

        Ext4.apply(this, {
            itemId: 'geoExportPanel',
            deferredRender: false,
            defaults: {
                style: 'padding: 5px;'
            },
            style: 'padding: 5px;',
            items: items,
            buttonAlign: 'left',
            buttons: [{
                text: 'Save',
                itemId: 'saveBtn',
                scope: this,
                handler: this.onSave
            },{
                text: 'Cancel',
                handler: function(){
                    window.location = LABKEY.ActionURL.buildURL('project', 'start')
                }
            },{
                text: 'Copy From',
                menu: {
                    defaults: {
                        scope: this,
                        handler: function(btn){
                            Ext.Msg.confirm('Import', 'This will replace all records in the current form with the GEO export from the selected folder.  Do you want to do this?', onConfirm, this);

                            function onConfirm(input){
                                if(input != 'yes')
                                    return;

                                Ext4.Msg.wait('Loading...');
                                Ext4.create('LABKEY.ext4.Store', {
                                    schemaName: 'microarray',
                                    queryName: 'geo_properties',
                                    filterArray: [
                                        LABKEY.Filter.create('container', btn.record.get('container'), LABKEY.Filter.Types.EQUALS)
                                    ],
                                    autoLoad: true,
                                    listeners: {
                                        scope: this,
                                        load: function(store){
                                            var recordStore = this.store;
                                            console.log(recordStore);
//                                            store.each(function(rec){
//                                                console.log(rec)
//                                            }, this);

                                            Ext4.Msg.hide();
                                        },
                                        exception: function(){
                                            console.log('error')
                                            Ext4.Msg.hide();
                                        }
                                    }
                                });
                            }
                        }
                    },
                    items: [{
                        text: 'Loading...'
                    }]
                }
            },{
                text: 'Export'
            }]
        });

        this.callParent();

        Ext4.create('LABKEY.ext4.Store', {
            schemaName: 'microarray',
            sql: 'select distinct p.container as container from microarray.geo_properties p',
            autoLoad: true,
            listeners: {
                scope: this,
                load: function(store){
                    var toAdd = [];
                    store.each(function(rec){
                        console.log(rec.data);
                        toAdd.push({
                            text: rec.get('container'),
                            record: rec
                        });
                    }, this);

                    var menu = this.down('menu');
                    menu.removeAll();
                    menu.add(toAdd);
                }
            }
        });

        this.store = Ext4.create('LABKEY.ext4.Store', {
            schemaName: 'microarray',
            queryName: 'geo_properties',
            columns: '*',
            autoLoad: true,
            listeners: {
                scope: this,
                load: this.onStoreLoad,
                update: this.onUpdate,
                syncexception: this.onStoreException,
                synccomplete: this.onSyncComplete
            }
        });

    },

    onStoreLoad: function(store){
        //remove extra rows
        this.items.each(function(tab){
            tab.onStoreLoad();
        }, this);

        store.each(function(rec){
            var tab = this.down('#' + rec.get('category'));
            if(!tab){
                console.log('no tab: ' + rec.get('category'));
                return;
            }

            tab.applyRecord(this, rec);

        }, this);

        this.onSyncComplete();
    },

    getSamplesTab: function(){
        return {
            xtype: 'tabpanel',
            title: 'Samples',
            itemId: 'Samples',
            height: 200,
            items: [{
                xtype: 'panel',
                title: 'Run Selection',
                items: [{
                    xtype: 'textfield',
                    fieldLabel: 'Source Assay',
                    width: 400
                }]
            },{
                xtype: 'panel',
                title: 'Query',
                items: [{
                    xtype: 'textarea',
                    fieldLabel: 'Field'
                }]
            }],
            onStoreLoad: function(store){

            },
            applyRecord: function(panel, rec){

            }
        };
    },

    getBaseSeriesProtocolTab: function(){
        return {
            xtype: 'panel',
            defaults: {
                border: false
            },
            buttonAlign: 'left',
            buttons: [{
                text: 'Add New Field',
                handler: function(btn){
                    var panel = btn.up('#geoExportPanel');
                    var newRow = btn.up('panel').add(panel.generateRow({editable: true}));
                    newRow.items.getAt(0).focus(false, 50);

                }
            }],
            applyRecord: function(panel, rec){
                if(!this.rendered){
                    this.on('afterrender', function(){
                        this.applyRecord(panel, rec);
                    }, this, {single: true, delay: 100});
                    return;
                }

                if(!rec.get('prop_name')){
                    console.log('no prop name')
                    return;
                }

                var row = this.down('panel[itemId="' + rec.get('prop_name') + '"]');
                if(!row){
                    console.log('no row: ' + rec.get('prop_name'));

                    //NOTE: we probably need to create it
                    row = this.add(panel.generateRow({
                        name: rec.get('prop_name'),
                        //rowid: rec.get('rowid'),
                        //value: rec.get('value'),
                        editable: true
                    }));
                    //return;
                }

                row.down('#prop_name').setValue(rec.get('prop_name'));
                row.down('#rowid').setValue(rec.get('rowid'));
                row.down('#value').setValue(rec.get('value'));
            },
            onStoreLoad: function(store){
                this.items.each(function(row){
                    if(!row.down('button').hidden){
                        console.log('editable')
                        this.remove(row)
                    }
                }, this)
            }
        }
    },

    getSeriesTab: function(){
        var config = Ext4.apply(this.getBaseSeriesProtocolTab(), {
            itemId: 'Series',
            title: 'Series',
            items: []
        });

        config.items.push(this.generateRow('Title'));
        config.items.push(this.generateRow('Summary'));
        config.items.push(this.generateRow('Overall Design'));
        config.items.push(this.generateRow('Contributor'));

        return config;
    },

    getProtocolsTab: function(){
        var config = Ext4.apply(this.getBaseSeriesProtocolTab(), {
            title: 'Protocols',
            itemId: 'Protocols',
            items: []
        });

        config.items.push(this.generateRow('Growth Protocol'));
        config.items.push(this.generateRow('Treatment Protocol'));
        config.items.push(this.generateRow('Extract Protocol'));
        config.items.push(this.generateRow('Label Protocol'));
        config.items.push(this.generateRow('Scan Protocol'));
        config.items.push(this.generateRow('Data Protocol'));

        return config;
    },

    generateRow: function(config){
        var name;
        if(typeof config == 'string'){
            name = config;
            config = {};
        }
        else
            name = config.name;

        return {
            xtype: 'panel',
            layout: 'hbox',
            itemId: name,
            canEdit: config.editable,
            bodyStyle: 'padding: 5px;',
            defaults: {
                border: false
            },
            items: [{
                xtype: (config.editable ? 'textfield' : 'displayfield'),
                itemId: 'prop_name',
                width: 200,
                bodyStyle: 'padding-right: 5px;',
                allowBlank: false,
                value: name
            },{
                xtype: 'textarea',
                width: 800,
                bodyStyle: 'padding-right: 5px;',
                itemId: 'value',
                value: config.value
            },{
                xtype: 'hidden',
                itemId: 'rowid',
                value: config.rowid
            },{
                xtype: 'button',
                text: '[X]',
                hidden: !config.editable,
                //scope: this,
                handler: function(btn){
                    var row = btn.up('panel');
                    var tab = row.up('panel');
                    var outerPanel = tab.up('panel');

                    var rowid = row.down('#rowid').getValue();

                    if(rowid){
                        var recIdx = outerPanel.store.find('rowid', rowid);
                        if(recIdx > -1){
                            outerPanel.store.remove(outerPanel.store.getAt(recIdx));
                        }
                    }

                    tab.remove(row);
                }
            }]
        }
    },

    onSave: function(btn){
        btn.setDisabled(true);
        Ext4.Msg.wait("Saving...");

        var panel = btn.up('panel');
        var store = panel.store;

        var data;
        var hasError;
        panel.items.each(function(tab){
            if(tab.itemId != 'Samples'){
                tab.items.each(function(row){
                    data = {};

                    data.rowid = row.down('#rowid').getValue();
                    data.prop_name = row.down('#prop_name').getValue();
                    row.itemId = data.prop_name;
                    data.value = row.down('#value').getValue();
                    data.category = tab.itemId;

                    if(!data.prop_name){
                        alert('One or more records is missing a name on the ' + tab.title + ' tab');
                        hasError = true;
                        return;
                    }

                    var recIdx = store.find('rowid', data.rowid);
                    if(data.rowid && recIdx != -1){
                        store.getAt(recIdx).set(data);
                    }
                    else {
                        console.log('creating new model');
                        console.log(data)
                        store.add(store.model.create(data));
                    }
                }, this);
            }
        }, this);

console.log(store.getCount())
        if(!hasError)
            store.sync();
        else
            this.onSyncComplete();
    },

    onSyncComplete: function(){
        if(Ext4.Msg.isVisible())
            Ext4.Msg.hide();
        this.down('#saveBtn').setDisabled(false)
    },

    onUpdate: function(store, rec){
//        console.log('update');
//        console.log(rec);

        var tab = this.down('#' + rec.get('category'));
        tab.applyRecord(this, rec);
    },

    onStoreException: function(store, response, operation){
        if(Ext4.Msg.isVisible())
            Ext4.Msg.hide();
        this.down('#saveBtn').setDisabled(false)

        if(response.errors && response.errors.exception)
            alert(response.errors.exception);
        else
            alert('There was an error that prevented saving');

        console.log(response)
    }
});


