import React, { PureComponent } from 'react';
import { LoadingSpinner, SelectInput } from "@labkey/components";

import { PlotOptions } from "../models";
import { getSelectOptions, getUniqueValues } from "../utils";

interface Props {
    plates: string[],
    spots: number[],
    samples: string[],
    controls: string[],
    plotOptions: PlotOptions,
    setPlotOption: (key: string, value: any, resetSampleSelection: boolean) => void
}

export class DataSelectionsPanel extends PureComponent<Props> {

    onSampleSelection = (name, formValue, selectedOptions) => {
        const selectedSamples = getUniqueValues(selectedOptions, 'value');
        this.props.setPlotOption('samples', selectedSamples.length > 0 ? selectedSamples : undefined, false);
    };

    onControlSelection = (name, formValue, selectedOptions) => {
        const selectedControls = getUniqueValues(selectedOptions, 'value');
        this.props.setPlotOption('controls', selectedControls.length > 0 ? selectedControls : undefined, false);
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
                        {plates.length > 1 &&
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

                        {spots.length > 1 &&
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
                                ? <SelectInput
                                    name='samples'
                                    key='samples'
                                    inputClass={'col-xs-12'}
                                    options={getSelectOptions(samples)}
                                    value={plotOptions.samples || []}
                                    onChange={this.onSampleSelection}
                                    showLabel={false}
                                    formsy={false}
                                    multiple={true}
                                    required={false}
                                    clearable={false}
                                />
                                : <LoadingSpinner msg={'Loading samples...'}/>
                            }
                        </div>

                        <div className={'plot-options-input-row'}>
                            <div className={'plot-options-field-label'}>Controls</div>
                            {controls
                                ? <SelectInput
                                    name='controls'
                                    key='controls'
                                    inputClass={'col-xs-12'}
                                    options={getSelectOptions(controls)}
                                    value={plotOptions.controls || []}
                                    onChange={this.onControlSelection}
                                    showLabel={false}
                                    formsy={false}
                                    multiple={true}
                                    required={false}
                                    clearable={false}
                                />
                                : <LoadingSpinner msg={'Loading controls...'}/>
                            }
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}