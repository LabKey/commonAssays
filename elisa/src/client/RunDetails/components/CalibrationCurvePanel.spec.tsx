import React from 'react';
import { mount } from 'enzyme';
import { CalibrationCurvePanel } from "./CalibrationCurvePanel";
import { getDefaultPlotOptions } from "../utils";
import { PlotButtonBar } from "./PlotButtonBar";

describe('<CalibrationCurvePanel/>', () => {
    test('default props', () => {
        const wrapper = mount(
            <CalibrationCurvePanel
                protocolId={1}
                runId={2}
                runPropertiesRow={{}}
                data={[]}
                plotOptions={getDefaultPlotOptions([], [])}
                curveFitData={undefined}
                columnInfo={{}}
            />
        );

        expect(wrapper.state('plotId')).not.toBe(undefined);
        expect(wrapper.find('.panel')).toHaveLength(1);
        expect(wrapper.find('.panel-heading').text()).toBe('Calibration Curve');
        expect(wrapper.find('.plot-panel-display')).toHaveLength(1);
        expect(wrapper.find(PlotButtonBar)).toHaveLength(1);

        wrapper.unmount();
    });
});