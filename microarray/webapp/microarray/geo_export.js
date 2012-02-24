/*
 * Copyright (c) 2010-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('Microarray.GeoExportPanel', {
    extend: 'Ext.tab.Panel',
    initComponent: function(){

        var items = [];
        items.push(this.getSeriesTab());
        items.push(this.getSamplesTab());
        items.push(this.getProtocolsTab());
        items.push(this.getCommentsTab());

        Ext4.apply(this, {
            itemId: 'geoExportPanel',
            defaults: {
                bodyStyle: 'padding: 5px;'
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
                                    containerPath: btn.record.get('entityid'),
                                    autoLoad: true,
                                    listeners: {
                                        scope: this,
                                        load: function(store){
                                            var recordStore = this.store;
                                            recordStore.each(function(rec){
                                                recordStore.remove(rec);
                                            }, this);

                                            store.each(function(rec){
                                                rec.set('rowid', null);
                                                this.applyRecord(rec);
//                                                recordStore.add(recordStore.model.create({
//                                                    category: rec.get('category'),
//                                                    prop_name: rec.get('prop_name'),
//                                                    value: rec.get('value')
//                                                }));
                                            }, this);

                                            //recordStore.fireEvent('load', recordStore);

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
                text: 'Export',
                handler: this.doExport
            }]
        });

        this.callParent();

        var sql = "select distinct p.container.path as path, p.container.entityid as entityid from microarray.geo_properties p where p.container != '"+LABKEY.container.id+"'";
        Ext4.create('LABKEY.ext4.Store', {
            schemaName: 'microarray',
            sql: sql,
            containerFilter: 'AllFolders',
            autoLoad: true,
            listeners: {
                scope: this,
                load: function(store){
                    var toAdd = [];
                    var distinct = {};
                    store.each(function(rec){
                        //HACK: if using workbooks, the containerFilter AllFolders can currently result in duplicates
                        //this is a crude workaround
//                        if(!distinct[rec.get('path')]){
                            rec.set('rowid', null);
                            toAdd.push({
                                text: rec.get('path'),
                                record: rec
                            });
                            distinct[rec.get('path')] = 1;
//                        }
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
            this.applyRecord(rec);
        }, this);

        this.onSyncComplete();
    },

    applyRecord: function(rec){
        var tab = this.down('#' + rec.get('category'));
        if(!tab){
            console.log('no tab: ' + rec.get('category'));
            return;
        }

        tab.applyRecord(this, rec);
    },

    getSamplesTab: function(){
        return {
            xtype: 'panel',
            title: 'Samples',
            itemId: 'Samples',
            deferredRender: true, //important!
            defaults: {
                style: 'padding-bottom: 20px;'
            },
            items: [{
                xtype: 'panel',
                title: 'Step 1: Select Assay',
                itemId: 'selectRuns',
                listeners: {
                    beforerender: function(panel){
                        LABKEY.Assay.getByType({
                            type: 'Microarray',
                            failure: LABKEY.Utils.onError,
                            success: function(result){
                                var combo = panel.down('#sourceAssay');
                                var store = combo.store;
                                store.removeAt(0);
                                var value = combo.getValue();
                                Ext4.each(result, function(assay){
                                    store.add(store.model.create({
                                        name: assay.name,
                                        rowid: assay.id
                                    }));
                                }, this);

                                if(!store.getCount()){
                                    store.add(store.model.create({
                                        name: 'No Microassay Assays Defined'
                                    }));
                                }

                                if(value){
                                    combo.setValue(value);
                                }
                                store.fireEvent('datachanged', store);

                            }
                        });
                    }
                },
                bodyStyle: 'padding: 5px;',
                items: [{
                    xtype: 'combo',
                    fieldLabel: 'Source Assay',
                    width: 400,
                    queryMode: 'local',
                    itemId: 'sourceAssay',
                    displayField: 'name',
                    valueField: 'rowid',
                    store: Ext4.create('Ext.data.Store', {
                        fields: ['name', 'rowid'],
                        data: [{
                            name: 'Loading...'
                        }]
                    }),
                    listeners: {
                        change: function(field, value){
                            var panel = field.up('#Samples').down('#chooseRunsPanel');
                            var recIdx = field.store.find('rowid', value);
                            if(recIdx > -1){
                                //clean up the other panel
                                var sqlPanel = panel.up('#Samples');
                                var grid = sqlPanel.down('#resultGrid');
                                if(grid)
                                    sqlPanel.remove(grid);

                                panel.addGridPanel(field.store.getAt(recIdx).get('name'));
                            }
                        }
                    }
                }]
            },{
                xtype: 'panel',
                title: 'Step 2: Choose Runs',
                itemId: 'chooseRunsPanel',
                //items: [],
                addGridPanel: function(name){
                    var grid = this.down('#runsGrid');

                    if(grid)
                        this.remove(grid);

                    this.add({
                        xtype: 'labkey-gridpanel',
                        itemId: 'runsGrid',
                        //multiSelect: true,
                        selModel: Ext4.create('Ext.selection.CheckboxModel', {
                            //xtype: 'checkboxmodel',
                            injectCheckbox: 'first',
                            listeners: {
                                scope: this,
                                selectionchange: function(model, records){
                                    var values = [];
                                    var name = model.store.getCanonicalFieldName('rowid');
                                    Ext4.each(records, function(r){
                                        values.push(r.get(name));
                                    }, this);

                                    var samplePanel = this.up('#Samples');
                                    samplePanel.down('#sample_ids').setValue(values.join(';'));
                                    samplePanel.doLayout();
                                }
                            }
                        }),
                        listeners: {
                            render: function(grid){
                                var panel = grid.up('panel').up('panel');
                                var sample_ids = panel.down('#sample_ids').getValue();
                                if(sample_ids){
                                    panel.setSampleIds(sample_ids);
                                }
                            }
                        },
                        store: Ext4.create('LABKEY.ext4.Store', {
                            schemaName: 'assay',
                            queryName: name + ' Runs',
                            columns: 'rowid,name,created,createdby',
                            autoLoad: true,
                            metadataDefaults: {
                                fixedWidthColumn: true,
                                hidden: false
                            },
                            metadata: {
//                                Name: {
//                                    columnConfig: {
//                                        width: 300
//                                    }
//                                },
//                                RunStringField: {
//                                    isAutoExpandColumn: true,
//                                    columnConfig: {
//                                        width: 650
//                                    }
//                                },
//                                Created: {
//                                    columnConfig: {
//                                        width: 200
//                                    }
//                                },
//                                CreatedBy: {
//                                    columnConfig: {
//                                        width: 150
//                                    }
//                                }
                            }
                        }),
                        minHeight: 400,
                        width: '100%',
                        //forceFit: true,
                        editable: false
                    });
                }
            },{
                xtype: 'panel',
                itemId: 'queryPanel',
                title: 'Step 3: Choose Columns',
                bodyStyle: 'padding: 5px;',
                items: [{
                    xtype: 'panel',
                    border: false,
                    width: 1000,
                    items: [{
                        xtype: 'textarea',
                        itemId: 'custom_sql',
                        fieldLabel: 'Custom SQL',
                        labelAlign: 'top',
                        width: 1000,
                        height: 300
                    },{
                        xtype: 'textfield',
                        itemId: 'run_field_name',
                        fieldLabel: 'Field Containing Run ID',
                        labelAlign: 'top',
                        value: 'Run/RowId'
                    },{
                        xtype: 'displayfield',
                        itemId: 'sample_ids',
                        fieldLabel: 'The query will be filtered to show only the following Run Ids.  If blank, all runs will be included',
                        labelAlign: 'top'
                        //hidden: true
                    }],
                    buttonAlign: 'left',
                    buttons: [{
                        xype: 'button',
                        text: 'Preview Results',
                        handler: function(btn){
                            var panel = btn.up('panel');
                            var sql = panel.down('#custom_sql').getValue();

                            if(!sql){
                                alert('Must enter a query');
                                return
                            }

                            var target = panel.up('#Samples');

                            var grid = target.down('#resultGrid');
                            if(grid)
                                target.remove(grid);

                            var runIds = target.down('#sample_ids').getValue();

                            var runField = target.down('#run_field_name').getValue();
                            if(runIds && !runField){
                                alert("You must supply the name of the field containing the Run's RowId");
                                return;
                            }

                            var storeCfg = {
                                schemaName: 'assay',
                                sql: sql,
                                autoLoad: true,
                                metadataDefaults: {
                                    fixedWidthColumn: true,
                                    columnConfig: {
                                        width: 175
                                    }
                                },
                                metadata: {
                                    Name: {
                                        columnConfig: {
                                            width: 300
                                        }
                                    },
                                    RunStringField: {
                                        isAutoExpandColumn: true,
                                        columnConfig: {
                                            width: 650
                                        }
                                    },
                                    Created: {
                                        columnConfig: {
                                            width: 200
                                        }
                                    },
                                    CreatedBy: {
                                        columnConfig: {
                                            width: 150
                                        }
                                    }
                                },
                                listeners: {
                                    scope: this,
                                    beforemetachange: function(store, meta){
                                        Ext4.each(meta.fields, function(field){
                                            if(field.name == runField){
                                                console.log(field)
                                            }
                                        }, this);
                                    },
                                    load: function(store){
                                        if(!store.getCount()){
                                            alert('No records returned');
                                            return;
                                        }

                                        var samplePanel = panel.up('panel').up('panel');

//                                        //add the sample IDs
//                                        var sampleField = samplePanel.down('#sample_ids');
//                                        var sampleIDs = [];
//                                        var name = store.getCanonicalFieldName('rowid');
//                                        store.each(function(rec){
//                                            sampleIDs.push(rec.get(name));
//                                        }, this);
//
//                                        sampleField.setValue(sampleIDs.join(';'));

                                        target.add({
                                            xtype: 'labkey-gridpanel',
                                            itemId: 'resultGrid',
                                            title: 'Step 4: Preview Results',
                                            store: store,
                                            width: '100%',
                                            //forceFit: true,
                                            editable: false
                                        });
                                    },
                                    exception: function(error){
                                        console.log('exception');
                                        console.log(error);
                                    }
                                }
                            };

                            if(runIds){
                                storeCfg.filterArray = [
                                    LABKEY.Filter.create(runField, runIds, LABKEY.Filter.Types.EQUALS_ONE_OF)
                                ]
                            }
                            Ext4.create('LABKEY.ext4.Store', storeCfg);

                        }
                    }]
                }]
            }],
            onStoreLoad: function(store){
                this.items.each(function(tab){
                    tab.items.each(function(item){
                        if(item.reset)
                            item.reset();

                    });
                }, this);
            },
            applyRecord: function(panel, rec){
                var field = this.down('#' + rec.get('prop_name'));
                if(field && field.setValue)
                    field.setValue(rec.get('value'));
                else {
                    console.log('not found: ' + rec.get('prop_name'));
                }

                if(rec.get('prop_name') == 'sample_ids'){
                    this.setSampleIds(rec.get('value'));
                }
            },
            onSave: function(panel, store){
                var field;
                var value;
                var recordIdx;
                var record;
                var fields = ['sourceAssay', 'custom_sql', 'sample_ids', 'run_field_name'];
                Ext4.each(fields, function(prop_name){
                    field = this.down('#' + prop_name);
                    value = field.getValue();

                    recordIdx = store.findBy(function(rec){
                        return rec.get('prop_name') == prop_name && rec.get('category') == 'Samples';
                    }, this);

                    if(recordIdx > -1 && !value){
                        store.removeAt(recordIdx);
                    }
                    else if(recordIdx == -1 && value){
                        store.add(store.model.create({
                            category: 'Samples',
                            prop_name: prop_name,
                            value: value
                        }));
                    }
                    else if(recordIdx > -1 && value){
                        record = store.getAt(recordIdx);
                        record.set({
                            category: 'Samples',
                            prop_name: prop_name,
                            value: value
                        });
                    }
                }, this);
            },
            setSampleIds: function(ids){
                var me = this;
                if(!ids)
                    return;

                var grid = this.down('#runsGrid');

                if(!grid){
                    if(this.isVisible()){
                        this.on('activate', function(tab){
                            var assayField = this.down('#sourceAssay');
                            if(assayField.getValue()){
                                var rec = assayField.store.find('id', assayField.getValue());
                                assayField.fireEvent('change', assayField, assayField.getValue());
                            }
                            me.setSampleIds(ids);
                        }, this, {single: true});
                    }
                    return;
                }

                if(!grid.store || !grid.store.hasLoaded()){
                    this.mon(grid.store, 'load', function(){
                        me.setSampleIds(ids);
                    }, this, {single: true, delay: 100});
                    return;
                }

                ids = ids.split(';');

                var recIdx;
                var name = grid.store.getCanonicalFieldName('rowid');
                Ext4.each(ids, function(id){
                    recIdx = grid.store.find(name, id);
                    if(recIdx > -1){
                        grid.getSelectionModel().select(recIdx, true);
                    }
                }, this);
            }
        };
    },

    getBaseSeriesProtocolTab: function(){
        return {
            xtype: 'panel',
            deferredRender: false,
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
                if(!rec.get('prop_name')){
                    console.log('no prop name');
                    return;
                }

                var row = this.down('panel[itemId="' + rec.get('prop_name') + '"]');
                if(!row){
                    row = this.add(panel.generateRow({
                        name: rec.get('prop_name'),
                        rowid: rec.get('rowid'),
                        value: rec.get('value'),
                        editable: true
                    }));
                }

                row.down('#prop_name').setValue(rec.get('prop_name'));
                row.down('#rowid').setValue(rec.get('rowid'));
                row.down('#value').setValue(rec.get('value'));
            },
            onStoreLoad: function(store){
                this.items.each(function(row){
                    if(!row.down('#removeBtn').hidden){
                        this.remove(row);
                    }
                    else {
                        row.items.each(function(item){
                            if(item.reset)
                                item.reset();
                        }, this);
                    }
                }, this);
            },
            onSave: function(panel, store){
                var data;
                var hasError;
                this.items.each(function(row){
                    data = {};

                    data.rowid = row.down('#rowid').getValue();
                    data.prop_name = row.down('#prop_name').getValue();
                    row.itemId = data.prop_name;
                    data.value = row.down('#value').getValue();
                    data.category = this.itemId;

                    if(!data.prop_name){
                        alert('One or more records is missing a name on the ' + this.title + ' tab');
                        hasError = true;
                        return;
                    }

                    var recIdx = store.find('rowid', data.rowid);
                    if(data.rowid && recIdx != -1){
                        store.getAt(recIdx).set(data);
                    }
                    else {
                        store.add(store.model.create(data));
                    }
                }, this);

                return hasError;
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

    getCommentsTab: function(){
        var config = Ext4.apply(this.getBaseSeriesProtocolTab(), {
            title: 'Comments',
            itemId: 'Comments',
            items: []
        });

        config.items.push(this.generateRow('Accession'));
        config.items.push(this.generateRow('Comments'));

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

        var configObj = {
            xtype: 'panel',
            layout: 'hbox',
            itemId: name,
            canEdit: config.editable,
            bodyStyle: 'padding: 5px;',
            defaults: {
                border: false,
                style: 'margin-right: 5px;'
            },
            items: [{
                xtype: (config.editable ? 'textfield' : 'displayfield'),
                itemId: 'prop_name',
                width: 200,
                //style: 'margin-right: 5px;',
                allowBlank: false,
                value: name
            },{
                xtype: 'textarea',
                width: 800,
                itemId: 'value',
                value: config.value
            },{
                xtype: 'hidden',
                itemId: 'rowid',
                value: config.rowid
            },{
                xtype: 'button',
                itemId: 'removeBtn',
                text: '[X]',
                hidden: !config.editable,
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
        };

        if(name == 'Contributor'){
            configObj.items[1].xtype = 'labkey-contributorfield';
        }

        return configObj;
    },

    onSave: function(btn){
        btn.setDisabled(true);
        Ext4.Msg.wait("Saving...");

        var panel = btn.up('panel');
        var store = panel.store;

        var data;
        var hasError;
        panel.items.each(function(tab){
            if(tab.onSave(panel, store) === true){
                hasError = true;
                return false;
            }
        }, this);

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
        var tab = this.down('#' + rec.get('category'));
        tab.applyRecord(this, rec);
    },

    onStoreException: function(store, response, operation){
        if(Ext4.Msg.isVisible())
            Ext4.Msg.hide();
        this.down('#saveBtn').setDisabled(false);

        if(response.errors && response.errors.exception)
            alert(response.errors.exception);
        else
            alert('There was an error that prevented saving');

        console.log(response)
    },

    doExport: function(btn){
        var panel = btn.up('panel')
        var store = panel.store;

        var sections = {
            Series: [['SERIES'], ['# This section describes the overall experiment.']],
            Samples: [['SAMPLES'], ['# 2 versions of each Agilent platform are represented in GEO. Include the Accession Number of the "Feature Number" version in the platform column.']],
            Protocols: [['PROTOCOLS'], ['# Protocols which are applicable to specific Samples or specific channels can be included in additional columns of the SAMPLES section instead.']]
        };

        if(store.getCount()){
            store.each(function(rec){
                if(rec.get('category') == 'Comments')
                    return;

                if(rec.get('category') != 'Samples'){
                    if(rec.get('prop_name') == 'Contributor' && rec.get('category') == 'Series'){
                        var value = rec.get('value').split('\n');
                        var label;
                        Ext4.each(value, function(item, idx){
                            if(idx == 0)
                                label = rec.get('prop_name');
                            else
                                label = '';

                            sections[rec.get('category')].push([label, item]);

                        }, this);
                    }
                    else {
                        sections[rec.get('category')].push([rec.get('prop_name'), rec.get('value')]);
                    }
                }
            });
        }

        //get sample info:
        var resultGrid = panel.down('#resultGrid');
        if(!resultGrid || !resultGrid.store){
            alert('No results have been loaded.  Please click \'preview results\' on the Samples tab');
            return;
        }

        var resultStore = resultGrid.store;
        var fields = resultStore.getFields();
        var row = [];
        fields.each(function(field){
            row.push(field.name);
        }, this);
        sections.Samples.push(row);

        var val;
        resultStore.each(function(rec){
            row = [];
            fields.each(function(field){
                val = rec.get(field.name);
                val = Ext4.isDefined(val) ? val : '';
                row.push(val);
            }, this);
            sections.Samples.push(row);
        }, this);

        var data = [['# Use this template for a Agilent one-color experiment submission.']].concat(sections.Series).concat(sections.Samples).concat(sections.Protocols)
        LABKEY.Utils.convertToExcel({
	        fileName: 'GEO_Export.xls',
	        sheets: [{
                name: 'sheet1',
                data: data
            }]
        });
    }
});

Ext4.define('LABKEY.ext4.ContributorField', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.labkey-contributorfield',
    initComponent: function(){
        Ext4.apply(this, {
            border: false,
            defaults: {
                border: false,
                xtype: 'textfield'
            },
            buttonAlign: 'left',
            buttons: [{
                text: 'Add Contributor',
                scope: this,
                handler: function(btn){
                    btn.up('panel').add({
                        xtype: 'textfield',
                        width: 800
                    })
                }
            }]
        });

        this.callParent();
        this.setValue();
    },
    setValue: function(val){
        this.removeAll();

        var toAdd = [];

        if(val){
            val = val.split('\n');

            Ext4.each(val, function(item){
                toAdd.push({
                    xtype: 'textfield',
                    width: 800,
                    value: item
                });
            });
        }
        else {
            toAdd.push({
                xtype: 'textfield',
                width: 800
            })
        }

        this.add(toAdd);
    },
    getValue: function(){
        var values = [];
        this.items.each(function(item){
            if(item.getValue())
                values.push(item.getValue());
        }, this);

        return values.join('\n');
    }

});

