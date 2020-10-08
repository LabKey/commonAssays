import React, { PureComponent } from 'react';
import { Query, Filter } from "@labkey/api";
import { Alert, LoadingSpinner } from "@labkey/components";

import {
    CONTROL_COL_NAME,
    ID_COL_NAME,
    RUN_COLUMN_NAMES,
    SAMPLE_COL_NAME,
    SAMPLE_COLUMN_NAMES, STANDARDS_LABEL,
    WELL_GROUP_COL_NAME
} from "./constants";
import { CurveFitData, PlotOptions } from "./models";
import {
    filterDataByPlotOptions,
    getUniqueIdsForPlotSelections,
    getUniqueValues,
    shouldReloadCurveFitData
} from "./utils";
import { CalibrationCurvePanel } from "./components/CalibrationCurvePanel";
import { DataSelectionsPanel } from "./components/DataSelectionsPanel";
import { PlotOptionsPanel } from "./components/PlotOptionsPanel";

import './RunDetails.scss';
import { getCurveFitXYPairs } from "./actions";

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
    dataError: string
}

export class RunDetails extends PureComponent<Props, State> {
    state: Readonly<State> = {
        runPropertiesRow: undefined,
        runPropertiesError: undefined,
        data: undefined,
        dataError:  undefined
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
            success: (response) => {
                const data = [];
                response.rows.forEach((row) => {
                    // consolidate the control name for the standard wells so they plot the same color
                    const isStandard = row[WELL_GROUP_COL_NAME] === 'Standards';
                    if (isStandard) {
                        row[CONTROL_COL_NAME] = STANDARDS_LABEL;
                    }

                    // if the row has a control value, make sure the sample column is null
                    if (row[CONTROL_COL_NAME]) {
                        row[SAMPLE_COL_NAME] = null;
                    }

                    row[ID_COL_NAME] = row[CONTROL_COL_NAME] || row[SAMPLE_COL_NAME];
                    data.push(row);
                });

                this.setState(() => ({ data }));
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
    data: any[]
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

class RunDetailsImpl extends PureComponent<ImplProps, ImplState> {
    constructor(props: ImplProps) {
        super(props);

        const plates = getUniqueValues(props.data, 'PlateName');
        const spots = getUniqueValues(props.data, 'Spot');
        const plotOptions = {
            showCurve: true,
            showLegend: true,
            plateName: plates.length > 1 ? plates[0] : undefined,
            spot: spots.length > 1 ? spots[0] : undefined,
            showAllSamples: true,
            samples: [],
            showAllControls: true,
            controls: [],
            xAxisScale: 'linear',
            yAxisScale: 'linear'
        } as PlotOptions;
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

        const updatedPlotOptions = {
            ...plotOptions,
            [key]: value
        };

        updatedPlotOptions.showAllSamples = resetIdSelection ? true : updatedPlotOptions.showAllSamples;
        updatedPlotOptions.samples = resetIdSelection || updatedPlotOptions.showAllSamples ? [] : updatedPlotOptions.samples;
        updatedPlotOptions.showAllControls = resetIdSelection ? true : updatedPlotOptions.showAllControls;
        updatedPlotOptions.controls = resetIdSelection || updatedPlotOptions.showAllControls ? [] : updatedPlotOptions.controls;

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