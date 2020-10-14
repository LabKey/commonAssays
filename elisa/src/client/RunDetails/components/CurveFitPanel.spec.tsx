import React from 'react';
import { mount } from 'enzyme';

import { CurveFitPanel } from "./CurveFitPanel";
import { CurveFitData, PlotOptions } from "../models";
import { CurveFitDataDisplay } from "./CurveFitDataDisplay";
import { LabelHelpTip } from "@labkey/components";

describe('<CurveFitPanel/>', () => {
    test('unchecked and no curve fit data', () => {
        const wrapper = mount(
            <CurveFitPanel
                runPropertiesRow={undefined}
                plotOptions={{} as PlotOptions}
                curveFitData={undefined}
                setPlotOption={jest.fn}
            />
        );

        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        const checkboxes = wrapper.find('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(1);
        expect(checkboxes.at(0).prop('checked')).toBeFalsy(); // showCurve
        expect(wrapper.find('.panel-heading').last().text()).toBe('Curve Fit: Linear');
        expect(wrapper.find(CurveFitDataDisplay)).toHaveLength(0);
        expect(wrapper.find(LabelHelpTip)).toHaveLength(1);

        wrapper.unmount();
    });

    test('checked and 4 param curve fit data', () => {
        const wrapper = mount(
            <CurveFitPanel
                runPropertiesRow={{CurveFitMethod: '4 Parameter'}}
                plotOptions={{showLegend: true, xAxisScale: 'log', yAxisScale: 'log', showCurve: true} as PlotOptions}
                curveFitData={{fitParameters: "N/A"} as CurveFitData}
                setPlotOption={jest.fn}
            />
        );

        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        const checkboxes = wrapper.find('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(1);
        expect(checkboxes.at(0).prop('checked')).toBeTruthy(); // showCurve
        expect(wrapper.find('.panel-heading').last().text()).toBe('Curve Fit: 4 Parameter');
        expect(wrapper.find(CurveFitDataDisplay)).toHaveLength(1);
        expect(wrapper.find(LabelHelpTip)).toHaveLength(1);

        wrapper.unmount();
    });
});