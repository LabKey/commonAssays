import React, { PureComponent } from 'react';

import { PlotOptions } from "../models";
import { SelectInput } from "@labkey/components";
import { getSelectOptions } from "../utils";

interface Props {
    plotOptions: PlotOptions,
    columnInfo: {[key: string]: any},
    measures: string[],
    setPlotOption: (key: string, value: any, resetIdSelection: boolean) => void
}

export class PlotOptionsPanel extends PureComponent<Props> {

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
        const { plotOptions, measures, columnInfo, setPlotOption } = this.props;

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Plot Options
                </div>
                <div className="panel-body">
                    <div className={'plot-options-section'}>
                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>
                                X-Axis
                                <span className={'plot-options-show-all pull-right'}>
                                    <input
                                        type="checkbox"
                                        name='xAxisScale'
                                        checked={plotOptions.xAxisScale === 'log'}
                                        onChange={this.onShowXAxisLogCheck}
                                    />
                                    Log scale
                                </span>
                            </div>
                            <SelectInput
                                name='xAxisMeasure'
                                key='xAxisMeasure'
                                inputClass={'col-xs-12'}
                                options={getSelectOptions(measures, columnInfo)}
                                value={plotOptions.xAxisMeasure}
                                onChange={(key, value) => setPlotOption('xAxisMeasure', value, false)}
                                showLabel={false}
                                formsy={false}
                                multiple={false}
                                required={true}
                                clearable={false}
                            />
                        </div>

                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>
                                Y-Axis
                                <span className={'plot-options-show-all pull-right'}>
                                    <input
                                        type="checkbox"
                                        name='yAxisScale'
                                        checked={plotOptions.yAxisScale === 'log'}
                                        onChange={this.onShowYAxisLogCheck}
                                    />
                                    Log scale
                                </span>
                            </div>
                            <SelectInput
                                name='yAxisMeasure'
                                key='yAxisMeasure'
                                inputClass={'col-xs-12'}
                                options={getSelectOptions(measures, columnInfo)}
                                value={plotOptions.yAxisMeasure}
                                onChange={(key, value) => setPlotOption('yAxisMeasure', value, false)}
                                showLabel={false}
                                formsy={false}
                                multiple={false}
                                required={true}
                                clearable={false}
                            />
                        </div>

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
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}