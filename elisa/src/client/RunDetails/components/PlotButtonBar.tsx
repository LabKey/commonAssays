import React, { PureComponent } from 'react';
import $ from 'jquery';
import { getServerContext } from "@labkey/api";

import { PlotOptions } from "../models";
import { getPlotTitle, getResultsViewURL } from "../utils";
import { exportSVGToFile } from "../actions";

interface Props {
    protocolId: number,
    runId: number,
    runPropertiesRow: {[key: string]: any},
    plotOptions: PlotOptions,
    plotId: string
}

export class PlotButtonBar extends PureComponent<Props> {

    getSVGElement() {
        return $('#' + this.props.plotId + '>svg');
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

    render() {
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
}