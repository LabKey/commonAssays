import React from 'react';
import { mount } from 'enzyme';

import { PlotOptionsPanel } from "./PlotOptionsPanel";
import { CurveFitData, PlotOptions } from "../models";
import { CurveFitDataDisplay } from "./CurveFitDataDisplay";

describe('<PlotOptionsPanel/>', () => {
    test('all unchecked', () => {
        const wrapper = mount(
            <PlotOptionsPanel
                plotOptions={{} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        expect(wrapper.find('.plot-options-input-row')).toHaveLength(1);
        expect(wrapper.find('.plot-options-field-label')).toHaveLength(1);
        const checkboxes = wrapper.find('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(3);
        expect(checkboxes.at(0).prop('checked')).toBeFalsy(); // showLegend
        expect(checkboxes.at(1).prop('checked')).toBeFalsy(); // xAxisScale
        expect(checkboxes.at(2).prop('checked')).toBeFalsy(); // yAxisScale

        wrapper.unmount();
    });

    test('all checked', () => {
        const wrapper = mount(
            <PlotOptionsPanel
                plotOptions={{showLegend: true, xAxisScale: 'log', yAxisScale: 'log', showCurve: true} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        expect(wrapper.find('.plot-options-input-row')).toHaveLength(1);
        expect(wrapper.find('.plot-options-field-label')).toHaveLength(1);
        const checkboxes = wrapper.find('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(3);
        expect(checkboxes.at(0).prop('checked')).toBeTruthy(); // showLegend
        expect(checkboxes.at(1).prop('checked')).toBeTruthy(); // xAxisScale
        expect(checkboxes.at(2).prop('checked')).toBeTruthy(); // yAxisScale

        wrapper.unmount();
    });
});