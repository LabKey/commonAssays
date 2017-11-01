/*
 * Copyright (c) 2011-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
* User: cnathe
* Date: Sept 19, 2011
*/

/**
 * Class to create a panel for selecting the graphing parameters for the Levey-Jennings trending report
 *
 * @params titration
 * @params assayName (protocol)
 */
LABKEY.LeveyJenningsGraphParamsPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.controlName || config.controlName == "null")
        {
            throw "You must specify a controlName!";
        }
        if (!config.controlType || config.controlType == "null")
        {
            throw "You must specify a controlType!";
        }
        if (!config.assayName || config.assayName == "null")
        {
            throw "You must specify a assayName!";
        }

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            padding: 15,
            items: [],
            title: 'Choose Graph Parameters',
            bodyStyle: 'background-color:#EEEEEE',
            labelAlign: 'top',
            width: 225,
            height: 513,
            border: true,
            cls: 'extContainer'
        });

        this.addEvents('applyGraphBtnClicked', 'graphParamsChanged');

        LABKEY.LeveyJenningsGraphParamsPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        this.paramsToLoad = 3;
        var items = [];

        // need to distinguish between null analyte/isotype/conjugate on URL and not requested (i.e. not on URL)
        if (LABKEY.ActionURL.getParameter("analyte") != undefined)
        {
            this.analyte = LABKEY.ActionURL.getParameter("analyte");
        }
        if (LABKEY.ActionURL.getParameter("isotype") != undefined)
        {
            this.isotype = LABKEY.ActionURL.getParameter("isotype");
        }
        if (LABKEY.ActionURL.getParameter("conjugate") != undefined)
        {
            this.conjugate = LABKEY.ActionURL.getParameter("conjugate");
        }

        // add grid panel element for selection of the antigen/analyte
        this.analyteGrid = new Ext.grid.GridPanel({
            id: 'analyte-grid-panel',
            fieldLabel: 'Antigens',
            disabled: true,
            height: 225,
            border: true,
            frame: false,
            hideHeaders: true,
            viewConfig: {forceFit: true},
            selModel: new Ext.grid.RowSelectionModel({singleSelect: true}),
            store: new Ext.data.ArrayStore({fields: ['value', 'display']}),
            columns: [{header: '', dataIndex:'value', renderer: this.tooltipRenderer}],
            listeners: {
                scope: this,
                'rowClick': function(grid, rowIndex) {
                    if (grid.getSelectionModel().hasSelection())
                    {
                        this.analyte = grid.getSelectionModel().getSelected().get("value");
                    }
                    else
                    {
                        this.analyte = undefined;
                    }

                    this.filterIsotypeCombo();
                    this.enableApplyGraphButton();
                    this.fireEvent('graphParamsChanged');
                }
            }
        });
        this.analyteGrid.getStore().on('load', function(store, records, options) {
            var index = store.findExact('value', this.analyte);
            if (this.analyte != undefined && index > -1)
            {
                this.analyteGrid.getSelectionModel().selectRow(index);
                this.analyteGrid.fireEvent('rowClick', this.analyteGrid, index);
                this.analyteGrid.getView().focusRow(index);  // TODO: this doesn't seem to be working
            }
            else
            {
                this.analyte = undefined;
                this.isotype = undefined;
                this.conjugate = undefined;
            }
            
            this.analyteGrid.enable();

            this.paramsToLoad--;
            if (this.paramsToLoad == 0)
            {
                this.allParamsLoaded();
            }
        }, this);
        items.push(this.analyteGrid);

        // add combo-box element for selection of the isotype
        this.isotypeCombobox = new Ext.form.ComboBox({
            id: 'isotype-combo-box',
            fieldLabel: 'Isotype',
            disabled: true,
            anchor: '100%',
            store: new Ext.data.ArrayStore({fields: ['value', 'display']}),
            editable: false,
            triggerAction: 'all',
            mode: 'local',
            valueField: 'value',
            displayField: 'display',
            tpl: '<tpl for="."><div class="x-combo-list-item">{display:htmlEncode}</div></tpl>',
            listeners: {
                scope: this,
                'select': function(combo, record, index) {
                    this.isotype = combo.getValue();
                    this.filterConjugateCombo();
                    this.enableApplyGraphButton();
                    this.fireEvent('graphParamsChanged');
                }
            }
        });
        this.isotypeCombobox.getStore().on('load', function(store, records, options) {
            if (this.isotype != undefined && store.findExact('value', this.isotype) > -1)
            {
                this.isotypeCombobox.setValue(this.isotype);
                this.isotypeCombobox.fireEvent('select', this.isotypeCombobox);
                this.isotypeCombobox.enable();
            }
            else
            {
                this.isotype = undefined;
                this.conjugate = undefined;
                this.conjugateCombobox.clearValue();
                this.conjugateCombobox.disable();
            }

            this.paramsToLoad--;
            if (this.paramsToLoad == 0)
            {
                this.allParamsLoaded();
            }
        }, this);
        items.push(this.isotypeCombobox);

        // add combo-box element for selection of the conjugate
        this.conjugateCombobox = new Ext.form.ComboBox({
            id: 'conjugate-combo-box',
            fieldLabel: 'Conjugate',
            disabled: true,
            anchor: '100%',
            store: new Ext.data.ArrayStore({fields: ['value', 'display']}),
            editable: false,
            triggerAction: 'all',
            mode: 'local',
            valueField: 'value',
            displayField: 'display',
            tpl: '<tpl for="."><div class="x-combo-list-item">{display:htmlEncode}</div></tpl>',
            listeners: {
                scope: this,
                'select': function(combo, record, index) {
                    this.conjugate = combo.getValue();
                    this.enableApplyGraphButton();
                    this.fireEvent('graphParamsChanged');
                }
            }
        });
        this.conjugateCombobox.getStore().on('load', function(store, records, options) {
            if (this.conjugate != undefined && store.findExact('value', this.conjugate) > -1)
            {
                this.conjugateCombobox.setValue(this.conjugate);
                this.conjugateCombobox.enable();
            }
            else
            {
                this.conjugate = undefined;
            }

            this.paramsToLoad--;
            if (this.paramsToLoad == 0)
            {
                this.allParamsLoaded();
            }
        }, this);
        items.push(this.conjugateCombobox);

        var sql;
        if (this.controlType == 'Titration')
        {
            sql =  'SELECT DISTINCT x.Analyte.Name AS Analyte, x.Titration.Run.Isotype AS Isotype, x.Titration.Run.Conjugate AS Conjugate, '
                + 'FROM AnalyteTitration AS x '
                + ' WHERE x.Titration.IncludeInQcReport=true AND x.Titration.Name = \'' + this.controlName.replace(/'/g, "''") + '\'';
        }
        else
        {
            sql =  'SELECT DISTINCT x.Analyte.Name AS Analyte, x.SinglePointControl.Run.Isotype AS Isotype, x.SinglePointControl.Run.Conjugate AS Conjugate, '
                + 'FROM AnalyteSinglePointControl AS x'
                + ' WHERE x.SinglePointControl.Name = \'' + this.controlName.replace(/'/g, "''") + '\'';
        }

        // store of all of the unique analyte/isotype/conjugate combinations for this assay design
        this.graphParamStore = new Ext.data.Store({
            autoLoad: true,
            reader: new Ext.data.JsonReader({
                    root:'rows'
                },
                [{name: 'Analyte'}, {name: 'Isotype'}, {name: 'Conjugate'}]
            ),
            proxy: new Ext.data.HttpProxy({
                method: 'GET',
                url : LABKEY.ActionURL.buildURL('query', 'executeSql', LABKEY.ActionURL.getContainer(), {
                    containerFilter: LABKEY.Query.containerFilter.allFolders,
                    schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
                    sql: sql
                })
            }),
            sortInfo: {
                field: 'Analyte',
                direction: 'ASC'
            },
            listeners: {
                scope: this,
                'load': function(store, recrods, options){
                    // load data into the stores for each of the 3 graph params based on unique values from this store
                    this.analyteGrid.getStore().loadData(this.getArrayStoreData(store, 'Analyte'));
                    //this.isotypeCombobox.getStore().loadData(this.getArrayStoreData(store, 'Isotype'));
                    //this.conjugateCombobox.getStore().loadData(this.getArrayStoreData(store, 'Conjugate'));
                }
            },
            scope: this
        });        

        // add button to apply selections to the generated graph/report
        this.applyGraphButton = new Ext.Button({
            text: 'Apply',
            disabled: true,
            handler: function(){
                // fire the applyGraphBtnClicked event so other panels can update based on the selected params
                this.fireEvent('applyGraphBtnClicked', this.analyte, this.isotype, this.conjugate);
            },
            scope: this
        });
        items.push(this.applyGraphButton);

        this.items = items;

        LABKEY.LeveyJenningsGraphParamsPanel.superclass.initComponent.call(this);
    },

    enableApplyGraphButton: function() {
        var enable = (this.analyte != undefined && this.isotype != undefined && this.conjugate != undefined);
        this.applyGraphButton.setDisabled(!enable);
        return enable;
    },

    filterIsotypeCombo: function() {
        if (this.analyte != undefined)
        {
            this.isotypeCombobox.clearValue();
            this.graphParamStore.filter([{property: 'Analyte', value: this.analyte, anyMatch: false, exactMatch: true}]);
            this.isotypeCombobox.getStore().loadData(this.getArrayStoreData(this.graphParamStore, 'Isotype'));
            this.isotypeCombobox.enable();
        }
        else
        {
            this.isotypeCombobox.clearValue();
            this.isotypeCombobox.disable();
            this.conjugateCombobox.clearValue();
            this.conjugateCombobox.disable();
        }
    },

    filterConjugateCombo: function() {
        if (this.isotype != undefined)
        {
            this.conjugateCombobox.clearValue();
            this.graphParamStore.filter({
                fn: function(record) {
                    return record.get('Analyte') == this.analyte && record.get('Isotype') == this.isotype;
                },
                scope: this
            }),
            this.conjugateCombobox.getStore().loadData(this.getArrayStoreData(this.graphParamStore, 'Conjugate'));
            this.conjugateCombobox.enable();
        }
        else
        {
            this.conjugateCombobox.clearValue();
            this.conjugateCombobox.disable();
        }
    },

    getArrayStoreData: function(store, colName) {
        var storeData = [];
        Ext.each(store.collect(colName, true, false).sort(), function(value){
            storeData.push([value, value == "" ? "[None]" : value]);
        });
        return storeData;
    },

    allParamsLoaded: function() {
        if (this.enableApplyGraphButton())
        {
            // fire the applyGraphBtnClicked event so other panels can update based on the selected params
            this.fireEvent('applyGraphBtnClicked', this.analyte, this.isotype, this.conjugate);
        }
    },

    tooltipRenderer: function(value, p, record) {
        var msg = Ext.util.Format.htmlEncode(value);
        p.attr = 'ext:qtip="' + msg + '"';
        return "<span class='grid-cell-nowrap'>" + msg + "</span>";
    }
});
