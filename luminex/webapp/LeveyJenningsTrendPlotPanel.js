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

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        // initialize the top toolbar with date range selection fields
        this.startDateLabel = new Ext.form.Label({text: 'Start Date'});
        this.startDateField = new Ext.form.DateField({});
        this.endDateLabel = new Ext.form.Label({text: 'End Date'});
        this.endDateField = new Ext.form.DateField({});
        this.refreshGraphButton = new Ext.Button({text: 'Refresh Graph', handler: this.refreshGraphWithDates, scope: this});
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
            title: "EC50",
            html: "<div id='EC50TrendPlotDiv'></div>"
        });
        this.aucPanel = new Ext.Panel({
            title: "AUC",
            html: "<div id='AUCTrendPlotDiv'>Coming Soon</div>"
        });
        this.mfiPanel = new Ext.Panel({
            title: "High MFI",
            html: "<div id='HighMFITrendPlotDiv'>Coming Soon</div>"
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

        // show the trending tab panel and date range selection toolbar
        this.enable();

        this.displayTrendPlot();
    },

    displayTrendPlot: function() {
        // determine which tab is selected to know which div to update
        var trendDiv = 'EC50TrendPlotDiv';

        Ext.get(trendDiv).update('Loading...');
        var config = {reportId: 'module:luminex/schemas/core/Containers/EC50TrendPlot.r', showSection: 'ec50trend_png'};
        // Ext.urlEncode({Protocol: this.assayName}).replace('Protocol=','')
        config['Protocol'] = this.assayName;
        config['Titration'] = this.titration;
        config['Analyte'] = this.analyte;
        config['Isotype'] = this.isotype;
        config['Conjugate'] = this.conjugate;
        var wikiWebPartRenderer = new LABKEY.WebPart({
               partName: 'Report',
               renderTo: trendDiv,
               frame: 'none',
               partConfig: config
        });
        wikiWebPartRenderer.render();
    },

    refreshGraphWithDates: function() {
        alert('Support for user defined date range coming soon.');
    }
});