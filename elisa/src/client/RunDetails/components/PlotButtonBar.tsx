import React, { PureComponent } from 'react';
import { getServerContext } from "@labkey/api";

import { CommonRunProps, PlotOptions } from "../models";
import { getPlotTitle, getResultsViewURL } from "../utils";
import { exportSVGToFile } from "../actions";

interface Props extends CommonRunProps {
    plotOptions: PlotOptions,
    plotElement: HTMLDivElement
}

export class PlotButtonBar extends PureComponent<Props> {

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
        const { runPropertiesRow, plotOptions, plotElement } = this.props;
        const title = getPlotTitle(runPropertiesRow?.Name, plotOptions);
        exportSVGToFile(plotElement.firstChild, format, title);
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