import React, { PureComponent } from 'react';

import { CurveFitData, PlotOptions } from "../models";
import { CurveFitDataDisplay } from "./CurveFitDataDisplay";

interface Props {
    runPropertiesRow: {[key: string]: any},
    plotOptions: PlotOptions,
    setPlotOption: (key: string, value: any, resetIdSelection: boolean) => void,
    curveFitData: CurveFitData
}

export class CurveFitPanel extends PureComponent<Props> {

    onShowCurveCheck = (evt) => {
        this.props.setPlotOption('showCurve', evt.target.checked, false);
    };

    render() {
        const { plotOptions, runPropertiesRow, curveFitData } = this.props;

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Curve Fit: {runPropertiesRow?.CurveFitMethod ?? 'Linear'}
                </div>
                <div className="panel-body">
                    <div className={'plot-options-section'}>
                        <div>
                            <input
                                type="checkbox"
                                name='showCurve'
                                checked={plotOptions.showCurve}
                                onChange={this.onShowCurveCheck}
                            />
                            Show curve fit line
                            {plotOptions.showCurve && curveFitData &&
                                <CurveFitDataDisplay curveFitData={curveFitData}/>
                            }
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}