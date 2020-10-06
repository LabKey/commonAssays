import React, { PureComponent } from 'react';
import { Query, Filter } from "@labkey/api";
import { Alert, LoadingSpinner } from "@labkey/components";

import { RunProperties } from "./components/RunProperties";

import './RunDetails.scss';
import { CalibrationCurve } from "./components/CalibrationCurve";

export interface AppContext {
    protocolId: number;
    runId: number;
    schemaName: string;
    fitParams: number[];
}

interface Props {
    context: AppContext;
}

interface State {
    runPropertiesRow: {[key: string]: any},
    runPropertiesError: string
    plotRows: [{[key: string]: any}],
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
            columns: 'Name,Created,CreatedBy/DisplayName,CurveFitMethod,RSquared,CurveFitParams',
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
            columns: 'SpecimenLsid/Property/SpecimenId,WellLocation,Absorption,Concentration',
            filterArray: [
                Filter.create('Run/RowId', runId),
                // LABKEY.Filter.create('PlateName', 'Plate_1LK05A1071'),
                // LABKEY.Filter.create('Spot', 1),
            ],
            success: (data) => {
                this.setState(() => ({ plotRows: data.rows }));
            },
            failure: (reason) => {
                console.error(reason);
                this.setState(() => ({ plotError: reason.exception }));
            }
        })
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
                            : <LoadingSpinner msg={'Loading plot...'}/>
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