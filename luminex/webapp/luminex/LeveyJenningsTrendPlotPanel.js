/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
        if (!config.controlName || config.controlName == "null")
            throw "You must specify a control name!";
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

        // setup variable switching between Titration and SinglePointControl column names
        if (this.controlType == 'Titration') {
            this.controlTypeColumnName = "Titration";
        } else {
            this.controlTypeColumnName = "SinglePointControl";
        }

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
            handler: this.applyGraphFilter,
            scope: this
        });

        // initialize the clear graph button
        this.clearFilterButton = new Ext.Button({
            disabled: true,
            text: 'Clear',
            handler: this.clearGraphFilter,
            scope: this
        });

        var tbspacer = {xtype: 'tbspacer', width: 5};

        var items = [
            this.scaleLabel, tbspacer,
            this.scaleCombo, tbspacer,
            {xtype: 'tbseparator'}, tbspacer,
            this.startDateLabel, tbspacer,
            this.startDateField, tbspacer,
            this.endDateLabel, tbspacer,
            this.endDateField, tbspacer
        ];
        if (this.networkExists) {
            items.push(this.networkLabel);
            items.push(tbspacer);
            items.push(this.networkCombobox);
            items.push(tbspacer);

        }
        if (this.protocolExists) {
            items.push(this.protocolLabel);
            items.push(tbspacer);
            items.push(this.protocolCombobox);
            items.push(tbspacer);
        }
        items.push(this.applyFilterButton);
        items.push(tbspacer);
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
        this.singlePointControlPanel = new Ext.Panel({
            itemId: "MFI",
            title: "MFI",
            html: "<div id='MFITrendPlotDiv' class='spcTrendPlot'></div>",
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
            // show different tabs if the report is qc titration report vs. qc single point control report
            items: this.getTitrationSinglePointControlItems()
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

        // remove any previously entered values from the start date, end date, network, etc. fileds
        this.clearGraphFilter();

        // (re)load the network and protocol filter combos based on the selected params
        this.loadNetworkAndProtocol();

        // show the trending tab panel and date range selection toolbar
        this.enable();

        this.setTabsToRender();
        this.displayTrendPlot();
    },

    loadNetworkAndProtocol: function() {
        var sqlFragment = ' FROM "Analyte' + this.controlTypeColumnName + '" AS x'
            + ' WHERE x.' + this.controlTypeColumnName + '.Name = \'' + this.controlName.replace(/'/g, "''") + '\''
            + ' AND x.Analyte.Name = \'' + this.analyte.replace(/'/g, "''") + '\''
            // this check added to only select analytes uploaded after EC50, AUC, and MaxFI calculations were added to server
            + ' AND x.' + (this.controlType == "Titration" ? "MaxFI" : "AverageFiBkgd") + ' IS NOT NULL';

        if (this.networkExists) {
            LABKEY.Query.executeSql({
                schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
                sql: "SELECT DISTINCT x." + this.controlTypeColumnName + ".Run.Batch.Network" + sqlFragment,
                containerFilter: LABKEY.Query.containerFilter.allFolders,
                success: function(data) {
                    this.networkCombobox.getStore().loadData(this.getArrayStoreData(data.rows, 'Network'));
                    this.networkCombobox.setValue(this.ANY_FIELD);
                },
                scope: this
            });
        }

        if (this.protocolExists) {
            LABKEY.Query.executeSql({
                schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
                sql: "SELECT DISTINCT x." + this.controlTypeColumnName + ".Run.Batch.CustomProtocol AS CustomProtocol" + sqlFragment,
                containerFilter: LABKEY.Query.containerFilter.allFolders,
                success: function(data) {
                    this.protocolCombobox.getStore().loadData(this.getArrayStoreData(data.rows, 'CustomProtocol'));
                    this.protocolCombobox.setValue(this.ANY_FIELD);
                },
                scope: this
            });
        }
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
            config['Protocol'] = LABKEY.QueryKey.encodePart(this.assayName);
            config['Titration'] = this.controlName;
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
            if (this.controlType == "Titration") {
                config['isTitration'] = true;
            }

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
                           this.pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('">PDF output file'));
                           this.pdfHref = this.pdfHref.replace(/&amp;/g, "&");
                           this.fireEvent('togglePdfBtn', true);
                       }
                   },
                   failure: function(response){},
                   scope: this
            }).render();
            
        }
    },

    applyGraphFilter: function() {
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

    clearGraphFilter: function() {
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

    getArrayStoreData: function(rows, colName) {
        var storeData = [ [this.ANY_FIELD, this.ANY_FIELD] ];
        Ext.each(rows, function(row){
            var value = row[colName];
            storeData.push([value, value == null ? '[Blank]' : value]);
        });
        return storeData;
    },

    getTitrationSinglePointControlItems: function() {
        if (this.controlType == "Titration") {
            return([this.ec504plPanel, this.ec505plPanel, this.aucPanel, this.mfiPanel]);
        } else if (this.controlType = "SinglePoint") {
            return([this.singlePointControlPanel]);
        }
        return null;
    }
});
