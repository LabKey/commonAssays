import React from 'react';
import { mount } from 'enzyme';
import { Alert, waitForLifecycle } from "@labkey/components";

import { RunDetailsImpl } from "./RunDetails";
import { TEST_PLOT_DATA } from "./utils.spec";
import { DataSelectionsPanel } from "./components/DataSelectionsPanel";
import { PlotOptionsPanel } from "./components/PlotOptionsPanel";
import { CalibrationCurvePanel } from "./components/CalibrationCurvePanel";
import { getDefaultPlotOptions } from "./utils";
import { CurveFitPanel } from "./components/CurveFitPanel";
import { REQUIRED_COLUMN_NAMES } from "./constants";

describe('<RunDetailsImpl/>', () => {
    test('check initial state from props', async () => {
        const wrapper = mount(
            <RunDetailsImpl
                protocolId={1}
                runId={2}
                runPropertiesRow={undefined}
                data={TEST_PLOT_DATA}
                columnInfo={undefined}
                measures={[]}
            />
        );
        await waitForLifecycle(wrapper);
        const defaultPlotOptions = getDefaultPlotOptions(['p1','p2'], [1,2]);

        expect(wrapper.state('filteredData')).toHaveLength(2);
        expect(JSON.stringify(wrapper.state('plotOptions'))).toBe(JSON.stringify(defaultPlotOptions));
        expect(wrapper.state('plates')).toHaveLength(2);
        expect(wrapper.state('spots')).toHaveLength(2);
        expect(wrapper.state('samples')).toHaveLength(1);
        expect(wrapper.state('controls')).toHaveLength(1);
        expect(wrapper.state('curveFitData')).toBe(undefined);

        wrapper.unmount();
    });

    test('check initial display', async () => {
        const wrapper = mount(
            <RunDetailsImpl
                protocolId={1}
                runId={2}
                runPropertiesRow={undefined}
                data={TEST_PLOT_DATA}
                columnInfo={undefined}
                measures={[]}
            />
        );
        await waitForLifecycle(wrapper);

        expect(wrapper.find('.run-details-left')).toHaveLength(1);
        expect(wrapper.find(DataSelectionsPanel)).toHaveLength(1);
        expect(wrapper.find(PlotOptionsPanel)).toHaveLength(1);
        expect(wrapper.find(CurveFitPanel)).toHaveLength(1);
        expect(wrapper.find('.run-details-right')).toHaveLength(1);
        expect(wrapper.find(CalibrationCurvePanel)).toHaveLength(1);
        expect(wrapper.find(Alert)).toHaveLength(0);

        wrapper.unmount();
    });

    test('missing field keys', async () => {
        const wrapper = mount(
            <RunDetailsImpl
                protocolId={1}
                runId={2}
                runPropertiesRow={undefined}
                data={TEST_PLOT_DATA}
                columnInfo={{}}
                measures={[]}
            />
        );
        await waitForLifecycle(wrapper);

        const alerts = wrapper.find(Alert);
        expect(alerts).toHaveLength(1);
        expect(alerts.first().text()).toContain(REQUIRED_COLUMN_NAMES.join(', '));

        wrapper.unmount();
    });
});