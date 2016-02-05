/*
 * Copyright (c) 2012-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('Microarray.GeoExportPanel', {
    extend: 'Ext.tab.Panel',
    initComponent: function(){

        var items = [
            this.getSeriesTab(),
            this.getSamplesTab(),
            this.getProtocolsTab(),
            this.getCommentsTab()
        ];

        Ext4.apply(this, {
            itemId: 'geoExportPanel',
            defaults: {
                bodyStyle: 'padding: 5px;'
            },
            //style: 'padding: 5px;',
            items: items,
            buttonAlign: 'left',
            buttons: [{
                text: 'Save',
                itemId: 'saveBtn',
                hidden: !LABKEY.Security.currentUser.canUpdate,
                scope: this,
                handler: this.onSave
            },{
                text: 'Cancel',
                handler: function(){
                    window.location = LABKEY.ActionURL.buildURL('project', 'start')
                }
            },{
                text: 'Copy From',
                hidden: !LABKEY.Security.currentUser.canUpdate,
                menu: {
                    defaults: {
                        scope: this,
                        handler: function(btn){
                            if(!btn.record)
                                return;

                            Ext4.Msg.confirm('Import', 'This will replace all records in the current form with the GEO export from the selected folder.  Do you want to do this?', onConfirm, this);

                            function onConfirm(input){
                                if(input != 'yes')
                                    return;

                                Ext4.Msg.wait('Loading...');

                                Ext4.create('LABKEY.ext4.Store', {
                                    schemaName: 'microarray',
                                    queryName: 'geo_properties',
                                    containerPath: btn.record.get('entityid'),
                                    filterArray: [
                                        //dont allow run IDs to be copied.  in theory these are container-specific anyway
                                        LABKEY.Filter.create('prop_name', 'run_ids', LABKEY.Filter.Types.NEQ)
                                    ],
                                    autoLoad: true,
                                    listeners: {
                                        scope: this,
                                        load: function(store){
                                            var recordStore = this.store;
                                            recordStore.removeAll();

                                            //reset all items
                                            this.items.each(function(tab){
                                                tab.onStoreLoad();
                                            }, this);

                                            store.each(function(rec){
                                                rec.set('rowid', null);
                                                this.applyRecord(rec);
                                            }, this);

                                            Ext4.Msg.hide();
                                        },
                                        exception: function(){
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
                text: 'Export GEO Template',
                //hidden: !LABKEY.Security.currentUser.canUpdate,
                scope: this,
                handler: this.doExport
                //NOTE: the Katze lab originally asked for a button to export raw data, but then decided not the add it.
                //this code is a start at implementing that feature
//            },{
//                text: 'Export Raw Data',
//                scope: this,
//                handler: function(btn){
//                    var panel = btn.up('panel');
//
//                    var sourceAssayField = panel.down('#sourceAssay');
//                    var sourceAssay = sourceAssayField.getValue();
//                    if(!sourceAssay){
//                        alert("You must choose an assay");
//                        return;
//                    }
//                    var assayRec = sourceAssayField.store.find('rowid', sourceAssay);
//
//                    var runIds = panel.down('#run_ids').getValue();
//                    if(!runIds){
//                        alert("You must choose one or more runs to include");
//                        return;
//                    }
//
//                    var form = new Ext.FormPanel({
//                        url: LABKEY.ActionURL.buildURL("experiment", "exportRunFiles"),
//                        method: 'GET',
//                        baseParams: {
//                            fileExportType: 'all',
//                            zipFileName: 'Exported Microarray Runs.zip',
//                            '.select': runIds.split(';'),
//                            exportType: 'BROWSER_DOWNLOAD',
//                            dataRegionSelectionKey: 'GEO_excel_selection' //this is completely made up.  however, the server requires that you supply something
//                        },
//                        fileUpload: true
//                    }).render(document.getElementsByTagName('body')[0]);
//                    form.getForm().submit();
//
//                }
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
                    store.each(function(rec){
                        rec.set('rowid', null);
                        toAdd.push({
                            text: rec.get('path'),
                            record: rec
                        });
                    }, this);

                    var menu = this.down('menu');
                    menu.removeAll();

                    if(!toAdd.length){
                        toAdd.push({
                            text: "No other exports have been saved"
                        });
                    }
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
                exception: this.onStoreException,
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
                        var combo = panel.down('#sourceAssay');
                        var store = combo.store;
                        var value = combo.getValue();
                        if(!combo.store.getCount()){
                            alert('No microarray assays have been defined in this folder');
                        }

                        combo.setValue(value);
                    }
                },
                bodyStyle: 'padding: 5px;',
                items: [{
                    xtype: 'labkey-combo',
                    fieldLabel: 'Source Assay',
                    width: 400,
                    queryMode: 'local',
                    itemId: 'sourceAssay',
                    disabled: !LABKEY.Security.currentUser.canUpdate,
                    displayField: 'Name',
                    valueField: 'RowId',
                    store: Ext4.create('LABKEY.ext4.Store', {
                        schemaName: 'assay',
                        queryName: 'AssayList',
                        columns: 'Name,RowId,Type',
                        filterArray: [
                            LABKEY.Filter.create('LSID', ':MicroarrayAssayProtocol.', LABKEY.Filter.Types.CONTAINS)
                        ],
                        autoLoad: true
                    }),
                    listeners: {
                        buffer: 50,
                        change: function(field, value){
                            var panel = field.up('#Samples').down('#chooseRunsPanel');
                            var recIdx = field.store.find('RowId', value);
                            var sqlPanel = panel.up('#Samples');

                            //remove the grid of runs
                            var grid = sqlPanel.down('#resultGrid');
                            if(grid)
                                sqlPanel.remove(grid);

                            if(recIdx > -1){
                                panel.addGridPanel(field.store.getAt(recIdx).get('Name'));
                            }
                            else {
                                var runGrid = panel.down('#runsGrid');
                                if(runGrid){
                                    panel.remove(runGrid);
                                }

                                if(!field.store.getCount()){
                                    field.clearManagedListeners();
                                    field.mon(field.store, 'datachanged', function(store){
                                        field.fireEvent('change', field, value);
                                    }, this, {single: true});
                                }
                                else {
                                    if(value){
                                        alert('Unable to find assay with ID: ' + value + '.  The source assay will be reset.');
                                        field.setValue(null);
                                    }
                                    sqlPanel.down('#run_ids').setValue(null);
                                }

                                sqlPanel.doLayout();
                            }
                        },
                        select: function(field, record){
                            var panel = field.up('#Samples').down('#chooseRunsPanel');
                            panel.up('#Samples').down('#run_ids').setValue(null);
                        }
                    }
                },{
                    xtype: 'labkey-combo',
                    fieldLabel: 'Sample Set',
                    width: 400,
                    queryMode: 'local',
                    itemId: 'sourceSampleSet',
                    disabled: !LABKEY.Security.currentUser.canUpdate,
                    displayField: 'Name',
                    valueField: 'Name',
                    store: Ext4.create('LABKEY.ext4.Store', {
                        schemaName: 'exp',
                        queryName: 'SampleSets',
                        autoLoad: true,
                        listeners: {
                            scope: this,
                            exception: LABKEY.Utils.onError,
                            delay: 100, //
                            load: function(store){
                                var active;
                                var field = this.down('#sourceSampleSet');
                                if(!field.getValue() && store.getCount()){
                                    store.each(function(r){
                                        if(r.get('Active')){
                                            field.setValue(r.get('Name'));
                                        }
                                    }, this);
                                }
                            }
                        }
                    })
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
                        disabled: !LABKEY.Security.currentUser.canUpdate,
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
                                    samplePanel.down('#run_ids').setValue(values.join(';'));
                                    samplePanel.doLayout();
                                }
                            }
                        }),
                        listeners: {
                            afterrender: function(grid){
                                var panel = grid.up('panel').up('panel');
                                var run_ids = panel.down('#run_ids').getValue();
                                if(run_ids){
                                    panel.setRunIds(run_ids);
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
                                hidden: false,
                                columnConfig: {
                                    width: 220,
                                    showLink: false
                                }
                            }
                        }),
                        minHeight: 400,
                        width: '100%',
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
                    //disabled: !LABKEY.Security.currentUser.canUpdate,
                    border: false,
                    width: 1000,
                    items: [{
                        xtype: 'textarea',
                        itemId: 'custom_sql',
                        fieldLabel: 'Custom SQL',
                        disabled: !LABKEY.Security.currentUser.canUpdate,
                        labelAlign: 'top',
                        width: 1000,
                        height: 300
//                    },{
//                        xtype: 'textfield',
//                        itemId: 'run_field_name',
//                        fieldLabel: 'Field Containing Run ID',
//                        labelAlign: 'top',
//                        value: 'Run/RowId'
                    },{
                        border: false,
                        style: 'padding: 5px;',
                        html: 'NOTE: In order to filter you custom query to show only the selected run IDs you must include the substitution ${RUN_IDS} in your SQL.  This will automtatically be converted to contain the Run IDs.  Other supported substitutions include:' +
                            '<br>${ASSAY_NAME}, which will match the name of the assay, chosen using the dropdown menu above' +
                            '<br>${SAMPLE_SET_NAME}, which will match the name of the sample set, chosen using the dropdown above' +
                            '<br><br>An example SQL statement could look like: <br><br>' +
                            'SELECT r.RowId, s.Study, r.Name <br>FROM assay."${ASSAY_NAME} Data" r <br>LEFT JOIN samples."${SAMPLE_SET_NAME}" s ON (s.RowId = r.SampleId)<br>WHERE r.Run.RowId IN (${RUN_IDS})'
                    },{
                        xtype: 'displayfield',
                        itemId: 'run_ids',
                        labelAlign: 'top',
                        hidden: true,
                        listeners: {
                            change: function(btn){
                                var samplePanel = btn.up('#Samples');
                                var grid = samplePanel.down('#resultGrid');
                                if(grid){
                                    grid.store.removeAll();
                                    samplePanel.remove(grid);
                                }
                            }
                        }
                    }],
                    buttonAlign: 'left',
                    buttons: [{
                        xype: 'button',
                        text: 'Preview Results',
                        disabled: !LABKEY.Security.currentUser.canUpdate,
                        scope: this,
                        handler: this.previewResults
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
                {
                    var val = rec.get('value');
                    if(val && rec.get('prop_name') == 'sourceAssay')
                        val = parseInt(val);

                    field.setValue(val);
                }
                else {
                    console.log('not found: ' + rec.get('prop_name'));
                }

                if(rec.get('prop_name') == 'run_ids'){
                    this.setRunIds(rec.get('value'));
                }
            },
            onSave: function(panel, store){
                var field;
                var value;
                var recordIdx;
                var record;
                var fields = ['sourceAssay', 'sourceSampleSet', 'custom_sql', 'run_ids'];
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
            setRunIds: function(ids){
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
                            me.setRunIds(ids);
                        }, this, {single: true});
                    }
                    return;
                }

                if(!grid.store || grid.store.loading){
                    this.mon(grid.store, 'load', function(){
                        me.setRunIds(ids);
                    }, this, {single: true, delay: 100});
                    return;
                }

                if(!grid.getView().viewReady){
                    grid.getView().on('viewready', function(view){
                        this.setRunIds(ids);
                    }, this, {single: true});
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

    generateSql: function(){
        var samplePanel = this.down('#Samples');

        var assayField = samplePanel.down('#sourceAssay');
        if(!assayField.getValue()){
            alert("You must choose an assay");
            return;
        }

        var assayName = assayField.store.getById(assayField.getValue()).get('Name');

        var sql = samplePanel.down('#custom_sql').getValue();
        if(!sql){
            alert('Must enter a SQL statement to return the results');
            return;
        }

        var runIds = samplePanel.down('#run_ids').getValue();
        if(!runIds){
            alert("You must choose one or more runs to include");
            return;
        }
        runIds = runIds.split(';');

        var re = new RegExp(/\${RUN_IDS}/i);
        if(!sql.match(re)){
            alert('SQL does not contain ${RUN_IDS}');
            return;
        }
        sql = sql.replace(re, runIds.join(","));

        re = new RegExp(/\${ASSAY_NAME}/i);
        if(sql.match(re) && !assayName){
            alert('SQL contain ${ASSAY_NAME}, but no assay was selected');
            return;
        }
        sql = sql.replace(re, assayName);

        re = new RegExp(/\${SAMPLE_SET_NAME}/i);
        var sampleSet = samplePanel.down('#sourceSampleSet').getValue();
        if(sql.match(re) && !sampleSet){
            alert('SQL contain ${SAMPLE_SET_NAME}, but no sample set was selected');
            return;
        }
        sql = sql.replace(re, sampleSet);

        return sql;
    },

    previewResults: function(){
        var samplePanel = this.down('#Samples');

        var grid = samplePanel.down('#resultGrid');
        if(grid)
            samplePanel.remove(grid);

        var sql = this.generateSql();
        if(!sql){
            return
        }

        var storeCfg = {
            schemaName: 'assay',
            sql: sql,
            autoLoad: true,
            metadataDefaults: {
                fixedWidthColumn: true,
                columnConfig: {
                    width: 220
                }
            },
            listeners: {
                scope: this,
                load: function(store, records, success){
                    if(!success){
                        return;
                    }

                    if(!store.getCount()){
                        alert('No records returned');
                        return;
                    }

                    if(!samplePanel.rendered || !samplePanel.isVisible()){
                        samplePanel.on('activate', function(tab){
                            store.fireEvent('load', store);
                        }, this);
                    }
                    else {
                        if(!samplePanel.down('#resultGrid')){
                            samplePanel.add({
                                xtype: 'labkey-gridpanel',
                                itemId: 'resultGrid',
                                title: 'Step 4: Preview Results',
                                disabled: !LABKEY.Security.currentUser.canUpdate,
                                store: store,
                                width: '100%',
                                //forceFit: true,
                                editable: false,
                                listeners: {
                                    scope: this,
                                    beforeload: function(operation){
                                        var grid = this.down('#resultGrid');
                                        var store = grid.store;
                                        var sql = this.down('#custom_sql').getValue();

                                        if(sql){
                                            store.sql = sql;
                                            operation.sql = sql;
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
        };

        samplePanel.resultStore = Ext4.create('LABKEY.ext4.Store', storeCfg);

    },

    getBaseSeriesProtocolTab: function(){
        return {
            xtype: 'panel',
            deferredRender: false,
            defaults: {
                border: false
            },
            style: 'padding-bottom: 10px;',
            buttonAlign: 'left',
            buttons: [{
                text: 'Add New Field',
                disabled: !LABKEY.Security.currentUser.canUpdate,
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

                var recordsToAdd = [];
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
                        return false;
                    }

                    var recIdx = store.find('rowid', data.rowid);
                    if(data.rowid && recIdx != -1){
                        store.getAt(recIdx).set(data);
                    }
                    else {
                        //check for duplicates
                        var dupIdx = store.findBy(function(rec){
                            return rec.get('category') == data.category && rec.get('prop_name').toLowerCase() == data.prop_name.toLowerCase();
                        }, this);

                        if(dupIdx > -1){
                            hasError = true;
                            alert('Error: you cannot have duplicate property names (' + data.prop_name + '). The form was not saved.');
                            return false;
                        }
                        recordsToAdd.push(store.model.create(data));
                    }
                }, this);

                if(!hasError)
                    store.add(recordsToAdd);

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
                value: name,
                disabled: !LABKEY.Security.currentUser.canUpdate,
                listeners: {
                    scope: this,
                    //delay: 100,
                    blur: function(field){
                        var val = field.getValue();
                        if(Ext4.isEmpty(val))
                            return;

                        val = Ext4.String.trim(val);
                        var panel = field.up('panel').up('panel');

                        var prop_name_field;
                        var field_value;
                        var error;
                        panel.items.each(function(row){
                            prop_name_field = row.down('#prop_name');
                            if(prop_name_field == field)
                                return;
                            field_value = prop_name_field.getValue();

                            if(!field_value)
                                return;
                            field_value = Ext4.String.trim(field_value.toString().toLowerCase());

                            if(field_value == val.toLowerCase()){
                                alert('The name ' + val + ' is already in use');
                                field.reset();
                            }
                        }, this);
                    }
                }
            },{
                xtype: 'textarea',
                width: 800,
                itemId: 'value',
                value: config.value,
                disabled: !LABKEY.Security.currentUser.canUpdate
            },{
                xtype: 'hidden',
                itemId: 'rowid',
                value: config.rowid,
                disabled: !LABKEY.Security.currentUser.canUpdate
            },{
                xtype: 'button',
                itemId: 'removeBtn',
                text: 'Remove',
                hidden: !config.editable,
                disabled: !LABKEY.Security.currentUser.canUpdate,
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
            configObj.items[1].xtype = 'microarray-contributorfield';
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

        store.un('synccomplete', this.exportAfterSave, this);
    },

    doExport: function(){
        var store = this.store;

        store.on('synccomplete', function(store){
            this.exportAfterSave();
        }, this, {single: true});

        //find the result store, create if needed, and always reload
        var resultStore = this.down('#Samples').resultStore;
        if(!resultStore){
            this.previewResults();
            resultStore = this.down('#Samples').resultStore;
        }

        if(!resultStore){
            return;  //for this to happen, there must have been an error such as missing SQL or no run IDs
        }

        var sql = this.generateSql();
        if(!sql){
            return;
        }
        resultStore.sql = sql;
        resultStore.load();

        var saveBtn = this.down('#saveBtn');
        this.onSave(saveBtn);
    },

    exportAfterSave: function(){
        var sections = {
            Series: [['SERIES'], ['# This section describes the overall experiment.']],
            Samples: [['SAMPLES'], ['# 2 versions of each Agilent platform are represented in GEO. Include the Accession Number of the "Feature Number" version in the platform column.']],
            Protocols: [['PROTOCOLS'], ['# Protocols which are applicable to specific Samples or specific channels can be included in additional columns of the SAMPLES section instead.']]
        };

        //get sample info:
        var resultStore = this.down('#Samples').resultStore;
        if(resultStore.isLoading()){
            resultStore.on('load', function(store, records, success){
                if(success)
                    this.exportAfterSave();
            }, this, {single: true});
            return;
        }

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
                val = Ext4.isDefined(val) ? LABKEY.ext4.Util.getDisplayString(val, field, rec, resultStore) : '';
                row.push(val);
            }, this);
            sections.Samples.push(row);
        }, this);

        //handle other fields
        var store = this.store;
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

        var data = [['# Use this template for a Agilent one-color experiment submission.']].concat(sections.Series).concat(sections.Samples).concat(sections.Protocols)
        //NOTE: this causes a page navigation, which can disrupt the
        LABKEY.Utils.convertToExcel.defer(50, this, [{
	        fileName: 'GEO_Export.xls',
	        sheets: [{
                name: 'sheet1',
                data: data
            }]
        }]);
    }
});

Ext4.define('Microarray.ContributorField', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.microarray-contributorfield',
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
                disabled: !LABKEY.Security.currentUser.canUpdate,
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
                    disabled: !LABKEY.Security.currentUser.canUpdate,
                    width: 800,
                    value: item
                });
            });
        }
        else {
            toAdd.push({
                xtype: 'textfield',
                disabled: !LABKEY.Security.currentUser.canUpdate,
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

