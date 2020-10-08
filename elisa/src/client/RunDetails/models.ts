
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
    yAxisScale: string
}