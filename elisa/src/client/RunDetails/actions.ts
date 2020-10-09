import { Ajax, ActionURL, Utils } from "@labkey/api";
import { CurveFitData } from "./models";
import { getMaxFromData, getMinFromData } from "./utils";
import { X_AXIS_PROP } from "./constants";

export function getCurveFitXYPairs(protocolId: number, runId: number, plateName: string, spot: number, data: any[]): Promise<CurveFitData> {
    return new Promise((resolve, reject) => {
        const xMin = getMinFromData(data, X_AXIS_PROP);
        let xMax = getMaxFromData(data, X_AXIS_PROP);
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