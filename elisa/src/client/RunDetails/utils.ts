import { Utils, getServerContext, ActionURL } from '@labkey/api';
import { naturalSort } from "@labkey/components";

import {
    CONTROL_COL_NAME,
    HOVER_COLUMN_NAMES,
    ID_COL_NAME, PLOT_HEIGHT,
    SAMPLE_COL_NAME,
    STANDARDS_LABEL,
    WELL_GROUP_COL_NAME,
    DEFAULT_X_AXIS_PROP,
    DEFAULT_Y_AXIS_PROP,
    SPOT_COL_NAME,
    REQUIRED_COLUMN_NAMES
} from "./constants";
import { CurveFitData, PlotOptions, SelectOptions } from "./models";

const valExponentialDigits = 6;
export function tickFormatFn(value) {
    if (Utils.isNumber(value) && Math.abs(Math.round(value)).toString().length >= valExponentialDigits) {
        return value.toExponential();
    }
    return value;
}

export function filterDataByPlotOptions(data: any[], samples: string[], controls: string[], options: PlotOptions, includeIdFilter = true): any[] {
    const { plateName, spot, showAllSamples, showAllControls } = options;
    const selectedSamples = showAllSamples ? samples : options.samples;
    const selectedControls = showAllControls ? controls : options.controls;
    const selectedIds = selectedSamples.concat(selectedControls);

    return data.filter((row) => {
        return (plateName === undefined || row['PlateName'] === plateName)
            && (spot === undefined || row[SPOT_COL_NAME] === spot)
            && (!includeIdFilter || selectedIds.indexOf(row[ID_COL_NAME]) > -1)
    });
}

export function getMinFromData(data: any[], prop: string, includeNull = true): number {
    let values = data.map((row) => row[prop] as number);
    if (!includeNull) {
        values = values.filter(d => d !== null);
    }
    return Math.min(...values);
}

export function getMaxFromData(data: any[], prop: string, includeNull = true): number {
    let values = data.map((row) => row[prop] as number);
    if (!includeNull) {
        values = values.filter(d => d !== null);
    }
    return Math.max(...values);
}

export function getUniqueValues(dataArray: any[], prop: string, includeNull = true): any[] {
    let values = dataArray.map(d => d[prop]);
    if (!includeNull) {
        values = values.filter(d => d !== null && d !== undefined);
    }
    return [...new Set(values)].sort(naturalSort);
}

export function getSelectOptions(data: any[], columnInfo?: {[key: string]: any}): SelectOptions[] {
    return data.map((value) => {
        let label = value || 'null';
        if (columnInfo && columnInfo[value]) {
            label = columnInfo[value].caption;
        }

        return {
            value,
            label
        }
    });
}

export function getResultsViewURL(protocolId: number, runId: number, plotOptions: PlotOptions) {
    const { plateName, spot, samples, controls, showAllSamples, showAllControls } = plotOptions;

    const params = {
        rowId: protocolId, 'Data.Run/RowId~eq': runId
    };
    if (plateName !== undefined) {
        params['Data.PlateName~eq'] = plateName;
    }
    if (spot !== undefined) {
        params['Data.' + SPOT_COL_NAME + '~eq'] = spot;
    }

    if (!showAllSamples) {
        // add in an extra separator to make this an OR between the samples and controls
        params['Data.' + SAMPLE_COL_NAME + '~in'] = samples.join(';') + ';';
    }

    if (!showAllControls) {
        // add in an extra separator to make this an OR between the samples and controls
        if (controls.indexOf(STANDARDS_LABEL) > -1) {
            if (showAllSamples || samples.length > 0) {
                params['Data.' + WELL_GROUP_COL_NAME + '~containsoneof'] = 'Standards;Sample';
            }
            else {
                params['Data.' + WELL_GROUP_COL_NAME + '~eq'] = 'Standards';
            }
        }
        else {
            params['Data.' + CONTROL_COL_NAME + '~in'] = controls.join(';') + ';';
        }
    }

    return ActionURL.buildURL('assay', 'assayResults', undefined, params);
}

export function getPlotTitle(runName: string, plotOptions: PlotOptions): string {
    const { plateName, spot } = plotOptions;
    return runName
        + (plateName !== undefined ? ' - ' + plateName : '')
        + (spot !== undefined ? ' - Spot ' + spot : '');
}

export function getUniqueIdsForPlotSelections(data: any[], plotOptions: PlotOptions, colName: string): string[] {
    const filteredData = filterDataByPlotOptions(data, [], [], plotOptions, false);
    return getUniqueValues(filteredData, colName, false);
}

export function shouldReloadCurveFitData(prevPlotOptions: PlotOptions, newPlotOptions: PlotOptions): boolean {
    return prevPlotOptions.plateName !== newPlotOptions.plateName || prevPlotOptions.spot !== newPlotOptions.spot;
}

export function getPlotConfigFromOptions(
    renderId: string, width: number, plotOptions: PlotOptions, data: any[], curveFitData: CurveFitData,
    runName: string, columnInfo: {[key: string]: any}
): {[key: string]: any} {
    const LABKEY = getServerContext();

    const aes = {
        x: plotOptions.xAxisMeasure,
        y: plotOptions.yAxisMeasure,
        hoverText: function(row){
            const cols = HOVER_COLUMN_NAMES.concat([plotOptions.xAxisMeasure, plotOptions.yAxisMeasure]);
            return cols.map((col) => {
                return col + ': ' + row[col];
            }).join('\n');
        }
    };
    if (plotOptions.showLegend) {
        aes['color'] = ID_COL_NAME;
    }

    const layers = [];
    if (data !== undefined) {
        layers.push(
            new LABKEY.vis.Layer({
                data,
                geom: new LABKEY.vis.Geom.Point({
                    plotNullPoints: true,
                    size: 5,
                    opacity: .5,
                    color: '#116596'
                }),
                aes
            })
        );
    }
    if (plotOptions.showCurve && curveFitData) {
        layers.push(
            new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Path({
                    size: 2,
                    color: '#555'
                }),
                aes: {x: 'x', y: 'y'},
                data: curveFitData.points
            })
        );
    }

    const margins = {};
    if (!plotOptions.showLegend) {
        margins['right'] = 10;
    }

    const scales = {
        x: {scaleType: 'continuous', trans: plotOptions.xAxisScale, tickFormat: tickFormatFn},
        y: {scaleType: 'continuous', trans: plotOptions.yAxisScale, tickFormat: tickFormatFn}
    };

    return {
        renderTo: renderId,
        width,
        height: PLOT_HEIGHT,
        layers,
        labels: {
            main: { value: getPlotTitle(runName, plotOptions) },
            x: { value: columnInfo[plotOptions.xAxisMeasure]?.caption || plotOptions.xAxisMeasure },
            y: { value: columnInfo[plotOptions.yAxisMeasure]?.caption || plotOptions.yAxisMeasure }
        },
        margins,
        scales
    };
}

export function getColumnInfoFromQueryDetails(queryInfo: {[key: string]: any}): {[key: string]: any} {
    const columnInfo = {}, measures = [DEFAULT_X_AXIS_PROP, DEFAULT_Y_AXIS_PROP];
    queryInfo.columns.forEach((col) => {
        columnInfo[col.fieldKey] = col;
        if (col.measure && col.fieldKey !== SPOT_COL_NAME && measures.indexOf(col.fieldKey) === -1) {
            measures.push(col.fieldKey);
        }
    });

    return {columnInfo, measures: measures.sort()};
}

export function parsePlotDataFromResponse(response: {[key: string]: any}): any[] {
    const data = [];
    response.rows.forEach((row) => {
        // consolidate the control name for the standard wells so they plot the same color
        const isStandard = row[WELL_GROUP_COL_NAME] === 'Standards';
        const isControl = row[WELL_GROUP_COL_NAME]?.startsWith('Control');
        if (isStandard) {
            row[CONTROL_COL_NAME] = STANDARDS_LABEL;
        }
        // if the row is a control but we don't have a control name, set it to a default value
        else if (isControl && !row[CONTROL_COL_NAME]) {
            row[CONTROL_COL_NAME] = 'Unknown Control';
        }

        // if the row has a control value, make sure the sample column is null
        if (row[CONTROL_COL_NAME]) {
            row[SAMPLE_COL_NAME] = null;
        }

        row[ID_COL_NAME] = row[CONTROL_COL_NAME] || row[SAMPLE_COL_NAME];
        data.push(row);
    });

    return data;
}

export function getUpdatedPlotOptions(key: string, value: any, resetIdSelection: boolean, plotOptions: PlotOptions): PlotOptions {
    const updatedPlotOptions = {
        ...plotOptions,
        [key]: value
    };

    updatedPlotOptions.showAllSamples = resetIdSelection ? true : updatedPlotOptions.showAllSamples;
    updatedPlotOptions.samples = resetIdSelection || updatedPlotOptions.showAllSamples ? [] : updatedPlotOptions.samples;
    updatedPlotOptions.showAllControls = resetIdSelection ? true : updatedPlotOptions.showAllControls;
    updatedPlotOptions.controls = resetIdSelection || updatedPlotOptions.showAllControls ? [] : updatedPlotOptions.controls;
    return updatedPlotOptions;
}

export function getDefaultPlotOptions(plates: string[], spots: number[]): PlotOptions {
    return {
        showCurve: true,
        showLegend: true,
        plateName: arrayHasNonNullValues(plates) ? plates[0] : undefined,
        spot: arrayHasNonNullValues(spots) ? spots[0] : undefined,
        showAllSamples: true,
        samples: [],
        showAllControls: true,
        controls: [],
        xAxisScale: 'linear',
        xAxisMeasure: DEFAULT_X_AXIS_PROP,
        yAxisScale: 'linear',
        yAxisMeasure: DEFAULT_Y_AXIS_PROP
    } as PlotOptions;
}

export function arrayHasNonNullValues(arr: any[]): boolean {
    return arr?.length > 1 || (arr?.length === 1 && arr[0] !== null);
}

export function getMissingFields(columnInfo: {[key: string]: any}): string[] {
    const missing = [];
    if (columnInfo) {
        REQUIRED_COLUMN_NAMES.forEach((col) => {
            if (!columnInfo[col]) {
                missing.push(col);
            }
        });
    }
    return missing
}