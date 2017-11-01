/*
 * Copyright (c) 2011-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
* User: cnathe
* Date: Sept 20, 2011
*/

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
            width: 865,
            border: false,
            cls: 'extContainer',
            disabled: true,
            yAxisScale: 'linear'
        });

        this.addEvents('reportFilterApplied', 'togglePdfBtn');

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        var labelStyle = 'font-size: 13px; padding-top: 4px;';
        this.ANY_FIELD = '[ANY]';  // constant value used for turning filtering off

        this.startDate = null;
        this.endDate = null;
        this.network = null;
        this.networkAny = true;  // false - turns on the filter in R and in Data Panel
        this.protocol = null;
        this.protocolAny = true; // false - turns on the filter in R and in Data Panel

        // initialize the y-axis scale combo for the top toolbar
        this.scaleLabel = new Ext.form.Label({
            text: 'Y-Axis Scale:',
            style: labelStyle
        });
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
                    this.updateTrendPlot();
                }
            }
        });

        // initialize the date range selection fields for the top toolbar
        this.startDateLabel = new Ext.form.Label({
            text: 'Start Date:',
            style: labelStyle
        });
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
        this.endDateLabel = new Ext.form.Label({
            text: 'End Date:',
            style: labelStyle
        });
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
            this.networkLabel = new Ext.form.Label({
                text: 'Network:',
                style: labelStyle
            });
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
            this.protocolLabel = new Ext.form.Label({
                text: 'Protocol:',
                style: labelStyle
            });
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
            handler: function() {
                this.clearGraphFilter(false);
            },
            scope: this
        });

        var tbspacer = {xtype: 'tbspacer', width: 5};

        var items = [
            this.scaleLabel, tbspacer,
            this.scaleCombo, tbspacer,
            '->',
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
            style: 'border: solid 1px #d0d0d0; padding: 5px 10px;',
            items: items
        });

        // initialize the tab panel that will show the trend plots
        this.ec504plPanel = new Ext.Panel({
            itemId: "EC504PL",
            title: LABKEY.LeveyJenningsPlotHelper.PlotTypeMap["EC504PL"],
            html: "<div id='EC504PLTrendPlotDiv' class='ljTrendPlot'>To begin, choose an Antigen, Isotype, and Conjugate from the panel to the left and click the Apply button.</div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.ec505plPanel = new Ext.Panel({
            itemId: "EC505PL",
            title: LABKEY.LeveyJenningsPlotHelper.PlotTypeMap["EC505PL"],
            html: "<div id='EC505PLTrendPlotDiv' class='ljTrendPlot'></div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.aucPanel = new Ext.Panel({
            itemId: "AUC",
            title: LABKEY.LeveyJenningsPlotHelper.PlotTypeMap["AUC"],
            html: "<div id='AUCTrendPlotDiv' class='ljTrendPlot'></div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.mfiPanel = new Ext.Panel({
            itemId: "HighMFI",
            title: LABKEY.LeveyJenningsPlotHelper.PlotTypeMap["HighMFI"],
            html: "<div id='HighMFITrendPlotDiv' class='ljTrendPlot'></div>",
            deferredRender: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.singlePointControlPanel = new Ext.Panel({
            itemId: "MFI",
            title: "MFI",
            html: "<div id='MFITrendPlotDiv' class='ljTrendPlot'></div>",
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

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.initComponent.call(this);
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // remove any previously entered values from the start date, end date, network, etc. fileds
        this.clearGraphFilter(true);

        // show the trending tab panel and date range selection toolbar
        this.enable();

        this.setTrendPlotLoading();
    },

    trackingDataLoaded: function(store)
    {
        if (this.networkExists)
        {
            this.networkCombobox.getStore().loadData(this.getArrayStoreData(store.collect("Network", true), this.networkCombobox.getValue()));
        }

        if (this.protocolExists)
        {
            this.protocolCombobox.getStore().loadData(this.getArrayStoreData(store.collect("CustomProtocol", true), this.protocolCombobox.getValue()));
        }

        this.updateTrendPlot(store);
    },

    setTrendPlotLoading: function() {
        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        Ext.get(trendDiv).update('Loading plot...');
        this.togglePDFExportBtn(false);
    },

    updateTrendPlot: function(store)
    {
        this.togglePDFExportBtn(false);

        // stash the store's records so that they can be re-used on tab change
        if (store) {
            this.trendDataStore = store;
        }

        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        var plotConfig = {
            store: this.trendDataStore,
            plotType: plotType,
            renderDiv: trendDiv,
            assayName: this.assayName,
            controlName: this.controlName,
            analyte: this.analyte,
            isotype: this.isotype,
            conjugate: this.conjugate,
            yAxisScale: this.yAxisScale
        };

        var renderType = LABKEY.LeveyJenningsPlotHelper.renderPlot(plotConfig);

        // export to PDF doesn't work for IE<9
        this.togglePDFExportBtn(renderType == 'd3');
    },

    activateTrendPlotPanel: function(panel) {
        // only update plot if the graph params have been selected
        if (this.analyte != undefined && this.isotype != undefined && this.conjugate != undefined)
            this.updateTrendPlot();
    },

    togglePDFExportBtn: function(enable) {
        this.fireEvent('togglePdfBtn', enable);
    },

    exportToPdf: function() {
        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        var svgEls = Ext.get(trendDiv).select('svg');
        if (svgEls.elements.length > 0)
        {
            var title = 'Levey-Jennings ' + this.trendTabPanel.getActiveTab().title + ' Trend Plot';
            LABKEY.vis.SVGConverter.convert(svgEls.elements[0], LABKEY.vis.SVGConverter.FORMAT_PDF, title);
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

            this.fireEvent('reportFilterApplied', this.startDate, this.endDate, this.network, this.networkAny, this.protocol, this.protocolAny);
        }
    },

    clearGraphFilter: function(clearOnly) {
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

        if (clearOnly)
            return;

        this.fireEvent('reportFilterApplied', this.startDate, this.endDate, this.network, this.networkAny, this.protocol, this.protocolAny);
    },

    getStartDate: function() {
        return this.startDate ? this.startDate : null;
    },

    getEndDate: function() {
        return this.endDate ? this.endDate : null;
    },

    getArrayStoreData: function(arr, prevVal) {
        // if there is a previously selected combo value, make sure it exists in the array data
        if (prevVal && prevVal != '[ANY]' && arr.indexOf(prevVal) == -1){
            arr.push(prevVal);
        }

        var storeData = [ [this.ANY_FIELD, this.ANY_FIELD] ];
        Ext.each(arr.sort(), function(value){
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
