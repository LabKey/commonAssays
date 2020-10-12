import React from 'react';
import { mount } from 'enzyme';

import { PlotOptionsPanel } from "./PlotOptionsPanel";
import { CurveFitData, PlotOptions } from "../models";
import { CurveFitDataDisplay } from "./CurveFitDataDisplay";

describe('<PlotOptionsPanel/>', () => {
    test('all unchecked and no curve fit data', () => {
        const wrapper = mount(
            <PlotOptionsPanel
                runPropertiesRow={undefined}
                plotOptions={{} as PlotOptions}
                curveFitData={undefined}
                setPlotOption={jest.fn}
            />
        );

        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        expect(wrapper.find('.plot-options-input-row')).toHaveLength(2);
        expect(wrapper.find('.plot-options-field-label')).toHaveLength(2);
        const checkboxes = wrapper.find('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(4);
        expect(checkboxes.at(0).prop('checked')).toBeFalsy(); // showLegend
        expect(checkboxes.at(1).prop('checked')).toBeFalsy(); // xAxisScale
        expect(checkboxes.at(2).prop('checked')).toBeFalsy(); // yAxisScale
        expect(checkboxes.at(3).prop('checked')).toBeFalsy(); // showCurve
        expect(wrapper.find('.plot-options-field-label').last().text()).toBe('Curve Fit: Linear');
        expect(wrapper.find(CurveFitDataDisplay)).toHaveLength(0);

        wrapper.unmount();
    });

    test('all checked and 4 param curve fit data', () => {
        const wrapper = mount(
            <PlotOptionsPanel
                runPropertiesRow={{CurveFitMethod: '4 Parameter'}}
                plotOptions={{showLegend: true, xAxisScale: 'log', yAxisScale: 'log', showCurve: true} as PlotOptions}
                curveFitData={{fitParameters: "N/A"} as CurveFitData}
                setPlotOption={jest.fn}
            />
        );

        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        expect(wrapper.find('.plot-options-input-row')).toHaveLength(2);
        expect(wrapper.find('.plot-options-field-label')).toHaveLength(2);
        const checkboxes = wrapper.find('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(4);
        expect(checkboxes.at(0).prop('checked')).toBeTruthy(); // showLegend
        expect(checkboxes.at(1).prop('checked')).toBeTruthy(); // xAxisScale
        expect(checkboxes.at(2).prop('checked')).toBeTruthy(); // yAxisScale
        expect(checkboxes.at(3).prop('checked')).toBeTruthy(); // showCurve
        expect(wrapper.find('.plot-options-field-label').last().text()).toBe('Curve Fit: 4 Parameter');
        expect(wrapper.find(CurveFitDataDisplay)).toHaveLength(1);

        wrapper.unmount();
    });
});