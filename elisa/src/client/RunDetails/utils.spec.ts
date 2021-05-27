import { PlotOptions } from "./models";
import {
    arrayHasNonNullValues, filterDataByPlotOptions, getDefaultPlotOptions, getMaxFromData,
    getMinFromData, getMissingFields, getPlotConfigFromOptions, getPlotTitle, getResultsViewURL, getSelectOptions,
    getUniqueIdsForPlotSelections, getUniqueValues, getUpdatedPlotOptions, parsePlotDataFromResponse,
    shouldReloadCurveFitData, tickFormatFn
} from "./utils";
import {
    CONTROL_COL_NAME, ID_COL_NAME, PLOT_HEIGHT, SAMPLE_COL_NAME, STANDARDS_LABEL, WELL_GROUP_COL_NAME,
    DEFAULT_X_AXIS_PROP, DEFAULT_Y_AXIS_PROP, REQUIRED_COLUMN_NAMES
} from "./constants";

export const TEST_PLOT_DATA = [
    {PlateName: null, Spot: null, [SAMPLE_COL_NAME]: null, [CONTROL_COL_NAME]: null, ID: null, Value: 1.1},
    {PlateName: null, Spot: null, [SAMPLE_COL_NAME]: null, [CONTROL_COL_NAME]: 'b', ID: 'b', Value: 10.1},
    {PlateName: null, Spot: null, [SAMPLE_COL_NAME]: 'a', [CONTROL_COL_NAME]: null, ID: 'a', Value: 1.2},
    {PlateName: 'p2', Spot: 1, [SAMPLE_COL_NAME]: 'a', [CONTROL_COL_NAME]: null, ID: 'a', Value: 122.1},
    {PlateName: 'p2', Spot: 2, [SAMPLE_COL_NAME]: 'a', [CONTROL_COL_NAME]: null, ID: 'a', Value: 12.1},
    {PlateName: 'p2', Spot: 2, [SAMPLE_COL_NAME]: null, [CONTROL_COL_NAME]: 'b', ID: 'b', Value: 10.1},
    {PlateName: 'p1', Spot: 1, [SAMPLE_COL_NAME]: 'a', [CONTROL_COL_NAME]: null, ID: 'a', Value: 10.1},
    {PlateName: 'p1', Spot: 1, [SAMPLE_COL_NAME]: null, [CONTROL_COL_NAME]: 'b', ID: 'b', Value: -1.1},
    {PlateName: 'p1', Spot: 2, [SAMPLE_COL_NAME]: 'a', [CONTROL_COL_NAME]: null, ID: 'a', Value: 0.1},
];

describe('utils', () => {
    test('tickFormatFn', () => {
        expect(tickFormatFn(undefined)).toBe(undefined);
        expect(tickFormatFn(null)).toBe(null);
        expect(tickFormatFn(0)).toBe(0);
        expect(tickFormatFn(12345)).toBe(12345);
        expect(tickFormatFn(123456)).toBe('1.23456e+5');
        expect(tickFormatFn(-12345)).toBe(-12345);
        expect(tickFormatFn(-123456)).toBe('-1.23456e+5');
    });

    test('filterDataByPlotOptions', () => {
        let plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, showAllControls: true} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, [], [], plotOptions, false)).toHaveLength(TEST_PLOT_DATA.length);

        plotOptions = {plateName: 'p1', spot: undefined, showAllSamples: true, showAllControls: true} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, [], [], plotOptions, false)).toHaveLength(3);

        plotOptions = {plateName: 'p1', spot: 1, showAllSamples: true, showAllControls: true} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, [], [], plotOptions, false)).toHaveLength(2);

        plotOptions = {plateName: 'p2', spot: 1, showAllSamples: true, showAllControls: true} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, [], [], plotOptions, false)).toHaveLength(1);

        plotOptions = {plateName: 'p2', spot: 3, showAllSamples: true, showAllControls: true} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, [], [], plotOptions, false)).toHaveLength(0);

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, showAllControls: true} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, ['a'], ['b'], plotOptions, false)).toHaveLength(TEST_PLOT_DATA.length);
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, ['a'], ['b'], plotOptions, true)).toHaveLength(8);

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: false, samples: [], showAllControls: false, controls: []} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, ['a'], ['b'], plotOptions, true)).toHaveLength(0);

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: false, samples: ['a', 'c'], showAllControls: false, controls: []} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, ['a'], ['b'], plotOptions, true)).toHaveLength(5);

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: false, samples: [], showAllControls: false, controls: ['b', 'c']} as PlotOptions;
        expect(filterDataByPlotOptions(TEST_PLOT_DATA, ['a'], ['b'], plotOptions, true)).toHaveLength(3);
    });

    test('getMinFromData', () => {
        expect(getMinFromData(TEST_PLOT_DATA, 'Value')).toBe(-1.1);
        expect(getMinFromData(TEST_PLOT_DATA, 'Value', false)).toBe(-1.1);
        expect(getMinFromData(TEST_PLOT_DATA, 'Spot')).toBe(0);
        expect(getMinFromData(TEST_PLOT_DATA, 'Spot', false)).toBe(1);
        expect(getMinFromData(TEST_PLOT_DATA, 'ID')).toBeNaN();
        expect(getMinFromData(TEST_PLOT_DATA, 'ID', false)).toBeNaN();
    });

    test('getMaxFromData', () => {
        expect(getMaxFromData(TEST_PLOT_DATA, 'Value')).toBe(122.1);
        expect(getMaxFromData(TEST_PLOT_DATA, 'Value', false)).toBe(122.1);
        expect(getMaxFromData(TEST_PLOT_DATA, 'Spot')).toBe(2);
        expect(getMaxFromData(TEST_PLOT_DATA, 'Spot', false)).toBe(2);
        expect(getMaxFromData(TEST_PLOT_DATA, 'ID')).toBeNaN();
        expect(getMaxFromData(TEST_PLOT_DATA, 'ID', false)).toBeNaN();
    });

    test('getUniqueValues', () => {
        expect(JSON.stringify(getUniqueValues(TEST_PLOT_DATA, 'PlateName'))).toBe('["p1","p2",null]');
        expect(JSON.stringify(getUniqueValues(TEST_PLOT_DATA, 'PlateName', false))).toBe('["p1","p2"]');
        expect(JSON.stringify(getUniqueValues(TEST_PLOT_DATA, 'Spot'))).toBe('[1,2,null]');
        expect(JSON.stringify(getUniqueValues(TEST_PLOT_DATA, 'Spot', false))).toBe('[1,2]');
        expect(JSON.stringify(getUniqueValues(TEST_PLOT_DATA, 'ID'))).toBe('["a","b",null]');
        expect(JSON.stringify(getUniqueValues(TEST_PLOT_DATA, 'ID', false))).toBe('["a","b"]');
    });

    test('getSelectOptions', () => {
        const options = getSelectOptions([null, 1, 'a', undefined, 'b'], {b: {caption: 'TEST B'}});
        expect(options.length).toBe(5);
        expect(options[0].value).toBe(null);
        expect(options[0].label).toBe('null');
        expect(options[1].value).toBe(1);
        expect(options[1].label).toBe(1);
        expect(options[2].value).toBe('a');
        expect(options[2].label).toBe('a');
        expect(options[3].value).toBe(undefined);
        expect(options[3].label).toBe('null');
        expect(options[4].value).toBe('b');
        expect(options[4].label).toBe('TEST B');
    });

    test('getResultsViewURL', () => {
        const baseHref = 'undefined/assay/assayResults.view?rowId=1&Data.Run%2FRowId~eq=2';

        let plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, samples: ['a'], showAllControls: true, controls: ['b']} as PlotOptions;
        expect(getResultsViewURL(1, 2, plotOptions)).toBe(baseHref);

        plotOptions = {plateName: 'p1', spot: 3, showAllSamples: true, samples: ['a'], showAllControls: true, controls: ['b']} as PlotOptions;
        expect(getResultsViewURL(1, 2, plotOptions)).toBe(baseHref + '&Data.PlateName~eq=p1&Data.Spot~eq=3');

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: false, samples: ['a'], showAllControls: true, controls: ['b']} as PlotOptions;
        expect(getResultsViewURL(1, 2, plotOptions)).toBe(baseHref + '&Data.SpecimenLsid%2FProperty%2FSpecimenId~in=a%3B');

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, samples: ['a'], showAllControls: false, controls: ['b']} as PlotOptions;
        expect(getResultsViewURL(1, 2, plotOptions)).toBe(baseHref + '&Data.ControlId~in=b%3B');

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, samples: ['a'], showAllControls: false, controls: [STANDARDS_LABEL]} as PlotOptions;
        expect(getResultsViewURL(1, 2, plotOptions)).toBe(baseHref + '&Data.WellgroupName~containsoneof=Standards%3BSample');

        plotOptions = {plateName: undefined, spot: undefined, showAllSamples: false, samples: [], showAllControls: false, controls: [STANDARDS_LABEL]} as PlotOptions;
        expect(getResultsViewURL(1, 2, plotOptions)).toBe(baseHref + '&Data.SpecimenLsid%2FProperty%2FSpecimenId~in=%3B&Data.WellgroupName~eq=Standards');
    });

    test('getPlotTitle', () => {
        const runName = 'TEST';
        let plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, samples: ['a'], showAllControls: true, controls: ['b']} as PlotOptions;
        expect(getPlotTitle(runName, plotOptions)).toBe(runName);

        plotOptions = {plateName: 'p1', spot: 0, showAllSamples: true, samples: ['a'], showAllControls: true, controls: ['b']} as PlotOptions;
        expect(getPlotTitle(runName, plotOptions)).toBe(runName + ' - p1 - Spot 0');
    });

    test('getUniqueIdsForPlotSelections', () => {
        let plotOptions = {plateName: undefined, spot: undefined, showAllSamples: true, samples: [], showAllControls: true, controls: []} as PlotOptions;
        expect(JSON.stringify(getUniqueIdsForPlotSelections(TEST_PLOT_DATA, plotOptions, 'ID'))).toBe('["a","b"]');

        plotOptions = {plateName: 'p1', spot: 1, showAllSamples: true, samples: [], showAllControls: true, controls: []} as PlotOptions;
        expect(JSON.stringify(getUniqueIdsForPlotSelections(TEST_PLOT_DATA, plotOptions, 'ID'))).toBe('["a","b"]');
        plotOptions = {plateName: 'p1', spot: 1, showAllSamples: false, samples: [], showAllControls: false, controls: []} as PlotOptions;
        expect(JSON.stringify(getUniqueIdsForPlotSelections(TEST_PLOT_DATA, plotOptions, 'ID'))).toBe('["a","b"]');

        plotOptions = {plateName: 'p1', spot: 2, showAllSamples: true, samples: [], showAllControls: true, controls: []} as PlotOptions;
        expect(JSON.stringify(getUniqueIdsForPlotSelections(TEST_PLOT_DATA, plotOptions, 'ID'))).toBe('["a"]');
        plotOptions = {plateName: 'p1', spot: 2, showAllSamples: false, samples: [], showAllControls: false, controls: []} as PlotOptions;
        expect(JSON.stringify(getUniqueIdsForPlotSelections(TEST_PLOT_DATA, plotOptions, 'ID'))).toBe('["a"]');
    });

    test('shouldReloadCurveFitData', () => {
        let plotOptions1 = {plateName: undefined, spot: undefined} as PlotOptions;
        let plotOptions2 = {plateName: undefined, spot: undefined} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeFalsy();
        plotOptions1 = {plateName: undefined, spot: undefined} as PlotOptions;
        plotOptions2 = {plateName: undefined, spot: 0} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeTruthy();
        plotOptions1 = {plateName: undefined, spot: undefined} as PlotOptions;
        plotOptions2 = {plateName: 'p1', spot: undefined} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeTruthy();
        plotOptions1 = {plateName: undefined, spot: undefined} as PlotOptions;
        plotOptions2 = {plateName: 'p1', spot: 0} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeTruthy();

        plotOptions1 = {plateName: 'p1', spot: 0} as PlotOptions;
        plotOptions2 = {plateName: 'p1', spot: 0} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeFalsy();
        plotOptions1 = {plateName: 'p1', spot: 0} as PlotOptions;
        plotOptions2 = {plateName: 'p1', spot: 1} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeTruthy();
        plotOptions1 = {plateName: 'p1', spot: 0} as PlotOptions;
        plotOptions2 = {plateName: 'p2', spot: 0} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeTruthy();
        plotOptions1 = {plateName: 'p1', spot: 0} as PlotOptions;
        plotOptions2 = {plateName: 'p2', spot: 1} as PlotOptions;
        expect(shouldReloadCurveFitData(plotOptions1, plotOptions2)).toBeTruthy();
    });

    test('getDefaultPlotOptions', () => {
        expect(getDefaultPlotOptions(undefined, undefined).plateName).toBe(undefined);
        expect(getDefaultPlotOptions(null, undefined).plateName).toBe(undefined);
        expect(getDefaultPlotOptions([], undefined).plateName).toBe(undefined);
        expect(getDefaultPlotOptions(['a'], undefined).plateName).toBe('a');
        expect(getDefaultPlotOptions(['a','b'], undefined).plateName).toBe('a');

        expect(getDefaultPlotOptions(undefined, undefined).spot).toBe(undefined);
        expect(getDefaultPlotOptions(undefined, null).spot).toBe(undefined);
        expect(getDefaultPlotOptions(undefined, []).spot).toBe(undefined);
        expect(getDefaultPlotOptions(undefined, [0]).spot).toBe(0);
        expect(getDefaultPlotOptions(undefined, [0,1]).spot).toBe(0);
    });

    test('getPlotConfigFromOptions', () => {
        let plotOptions = {plateName: undefined, spot: undefined, showLegend: false, showCurve: false, xAxisScale: 'linear', yAxisScale: 'linear', xAxisMeasure: DEFAULT_X_AXIS_PROP, yAxisMeasure: DEFAULT_Y_AXIS_PROP} as PlotOptions;
        let config = getPlotConfigFromOptions('id', 100, plotOptions, undefined, undefined, 'TEST', {});
        expect(config.renderTo).toBe('id');
        expect(config.width).toBe(100);
        expect(config.height).toBe(PLOT_HEIGHT);
        expect(config.layers.length).toBe(0);
        expect(config.labels.main.value).toBe('TEST');
        expect(config.labels.x.value).toBe(DEFAULT_X_AXIS_PROP);
        expect(config.labels.y.value).toBe(DEFAULT_Y_AXIS_PROP);
        expect(config.margins.right).toBe(10);
        expect(config.scales.x.trans).toBe('linear');
        expect(config.scales.y.trans).toBe('linear');

        plotOptions = {plateName: 'p1', spot: 2, showLegend: true, showCurve: true, xAxisScale: 'log', yAxisScale: 'log', xAxisMeasure: 'TestX', yAxisMeasure: 'TestY'} as PlotOptions;
        config = getPlotConfigFromOptions('id', 100, plotOptions, undefined, undefined, 'TEST', {
            [DEFAULT_X_AXIS_PROP]: {caption: 'TEST2'},
            [DEFAULT_Y_AXIS_PROP]: {caption: 'TEST3'},
            TestX: {caption: 'TEST4'},
            TestY: {caption: 'TEST5'}
        });
        expect(config.renderTo).toBe('id');
        expect(config.width).toBe(100);
        expect(config.height).toBe(PLOT_HEIGHT);
        expect(config.layers.length).toBe(0);
        expect(config.labels.main.value).toBe('TEST - p1 - Spot 2');
        expect(config.labels.x.value).toBe('TEST4');
        expect(config.labels.y.value).toBe('TEST5');
        expect(config.margins.right).toBe(undefined);
        expect(config.scales.x.trans).toBe('log');
        expect(config.scales.y.trans).toBe('log');
    });

    function validateRow(row, expectedControl, expectedSample, expectedId) {
        expect(row[CONTROL_COL_NAME]).toBe(expectedControl);
        expect(row[SAMPLE_COL_NAME]).toBe(expectedSample);
        expect(row[ID_COL_NAME]).toBe(expectedId);
    }

    test('parsePlotDataFromResponse', () => {
        expect(parsePlotDataFromResponse({rows: []})).toHaveLength(0);

        const rows = [
            {[WELL_GROUP_COL_NAME]: 'Standards', [CONTROL_COL_NAME]: 'C1', [SAMPLE_COL_NAME]: 'S1'},
            {[WELL_GROUP_COL_NAME]: 'Standards', [CONTROL_COL_NAME]: 'C2', [SAMPLE_COL_NAME]: null},
            {[WELL_GROUP_COL_NAME]: 'Standards', [CONTROL_COL_NAME]: null, [SAMPLE_COL_NAME]: 'S2'},
            {[WELL_GROUP_COL_NAME]: 'Other', [CONTROL_COL_NAME]: 'C3', [SAMPLE_COL_NAME]: 'S3'},
            {[WELL_GROUP_COL_NAME]: 'Other', [CONTROL_COL_NAME]: 'C4', [SAMPLE_COL_NAME]: null},
            {[WELL_GROUP_COL_NAME]: 'Other', [CONTROL_COL_NAME]: null, [SAMPLE_COL_NAME]: 'S4'},
            {[WELL_GROUP_COL_NAME]: 'Control 1', [CONTROL_COL_NAME]: 'C5', [SAMPLE_COL_NAME]: 'S5'},
            {[WELL_GROUP_COL_NAME]: 'Control 1', [CONTROL_COL_NAME]: null, [SAMPLE_COL_NAME]: 'S6'},
            {[WELL_GROUP_COL_NAME]: 'Control 1', [SAMPLE_COL_NAME]: 'S7'},
        ];
        const data = parsePlotDataFromResponse({rows});
        expect(data).toHaveLength(9);
        validateRow(data[0], 'Standard', null, 'Standard');
        validateRow(data[1], 'Standard', null, 'Standard');
        validateRow(data[2], 'Standard', null, 'Standard');
        validateRow(data[3], 'C3', null, 'C3');
        validateRow(data[4], 'C4', null, 'C4');
        validateRow(data[5], null, 'S4', 'S4');
        validateRow(data[6], 'C5', null, 'C5');
        validateRow(data[7], 'Unknown Control', null, 'Unknown Control');
        validateRow(data[8], 'Unknown Control', null, 'Unknown Control');
    });

    function validateOptions(plotOptions: PlotOptions, showAllSamples: boolean, sampleLength: number, showAllControls: boolean, controlLength: number) {
        expect(plotOptions.showAllSamples).toBe(showAllSamples);
        expect(plotOptions.samples.length).toBe(sampleLength);
        expect(plotOptions.showAllControls).toBe(showAllControls);
        expect(plotOptions.controls.length).toBe(controlLength);
    }

    test('getUpdatedPlotOptions', () => {
        let plotOptions = {showAllSamples: false, samples: ['a'], showAllControls: false, controls: ['b']} as PlotOptions;
        validateOptions(getUpdatedPlotOptions('showLegend', true, false, plotOptions), false, 1, false, 1);
        validateOptions(getUpdatedPlotOptions('showLegend', true, true, plotOptions), true, 0, true, 0);
        validateOptions(getUpdatedPlotOptions('showAllSamples', true, false, plotOptions), true, 0, false, 1);
        validateOptions(getUpdatedPlotOptions('showAllControls', true, false, plotOptions), false, 1, true, 0);
    });

    test('arrayHasNonNullValues', () => {
        expect(arrayHasNonNullValues(undefined)).toBeFalsy();
        expect(arrayHasNonNullValues(null)).toBeFalsy();
        expect(arrayHasNonNullValues([])).toBeFalsy();
        expect(arrayHasNonNullValues([null])).toBeFalsy();
        expect(arrayHasNonNullValues(['a'])).toBeTruthy();
        expect(arrayHasNonNullValues(['a','b'])).toBeTruthy();
        expect(arrayHasNonNullValues([0])).toBeTruthy();
        expect(arrayHasNonNullValues([0,1])).toBeTruthy();
    });

    test('getMissingFields', () => {
        expect(getMissingFields(undefined).join(',')).toBe('');
        expect(getMissingFields({}).join(',')).toBe(REQUIRED_COLUMN_NAMES.join(','));

        const colInfo = {};
        REQUIRED_COLUMN_NAMES.forEach((col) => colInfo[col] = {});
        expect(getMissingFields(colInfo).join(',')).toBe('');
    });
});