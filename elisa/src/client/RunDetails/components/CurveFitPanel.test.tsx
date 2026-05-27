/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import { render } from '@testing-library/react';

import { CurveFitPanel } from './CurveFitPanel';
import { CurveFitData, PlotOptions } from '../models';

describe('CurveFitPanel', () => {
    test('unchecked and no curve fit data', () => {
        render(
            <CurveFitPanel
                curveFitData={undefined}
                plotOptions={{} as PlotOptions}
                runPropertiesRow={undefined}
                setPlotOption={jest.fn()}
            />
        );

        expect(document.querySelectorAll('.panel-default')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-section')).toHaveLength(1);
        const checkboxes = document.querySelectorAll('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(1);
        expect(checkboxes[0]).not.toBeChecked(); // showCurve
        expect(document.querySelector('.panel-heading')).toHaveTextContent('Curve Fit: Linear');
        expect(document.querySelectorAll('.curve-fit-data-display')).toHaveLength(0);
        expect(document.querySelectorAll('.label-help-target')).toHaveLength(1);
    });

    test('checked and 4 param curve fit data', () => {
        render(
            <CurveFitPanel
                curveFitData={{ fitParameters: 'N/A' } as CurveFitData}
                plotOptions={{ showLegend: true, xAxisScale: 'log', yAxisScale: 'log', showCurve: true } as PlotOptions}
                runPropertiesRow={{ CurveFitMethod: '4 Parameter' }}
                setPlotOption={jest.fn()}
            />
        );

        expect(document.querySelectorAll('.panel-default')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-section')).toHaveLength(1);
        const checkboxes = document.querySelectorAll('input[type="checkbox"]');
        expect(checkboxes).toHaveLength(1);
        expect(checkboxes[0]).toBeChecked(); // showCurve
        expect(document.querySelector('.panel-heading')).toHaveTextContent('Curve Fit: 4 Parameter');
        expect(document.querySelectorAll('.curve-fit-data-display')).toHaveLength(1);
        expect(document.querySelectorAll('.label-help-target')).toHaveLength(1);
    });
});
