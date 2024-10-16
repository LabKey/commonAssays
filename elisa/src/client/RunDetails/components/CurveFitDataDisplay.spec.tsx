import React from 'react';
import { mount } from 'enzyme';
import { Alert } from "@labkey/components";

import { CurveFitDataDisplay } from "./CurveFitDataDisplay";
import { CurveFitData } from "../models";

describe('<CurveFitDataDisplay/>', () => {
    test('with fit params', () => {
        const wrapper = mount(
            <CurveFitDataDisplay
                curveFitData={{
                    rSquared: 0.999,
                    fitParameters: "{\"a\":1,\"b\":2}"
                } as CurveFitData}
            />
        );

        const divs = wrapper.find('.curve-fit-field-label');
        expect(divs).toHaveLength(2);
        expect(divs.first().text()).toBe('R Squared: 0.999');
        expect(divs.last().text()).toBe('Fit Parameters: ');
        expect(wrapper.find('pre')).toHaveLength(1);
        expect(wrapper.find(Alert)).toHaveLength(0);

        wrapper.unmount();
    });

    test('without fit params', () => {
        const wrapper = mount(
            <CurveFitDataDisplay
                curveFitData={{
                    rSquared: 0.999,
                    fitParameters: "N/A"
                } as CurveFitData}
            />
        );

        const divs = wrapper.find('.curve-fit-field-label');
        expect(divs).toHaveLength(2);
        expect(divs.first().text()).toBe('R Squared: 0.999');
        expect(divs.last().text()).toBe('Fit Parameters: N/A');
        expect(wrapper.find('pre')).toHaveLength(0);
        expect(wrapper.find(Alert)).toHaveLength(0);

        wrapper.unmount();
    });

    test('with error', () => {
        const wrapper = mount(
            <CurveFitDataDisplay
                curveFitData={{
                    error: 'Test error'
                } as CurveFitData}
            />
        );

        expect(wrapper.find('.curve-fit-field-label')).toHaveLength(0);
        expect(wrapper.find('pre')).toHaveLength(0);
        expect(wrapper.find(Alert)).toHaveLength(1);

        wrapper.unmount();
    });
});