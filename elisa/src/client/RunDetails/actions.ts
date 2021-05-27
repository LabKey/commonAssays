import { Ajax, ActionURL, Utils, getServerContext } from "@labkey/api";

import { CurveFitData } from "./models";
import { getMaxFromData, getMinFromData } from "./utils";
import { DEFAULT_X_AXIS_PROP } from "./constants";

export function getCurveFitXYPairs(protocolId: number, runId: number, plateName: string, spot: number, data: any[]): Promise<CurveFitData> {
    return new Promise((resolve, reject) => {
        const xMin = getMinFromData(data, DEFAULT_X_AXIS_PROP);
        let xMax = getMaxFromData(data, DEFAULT_X_AXIS_PROP);
        if (xMin === xMax) {
            xMax += 1;
        }

        Ajax.request({
            url: ActionURL.buildURL('elisa', 'getCurveFitXYPairs.api'),
            method: 'POST',
            params: {
                rowId: protocolId,
                runId,
                plateName: plateName ?? 'PLACEHOLDER_PLATE',
                spot: spot ?? 1,
                xMin: xMin,
                xMax: xMax,
                numberOfPoints: 10000
            },
            success: Utils.getCallbackWrapper((data) => resolve(data)),
            failure: Utils.getCallbackWrapper((error) => reject(error))
        });
    });
}

export function exportSVGToFile(svgEl, format, title: string) {
    if (svgEl) {
        const LABKEY = getServerContext();
        LABKEY.vis.SVGConverter.convert(svgEl, format, 'Calibration Curve - ' + title);
    }
}