import React, { PureComponent } from 'react';
import $ from 'jquery';
import { getServerContext, ActionURL } from '@labkey/api';
import { generateId } from '@labkey/components';
import { exportSVGToFile, linearCurveFitFn, tickFormatFn } from "../utils";

interface Props {
    protocolId: number;
    runId: number;
    runName: string;
    fitParams: number[];
    data: [{[key: string]: any}];
}

interface State {
    plotId: string
}

export class CalibrationCurve extends PureComponent<Props, State> {

    constructor(props: Props) {
        super(props);

        this.state = {
            plotId: generateId('calibration-curve-')
        };
    }

    componentDidMount(): void {
        this.renderPlot();
    }

    getPlotElement() {
        return $('#' + this.state.plotId + '>svg');
    }

    renderPlot() {
        const { runName, fitParams, data } = this.props;
        const LABKEY = getServerContext();

        // TODO add point hover details
        // TODO adjust default size

        new LABKEY.vis.Plot({
            renderTo: this.state.plotId,
            width: 1095,
            height: 406,
            labels: {
                main: {
                    value: runName
                },
                x: {
                    value: "Concentration"
                },
                y: {
                    value:"Absorption"
                }
            },
            layers: [
                new LABKEY.vis.Layer({
                    data: data,
                    geom: new LABKEY.vis.Geom.Point({
                        size: 5,
                        opacity: .5,
                        color: '#116596'
                    }),
                    aes: {
                        x: 'Concentration',
                        y: 'Absorption'
                    }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Path({
                        size: 2,
                        color: '#555'
                    }),
                    aes: {x: 'x', y: 'y'},
                    data: LABKEY.vis.Stat.fn(linearCurveFitFn(fitParams), 2, 0, 100)
                })
            ],
            scales: {
                x: {scaleType: 'continuous', trans: 'linear', tickFormat: tickFormatFn},
                y: {scaleType: 'continuous', trans: 'linear', tickFormat: tickFormatFn}
            }
        }).render();
    }

    goToResults = () => {
        const { runId, protocolId } = this.props;

        window.location.href = ActionURL.buildURL('assay', 'assayResults', undefined, {
            rowId: protocolId, 'Data.Run/RowId~eq': runId
        });
    };

    exportToPNG = () => {
        const LABKEY = getServerContext();
        exportSVGToFile(this.getPlotElement(), LABKEY.vis.SVGConverter.FORMAT_PNG);
    };

    exportToPDF = () => {
        const LABKEY = getServerContext();
        exportSVGToFile(this.getPlotElement(), LABKEY.vis.SVGConverter.FORMAT_PDF);
    };

    render() {
        return (
            <>
                <div id={this.state.plotId}/>
                <button className={'labkey-button'} onClick={this.exportToPNG}>
                    Export to PNG
                </button>
                <button className={'labkey-button'} onClick={this.exportToPDF}>
                    Export to PDF
                </button>
                <button className={'labkey-button primary'} onClick={this.goToResults}>
                    View Results
                </button>
            </>
        )
    }
}