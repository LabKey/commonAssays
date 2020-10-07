import { Utils, getServerContext, ActionURL } from '@labkey/api';
import { naturalSort } from "@labkey/components";

import { SAMPLE_COL_NAME } from "./constants";
import { PlotOptions, SelectOptions } from "./models";

// TODO update or remove
export function formatFitParams(row) {
    if (row.CurveFitParams) {
        const parts = row.CurveFitParams.split('&');
        return 'Slope : ' + formatNumber(parts[0])
            + ', Intercept : ' + formatNumber(parts[1]);
    }
    return 'There was an error loading the curve fit parameters.';
}

export function formatNumber(value) {
    return (Math.round(value * 10000) / 10000).toFixed(4);
}

const valExponentialDigits = 6;
export function tickFormatFn(value) {
    if (Utils.isNumber(value) && Math.abs(Math.round(value)).toString().length >= valExponentialDigits) {
        return value.toExponential();
    }
    return value;
}

// TODO update or remove
export function linearCurveFitFn(params){
    if (params && params.length >= 2) {
        return function(x){return x * params[0] + params[1];}
    }
    return function(x) {return x;}
}

export function exportSVGToFile(svgEl, format) {
    if (svgEl && svgEl.length > 0) {
        const LABKEY = getServerContext();
        const title = 'Calibration Curve';
        LABKEY.vis.SVGConverter.convert(svgEl[0], format, title);
    }
}

export function filterDataByPlotOptions(data: any[], options: PlotOptions, includeSampleFilter = true): any[] {
    const { plateName, spot, samples } = options;
    return data.filter((row) => {
        return (plateName === undefined || row['PlateName'] === plateName)
            && (spot === undefined || row['Spot'] === spot)
            && (!includeSampleFilter || samples === undefined || samples.indexOf(row[SAMPLE_COL_NAME]) > -1)
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

export function getUniqueValues(dataArray: any[], prop: string): any[] {
    return [...new Set(dataArray.map(d => d[prop]))].sort(naturalSort);
}

export function getSelectOptions(data: any[]): SelectOptions[] {
    return data.map((value) => ({value: value, label: value || 'null'}));
}

export function getResultsViewURL(protocolId: number, runId: number, plotOptions: PlotOptions) {
    const { plateName, spot, samples } = plotOptions;

    const params = {
        rowId: protocolId, 'Data.Run/RowId~eq': runId
    };
    if (plateName !== undefined) {
        params['Data.PlateName~eq'] = plateName;
    }
    if (spot !== undefined) {
        params['Data.Spot~eq'] = spot;
    }
    if (samples !== undefined) {
        params['Data.' + SAMPLE_COL_NAME + '~in'] = samples.join(';');
    }

    return ActionURL.buildURL('assay', 'assayResults', undefined, params);
}