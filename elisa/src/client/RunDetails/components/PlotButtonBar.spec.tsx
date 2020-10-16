import React from 'react';
import { mount } from 'enzyme';

import { PlotButtonBar } from "./PlotButtonBar";
import { PlotOptions } from "../models";

describe('<PlotButtonBar/>', () => {
    test('default props', () => {
        const wrapper = mount(
            <PlotButtonBar
                protocolId={1}
                runId={1}
                runPropertiesRow={undefined}
                plotOptions={{showAllSamples:true, showAllControls:true} as PlotOptions}
            />
        );

        expect(wrapper.find('.plot-button-bar')).toHaveLength(1);
        const buttons = wrapper.find('.labkey-button');
        expect(buttons).toHaveLength(3);
        expect(buttons.first().prop('target')).toBe('_blank');
        expect(buttons.first().prop('href')).toBe('undefined/assay/assayResults.view?rowId=1&Data.Run%2FRowId~eq=1');

        wrapper.unmount();
    });
});