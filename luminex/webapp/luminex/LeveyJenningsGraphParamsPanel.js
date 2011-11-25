/*
 * Copyright (c) 2011 LabKey Corporation
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
        if (!config.titration || config.titration == "null")
        {
            throw "You must specify a titration!";
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
            height: 460,
            border: true,
            cls: 'extContainer'
        });

        this.addEvents('resetGraphBtnClicked');        

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

        // add combo-box element for selection of the antigen/analyte
        this.analyteGrid = new Ext.grid.GridPanel({
            id: 'analtye-grid-panel',
            fieldLabel: 'Antigens',
            height: 225,
            border: true,
            frame: false,
            hideHeaders: true,
            viewConfig: {forceFit: true},
            selModel: new Ext.grid.RowSelectionModel({singleSelect: true}),
            store: new Ext.data.Store({
                autoLoad: true,
                reader: new Ext.data.JsonReader({
                        root:'rows'
                    },
                    [{name: 'value'}]
                ),
                proxy: new Ext.data.HttpProxy({
                    method: 'GET',
                    url : LABKEY.ActionURL.buildURL('query', 'executeSql', LABKEY.ActionURL.getContainer(), {
                        containerFilter: LABKEY.Query.containerFilter.allFolders,
                        schemaName: 'assay',
                        sql: 'SELECT DISTINCT x.Analyte.Name AS value FROM "' + this.assayName + ' AnalyteTitration" AS x '
                            + ' WHERE x.Titration.Name = \'' + this.titration.replace(/'/g, "''") + '\''
                            + ' AND x.MaxFI IS NOT NULL' // this check added to only select analytes uploaded after EC50, AUC, and MaxFI calculations were added to server
                    })
                }),
                sortInfo: {
                    field: 'value',
                    direction: 'ASC'
                },
                scope: this
            }),
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

                    this.enableResetGraphButton();
                }
            }
        });
        this.analyteGrid.getStore().on('load', function(store, records, options) {
            var index = store.findExact('value', this.analyte);
            if (this.analyte != undefined && index > -1)
            {
                this.analyteGrid.getSelectionModel().selectRow(index);
                this.analyteGrid.getView().focusRow(index);  // TODO: this doesn't seem to be working
            }
            else
            {
                this.analyte = undefined;
            }

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
            anchor: '100%',
            store: new Ext.data.Store({
                autoLoad: true,
                reader: new Ext.data.JsonReader({
                        root:'rows'
                    },
                    [{name: 'value'}, {name: 'display'}]
                ),
                proxy: new Ext.data.HttpProxy({
                    method: 'GET',
                    url : LABKEY.ActionURL.buildURL('query', 'executeSql', LABKEY.ActionURL.getContainer(), {
                        containerFilter: LABKEY.Query.containerFilter.allFolders,
                        schemaName: 'assay',
                        sql: 'SELECT DISTINCT x.Titration.Run.Isotype AS value, '
                            + 'CASE WHEN x.Titration.Run.Isotype IS NULL THEN \'[None]\' ELSE x.Titration.Run.Isotype END AS display '
                            + 'FROM "' + this.assayName + ' AnalyteTitration" AS x '
                            + ' WHERE x.Titration.Name = \'' + this.titration.replace(/'/g, "''") + '\''
                            + ' AND x.MaxFI IS NOT NULL' // this check added to only select analytes uploaded after EC50, AUC, and MaxFI calculations were added to server
                    })
                }),
                sortInfo: {
                    field: 'value',
                    direction: 'ASC'
                },
                scope: this
            }),
            editable: false,
            triggerAction: 'all',
            mode: 'local',
            valueField: 'value',
            displayField: 'display',
            listeners: {
                scope: this,
                'select': function(combo, record, index) {
                    this.isotype = combo.getValue();
                    this.enableResetGraphButton();
                }
            }
        });
        this.isotypeCombobox.getStore().on('load', function(store, records, options) {
            if (this.isotype != undefined && store.findExact('value', this.isotype) > -1)
            {
                this.isotypeCombobox.setValue(this.isotype);
            }
            else
            {
                this.isotype = undefined;
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
            anchor: '100%',
            store: new Ext.data.Store({
                autoLoad: true,
                reader: new Ext.data.JsonReader({
                        root:'rows'
                    },
                    [{name: 'value'}, {name: 'display'}]
                ),
                proxy: new Ext.data.HttpProxy({
                    method: 'GET',
                    url : LABKEY.ActionURL.buildURL('query', 'executeSql', LABKEY.ActionURL.getContainer(), {
                        containerFilter: LABKEY.Query.containerFilter.allFolders,
                        schemaName: 'assay',
                        sql: 'SELECT DISTINCT x.Titration.Run.Conjugate AS value, '
                            + 'CASE WHEN x.Titration.Run.Conjugate IS NULL THEN \'[None]\' ELSE x.Titration.Run.Conjugate END AS display '
                            + 'FROM "' + this.assayName + ' AnalyteTitration" AS x '
                            + ' WHERE x.Titration.Name = \'' + this.titration.replace(/'/g, "''") + '\''
                            + ' AND x.MaxFI IS NOT NULL' // this check added to only select analytes uploaded after EC50, AUC, and MaxFI calculations were added to server
                    })
                }),
                sortInfo: {
                    field: 'value',
                    direction: 'ASC'
                },
                scope: this
            }),
            editable: false,
            triggerAction: 'all',
            mode: 'local',
            valueField: 'value',
            displayField: 'display',
            listeners: {
                scope: this,
                'select': function(combo, record, index) {
                    this.conjugate = combo.getValue();
                    this.enableResetGraphButton();
                }
            }
        });
        this.conjugateCombobox.getStore().on('load', function(store, records, options) {
            if (this.conjugate != undefined && store.findExact('value', this.conjugate) > -1)
            {
                this.conjugateCombobox.setValue(this.conjugate);
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

        // add button to apply selections to the generated graph/report
        this.resetGraphButton = new Ext.Button({
            text: 'Reset Graph',
            disabled: true,
            handler: function(){
                // fire the resetGraphBtnClicked event so other panels can update based on the selected params
                this.fireEvent('resetGraphBtnClicked', this.analyte, this.isotype, this.conjugate);
            },
            scope: this
        });
        items.push(this.resetGraphButton);

        this.items = items;

        LABKEY.LeveyJenningsGraphParamsPanel.superclass.initComponent.call(this);
    },

    enableResetGraphButton: function() {
        var enable = (this.analyte != undefined && this.isotype != undefined && this.conjugate != undefined);
        this.resetGraphButton.setDisabled(!enable);
        return enable;
    },

    allParamsLoaded: function() {
        if (this.enableResetGraphButton())
        {
            // fire the resetGraphBtnClicked event so other panels can update based on the selected params
            this.fireEvent('resetGraphBtnClicked', this.analyte, this.isotype, this.conjugate);
        }
    },

    tooltipRenderer: function(value, p, record) {
        var msg = Ext.util.Format.htmlEncode(value);
        p.attr = 'ext:qtip="' + msg + '"';
        return msg;
    }
});
