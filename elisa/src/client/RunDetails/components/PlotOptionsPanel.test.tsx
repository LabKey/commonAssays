import React from 'react';
import { render } from '@testing-library/react';

import { PlotOptionsPanel } from './PlotOptionsPanel';
import { PlotOptions } from '../models';

describe('PlotOptionsPanel', () => {
    test('all unchecked', () => {
        render(
            <PlotOptionsPanel
                columnInfo={undefined}
                measures={[]}
                plotOptions={{} as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        expect(document.querySelectorAll('.panel-default')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-section')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-input-row')).toHaveLength(3);
        expect(document.querySelectorAll('.plot-options-field-label')).toHaveLength(3);
        expect(document.querySelectorAll('.plot-options-show-all')).toHaveLength(2);
        const checkboxes = document.querySelectorAll('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(3);
        expect(checkboxes[0]).not.toBeChecked(); // showLegend
        expect(checkboxes[1]).not.toBeChecked(); // xAxisScale
        expect(checkboxes[2]).not.toBeChecked(); // yAxisScale
        expect(document.querySelectorAll('.select-input-container')).toHaveLength(2);
    });

    test('all checked', () => {
        render(
            <PlotOptionsPanel
                columnInfo={undefined}
                measures={[]}
                plotOptions={{ showLegend: true, xAxisScale: 'log', yAxisScale: 'log', showCurve: true } as PlotOptions}
                setPlotOption={jest.fn}
            />
        );

        expect(document.querySelectorAll('.panel-default')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-section')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-input-row')).toHaveLength(3);
        expect(document.querySelectorAll('.plot-options-field-label')).toHaveLength(3);
        expect(document.querySelectorAll('.plot-options-show-all')).toHaveLength(2);
        const checkboxes = document.querySelectorAll('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(3);
        expect(checkboxes[0]).toBeChecked(); // showLegend
        expect(checkboxes[1]).toBeChecked(); // xAxisScale
        expect(checkboxes[2]).toBeChecked(); // yAxisScale
        expect(document.querySelectorAll('.select-input-container')).toHaveLength(2);
    });
});
