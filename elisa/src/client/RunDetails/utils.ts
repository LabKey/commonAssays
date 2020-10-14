import { Utils, getServerContext, ActionURL } from '@labkey/api';
import { naturalSort } from "@labkey/components";

import {
    CONTROL_COL_NAME,
    HOVER_COLUMN_NAMES,
    ID_COL_NAME, PLOT_HEIGHT,
    SAMPLE_COL_NAME,
    STANDARDS_LABEL,
    WELL_GROUP_COL_NAME,
    X_AXIS_PROP, Y_AXIS_PROP
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
            && (spot === undefined || row['Spot'] === spot)
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
        values = values.filter(d => d !== null);
    }
    return [...new Set(values)].sort(naturalSort);
}

export function getSelectOptions(data: any[]): SelectOptions[] {
    return data.map((value) => ({value: value, label: value || 'null'}));
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
        params['Data.Spot~eq'] = spot;
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
        x: X_AXIS_PROP,
        y: Y_AXIS_PROP,
        hoverText: function(row){
            return HOVER_COLUMN_NAMES.map((col) => {
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
            x: { value: columnInfo[X_AXIS_PROP] ? columnInfo[X_AXIS_PROP].caption : X_AXIS_PROP },
            y: { value: columnInfo[Y_AXIS_PROP] ? columnInfo[Y_AXIS_PROP].caption : Y_AXIS_PROP }
        },
        margins,
        scales
    };
}

export function getColumnInfoFromQueryDetails(queryInfo: {[key: string]: any}): {[key: string]: any} {
    const columnInfo = {};
    queryInfo.columns.forEach((col) => {
        columnInfo[col.fieldKey] = col;
    });

    return columnInfo;
}

export function parsePlotDataFromResponse(response: {[key: string]: any}): any[] {
    const data = [];
    response.rows.forEach((row) => {
        // consolidate the control name for the standard wells so they plot the same color
        const isStandard = row[WELL_GROUP_COL_NAME] === 'Standards';
        if (isStandard) {
            row[CONTROL_COL_NAME] = STANDARDS_LABEL;
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
        yAxisScale: 'linear'
    } as PlotOptions;
}

export function arrayHasNonNullValues(arr: any[]): boolean {
    return arr?.length > 1 || (arr?.length === 1 && arr[0] !== null);
}