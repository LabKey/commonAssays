import React, { PureComponent } from 'react';
import $ from 'jquery';
import { getServerContext } from '@labkey/api';
import { generateId } from '@labkey/components';

import { exportSVGToFile, getMaxFromData, getMinFromData, getPlotTitle, getResultsViewURL, tickFormatFn } from "../utils";
import { PlotOptions } from "../models";
import { SAMPLE_COL_NAME, SAMPLE_COLUMN_NAMES, X_AXIS_PROP, Y_AXIS_PROP } from "../constants";

interface Props {
    protocolId: number,
    runId: number,
    runPropertiesRow: {[key: string]: any},
    data: any[],
    plotOptions: PlotOptions
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
        const { runPropertiesRow, plotOptions, data } = this.props;
        const LABKEY = getServerContext();

        this.getPlotElement().html('<div class="loading-msg"><i class="fa fa-spinner fa-pulse"></i> Rendering plot...</div>');

        setTimeout(() => {
            this.getPlotElement().html('');

            const aes = {
                x: X_AXIS_PROP,
                y: Y_AXIS_PROP,
                hoverText: function(row){
                    return SAMPLE_COLUMN_NAMES.map((col) => {
                        const label = col === SAMPLE_COL_NAME ? 'Sample' : col;
                        return label + ': ' + row[col];
                    }).join('\n');
                }
            };
            if (plotOptions.showLegend) {
                aes['color'] = SAMPLE_COL_NAME;
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

            if (plotOptions.showCurve) {
                const xMin = getMinFromData(data, X_AXIS_PROP);
                const xMax = getMaxFromData(data, X_AXIS_PROP);
                const yMin = getMinFromData(data, Y_AXIS_PROP);
                const yMax = getMaxFromData(data, Y_AXIS_PROP);

                layers.push(
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.Path({
                            size: 2,
                            color: '#555'
                        }),
                        aes: {x: 'x', y: 'y'},
                        // TODO add in support for curve fit line display
                        data: [{x: xMin, y: yMin}, {x: xMax, y: yMax}]
                    })
                );
            }

            const margins = {};
            if (!plotOptions.showLegend) {
                margins['right'] = 10;
            }

            new LABKEY.vis.Plot({
                renderTo: this.state.plotId,
                width: this.getPlotWidth(),
                height: 600,
                layers,
                labels: {
                    main: { value: getPlotTitle(runPropertiesRow?.Name, plotOptions) },
                    x: { value: X_AXIS_PROP },
                    y: { value: Y_AXIS_PROP }
                },
                margins,
                scales: {
                    x: {scaleType: 'continuous', trans: plotOptions.xAxisScale, tickFormat: tickFormatFn},
                    y: {scaleType: 'continuous', trans: plotOptions.yAxisScale, tickFormat: tickFormatFn}
                }
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