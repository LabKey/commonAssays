Ext.namespace('LABKEY', 'Luminex.panel');
LABKEY.LeveyJenningsPlotHelper = {};

// NOTE: we should be able to avoid passing around values.
// NOTE: consider setting this up to be config based... potential defaulting values here...
LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore = function(config)
{
    // NOTE: leaving this method in it's abstracted form
    function getBaseWhereClause(controlType, analyte, controlName, isotype, conjugate) {
        var controlTypeColName = controlType == "SinglePoint" ? "SinglePointControl" : controlType;
        var whereClause = " WHERE Analyte.Name='" + analyte.replace(/'/g, "''") + "'"
                + " AND " + controlTypeColName + ".Name='" + controlName.replace(/'/g, "''") + "'"
                + (isotype != '' ? " AND " + controlTypeColName + ".Run.Isotype='" + isotype.replace(/'/g, "''") + "'"
                        : " AND " + controlTypeColName + ".Run.Isotype IS NULL")
                + (conjugate != '' ? " AND " + controlTypeColName + ".Run.Conjugate='" + conjugate.replace(/'/g, "''") + "'"
                        : " AND " + controlTypeColName + ".Run.Conjugate IS NULL");

        if (controlType == "Titration") {
            whereClause += " AND Titration.IncludeInQcReport=true";
        }

        return whereClause;
    }

    // build the array of filters to be applied to the store
    var whereClause = getBaseWhereClause(config.controlType, config.analyte, config.controlName, config.isotype, config.conjugate);

    // add on any filtering (from LeveyJenningsTrackingDataPanel.js)
    if (config.whereClause) {
        whereClause += config.whereClause;
    }

    // generate sql for the data store (columns depend on the control type)
    var controlTypeColName = config.controlType == "SinglePoint" ? "SinglePointControl" : config.controlType;
    var sql = "SELECT Analyte"
            + ", " + controlTypeColName + ".Run.Created" // NOTE: necessary for union case
            + ", Analyte.Data.AcquisitionDate"
            + ", Analyte.Properties.LotNumber"
            + ", " + controlTypeColName
            + ", " + controlTypeColName + ".Run.Isotype"
            + ", " + controlTypeColName + ".Run.Conjugate"
            + ", " + controlTypeColName + ".Run.RowId AS RunRowId"
            + ", " + controlTypeColName + ".Run.Name AS RunName"
            + ", " + controlTypeColName + ".Run.Folder.Name AS FolderName"
            + ", " + controlTypeColName + ".Run.Folder.EntityId"
            + (config.networkExists ? ", " + controlTypeColName + ".Run.Batch.Network" : "")
            + (config.protocolExists ? ", " + controlTypeColName + ".Run.Batch.CustomProtocol" : "")
            + ", " + controlTypeColName + ".Run.NotebookNo"
            + ", " + controlTypeColName + ".Run.AssayType"
            + ", " + controlTypeColName + ".Run.ExpPerformer"
            + ", GuideSet.Created AS GuideSetCreated"
            + ", IncludeInGuideSetCalculation"
            + ", GuideSet.ValueBased AS GuideSetValueBased";
    if (config.controlType == "Titration")
    {
        sql += ", \"Four ParameterCurveFit\".EC50 AS EC504PL, \"Four ParameterCurveFit\".EC50QCFlagsEnabled AS EC504PLQCFlagsEnabled"
        + ", \"Five ParameterCurveFit\".EC50 AS EC505PL, \"Five ParameterCurveFit\".EC50QCFlagsEnabled AS EC505PLQCFlagsEnabled"
        + ", TrapezoidalCurveFit.AUC, TrapezoidalCurveFit.AUCQCFlagsEnabled"
        + ", MaxFI AS HighMFI, MaxFIQCFlagsEnabled AS HighMFIQCFlagsEnabled"
            //columns needed for guide set ranges (value based or run based)
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.EC504PLAverage ELSE GuideSet.\"Four ParameterCurveFit\".EC50Average END AS GuideSetEC504PLAverage"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.EC504PLStdDev ELSE GuideSet.\"Four ParameterCurveFit\".EC50StdDev END AS GuideSetEC504PLStdDev"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.EC505PLAverage ELSE GuideSet.\"Five ParameterCurveFit\".EC50Average END AS GuideSetEC505PLAverage"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.EC505PLStdDev ELSE GuideSet.\"Five ParameterCurveFit\".EC50StdDev END AS GuideSetEC505PLStdDev"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.AUCAverage ELSE GuideSet.TrapezoidalCurveFit.AUCAverage END AS GuideSetAUCAverage"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.AUCStdDev ELSE GuideSet.TrapezoidalCurveFit.AUCStdDev END AS GuideSetAUCStdDev"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.MaxFIAverage ELSE GuideSet.TitrationMaxFIAverage END AS GuideSetHighMFIAverage"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.MaxFIStdDev ELSE GuideSet.TitrationMaxFIStdDev END AS GuideSetHighMFIStdDev"
        + " FROM AnalyteTitration "
        + whereClause;
    }
    else if (config.controlType == "SinglePoint")
    {
        sql += ", AverageFiBkgd AS MFI, AverageFiBkgdQCFlagsEnabled AS MFIQCFlagsEnabled"
            //columns needed for guide set ranges (value based or run based)
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.MaxFIAverage ELSE GuideSet.SinglePointControlFIAverage END AS GuideSetMFIAverage"
        + ", CASE WHEN GuideSet.ValueBased=true THEN GuideSet.MaxFIStdDev ELSE GuideSet.SinglePointControlFIStdDev END AS GuideSetMFIStdDev"
        + " FROM AnalyteSinglePointControl "
        + whereClause;
    }

    if (config.orderBy)
        sql += config.orderBy;

    // NOTE: watch out for this case. mixing with other params might end badly (consider orderby)
    if (config.centerDate)
    {
        var finalSql = sql;
        finalSql += " AND Analyte.Data.AcquisitionDate >= CAST('" + config.centerDate + "' AS DATE)";
        finalSql += " UNION " + sql;
        finalSql += " AND Analyte.Data.AcquisitionDate < CAST('" + config.centerDate + "' AS DATE)";
        finalSql += " ORDER BY Analyte.Data.AcquisitionDate DESC, "+controlTypeColName+".Run.Created DESC LIMIT 30 ";

        sql = finalSql; // swap back
    }

    var store = new LABKEY.ext.Store({
        autoLoad: false,
        schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(config.assayName),
        sql: sql,
        containerFilter: LABKEY.Query.containerFilter.allFolders,
        scope: config.scope
    });

    // not assuming scope for now...
    if (config.loadListener)
        store.addListener('load', config.loadListener, config.scope);

    return store;
};

// consider reducing scope of this object...?
LABKEY.LeveyJenningsPlotHelper.PlotTypeMap = Object.freeze({
    EC504PL: 'EC50 - 4PL',
    EC505PL: 'EC50 - 5PL',
    AUC: 'AUC',
    HighMFI: 'MFI',
    MFI: 'MFI' // not sure why we cannot get these named right.
});

LABKEY.LeveyJenningsPlotHelper.renderPlot = function(config)
{
    var plotData = [];
    var records = config.store.getRange();

    var _pushData = function(record)
    {
        plotData.push({
            xLabel: record.get('NotebookNo'),
            pointColor: record.get('LotNumber'),
            value: record.get(config.plotType),
            gsMean: record.get('GuideSet' + config.plotType + 'Average'),
            gsStdDev: record.get('GuideSet' + config.plotType + 'StdDev')
        });
    };

    // find center point and trim
    var xTickTagIndex;
    if (config.notebook)
    {
        var index;
        for (var i = 0; i < records.length; i++)
        {
            if (records[i].get('NotebookNo') == config.notebook)
            {
                index = i;
                break;
            }
        }

        // this logic finds the range of the store we want to use for populating our graph with center on the current selected notebook
        var maxIndex = records.length-1;
        var start = index-15;
        var end = index+15;

        if ( start < 0)
            end += -start;
        else if ( ending > maxIndex )
            start -= ending - maxIndex;

        start = start < 0 ? 0 : start;
        end = end > maxIndex ? maxIndex : end;

        // iterate backwards through the store records so that plot goes left to right
        for (var i = end; i >=start; i--)
            _pushData(records[i]);

        // get tick tag location and reverse it
        xTickTagIndex = end - (index - start);
    }
    else
    {
        // iterate backwards through the store records so that plot goes left to right
        for (var i = records.length-1; i >=0; i--)
            _pushData(records[i]);
    }


    // clear div
    Ext.get(config.renderDiv).update('');

    var renderType = Ext.isIE8 ? 'raphael' : 'd3';
    var title = config.controlName + ' ' + config.plotType + ' for ' + config.analyte + ' - '
              + (config.isotype ? config.isotype : '[None]') + ' '
              + (config.conjugate ? config.conjugate : '[None]');

    // note consider enum/map here
    if (config.plotType in LABKEY.LeveyJenningsPlotHelper.PlotTypeMap)
        var ytitle = LABKEY.LeveyJenningsPlotHelper.PlotTypeMap[config.plotType];
    else
        throw "You specified an invalid plotType! Check valid values in LABKEY.LeveyJenningsPlotHelper.PlotTypeMap.";

    var plotProperities = {
        value: 'value',
        mean: 'gsMean',
        stdDev: 'gsStdDev',
        xTickLabel: 'xLabel',
        yAxisScale: this.yAxisScale,
        color: 'pointColor',
        colorRange: ['black', 'red', 'green', 'blue', 'purple', 'orange', 'grey', 'brown'],
        hoverTextFn: function(row){
            return 'Notebook: ' + row.xLabel
                    + '\nLot Number: ' + row.pointColor
                    + '\n' + config.plotType + ': ' + row.value;
        }
    };

    if (xTickTagIndex != undefined && xTickTagIndex != null)
        plotProperities['xTickTagIndex'] = xTickTagIndex;

    var plot = LABKEY.vis.LeveyJenningsPlot({
        renderTo: config.renderDiv,
        rendererType: renderType,
        width: 850,
        height: 300,
        data: plotData,
        properties: plotProperities,
        gridLineColor: 'white',
        labels: {
            main: { value: title, fontSize: 16, position: 20 },
            y: {value: ytitle + (this.yAxisScale == 'log' ? ' (log)' : '')},
            x: {value: 'Assay'}
        }
    });
    plot.render();

    return renderType;
};

Luminex.panel.LeveyJenningsPlotPanel = Ext.extend(Ext.Panel, {

    initComponent : function() {

        var config = this.config;

        this.items = [{
            xtype: 'box',
            autoEl: {
                tag: 'div',
                id: 'leveyJennigsPlotDiv',
                html: ''
            },
            listeners: {
                afterrender: function() {
                    LABKEY.LeveyJenningsPlotHelper.renderPlot(config);
                }
            }
        }];

        Luminex.window.LeveyJenningsPlotPanel.superclass.initComponent.call(this);
    }
});

// plotType: EC504PL, EC505PL, AUC, HighMFI
LABKEY.LeveyJenningsPlotHelper.getLeveyJenningsPlotWindow = function(protocolId, analyteId, typeId, plotType, controlType)
{
    if (controlType == null)
        controlType = 'Titration';

    LABKEY.Assay.getById({
        id: protocolId,
        success: function(assay){
            if (Ext.isArray(assay) && assay.length == 1)
                _getConfig(assay[0].name);
        }
    });

    var _getConfig = function(assayName)
    {
        // note make sure to mix assayName into config...

        LABKEY.Query.selectRows({
            schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(assayName),
            //queryName: 'AnalyteTitration',
            queryName: 'Analyte'+controlType,
            columns: [controlType+'/Name', 'Analyte/Name', controlType+'/Run/Isotype', controlType+'/Run/Conjugate', controlType+'/Run/NotebookNo', 'Analyte/Data/AcquisitionDate'],
            filterArray: [
                LABKEY.Filter.create('Analyte', analyteId),
                LABKEY.Filter.create(controlType, typeId)
            ],
            success: function(data) {
                var row = data.rows[0];
                var config = {
                    assayName: assayName,
                    controlName: row[controlType+'/Name'],
                    controlType: controlType == "SinglePointControl" ? "SinglePoint" : "Titration",
                    analyte: row['Analyte/Name'],
                    isotype: row[controlType+'/Run/Isotype'],
                    conjugate: row[controlType+'/Run/Conjugate'],
                    scope: this, // shouldn't matter but might blow up without it.
                    plotType: plotType,
                    notebook: row[controlType+'/Run/NotebookNo'],
                    centerDate: row['Analyte/Data/AcquisitionDate']
                };

                _createWindow(config);
            }
        });
    };

    var _createWindow = function(config)
    {
        config.renderDiv = Ext.id();

        var window = new Ext.Window({
            title: 'Levey-Jennings Plot',
            width: 855,
            height: 325,
            bodyStyle: {
                "background-color": "white"
            },
            items: [{
                xtype: 'box',
                autoEl: {
                    tag: 'div',
                    id: config.renderDiv
                },
                listeners : {
                    afterrender: function() {

                        window.getEl().mask("Loading...", "x-mask-loading");

                        config.loadListener = function(store) {
                            config.store = store;
                            LABKEY.LeveyJenningsPlotHelper.renderPlot(config);
                            window.getEl().unmask();
                        };

                        var store = LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore(config);
                        store.load()
                    }
                }
            }]
        });

        window.show();
    }

};
