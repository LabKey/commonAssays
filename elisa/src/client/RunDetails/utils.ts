import { Utils, getServerContext } from '@labkey/api';

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