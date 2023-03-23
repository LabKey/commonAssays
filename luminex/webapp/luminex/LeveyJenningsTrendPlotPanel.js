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
 * @params titration or single point control
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
            labelAlign: 'left',
            width: 865,
            border: false,
            cls: 'extContainer',
            disabled: true,
            yAxisScale: 'linear'
        });

        this.addEvents('togglePdfBtn');

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        this.guideSetRangeStore = LABKEY.LeveyJenningsPlotHelper.getGuideSetRangesStore({
            assayName: this.assayName,
            scope: this,
            loadListener: function() {
                this.guideSetRangeStoreLoadComplete = true;
                this.updateTrendPlot();
            },
        });

        // initialize the y-axis scale combo for the top toolbar
        this.scaleLabel = new Ext.form.Label({
            text: 'Y-Axis Scale:',
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

        this.tbar = new Ext.Toolbar({
            style: 'background-color: #ffffff; padding: 5px 10px; border-color: #d0d0d0; border-width: 1px 1px 0 1px;',
            items: [
                '->',
                this.scaleLabel,
                {xtype: 'tbspacer', width: 5},
                this.scaleCombo,
            ]
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
            style: 'border-style: solid; border-width: 0 1px; border-color: #d0d0d0;',
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

        this.fbar = [
            {xtype: 'label', text: 'The default plot is showing the most recent ' + this.defaultRowSize + ' data points.'},
        ];

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.initComponent.call(this);

        this.fbar.hide();
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        this.enable();

        this.setTrendPlotLoading();
    },

    plotDataLoading: function(store, hasGuideSetUpdates)
    {
        if (hasGuideSetUpdates) {
            this.guideSetRangeStoreLoadComplete = false;
            this.guideSetRangeStore.load();
        }

        this.setTrendPlotLoading();
    },

    plotDataLoaded: function(store, hasReportFilter)
    {
        // stash the store's records so that they can be re-used on tab change
        if (store) {
            this.trendDataStore = store;
        }

        this.plotDataLoadComplete = true;
        this.fbar.setVisible(!hasReportFilter && this.trendDataStore.getTotalCount() >= this.defaultRowSize);
        this.updateTrendPlot();
    },

    setTrendPlotLoading: function() {
        this.plotDataLoadComplete = false;
        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        Ext.get(trendDiv).update('Loading plot...');
        this.togglePDFExportBtn(false);
    },

    updateTrendPlot: function()
    {
        // if we are still loading either the guide set range data or the plot data, return without rendering
        if (!this.guideSetRangeStoreLoadComplete || !this.plotDataLoadComplete) {
            return;
        }

        this.togglePDFExportBtn(false);

        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        var plotConfig = {
            renderDiv: trendDiv,
            dataStore: this.trendDataStore,
            guideSetRangeStore: this.guideSetRangeStore,
            controlType: this.controlType,
            plotType: plotType,
            assayName: this.assayName,
            controlName: this.controlName,
            analyte: this.analyte,
            isotype: this.isotype,
            conjugate: this.conjugate,
            yAxisScale: this.yAxisScale
        };

        LABKEY.LeveyJenningsPlotHelper.renderPlot(plotConfig);

        this.togglePDFExportBtn(true);
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

    getTitrationSinglePointControlItems: function() {
        if (this.controlType === "Titration") {
            var panels = [];
            if (this.has4PLCurveFit) panels.push(this.ec504plPanel);
            if (this.has5PLCurveFit) panels.push(this.ec505plPanel);
            panels.push(this.aucPanel);
            panels.push(this.mfiPanel);
            return(panels);
        } else if (this.controlType === "SinglePoint") {
            return([this.singlePointControlPanel]);
        }
        return null;
    }
});
