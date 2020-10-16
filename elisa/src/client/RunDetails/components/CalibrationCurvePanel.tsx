import React, { PureComponent } from 'react';
import { getServerContext } from '@labkey/api';
import { generateId } from '@labkey/components';

import { getPlotConfigFromOptions } from "../utils";
import { CommonRunProps, CurveFitData, PlotOptions } from "../models";
import { PlotButtonBar } from "./PlotButtonBar";

interface Props extends CommonRunProps {
    data: any[],
    plotOptions: PlotOptions,
    curveFitData: CurveFitData,
    columnInfo: {[key: string]: any}
}

interface State {
    plotId: string
}

export class CalibrationCurvePanel extends PureComponent<Props, State> {
    plotPanel: React.RefObject<HTMLDivElement>;
    plotElement: React.RefObject<HTMLDivElement>;

    state: Readonly<State> = {
        plotId: generateId('calibration-curve-')
    };

    constructor(props: Props) {
        super(props);

        this.plotPanel = React.createRef();
        this.plotElement = React.createRef();
    }

    componentDidMount(): void {
        this.renderPlot();
    }

    componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any): void {
        this.renderPlot();
    }

    renderPlot() {
        const { runPropertiesRow, plotOptions, data, curveFitData, columnInfo } = this.props;
        const LABKEY = getServerContext();

        this.plotElement.current.innerHTML = '<div class="loading-msg"><i class="fa fa-spinner fa-pulse"></i> Rendering plot...</div>';

        setTimeout(() => {
            // if we are showing the curve but don't have the points back yet, don't render this time
            if (plotOptions.showCurve && !curveFitData) {
                return;
            }

            this.plotElement.current.innerHTML = '';

            new LABKEY.vis.Plot(
                getPlotConfigFromOptions(
                    this.state.plotId, this.plotPanel.current.clientWidth, plotOptions, data, curveFitData,
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
                    <div ref={this.plotPanel} className={'plot-panel-section'}>
                        <div id={this.state.plotId} ref={this.plotElement} className={'plot-panel-display'}/>
                        <PlotButtonBar {...this.props} plotElement={this.plotElement.current}/>
                    </div>
                </div>
            </div>
        )
    }
}