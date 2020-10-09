import React, { PureComponent } from 'react';
import $ from 'jquery';
import { getServerContext } from '@labkey/api';
import { generateId } from '@labkey/components';

import { HOVER_COLUMN_NAMES, X_AXIS_PROP, Y_AXIS_PROP, ID_COL_NAME } from "../constants";
import { exportSVGToFile, getPlotTitle, getResultsViewURL, tickFormatFn } from "../utils";
import { CurveFitData, PlotOptions } from "../models";

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

    getSVGElement() {
        return $('#' + this.state.plotId + '>svg');
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

            const aes = {
                x: X_AXIS_PROP,
                y: Y_AXIS_PROP,
                hoverText: function(row){
                    return HOVER_COLUMN_NAMES.map((col) => {
                        return col + ': ' + row[col];
                    }).join('\n');
                }
            };
            if (plotOptions.showLegend) {
                aes['color'] = ID_COL_NAME;
            }

            const layers = [
                new LABKEY.vis.Layer({
                    data,
                    geom: new LABKEY.vis.Geom.Point({
                        plotNullPoints: true,
                        size: 5,
                        opacity: .5,
                        color: '#116596'
                    }),
                    aes
                })
            ];

            if (plotOptions.showCurve && curveFitData) {
                layers.push(
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.Path({
                            size: 2,
                            color: '#555'
                        }),
                        aes: {x: 'x', y: 'y'},
                        data: curveFitData.points
                    })
                );
            }

            const margins = {};
            if (!plotOptions.showLegend) {
                margins['right'] = 10;
            }

            const scales = {
                x: {scaleType: 'continuous', trans: plotOptions.xAxisScale, tickFormat: tickFormatFn},
                y: {scaleType: 'continuous', trans: plotOptions.yAxisScale, tickFormat: tickFormatFn}
            };

            new LABKEY.vis.Plot({
                renderTo: this.state.plotId,
                width: this.getPlotWidth(),
                height: 600,
                layers,
                labels: {
                    main: { value: getPlotTitle(runPropertiesRow?.Name, plotOptions) },
                    x: { value: columnInfo[X_AXIS_PROP] ? columnInfo[X_AXIS_PROP].caption : X_AXIS_PROP },
                    y: { value: columnInfo[Y_AXIS_PROP] ? columnInfo[Y_AXIS_PROP].caption : Y_AXIS_PROP }
                },
                margins,
                scales
            }).render();
        }, 250);
    }

    getViewResultsHref = () => {
        const { runId, protocolId, plotOptions } = this.props;
        return getResultsViewURL(protocolId, runId, plotOptions);
    };

    exportToPNG = () => {
        const LABKEY = getServerContext();
        this.exportToFile(LABKEY.vis.SVGConverter.FORMAT_PNG);
    };

    exportToPDF = () => {
        const LABKEY = getServerContext();
        this.exportToFile(LABKEY.vis.SVGConverter.FORMAT_PDF);
    };

    exportToFile = (format: string) => {
        const { runPropertiesRow, plotOptions } = this.props;
        const title = getPlotTitle(runPropertiesRow?.Name, plotOptions);
        exportSVGToFile(this.getSVGElement(), format, title);
    };

    renderPlotButtonBar() {
        return (
            <div className={'plot-button-bar'}>
                <a className={'labkey-button'} target={'_blank'} href={this.getViewResultsHref()}>
                    View Results Grid
                </a>
                <button className={'labkey-button'} onClick={this.exportToPNG}>
                    Export to PNG
                </button>
                <button className={'labkey-button'} onClick={this.exportToPDF}>
                    Export to PDF
                </button>
            </div>
        )
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
                        {this.renderPlotButtonBar()}
                    </div>
                </div>
            </div>
        )
    }
}