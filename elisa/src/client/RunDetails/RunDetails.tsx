import React, { PureComponent } from 'react';
import { Query, Filter } from "@labkey/api";
import { Alert, LoadingSpinner } from "@labkey/components";

import {
    CONTROL_COL_NAME,
    RUN_COLUMN_NAMES,
    SAMPLE_COL_NAME,
    SAMPLE_COLUMN_NAMES
} from "./constants";
import { CurveFitData, PlotOptions } from "./models";
import {
    filterDataByPlotOptions,
    getColumnInfoFromQueryDetails,
    getDefaultPlotOptions,
    getUniqueIdsForPlotSelections,
    getUniqueValues,
    getUpdatedPlotOptions,
    parsePlotDataFromResponse,
    shouldReloadCurveFitData
} from "./utils";
import { getCurveFitXYPairs } from "./actions";
import { CalibrationCurvePanel } from "./components/CalibrationCurvePanel";
import { DataSelectionsPanel } from "./components/DataSelectionsPanel";
import { PlotOptionsPanel } from "./components/PlotOptionsPanel";

import './RunDetails.scss';

export interface AppContext {
    protocolId: number,
    runId: number,
    schemaName: string,
}

interface Props {
    context: AppContext
}

interface State {
    runPropertiesRow: {[key: string]: any},
    runPropertiesError: string
    data: any[],
    dataError: string,
    columnInfo: {[key: string]: any}
}

export class RunDetails extends PureComponent<Props, State> {
    state: Readonly<State> = {
        runPropertiesRow: undefined,
        runPropertiesError: undefined,
        data: undefined,
        dataError:  undefined,
        columnInfo: {}
    };

    componentDidMount(): void {
        const { schemaName, runId } = this.props.context;

        Query.getQueryDetails({
            schemaName,
            queryName: 'Data',
            success: (queryInfo) => {
                this.setState(() => ({
                    columnInfo: getColumnInfoFromQueryDetails(queryInfo)
                }));
            }
        });

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
            success: (response) => {
                this.setState(() => ({
                    data: parsePlotDataFromResponse(response)
                }));
            },
            failure: (reason) => {
                console.error(reason);
                this.setState(() => ({ dataError: reason.exception }));
            }
        });
    }

    render() {
        const { runPropertiesError, dataError, runPropertiesRow, data } = this.state;

        if (runPropertiesError || dataError) {
            return <Alert>{runPropertiesError || dataError}</Alert>
        }

        if (!runPropertiesRow || !data) {
            return <LoadingSpinner msg={'Loading data...'}/>
        }

        return <RunDetailsImpl {...this.props.context} {...this.state}/>
    }
}

interface ImplProps {
    protocolId: number,
    runId: number,
    runPropertiesRow: {[key: string]: any},
    data: any[],
    columnInfo: {[key: string]: any}
}

interface ImplState {
    filteredData: any[]
    plates: string[],
    spots: number[],
    samples: string[],
    controls: string[],
    plotOptions: PlotOptions,
    curveFitData: CurveFitData
}

// exported just for jest testing
export class RunDetailsImpl extends PureComponent<ImplProps, ImplState> {
    constructor(props: ImplProps) {
        super(props);

        const plates = getUniqueValues(props.data, 'PlateName');
        const spots = getUniqueValues(props.data, 'Spot');
        const plotOptions = getDefaultPlotOptions(plates, spots);
        const samples = getUniqueIdsForPlotSelections(props.data, plotOptions, SAMPLE_COL_NAME);
        const controls = getUniqueIdsForPlotSelections(props.data, plotOptions, CONTROL_COL_NAME);

        this.state = {
            filteredData: filterDataByPlotOptions(props.data, samples, controls, plotOptions),
            plotOptions,
            plates,
            spots,
            samples,
            controls,
            curveFitData: undefined
        };
    }

    componentDidMount(): void {
        this.getCurveFitData();
    }

    componentDidUpdate(prevProps: Readonly<ImplProps>, prevState: Readonly<ImplState>, snapshot?: any): void
    {
        if (shouldReloadCurveFitData(prevState.plotOptions, this.state.plotOptions)) {
            this.getCurveFitData();
        }
    }

    getCurveFitData() {
        const { protocolId, runId, data } = this.props;
        const { plotOptions } = this.state;
        const filteredData = filterDataByPlotOptions(data, [], [], plotOptions, false);

        getCurveFitXYPairs(protocolId, runId, plotOptions.plateName, plotOptions.spot, filteredData)
            .then(curveFitData => {
                this.setState(() => ({ curveFitData }));
            });
    }

    setPlotOption = (key: string, value: any, resetIdSelection: boolean) => {
        const { data } = this.props;
        const { plotOptions, samples, controls, curveFitData } = this.state;
        const updatedPlotOptions = getUpdatedPlotOptions(key, value, resetIdSelection, plotOptions);

        this.setState(() => ({
            plotOptions: updatedPlotOptions,
            filteredData: filterDataByPlotOptions(data, samples, controls, updatedPlotOptions),
            samples: getUniqueIdsForPlotSelections(data, updatedPlotOptions, SAMPLE_COL_NAME),
            controls: getUniqueIdsForPlotSelections(data, updatedPlotOptions, CONTROL_COL_NAME),
            curveFitData: shouldReloadCurveFitData(plotOptions, updatedPlotOptions) ? undefined : curveFitData
        }));
    };

    render() {
        const { filteredData } = this.state;

        return (
            <div className={'row'}>
                <div className={'col col-xs-12 col-md-3 run-details-left'}>
                    <DataSelectionsPanel {...this.state} setPlotOption={this.setPlotOption}/>
                    <PlotOptionsPanel {...this.props} {...this.state} setPlotOption={this.setPlotOption}/>
                </div>
                <div className={'col col-xs-12 col-md-9 run-details-right'}>
                    <CalibrationCurvePanel {...this.props} {...this.state} data={filteredData}/>
                </div>
            </div>
        )
    }
}