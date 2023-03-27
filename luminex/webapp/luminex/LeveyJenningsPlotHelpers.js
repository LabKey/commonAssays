/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext.namespace('LABKEY', 'Luminex.panel');
LABKEY.LeveyJenningsPlotHelper = {};

LABKEY.LeveyJenningsPlotHelper.PlotTypeMap = {
    EC504PL: 'EC50 - 4PL',
    EC505PL: 'EC50 - 5PL Rumi',
    AUC: 'AUC',
    HighMFI: 'High MFI',
    MFI: 'MFI' // not sure why we cannot get these named right.
};

LABKEY.LeveyJenningsPlotHelper.TitrationColumnsMap = {
    "GuideSetId": "GuideSet",
    "AcquisitionDate": "Analyte/Data/AcquisitionDate",
    "LotNumber": "Analyte/Properties/LotNumber",
    "RunRowId": "Titration/Run/RowId",
    "AssayId": "Titration/Run/Name",
    "Network": "Titration/Run/Batch/Network",
    "NotebookNo": "Analyte/Data/Run/NotebookNo",
    "AssayType": "Analyte/Data/Run/AssayType",
    "ExpPerformer": "Analyte/Data/Run/ExpPerformer",
    "EC504PL": "Four ParameterCurveFit/EC50",
    "EC505PL": "Five ParameterCurveFit/EC50",
    "AUC": "TrapezoidalCurveFit/AUC",
    "HighMFI": "MaxFI",
};

LABKEY.LeveyJenningsPlotHelper.SinglePointControlColumnsMap = {
    "GuideSetId": "GuideSet",
    "AcquisitionDate": "Analyte/Data/AcquisitionDate",
    "LotNumber": "Analyte/Properties/LotNumber",
    "RunRowId": "SinglePointControl/Run/RowId",
    "AssayId": "SinglePointControl/Run/Name",
    "Network": "SinglePointControl/Run/Batch/Network",
    "NotebookNo": "SinglePointControl/Run/NotebookNo",
    "AssayType": "SinglePointControl/Run/AssayType",
    "ExpPerformer": "SinglePointControl/Run/ExpPerformer",
    "MFI": "AverageFiBkgd",
};

LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore = function(config)
{
    var isSinglePointControl = config.controlType === 'SinglePoint';
    var controlTypeColName = isSinglePointControl ? 'SinglePointControl' : config.controlType;
    var columnsMap = isSinglePointControl ? LABKEY.LeveyJenningsPlotHelper.SinglePointControlColumnsMap : LABKEY.LeveyJenningsPlotHelper.TitrationColumnsMap;

    var store = new LABKEY.ext.Store({
        autoLoad: true,
        schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(config.assayName),
        queryName: isSinglePointControl ? 'AnalyteSinglePointControl' : 'AnalyteTitration',
        columns: Object.values(columnsMap).join(','),
        filterArray: config.filters,
        sort: config.sort ? config.sort : '-Analyte/Data/AcquisitionDate, -' + controlTypeColName + '/Run/Created',
        maxRows: config.maxRows ? config.maxRows : -1,
        containerFilter: LABKEY.Query.containerFilter.allFolders,
        scope: config.scope
    });

    if (config.loadListener) {
        store.addListener('load', config.loadListener, config.scope);
    }

    return store;
};

LABKEY.LeveyJenningsPlotHelper.getGuideSetRangesStore = function(config)
{
    var sql = "SELECT gs.RowId, gs.Created, gs.ValueBased,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.EC504PLAverage ELSE cf.EC504PLAverage END AS GuideSetEC504PLAverage,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.EC504PLStdDev ELSE cf.EC504PLStdDev END AS GuideSetEC504PLStdDev,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.EC505PLAverage ELSE cf.EC505PLAverage END AS GuideSetEC505PLAverage,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.EC505PLStdDev ELSE cf.EC505PLStdDev END AS GuideSetEC505PLStdDev,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.AUCAverage ELSE cf.AUCAverage END AS GuideSetAUCAverage,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.AUCStdDev ELSE cf.AUCStdDev END AS GuideSetAUCStdDev,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.MaxFIAverage ELSE gs.TitrationMaxFIAverage END AS GuideSetHighMFIAverage,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.MaxFIStdDev ELSE gs.TitrationMaxFIStdDev END AS GuideSetHighMFIStdDev,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.MaxFIAverage ELSE gs.SinglePointControlFIAverage END AS GuideSetMFIAverage,\n" +
            "    CASE WHEN gs.ValueBased=true THEN gs.MaxFIStdDev ELSE gs.SinglePointControlFIStdDev END AS GuideSetMFIStdDev\n" +
            "FROM GuideSet gs\n" +
            "LEFT JOIN (\n" +
            "    SELECT\n" +
            "        GuideSetId,\n" +
            "        MIN(CASE WHEN CurveType = 'Trapezoidal' THEN AUCAverage ELSE NULL END) AS AUCAverage,\n" +
            "        MIN(CASE WHEN CurveType = 'Trapezoidal' THEN AUCStdDev ELSE NULL END) AS AUCStdDev,\n" +
            "        MIN(CASE WHEN CurveType = 'Four Parameter' THEN EC50Average ELSE NULL END) AS EC504PLAverage,\n" +
            "        MIN(CASE WHEN CurveType = 'Four Parameter' THEN EC50StdDev ELSE NULL END) AS EC504PLStdDev,\n" +
            "        MIN(CASE WHEN CurveType = 'Five Parameter' THEN EC50Average ELSE NULL END) AS EC505PLAverage,\n" +
            "        MIN(CASE WHEN CurveType = 'Five Parameter' THEN EC50StdDev ELSE NULL END) AS EC505PLStdDev\n" +
            "    FROM GuideSetCurveFit\n" +
            "    GROUP BY GuideSetId\n" +
            ") cf ON gs.RowId = cf.GuideSetId";

    var store = new LABKEY.ext.Store({
        autoLoad: true,
        schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(config.assayName),
        sql: sql,
        maxRows: -1,
        containerFilter: LABKEY.Query.containerFilter.allFolders,
        scope: config.scope
    });

    if (config.loadListener)
        store.addListener('load', config.loadListener, config.scope);

    return store;
};

LABKEY.LeveyJenningsPlotHelper.renderPlot = function(config)
{
    var plotData = [];
    var records = config.dataStore.getRange();
    var otherHoverProps = ['Network', 'AssayType', 'ExpPerformer', 'AcquisitionDate'];
    var columnsMap = config.controlType === 'SinglePoint' ? LABKEY.LeveyJenningsPlotHelper.SinglePointControlColumnsMap : LABKEY.LeveyJenningsPlotHelper.TitrationColumnsMap;

    var _pushData = function(record)
    {
        var data = {
            xLabel: record.get(columnsMap['NotebookNo']),
            pointColor: record.get(columnsMap['LotNumber']),
            value: record.get(columnsMap[config.plotType]),
        };

        // fall back to using the AssayId if the NotebookNo prop doesn't exist
        if (!record.get(columnsMap['NotebookNo'])) {
            data.xLabel = record.get(columnsMap['AssayId']);
        }

        // merge in the guide set range mean and stdDev values
        var gsRecordIndex = config.guideSetRangeStore ? config.guideSetRangeStore.findExact('RowId', record.get(columnsMap['GuideSetId'])) : -1;
        if (gsRecordIndex > -1) {
            var gsRecord = config.guideSetRangeStore.getAt(gsRecordIndex);
            data['gsMean'] = gsRecord.get('GuideSet' + config.plotType + 'Average');
            data['gsStdDev'] = gsRecord.get('GuideSet' + config.plotType + 'StdDev');
        }

        // add some other values to the data object for the hover display
        Ext.each(otherHoverProps, function(prop) {
            var val = record.get(columnsMap[prop]);

            // convert values that are date objects to a display string format
            if (val != null && LABKEY.Utils.isDate(val)) {
                val = new Date(val).format("Y-m-d"); // TODO use LABKEY.extDefaultDateFormat?
            }

            data[prop] = val;
        });

        plotData.push(data);
    };

    // find center point and trim
    var xTickTagIndex;
    if (config.runId)
    {
        var index;

        for (var i = 0; i < records.length; i++)
        {
            if (records[i].get(columnsMap['RunRowId']) == config.runId)
            {
                index = i;
                break;
            }
        }

        // this logic finds the range of the store we want to use for populating our graph with center on the current selected notebook
        var maxIndex = records.length-1;

        var windowRadius = 15;
        // check if test is passing in new window radius
        var param = LABKEY.ActionURL.getParameter("_testLJQueryLimit");
        if (param) windowRadius = parseInt(param);

        var start = index-windowRadius;
        var end = index+windowRadius;

        if ( start < 0)
            end += -start;
        else if ( end > maxIndex )
            start -= end - maxIndex;

        start = start < 0 ? 0 : start;
        end = end > maxIndex ? maxIndex : end;

        for (var i = start; i <= end; i++)
            _pushData(records[i]);

        // get tick tag location in the truncated list of records
        xTickTagIndex = index - start;
    }
    else
    {
        // iterate backwards through the store records so that plot goes left to right
        for (var i = records.length-1; i >=0; i--)
            _pushData(records[i]);
    }


    // clear div
    Ext.get(config.renderDiv).update('');

    // note consider enum/map here
    if (config.plotType in LABKEY.LeveyJenningsPlotHelper.PlotTypeMap)
        var ytitle = LABKEY.LeveyJenningsPlotHelper.PlotTypeMap[config.plotType];
    else
        throw "You specified an invalid plotType! Check valid values in LABKEY.LeveyJenningsPlotHelper.PlotTypeMap.";

    var title = config.controlName + ' ' + ytitle + ' for ' + config.analyte + ' - '
              + (config.isotype ? config.isotype : '[None]') + ' '
              + (config.conjugate ? config.conjugate : '[None]');

    var plotProperities = {
        value: 'value',
        mean: 'gsMean',
        stdDev: 'gsStdDev',
        xTickLabel: 'xLabel',
        yAxisScale: config.yAxisScale,
        color: 'pointColor',
        colorRange: ['black', 'red', 'green', 'blue', 'purple', 'orange', 'grey', 'brown'],
        hoverTextFn: function(row){
            var display = 'Notebook: ' + row.xLabel
                    + '\nLot Number: ' + (row.pointColor ? row.pointColor : '')
                    + '\n' + config.plotType + ': ' + row.value;

            // add any of the non-null extra display values to the hover
            Ext.each(otherHoverProps, function(prop) {
                if (row[prop] != null) {
                    display += '\n' + prop + ': ' + row[prop];
                }
            });

            return display;
        }
    };

    if (xTickTagIndex != undefined && xTickTagIndex != null)
        plotProperities['xTickTagIndex'] = xTickTagIndex;

    var plot = LABKEY.vis.LeveyJenningsPlot({
        renderTo: config.renderDiv,
        width: 850,
        height: 300,
        data: plotData,
        properties: plotProperities,
        gridLineColor: 'white',
        labels: {
            main: { value: title, fontSize: 16, position: 20 },
            y: {value: ytitle + (config.yAxisScale == 'log' ? ' (log)' : '')},
            x: {value: 'Assay'}
        }
    });
    plot.render();
};

// plotType: EC504PL, EC505PL, AUC, HighMFI
LABKEY.LeveyJenningsPlotHelper.getLeveyJenningsPlotWindow = function(protocolId, analyteId, typeId, plotType, controlType)
{
    var controlTypeColName = controlType === 'SinglePoint' ? 'SinglePointControl' : controlType;

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
            queryName: 'Analyte'+controlTypeColName,
            columns: [controlTypeColName+'/Name', 'Analyte/Name', controlTypeColName+'/Run/Isotype', controlTypeColName+'/Run/Conjugate', controlTypeColName+'/Run', 'Analyte/Data/AcquisitionDate'],
            filterArray: [
                LABKEY.Filter.create('Analyte', analyteId),
                LABKEY.Filter.create(controlTypeColName, typeId)
            ],
            success: function(data) {
                var row = data.rows[0];

                const filters = [
                    LABKEY.Filter.create('Analyte/Name', row['Analyte/Name']),
                    LABKEY.Filter.create(controlTypeColName + '/Name', row[controlTypeColName+'/Name']),
                    LABKEY.Filter.create(controlTypeColName + '/Run/Isotype', row[controlTypeColName+'/Run/Isotype']),
                    LABKEY.Filter.create(controlTypeColName + '/Run/Conjugate', row[controlTypeColName+'/Run/Conjugate']),
                ];
                if (controlType === 'Titration') {
                    filters.push(LABKEY.Filter.create('Titration/IncludeInQcReport', true));
                }

                var config = {
                    assayName: assayName,
                    controlName: row[controlTypeColName+'/Name'],
                    controlType: controlType,
                    analyte: row['Analyte/Name'],
                    isotype: row[controlTypeColName+'/Run/Isotype'],
                    conjugate: row[controlTypeColName+'/Run/Conjugate'],
                    yAxisScale: 'linear',
                    plotType: plotType,
                    runId: row[controlTypeColName+'/Run'],
                    filters: filters,
                    sort: 'Analyte/Data/AcquisitionDate, ' + controlTypeColName + '/Run/Created',
                    scope: this, // shouldn't matter but might blow up without it.
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
            modal: true,
            bodyStyle: {
                "background-color": "white"
            },
            items: [{
                xtype: 'box',
                autoEl: {
                    tag: 'div',
                    cls: 'ljplotdiv',
                    id: config.renderDiv
                },
                listeners : {
                    afterrender: function() {
                        window.getEl().mask("Loading...", "x-mask-loading");
                        var dataStoreLoaded = false;
                        var guideSetStoreLoaded = false;

                        config.loadListener = function(store) {
                            dataStoreLoaded = true;
                            config.dataStore = store;
                            if (guideSetStoreLoaded) {
                                LABKEY.LeveyJenningsPlotHelper.renderPlot(config);
                                window.getEl().unmask();
                            }
                        };
                        LABKEY.LeveyJenningsPlotHelper.getTrackingDataStore(config);

                        LABKEY.LeveyJenningsPlotHelper.getGuideSetRangesStore({
                            assayName: config.assayName,
                            loadListener: function(store) {
                                guideSetStoreLoaded = true;
                                config.guideSetRangeStore = store;
                                if (dataStoreLoaded) {
                                    LABKEY.LeveyJenningsPlotHelper.renderPlot(config);
                                    window.getEl().unmask();
                                }
                            },
                        });
                    }
                }
            }]
        });

        window.show();
    }

};
