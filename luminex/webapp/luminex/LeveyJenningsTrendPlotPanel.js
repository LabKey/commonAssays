/*
 * Copyright (c) 2011-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
* User: cnathe
* Date: Sept 20, 2011
*/

LABKEY.requiresCss("luminex/LeveyJenningsReport.css");

/**
 * Class to create a tab panel for displaying the R plot for the trending of EC50, AUC, and High MFI values for the selected graph parameters.
 *
 * @params titration
 * @params assayName
 */
LABKEY.LeveyJenningsTrendPlotPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // check that the config properties needed are present
        if (!config.titration || config.titration == "null")
            throw "You must specify a titration!";
        if (!config.assayName || config.assayName == "null")
            throw "You must specify a assayName!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            items: [],
            header: false,
            bodyStyle: 'background-color:#EEEEEE',
            labelAlign: 'left',
            width: 850,
            height: 370,
            border: false,
            cls: 'extContainer',
            disabled: true,
            yAxisScale: 'linear'
        });

        this.addEvents('reportFilterApplied', 'togglePdfBtn');

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        this.ANY_FIELD = '[ANY]';  // constant value used for turning filtering off

        this.plotRenderedHtml = null;
        this.pdfHref = null;
        this.startDate = null;
        this.endDate = null;
        this.network = null;
        this.networkAny = true;  // false - turns on the filter in R and in Data Panel
        this.protocol = null;
        this.protocolAny = true; // false - turns on the filter in R and in Data Panel

        // initialize the y-axis scale combo for the top toolbar
        this.scaleLabel = new Ext.form.Label({text: 'Y-Axis Scale:'});
        this.scaleCombo = new Ext.form.ComboBox({
            id: 'scale-combo-box',
            width: 75,
            triggerAction: 'all',
            mode: 'local',
            store: new Ext.data.ArrayStore({
                fields: ['value', 'display'],
                data: [['linear', 'Linear'], ['log', 'Log']]
            }),
            valueField: 'value',
            displayField: 'display',
            value: 'linear',
            forceSelection: true,
            editable: false,
            listeners: {
                scope: this,
                'select': function(cmp, newVal, oldVal) {
                    this.yAxisScale = cmp.getValue();
                    this.setTabsToRender();
                    this.displayTrendPlot();
                }
            }
        });

        // initialize the date range selection fields for the top toolbar
        this.startDateLabel = new Ext.form.Label({text: 'Start Date:'});
        this.startDateField = new Ext.form.DateField({
            id: 'start-date-field',
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '')
                        this.applyFilterButton.enable();
                },
                'invalid': function (df, msg) {
                    this.applyFilterButton.disable();
                }
            }
        });
        this.endDateLabel = new Ext.form.Label({text: 'End Date:'});
        this.endDateField = new Ext.form.DateField({
            id: 'end-date-field',
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '')
                        this.applyFilterButton.enable();
                },
                'invalid': function (df, msg) {
                    this.applyFilterButton.disable();
                }
            }
        });

        // Only create the network store and combobox if the Network column exists
        if (this.networkExists) {
            // Network Data store
            this.networkStore = new Ext.data.Store({
                autoLoad: true,
                reader: new Ext.data.JsonReader({
                        root:'rows'
                    },
                    [{name: 'Network'}]
                ),
                proxy: new Ext.data.HttpProxy({
                    method: 'GET',
                    url : LABKEY.ActionURL.buildURL('query', 'executeSql', LABKEY.ActionURL.getContainer(), {
                        containerFilter: LABKEY.Query.containerFilter.allFolders,
                        schemaName: 'assay',
                        sql: "SELECT DISTINCT x.Titration.Run.Batch.Network" // x.Titration.Run.Batch.CustomProtocol AS Protocol
                            + ' FROM "' + this.assayName + ' AnalyteTitration" AS x'
                            + ' WHERE x.Titration.Name = \'' + this.titration.replace(/'/g, "''") + '\''
                            + ' AND x.MaxFI IS NOT NULL' // this check added to only select analytes uploaded after EC50, AUC, and MaxFI calculations were added to server
                    })
                }),
                listeners: {
                    scope: this,
                    load: function(store, records, options){
                        // load data into the stores for each of the 2 params based on unique values from this store
                        this.networkCombobox.getStore().loadData(this.getArrayStoreData(store, 'Network'));
                        this.networkCombobox.setValue(this.ANY_FIELD);
                        this.networkAny = true;
                        this.network = null;
                    }
                },
                scope: this
            });

            // Add Network field for filtering
            this.networkLabel = new Ext.form.Label({text: 'Network:'});
            this.networkCombobox = new Ext.form.ComboBox({
                id: 'network-combo-box',
                width: 75,
                listWidth: 180,
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
                        if (combo.getValue() == this.ANY_FIELD) {
                            this.networkAny = true;
                            this.network = null;
                        } else {
                            this.networkAny = false;
                            this.network = combo.getValue();
                        }
                        this.applyFilterButton.enable();
                    }
                }
            });
            this.networkCombobox.getStore().on('load', function(store, records, options) {
                if (this.network != undefined && store.findExact('value', this.network) > -1)
                {
                    this.networkCombobox.setValue(this.network);
                    this.networkCombobox.fireEvent('select', this.networkCombobox);
                    this.networkCombobox.enable();
                }
                else
                {
                    this.network = undefined;
                }
            }, this);
        }

        // Only create the protocol if the CustomProtocol column exists
        if (this.protocolExists) {
            // Protocol Data store
            this.protocolStore = new Ext.data.Store({
                autoLoad: true,
                reader: new Ext.data.JsonReader({
                            root:'rows'
                        },
                        [{name: 'CustomProtocol'}]
                ),
                proxy: new Ext.data.HttpProxy({
                    method: 'GET',
                    url : LABKEY.ActionURL.buildURL('query', 'executeSql', LABKEY.ActionURL.getContainer(), {
                        containerFilter: LABKEY.Query.containerFilter.allFolders,
                        schemaName: 'assay',
                        sql: "SELECT DISTINCT x.Titration.Run.Batch.CustomProtocol AS CustomProtocol"
                                + ' FROM "' + this.assayName + ' AnalyteTitration" AS x'
                                + ' WHERE x.Titration.Name = \'' + this.titration.replace(/'/g, "''") + '\''
                                + ' AND x.MaxFI IS NOT NULL' // this check added to only select analytes uploaded after EC50, AUC, and MaxFI calculations were added to server
                    })
                }),
                listeners: {
                    scope: this,
                    load: function(store, records, options){
                        // load data into the stores for each of the 2 params based on unique values from this store
                        this.protocolCombobox.getStore().loadData(this.getArrayStoreData(store, 'CustomProtocol'));
                        this.protocolCombobox.setValue(this.ANY_FIELD);
                        this.protocolAny = true;
                        this.protocol = null;
                    }
                },
                scope: this
            });

            // Add Protocol field for filtering
            this.protocolLabel = new Ext.form.Label({text: 'Protocol:'});
            this.protocolCombobox = new Ext.form.ComboBox({
                id: 'protocol-combo-box',
                width: 75,
                listWidth: 180,
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
                        this.protocol = combo.getValue();
                        this.applyFilterButton.enable();

                        if (combo.getValue() == this.ANY_FIELD) {
                            this.protocolAny = true;
                            this.protocol = null;
                        } else {
                            this.protocolAny = false;
                            this.protocol = combo.getValue();
                        }
                        this.applyFilterButton.enable();
                    }
                }
            });
            this.protocolCombobox.getStore().on('load', function(store, records, options) {
                if (this.protocol != undefined && store.findExact('value', this.protocol) > -1)
                {
                    this.protocolCombobox.setValue(this.protocol);
                    this.protocolCombobox.fireEvent('select', this.protocolCombobox);
                    this.protocolCombobox.enable();
                }
                else
                {
                    this.protocol = undefined;
                }
            }, this);
        }

        // initialize the refesh graph button
        this.applyFilterButton = new Ext.Button({
            disabled: true,
            text: 'Apply',
            handler: this.applyDateFilter,
            scope: this
        });

        // initialize the clear graph button
        this.clearFilterButton = new Ext.Button({
            disabled: true,
            text: 'Clear',
            handler: this.clearDateFilter,
            scope: this
        });

        var spaceWidth = 5;
        var items = [
            this.scaleLabel,
            {xtype: 'tbspacer', width: spaceWidth},
            this.scaleCombo,
            {xtype: 'tbspacer', width: spaceWidth},
            {xtype: 'tbseparator'},
            {xtype: 'tbspacer', width: spaceWidth},
            this.startDateLabel,
            {xtype: 'tbspacer', width: spaceWidth},
            this.startDateField,
            {xtype: 'tbspacer', width: spaceWidth},
            this.endDateLabel,
            {xtype: 'tbspacer', width: spaceWidth},
            this.endDateField,
            {xtype: 'tbspacer', width: spaceWidth}
        ];
        if (this.networkExists) {
            items.push(this.networkLabel);
            items.push({xtype: 'tbspacer', width: spaceWidth});
            items.push(this.networkCombobox);
            items.push({xtype: 'tbspacer', width: spaceWidth});

        }
        if (this.protocolExists) {
            items.push(this.protocolLabel);
            items.push({xtype: 'tbspacer', width: spaceWidth});
            items.push(this.protocolCombobox);
            items.push({xtype: 'tbspacer', width: spaceWidth});
        }
        items.push(this.applyFilterButton);
        items.push({xtype: 'tbspacer', width: spaceWidth});
        items.push(this.clearFilterButton);

        this.tbar = new Ext.Toolbar({
            height: 30,
            buttonAlign: 'center',
            items: items
        });

        // initialize the tab panel that will show the trend plots
        this.ec504plPanel = new Ext.Panel({
            itemId: "EC50 4PL",
            title: "EC50 - 4PL",
            html: "<div id='EC50 4PLTrendPlotDiv' class='ec504plTrendPlot'>To begin, choose an Antigen, Isotype, and Conjugate from the panel to the left and click the Apply button.</div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.ec505plPanel = new Ext.Panel({
            itemId: "EC50 5PL",
            title: "EC50 - 5PL Rumi",
            html: "<div id='EC50 5PLTrendPlotDiv' class='ec505plTrendPlot'></div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.aucPanel = new Ext.Panel({
            itemId: "AUC",
            title: "AUC",
            html: "<div id='AUCTrendPlotDiv' class='aucTrendPlot'></div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.mfiPanel = new Ext.Panel({
            itemId: "High MFI",
            title: "High MFI",
            html: "<div id='High MFITrendPlotDiv' class='mfiTrendPlot'></div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.trendTabPanel = new Ext.TabPanel({
            autoScroll: true,
            activeTab: 0,
            defaults: {
                height: 308,
                padding: 5
            },
            items: [this.ec504plPanel, this.ec505plPanel, this.aucPanel, this.mfiPanel]
        });
        this.items.push(this.trendTabPanel);

        // add an additional panel to render the PDF export link HTML to (this will always be hidden)
        this.pdfPanel = new Ext.Panel({hidden: true});
        this.items.push(this.pdfPanel);

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.initComponent.call(this);
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // remove any previously entered values from the start and end date fields
        this.startDate = null;
        this.startDateField.reset();
        this.endDate = null;
        this.endDateField.reset();
        this.applyFilterButton.disable();

        // show the trending tab panel and date range selection toolbar
        this.enable();

        this.setTabsToRender();
        this.displayTrendPlot();
    },

    activateTrendPlotPanel: function(panel) {
        // if the graph params have been selected and the trend plot for this panel hasn't been loaded, then call displayTrendPlot
        if (this.analyte != undefined && this.isotype != undefined && this.conjugate != undefined)
            this.displayTrendPlot();
    },

    setTabsToRender: function() {
        // something about the report has changed and the plot needs to be set to re-render
        this.plotRenderedHtml = null;
        this.pdfHref = null;
        this.fireEvent('togglePdfBtn', false);
    },

    displayTrendPlot: function() {
        // determine which tab is selected to know which div to update
        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        Ext.get(trendDiv).update('Loading...');

        if (this.plotRenderedHtml)
        {
            Ext.get(trendDiv).update(this.plotRenderedHtml);
        }
        else
        {
            // build the config object of the properties that will be needed by the R report
            var config = {reportId: 'module:luminex/LeveyJenningsTrendPlot.r', showSection: 'Levey-Jennings Trend Plot'};
            config['Protocol'] = this.assayName;
            config['Titration'] = this.titration;
            config['Analyte'] = this.analyte;
            config['Isotype'] = this.isotype;
            config['Conjugate'] = this.conjugate;
            // provide either a start and end date or the max number of rows to display
            if (!this.startDate && !this.endDate && this.networkAny && this.protocolAny){
                config['MaxRows'] = this.defaultRowSize;
            } else {
                if (this.startDate) {
                    config['StartDate'] = this.startDate;
                }
                if (this.endDate) {
                    config['EndDate'] = this.endDate;
                }
                if (this.networkExists && !this.networkAny) { // networkAny turns off the filter in R
                    config['NetworkFilter'] = true;
                    config['Network'] = this.network;
                }
                if (this.protocolExists && !this.protocolAny) { // protocolAny turns off the filter in R
                    config['CustomProtocolFilter'] = true;
                    config['CustomProtocol'] = this.protocol;
                }
            }
            // add config for plotting in log scale
            if (this.yAxisScale == 'log')
                config['AsLog'] =  true;

            // call and display the Report webpart
            new LABKEY.WebPart({
                   partName: 'Report',
                   renderTo: trendDiv,
                   frame: 'none',
                   partConfig: config,
                   success: function() {
                       // store the HTML for the src plot image (image will be shifted to the relevant plot for other plot types)
                       this.plotRenderedHtml = Ext.getDom(trendDiv).innerHTML;
                   },
                   failure: function(response) {
                        Ext.get(trendDiv).update("Error: " + response.statusText);
                   },
                   scope: this
            }).render();

            // call the R plot code again to get a PDF output version of the plot
            config['PdfOut'] = true;
            new LABKEY.WebPart({
                   partName: 'Report',
                   renderTo: this.pdfPanel.getId(),
                   frame: 'none',
                   partConfig: config,
                   success: function() {
                       // ugly way of getting the href for the pdf file (to be used when the user clicks the export pdf button)
                       if (Ext.getDom(this.pdfPanel.getId()))
                       {
                           var html = Ext.getDom(this.pdfPanel.getId()).innerHTML;
                           this.pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('&amp;attachment=true'));
                           this.fireEvent('togglePdfBtn', true);
                       }
                   },
                   failure: function(response){},
                   scope: this
            }).render();
            
        }
    },

    applyDateFilter: function() {
        // make sure that at least one filter field is not null
        if (this.startDateField.getRawValue() == '' && this.endDateField.getRawValue() == '' && this.networkCombobox.getRawValue() == '' && this.protocolCombobox.getRawValue() == '')
        {
            Ext.Msg.show({
                title:'ERROR',
                msg: 'Please enter a value for filtering.',
                buttons: Ext.Msg.OK,
                icon: Ext.MessageBox.ERROR
            });
        }
        // verify that the start date is not after the end date
        else if (this.startDateField.getValue() > this.endDateField.getValue() && this.endDateField.getValue() != '')
        {
            Ext.Msg.show({
                title:'ERROR',
                msg: 'Please enter an end date that does not occur before the start date.',
                buttons: Ext.Msg.OK,
                icon: Ext.MessageBox.ERROR
            });
        }
        else
        {
            // get date values without the time zone info
            this.startDate = this.startDateField.getRawValue();
            this.endDate = this.endDateField.getRawValue();
            this.clearFilterButton.enable();

            this.setTabsToRender();
            this.displayTrendPlot();
            this.fireEvent('reportFilterApplied', this.startDate, this.endDate, this.network, this.networkAny, this.protocol, this.protocolAny);
        }
    },

    clearDateFilter: function() {
        this.startDate = null;
        this.startDateField.reset();
        this.endDate = null;
        this.endDateField.reset();
        this.applyFilterButton.disable();
        this.clearFilterButton.disable();
        this.network = null;
        if (this.networkCombobox) {
            this.networkCombobox.reset();
            this.networkCombobox.setValue(this.ANY_FIELD);
            this.networkAny = true;
            this.network = null;
        }
        this.protocol = null;
        if (this.protocolCombobox) {
            this.protocolCombobox.reset();
            this.protocolCombobox.setValue(this.ANY_FIELD);
            this.protocolAny = true;
            this.protocol = null;
        }


        this.setTabsToRender();
        this.displayTrendPlot();
        this.fireEvent('reportFilterApplied', this.startDate, this.endDate, this.network, this.networkAny, this.protocol, this.protocolAny);
    },

    getPdfHref: function() {
        return this.pdfHref ? this.pdfHref : null;
    },

    getStartDate: function() {
        return this.startDate ? this.startDate : null;
    },

    getEndDate: function() {
        return this.endDate ? this.endDate : null;
    },

    getArrayStoreData: function(store, colName) {
        var storeData = [ [this.ANY_FIELD, this.ANY_FIELD] ];
        Ext.each(store.collect(colName, true, false).sort(), function(value){
            if (value) {
                storeData.push([value, value]);
            } else {
                storeData.push([value, '[Blank]']);
            }
        });
        return storeData;
    }
});
