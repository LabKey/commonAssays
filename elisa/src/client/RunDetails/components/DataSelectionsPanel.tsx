import React, { PureComponent } from 'react';
import { LoadingSpinner, SelectInput } from "@labkey/components";

import { PlotOptions } from "../models";
import { arrayHasNonNullValues, getSelectOptions, getUniqueValues } from "../utils";

interface Props {
    plates: string[],
    spots: number[],
    samples: string[],
    controls: string[],
    plotOptions: PlotOptions,
    setPlotOption: (key: string, value: any, resetIdSelection: boolean) => void
}

export class DataSelectionsPanel extends PureComponent<Props> {

    onShowAllSamplesCheck = (evt) => {
        this.props.setPlotOption('showAllSamples', evt.target.checked, false);
    };

    onSampleSelection = (name, formValue, selectedOptions) => {
        const selectedSamples = getUniqueValues(selectedOptions, 'value');
        this.props.setPlotOption('samples', selectedSamples, false);
    };

    onShowAllControlsCheck = (evt) => {
        this.props.setPlotOption('showAllControls', evt.target.checked, false);
    };

    onControlSelection = (name, formValue, selectedOptions) => {
        const selectedControls = getUniqueValues(selectedOptions, 'value');
        this.props.setPlotOption('controls', selectedControls, false);
    };

    render() {
        const { plates, spots, samples, controls, plotOptions, setPlotOption } = this.props;

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Data Selections
                </div>
                <div className="panel-body">
                    <div className={'plot-options-section'}>
                        {arrayHasNonNullValues(plates) &&
                            <div className={'plot-options-input-row'}>
                                <div className={'plot-options-field-label'}>Plate Name</div>
                                <SelectInput
                                    name='plateName'
                                    key='plateName'
                                    inputClass={'col-xs-12'}
                                    options={getSelectOptions(plates)}
                                    value={plotOptions.plateName}
                                    onChange={(key, value) => setPlotOption('plateName', value, true)}
                                    showLabel={false}
                                    formsy={false}
                                    multiple={false}
                                    required={true}
                                    clearable={false}
                                />
                            </div>
                        }

                        {arrayHasNonNullValues(spots) &&
                            <div className={'plot-options-input-row'}>
                                <div className={'plot-options-field-label'}>Spot</div>
                                <SelectInput
                                    name='spot'
                                    key='spot'
                                    inputClass={'col-xs-12'}
                                    options={getSelectOptions(spots)}
                                    value={plotOptions.spot}
                                    onChange={(key, value) => setPlotOption('spot', value, true)}
                                    showLabel={false}
                                    formsy={false}
                                    multiple={false}
                                    required={true}
                                    clearable={false}
                                />
                            </div>
                        }

                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>Samples</div>
                            {samples
                                ? <>
                                    <div>
                                        <input
                                            type="checkbox"
                                            name='showAllSamples'
                                            checked={plotOptions.showAllSamples}
                                            onChange={this.onShowAllSamplesCheck}
                                        />
                                        Show all
                                    </div>
                                    <SelectInput
                                        name='samples'
                                        key='samples'
                                        inputClass={'col-xs-12'}
                                        placeholder={'Select samples to plot...'}
                                        disabled={plotOptions.showAllSamples}
                                        options={getSelectOptions(samples)}
                                        value={plotOptions.samples || []}
                                        onChange={this.onSampleSelection}
                                        showLabel={false}
                                        formsy={false}
                                        multiple={true}
                                        required={false}
                                        clearable={false}
                                    />
                                </>
                                : <LoadingSpinner msg={'Loading samples...'}/>
                            }
                        </div>

                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>Controls</div>
                            {controls
                                ? <>
                                    <div>
                                        <input
                                            type="checkbox"
                                            name='showAllControls'
                                            checked={plotOptions.showAllControls}
                                            onChange={this.onShowAllControlsCheck}
                                        />
                                        Show all
                                    </div>
                                    <SelectInput
                                        name='controls'
                                        key='controls'
                                        inputClass={'col-xs-12'}
                                        placeholder={'Select controls to plot...'}
                                        disabled={plotOptions.showAllControls}
                                        options={getSelectOptions(controls)}
                                        value={plotOptions.controls || []}
                                        onChange={this.onControlSelection}
                                        showLabel={false}
                                        formsy={false}
                                        multiple={true}
                                        required={false}
                                        clearable={false}
                                    />
                                </>
                                : <LoadingSpinner msg={'Loading controls...'}/>
                            }
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}