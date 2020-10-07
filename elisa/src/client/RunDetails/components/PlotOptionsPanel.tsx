import React, { PureComponent } from 'react';
import { LoadingSpinner, SelectInput } from "@labkey/components";

import { PlotOptions } from "../models";
import { getSelectOptions, getUniqueValues } from "../utils";
import { AXIS_SCALE_TYPES } from "../constants";

interface Props {
    plates: string[],
    spots: number[],
    samples: string[],
    plotOptions: PlotOptions,
    setPlotOption: (key: string, value: any, resetSampleSelection: boolean) => void
}

export class PlotOptionsPanel extends PureComponent<Props> {

    onShowCurveCheck = (evt) => {
        this.props.setPlotOption('showCurve', evt.target.checked, false);
    };

    onSampleSelection = (name, formValue, selectedOptions) => {
        const selectedSamples = getUniqueValues(selectedOptions, 'value');
        this.props.setPlotOption('samples', selectedSamples.length > 0 ? selectedSamples : undefined, false);
    };

    render() {
        const { plates, spots, samples, plotOptions, setPlotOption } = this.props;

        return (
            <div className={'plot-panel-section'}>
                {plates.length > 1 &&
                    <div className={'plot-options-input-row'}>
                        <div className={'plot-options-field-label'}>Plate Name</div>
                        <SelectInput
                            name='plateName'
                            key='plateName'
                            inputClass={'col-xs-11'}
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
                            inputClass={'col-xs-11'}
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
                    {/*TODO do we call these Samples or SpecimenIDs*/}
                    <div className={'plot-options-field-label'}>Samples</div>
                    {samples
                        ? <SelectInput
                            name='samples'
                            key='samples'
                            inputClass={'col-xs-11'}
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
                    <div className={'plot-options-field-label'}>X-Axis Scale Type</div>
                    <SelectInput
                        name='xAxisScale'
                        key='xAxisScale'
                        inputClass={'col-xs-11'}
                        options={AXIS_SCALE_TYPES}
                        value={plotOptions.xAxisScale}
                        onChange={(key, value) => setPlotOption('xAxisScale', value, false)}
                        showLabel={false}
                        formsy={false}
                        multiple={false}
                        required={true}
                        clearable={false}
                    />
                </div>

                <div className={'plot-options-input-row'}>
                    <div className={'plot-options-field-label'}>Y-Axis Scale Type</div>
                    <SelectInput
                        name='yAxisScale'
                        key='yAxisScale'
                        inputClass={'col-xs-11'}
                        options={AXIS_SCALE_TYPES}
                        value={plotOptions.yAxisScale}
                        onChange={(key, value) => setPlotOption('yAxisScale', value, false)}
                        showLabel={false}
                        formsy={false}
                        multiple={false}
                        required={true}
                        clearable={false}
                    />
                </div>

                <div className={'plot-options-input-row'}>
                    <input
                        type="checkbox"
                        name='showCurve'
                        checked={plotOptions.showCurve}
                        onChange={this.onShowCurveCheck}
                    />
                    Show curve fit line (placeholder)
                </div>
            </div>
        )
    }
}