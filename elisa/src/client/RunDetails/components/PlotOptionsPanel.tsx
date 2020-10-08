import React, { PureComponent } from 'react';

import { PlotOptions } from "../models";

interface Props {
    runPropertiesRow: {[key: string]: any},
    plotOptions: PlotOptions,
    setPlotOption: (key: string, value: any, resetIdSelection: boolean) => void
}

export class PlotOptionsPanel extends PureComponent<Props> {

    onShowCurveCheck = (evt) => {
        this.props.setPlotOption('showCurve', evt.target.checked, false);
    };

    onShowLegendCheck = (evt) => {
        this.props.setPlotOption('showLegend', evt.target.checked, false);
    };

    onShowXAxisLogCheck = (evt) => {
        this.props.setPlotOption('xAxisScale', evt.target.checked ? 'log' : 'linear', false);
    };

    onShowYAxisLogCheck = (evt) => {
        this.props.setPlotOption('yAxisScale', evt.target.checked ? 'log' : 'linear', false);
    };

    render() {
        const { plotOptions, runPropertiesRow } = this.props;

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Plot Options
                </div>
                <div className="panel-body">
                    <div className={'plot-options-section'}>
                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>Display Options</div>
                            <div>
                                <input
                                    type="checkbox"
                                    name='showLegend'
                                    checked={plotOptions.showLegend}
                                    onChange={this.onShowLegendCheck}
                                />
                                Show legend
                            </div>
                            <div>
                                <input
                                    type="checkbox"
                                    name='xAxisScale'
                                    checked={plotOptions.xAxisScale === 'log'}
                                    onChange={this.onShowXAxisLogCheck}
                                />
                                X-axis log scale
                            </div>
                            <div>
                                <input
                                    type="checkbox"
                                    name='yAxisScale'
                                    checked={plotOptions.yAxisScale === 'log'}
                                    onChange={this.onShowYAxisLogCheck}
                                />
                                Y-axis log scale
                            </div>
                        </div>

                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>Curve Fit: {runPropertiesRow?.CurveFitMethod}</div>
                            <div>
                                <input
                                    type="checkbox"
                                    name='showCurve'
                                    checked={plotOptions.showCurve}
                                    onChange={this.onShowCurveCheck}
                                />
                                Show curve fit line (placeholder)
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}