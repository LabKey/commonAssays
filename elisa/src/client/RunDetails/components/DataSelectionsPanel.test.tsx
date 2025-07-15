import React from 'react';
import { render } from '@testing-library/react';

import { DataSelectionsPanel } from './DataSelectionsPanel';
import { PlotOptions } from '../models';

describe('DataSelectionsPanel', () => {
    function validateInputs(inputCount: number, checkboxCount: number, loadingCount = 0) {
        expect(document.querySelectorAll('.panel-default')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-section')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-input-row')).toHaveLength(inputCount + loadingCount);
        expect(document.querySelectorAll('.plot-options-field-label')).toHaveLength(inputCount + loadingCount);
        expect(document.querySelectorAll('.select-input-container')).toHaveLength(inputCount);
        expect(document.querySelectorAll('.plot-options-show-all')).toHaveLength(checkboxCount);
        expect(document.querySelectorAll('input[type="checkbox"]')).toHaveLength(checkboxCount);
        expect(document.querySelectorAll('.fa-spinner')).toHaveLength(loadingCount);
    }

    test('no plates or spots', () => {
        render(
            <DataSelectionsPanel
                controls={['b']}
                plates={[]}
                plotOptions={{} as PlotOptions}
                samples={['a']}
                setPlotOption={jest.fn()}
                spots={[]}
            />
        );

        validateInputs(2, 2);
    });

    test('one plate and spot', () => {
        render(
            <DataSelectionsPanel
                controls={['b']}
                plates={['p1']}
                plotOptions={{} as PlotOptions}
                samples={['a']}
                setPlotOption={jest.fn()}
                spots={[0]}
            />
        );

        validateInputs(4, 2);
    });

    test('multiple plates and spots', () => {
        render(
            <DataSelectionsPanel
                controls={['b']}
                plates={['p1', 'p2']}
                plotOptions={{} as PlotOptions}
                samples={['a']}
                setPlotOption={jest.fn()}
                spots={[0, 1]}
            />
        );

        validateInputs(4, 2);
    });

    test('sample and controls loading', () => {
        render(
            <DataSelectionsPanel
                controls={undefined}
                plates={[]}
                plotOptions={{} as PlotOptions}
                samples={undefined}
                setPlotOption={jest.fn()}
                spots={[]}
            />
        );

        validateInputs(0, 0, 2);
    });
});
