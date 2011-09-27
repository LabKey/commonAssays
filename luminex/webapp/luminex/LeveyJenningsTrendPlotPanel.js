/*
 * Copyright (c) 2011 LabKey Corporation
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
            height: 360,
            border: false,
            cls: 'extContainer',
            disabled: true
        });

        this.addEvents('reportDateRangeApplied');

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // initialize the top toolbar with date range selection fields
        this.startDateLabel = new Ext.form.Label({text: 'Start Date'});
        this.startDateField = new Ext.form.DateField({
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '' && this.endDateField.isValid() && this.endDateField.getValue() != '')
                        this.refreshGraphButton.enable();
                },
                'invalid': function (df, msg) {
                    this.refreshGraphButton.disable();
                }
            }
        });
        this.endDateLabel = new Ext.form.Label({text: 'End Date'});
        this.endDateField = new Ext.form.DateField({
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '' && this.startDateField.isValid() && this.startDateField.getValue() != '')
                        this.refreshGraphButton.enable();
                },
                'invalid': function (df, msg) {
                    this.refreshGraphButton.disable();
                }
            }
        });

        // initialize the refesh graph button
        this.refreshGraphButton = new Ext.Button({
            disabled: true,
            text: 'Refresh Graph',
            handler: this.refreshGraphWithDates,
            scope: this
        });
        
        this.tbar = new Ext.Toolbar({
            height: 30,
            buttonAlign: 'center',
            items: [
                this.startDateLabel,
                {xtype: 'tbspacer', width: 10},
                this.startDateField,
                {xtype: 'tbspacer', width: 50},
                this.endDateLabel,
                {xtype: 'tbspacer', width: 10},
                this.endDateField,
                {xtype: 'tbspacer', width: 50},
                this.refreshGraphButton
            ]
        });

        // initialize the tab panel that will show the 3 different trend plots
        this.ec50Panel = new Ext.Panel({
            itemId: "EC50",
            title: "EC50",
            html: "<div id='EC50TrendPlotDiv'></div>",
            isRendered: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.aucPanel = new Ext.Panel({
            itemId: "AUC",
            title: "AUC",
            html: "<div id='AUCTrendPlotDiv'></div>",
            isRendered: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.mfiPanel = new Ext.Panel({
            itemId: "MaxMFI",
            title: "High MFI",
            html: "<div id='MaxMFITrendPlotDiv'></div>",
            isRendered: false,
            listeners: {
                scope: this,
                'activate': this.activateTrendPlotPanel
            }
        });
        this.trendTabPanel = new Ext.TabPanel({
            autoScroll: true,
            activeTab: 0,
            defaults: {
                height: 298,
                padding: 10
            },
            items: [this.ec50Panel, this.aucPanel, this.mfiPanel]
        });
        this.items.push(this.trendTabPanel);

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.initComponent.call(this);
    },

    // function called by the JSP when the graph params are selected and the "Reset Graph" button is clicked
    graphParamsSelected: function(analyte, isotype, conjugate) {
        // store the params locally
        this.analyte = analyte;
        this.isotype = isotype;
        this.conjugate = conjugate;

        // remove any previously entered values from the start and end date fields
        this.startDateField.reset();
        this.endDateField.reset();
        this.refreshGraphButton.disable();

        // show the trending tab panel and date range selection toolbar
        this.enable();

        this.setTabsToRender();
        this.displayTrendPlot();
    },

    activateTrendPlotPanel: function(panel) {
        // if the graph params have been selected and the trend plot for this panel hasn't been loaded, then call displayTrendPlot
        if (this.analyte && !panel.isRendered)
            this.displayTrendPlot();
    },

    setTabsToRender: function() {
        // something about the report has changed and all of the tabs need to be set to re-render
        this.ec50Panel.isRendered = false;
        this.aucPanel.isRendered = false;
        this.mfiPanel.isRendered = false;
    },

    displayTrendPlot: function() {
        // determine which tab is selected to know which div to update
        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = plotType + 'TrendPlotDiv';
        Ext.get(trendDiv).update('Loading...');

        // get the start and end date, if entered by the user
        var startDate = this.startDateField.getValue();
        var endDate = this.endDateField.getValue();

        // build the config object of the properties that will be needed by the R report
        var config = {reportId: 'module:luminex/schemas/core/Containers/LeveyJenningsTrendPlot.r', showSection: 'levey_jennings_trend_png'};
        // Ext.urlEncode({Protocol: this.assayName}).replace('Protocol=','')
        config['Protocol'] = this.assayName;
        config['PlotType'] = plotType;
        config['Titration'] = this.titration;
        config['Analyte'] = this.analyte;
        config['Isotype'] = this.isotype;
        config['Conjugate'] = this.conjugate;
        // provide either a start and end date or the max number of rows to display
        if (startDate != '' && endDate != '')
        {
            config['StartDate'] = startDate;
            config['EndDate'] = endDate;
        }
        else
        {
            config['MaxRows'] = this.defaultRowSize;
        }

        // call and display the Report webpart
        var wikiWebPartRenderer = new LABKEY.WebPart({
               partName: 'Report',
               renderTo: trendDiv,
               frame: 'none',
               partConfig: config
        });
        wikiWebPartRenderer.render();
        this.trendTabPanel.getActiveTab().isRendered = true;
    },

    refreshGraphWithDates: function() {
        // make sure that both date fields are not null
        if (this.startDateField.getValue() == '' || this.endDateField.getValue() == '')
        {
            Ext.Msg.alert("ERROR", "Please enter both a start date and an end date.");
        }
        else
        {
            this.setTabsToRender();
            this.displayTrendPlot();
            this.fireEvent('reportDateRangeApplied', this.startDateField.getValue(), this.endDateField.getValue());
        }
    }
});