
export type SelectOptions = {
    value: any,
    label: string
}

export type PlotOptions = {
    showCurve: boolean,
    showLegend: boolean,
    plateName: string,
    spot: number,
    showAllSamples: boolean;
    samples: string[],
    showAllControls: boolean;
    controls: string[],
    xAxisScale: string,
    xAxisMeasure: string,
    yAxisScale: string,
    yAxisMeasure: string
}

export type XYPair = {
    x: number,
    y: number
}

export type CurveFitData = {
    runId: number,
    curveFitMethod: string,
    rSquared: number,
    fitParameters: string,
    points: XYPair[],
    error: string
}

export interface CommonRunProps {
    protocolId: number,
    runId: number,
    runPropertiesRow: {[key: string]: any}
}