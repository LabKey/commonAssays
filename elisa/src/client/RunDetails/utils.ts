import { Utils, getServerContext, ActionURL } from '@labkey/api';
import { naturalSort } from "@labkey/components";

import { CONTROL_COL_NAME, ID_COL_NAME, SAMPLE_COL_NAME, STANDARDS_LABEL, WELL_GROUP_COL_NAME } from "./constants";
import { PlotOptions, SelectOptions } from "./models";

// export function formatNumber(value) {
//     return (Math.round(value * 10000) / 10000).toFixed(4);
// }

const valExponentialDigits = 6;
export function tickFormatFn(value) {
    if (Utils.isNumber(value) && Math.abs(Math.round(value)).toString().length >= valExponentialDigits) {
        return value.toExponential();
    }
    return value;
}

export function exportSVGToFile(svgEl, format, title: string) {
    if (svgEl && svgEl.length > 0) {
        const LABKEY = getServerContext();
        LABKEY.vis.SVGConverter.convert(svgEl[0], format, 'Calibration Curve - ' + title);
    }
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

export function getMinFromData(data: any[], prop: string): number {
    const values = data.map((row) => row[prop] as number);
    return Math.min(...values);
}

export function getMaxFromData(data: any[], prop: string): number {
    const values = data.map((row) => row[prop] as number);
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
                params['Data.' + WELL_GROUP_COL_NAME + '~containsoneof'] = STANDARDS_LABEL + ';Sample';
            }
            else {
                params['Data.' + WELL_GROUP_COL_NAME + '~in'] = STANDARDS_LABEL;
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