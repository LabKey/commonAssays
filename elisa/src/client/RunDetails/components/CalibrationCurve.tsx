import React, { PureComponent } from 'react';
import $ from 'jquery';
import { getServerContext } from '@labkey/api';
import { generateId } from '@labkey/components';

import {
    exportSVGToFile,
    filterDataByPlotOptions, getMaxFromData, getMinFromData, getResultsViewURL,
    getUniqueValues,
    linearCurveFitFn,
    tickFormatFn
} from "../utils";
import { PlotOptions } from "../models";
import { SAMPLE_COL_NAME, SAMPLE_COLUMN_NAMES, X_AXIS_PROP, Y_AXIS_PROP } from "../constants";
import { PlotOptionsPanel } from "./PlotOptionsPanel";

interface Props {
    protocolId: number;
    runId: number;
    runName: string;
    data: any[];
}

interface State {
    plotId: string,
    plates: string[],
    spots: number[],
    samples: string[],
    plotOptions: PlotOptions,
    filteredData: any[]
}

export class CalibrationCurve extends PureComponent<Props, State> {

    constructor(props: Props) {
        super(props);

        const plates = getUniqueValues(props.data, 'PlateName');
        const spots = getUniqueValues(props.data, 'Spot');

        this.state = {
            plotId: generateId('calibration-curve-'),
            plates,
            spots,
            samples: undefined,
            plotOptions: {
                showCurve: false,
                plateName: plates.length > 1 ? plates[0] : undefined,
                spot: spots.length > 1 ? spots[0] : undefined,
                xAxisScale: 'linear',
                yAxisScale: 'linear'
            } as PlotOptions,
            filteredData: undefined
        };
    }

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

    getTitle(): string {
        const { runName } = this.props;
        const { plateName, spot } = this.state.plotOptions;

        // TODO any other details to include in title?
        return runName
            + (plateName !== undefined ? ' - ' + plateName : '')
            + (spot !== undefined ? ' - Spot ' + spot : '');
    }

    setPlotOption = (key: string, value: any, resetSampleSelection: boolean) => {
        this.setState((state) => ({
            plotOptions: {
                ...state.plotOptions,
                samples: resetSampleSelection ? undefined : state.plotOptions.samples,
                [key]: value
            },
            filteredData: undefined // clear property so that it will be re-created on next request
        }));
    };

    getFilteredData(): any[] {
        const { filteredData } = this.state;

        if (filteredData === undefined) {
            this.updateFilteredData();
        }

        return filteredData;
    }

    updateFilteredData(): void {
        const { data } = this.props;
        const { plotOptions } = this.state;

        const filteredDataForSamples = filterDataByPlotOptions(data, plotOptions, false);
        const samples = getUniqueValues(filteredDataForSamples, SAMPLE_COL_NAME);

        this.setState(() => ({
            filteredData: filterDataByPlotOptions(data, plotOptions),
            samples
        }));
    }

    renderPlot() {
        const { plotOptions } = this.state;
        const LABKEY = getServerContext();

        this.getPlotElement().html('<div class="loading-msg"><i class="fa fa-spinner fa-pulse"></i> Rendering plot...</div>');
        setTimeout(() => {
            const width = $('.plot-panel').width() - 280;
            const data = this.getFilteredData();

            // call to getFilteredData will reset the filteredData state and recall this, so don't try rendering this time
            if (data === undefined) {
                return;
            }
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
                        // TODO add in support for non-linear curve fit
                        // data: LABKEY.vis.Stat.fn(linearCurveFitFn([1, 0]), 2, 0, 100)
                        data: [{x: xMin, y: yMin}, {x: xMax, y: yMax}]
                    })
                );
            }

            const margins = {};
            if (!plotOptions.showLegend) {
                margins['right'] = 10;
            }

            // TODO do we want colors per sample and legend when selected?
            new LABKEY.vis.Plot({
                renderTo: this.state.plotId,
                width,
                height: 500,
                layers,
                labels: {
                    main: { value: this.getTitle() },
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
        const { runId, protocolId } = this.props;
        return getResultsViewURL(protocolId, runId, this.state.plotOptions);
    };

    exportToPNG = () => {
        const LABKEY = getServerContext();
        exportSVGToFile(this.getSVGElement(), LABKEY.vis.SVGConverter.FORMAT_PNG);
    };

    exportToPDF = () => {
        const LABKEY = getServerContext();
        exportSVGToFile(this.getSVGElement(), LABKEY.vis.SVGConverter.FORMAT_PDF);
    };

    render() {
        return (
            <div className={'plot-panel'}>
                <PlotOptionsPanel {...this.state} setPlotOption={this.setPlotOption}/>
                <div className={'plot-panel-section'}>
                    <div id={this.state.plotId} className={'plot-panel-display'}/>
                    {/*TODO hide buttons until plot finishes rendering*/}
                    {/*TODO move export buttons to floating icons over plot*/}
                    {/*TODO where to put the view results button*/}

                    {/*TODO open in new tab?*/}
                    <a className={'labkey-button primary'} target={'_blank'} href={this.getViewResultsHref()}>
                        View Results
                    </a>
                    <span className={'pull-right'}>
                        <button className={'labkey-button'} onClick={this.exportToPNG}>
                            Export to PNG
                        </button>
                        <button className={'labkey-button'} onClick={this.exportToPDF}>
                            Export to PDF
                        </button>
                    </span>
                </div>
            </div>
        )
    }
}