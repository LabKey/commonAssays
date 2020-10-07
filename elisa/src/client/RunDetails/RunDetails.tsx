import React, { PureComponent } from 'react';
import { Query, Filter } from "@labkey/api";
import { Alert, LoadingSpinner } from "@labkey/components";

import { RUN_COLUMN_NAMES, SAMPLE_COLUMN_NAMES } from "./constants";
import { RunProperties } from "./components/RunProperties";
import { CalibrationCurve } from "./components/CalibrationCurve";

import './RunDetails.scss';

export interface AppContext {
    protocolId: number;
    runId: number;
    schemaName: string;
}

interface Props {
    context: AppContext;
}

interface State {
    runPropertiesRow: {[key: string]: any},
    runPropertiesError: string
    plotRows: any[],
    plotError: string
}

export class RunDetails extends PureComponent<Props, State> {
    state: Readonly<State> = {
        runPropertiesRow: undefined,
        runPropertiesError: undefined,
        plotRows: undefined,
        plotError:  undefined
    };

    componentDidMount(): void {
        const { schemaName, runId } = this.props.context;

        Query.selectRows({
            schemaName,
            queryName: 'Runs',
            columns: RUN_COLUMN_NAMES.join(','),
            filterArray: [Filter.create('RowId', runId)],
            success: (data) => {
                if (data?.rows?.length === 1) {
                    this.setState(() => ({ runPropertiesRow: data.rows[0] }));
                }
                else {
                    this.setState(() => ({ runPropertiesError: 'No properties found for run ID ' + runId + '.' }));
                }
            },
            failure: (reason) => {
                console.error(reason);
                this.setState(() => ({ runPropertiesError: reason.exception }));
            }
        });

        Query.selectRows({
            schemaName: schemaName,
            queryName: 'Data',
            columns: SAMPLE_COLUMN_NAMES.join(','),
            filterArray: [Filter.create('Run/RowId', runId)],
            success: (data) => {
                this.setState(() => ({ plotRows: data.rows }));
            },
            failure: (reason) => {
                console.error(reason);
                this.setState(() => ({ plotError: reason.exception }));
            }
        });
    }

    renderRunPropertiesPanel() {
        const { runPropertiesRow, runPropertiesError } = this.state;

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Run Properties
                </div>
                <div className="panel-body">
                    {runPropertiesError
                        ? <Alert>{runPropertiesError}</Alert>
                        : (runPropertiesRow
                            ? <RunProperties row={runPropertiesRow}/>
                            : <LoadingSpinner msg={'Loading properties...'}/>
                        )
                    }
                </div>
            </div>
        )
    }

    renderPlotPanel() {
        const { plotRows, plotError, runPropertiesRow } = this.state;

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    Calibration Curve
                </div>
                <div className="panel-body">
                    {plotError
                        ? <Alert>{plotError}</Alert>
                        : (plotRows
                            ? <CalibrationCurve {...this.props.context} data={plotRows} runName={runPropertiesRow?.Name}/>
                            : <LoadingSpinner msg={'Loading plot data...'}/>
                        )
                    }
                </div>
            </div>
        )
    }

    render() {
        return (
            <>
                {this.renderRunPropertiesPanel()}
                {this.renderPlotPanel()}
            </>
        )
    }
}