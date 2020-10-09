import React, { PureComponent } from 'react';
import $ from 'jquery';
import { getServerContext } from '@labkey/api';
import { generateId } from '@labkey/components';

import { getPlotConfigFromOptions } from "../utils";
import { CurveFitData, PlotOptions } from "../models";
import { PlotButtonBar } from "./PlotButtonBar";

interface Props {
    protocolId: number,
    runId: number,
    runPropertiesRow: {[key: string]: any},
    data: any[],
    plotOptions: PlotOptions,
    curveFitData: CurveFitData,
    columnInfo: {[key: string]: any}
}

interface State {
    plotId: string
}

export class CalibrationCurvePanel extends PureComponent<Props, State> {
    state: Readonly<State> = {
        plotId: generateId('calibration-curve-')
    };

    componentDidMount(): void {
        this.renderPlot();
    }

    componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any): void {
        this.renderPlot();
    }

    getPlotElement() {
        return $('#' + this.state.plotId);
    }

    getPlotWidth(): number {
        return $('.plot-panel-section').width();
    }

    renderPlot() {
        const { runPropertiesRow, plotOptions, data, curveFitData, columnInfo } = this.props;
        const LABKEY = getServerContext();

        this.getPlotElement().html('<div class="loading-msg"><i class="fa fa-spinner fa-pulse"></i> Rendering plot...</div>');

        setTimeout(() => {
            // if we are showing the curve but don't have the points back yet, don't render this time
            if (plotOptions.showCurve && !curveFitData) {
                return;
            }

            this.getPlotElement().html('');

            new LABKEY.vis.Plot(
                getPlotConfigFromOptions(
                    this.state.plotId, this.getPlotWidth(), plotOptions, data, curveFitData,
                    runPropertiesRow?.Name, columnInfo
                )
            ).render();
        }, 250);
    }

    render() {
        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Calibration Curve
                </div>
                <div className="panel-body">
                    <div className={'plot-panel-section'}>
                        <div id={this.state.plotId} className={'plot-panel-display'}/>
                        <PlotButtonBar {...this.props} plotId={this.state.plotId}/>
                    </div>
                </div>
            </div>
        )
    }
}