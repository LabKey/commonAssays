import React, { PureComponent, ReactNode } from 'react';
import { LabelHelpTip } from "@labkey/components";

import { CurveFitData, PlotOptions } from "../models";
import { CurveFitDataDisplay } from "./CurveFitDataDisplay";
import { DEFAULT_X_AXIS_PROP, DEFAULT_Y_AXIS_PROP } from "../constants";

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

    getStandardCurveInfo = (): ReactNode =>{
        return (
            <>
                <p>
                    The curve fit was derived using
                    the {DEFAULT_X_AXIS_PROP.toLowerCase()} and {DEFAULT_Y_AXIS_PROP.toLowerCase()} data
                    points for the standards well group based on the curve fit method selected for this run.
                </p>
                <p>
                    Note that selecting an x-axis or y-axis plot measure different then the default
                    will likely result in data points that don't line up with the plotted curve.
                </p>
            </>
        )
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
                            <LabelHelpTip
                                title={'Standard Curve Fit'}
                                body={this.getStandardCurveInfo}
                            />
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