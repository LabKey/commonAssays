import React from 'react';
import { mount } from 'enzyme';

import { DataSelectionsPanel } from "./DataSelectionsPanel";
import { PlotOptions } from "../models";
import { LoadingSpinner, SelectInput } from "@labkey/components";

describe('<DataSelectionsPanel/>', () => {
    function validateInputs(wrapper: any, inputCount: number, checkboxCount: number, loadingCount = 0) {
        expect(wrapper.find('.panel-default')).toHaveLength(1);
        expect(wrapper.find('.plot-options-section')).toHaveLength(1);
        expect(wrapper.find('.plot-options-input-row')).toHaveLength(inputCount + loadingCount);
        expect(wrapper.find('.plot-options-field-label')).toHaveLength(inputCount + loadingCount);
        expect(wrapper.find(SelectInput)).toHaveLength(inputCount);
        expect(wrapper.find('input[type="checkbox"]')).toHaveLength(checkboxCount);
        expect(wrapper.find(LoadingSpinner)).toHaveLength(loadingCount);
    }

    test('no plates or spots', () => {
        const wrapper = mount(
            <DataSelectionsPanel
                plates={[]}
                spots={[]}
                samples={['a']}
                controls={['b']}
                plotOptions={{} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        validateInputs(wrapper, 2, 2);
        wrapper.unmount();
    });

    test('one plate and spot', () => {
        const wrapper = mount(
            <DataSelectionsPanel
                plates={['p1']}
                spots={[0]}
                samples={['a']}
                controls={['b']}
                plotOptions={{} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        validateInputs(wrapper, 4, 2);
        wrapper.unmount();
    });

    test('multiple plates and spots', () => {
        const wrapper = mount(
            <DataSelectionsPanel
                plates={['p1', 'p2']}
                spots={[0,1]}
                samples={['a']}
                controls={['b']}
                plotOptions={{} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        validateInputs(wrapper, 4, 2);
        wrapper.unmount();
    });

    test('sample and controls loading', () => {
        const wrapper = mount(
            <DataSelectionsPanel
                plates={[]}
                spots={[]}
                samples={undefined}
                controls={undefined}
                plotOptions={{} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        validateInputs(wrapper, 0, 0, 2);
        wrapper.unmount();
    });
});