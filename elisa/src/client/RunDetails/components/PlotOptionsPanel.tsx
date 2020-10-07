import React, { PureComponent } from 'react';
import { SelectInput } from "@labkey/components";

import { PlotOptions } from "../models";
import { getSelectOptions } from "../utils";

interface Props {
    plates: string[],
    spots: number[],
    plotOptions: PlotOptions,
    setPlotOption: (key: string, value: any) => void
}

export class PlotOptionsPanel extends PureComponent<Props> {

    toggleShowCurve = (evt) => {
        this.props.setPlotOption('showCurve', evt.target.checked);
    };

    render() {
        const { plates, spots, plotOptions, setPlotOption } = this.props;

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
                            onChange={(key, value) => setPlotOption('plateName', value)}
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
                            onChange={(key, value) => setPlotOption('spot', value)}
                            showLabel={false}
                            formsy={false}
                            multiple={false}
                            required={true}
                            clearable={false}
                        />
                    </div>
                }

                {/*<div className={'plot-options-input-row'}>*/}
                {/*    <div className={'plot-options-field-label'}>Samples</div>*/}
                {/*    <SelectInput*/}
                {/*        name='samples'*/}
                {/*        key='samples'*/}
                {/*        inputClass={'col-xs-11'}*/}
                {/*        options={getSelectOptions(['sample 1', 'sample 2', 'sample 3', 'sample 4'])}*/}
                {/*        // value={plotOptions.samples}*/}
                {/*        // onChange={(key, value) => this.setPlotOption('spot', value)}*/}
                {/*        showLabel={false}*/}
                {/*        formsy={false}*/}
                {/*        multiple={true}*/}
                {/*        required={false}*/}
                {/*        clearable={false}*/}
                {/*    />*/}
                {/*</div>*/}

                <div className={'plot-options-input-row'}>
                    <div className={'plot-options-field-label'}>Show curve fit line (placeholder)</div>
                    <input
                        type="checkbox"
                        name='showCurve'
                        checked={plotOptions.showCurve}
                        onChange={this.toggleShowCurve}
                    />
                </div>
            </div>
        )
    }
}