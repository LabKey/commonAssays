/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import { render } from '@testing-library/react';

import { CurveFitDataDisplay } from './CurveFitDataDisplay';
import { CurveFitData } from '../models';

describe('CurveFitDataDisplay', () => {
    test('with fit params', () => {
        render(
            <CurveFitDataDisplay
                curveFitData={
                    {
                        rSquared: 0.999,
                        fitParameters: '{"a":1,"b":2}',
                    } as CurveFitData
                }
            />
        );

        const labels = document.querySelectorAll('.curve-fit-field-label');
        expect(labels).toHaveLength(2);
        expect(labels[0]).toHaveTextContent('R Squared: 0.999');
        expect(labels[1]).toHaveTextContent('Fit Parameters:');
        expect(document.querySelectorAll('pre')).toHaveLength(1);
        expect(document.querySelectorAll('.alert')).toHaveLength(0);
    });

    test('without fit params', () => {
        render(
            <CurveFitDataDisplay
                curveFitData={
                    {
                        rSquared: 0.999,
                        fitParameters: 'N/A',
                    } as CurveFitData
                }
            />
        );

        const labels = document.querySelectorAll('.curve-fit-field-label');
        expect(labels).toHaveLength(2);
        expect(labels[0]).toHaveTextContent('R Squared: 0.999');
        expect(labels[1]).toHaveTextContent('Fit Parameters: N/A');
        expect(document.querySelectorAll('pre')).toHaveLength(0);
        expect(document.querySelectorAll('.alert')).toHaveLength(0);
    });

    test('with error', () => {
        render(
            <CurveFitDataDisplay
                curveFitData={
                    {
                        error: 'Test error',
                    } as CurveFitData
                }
            />
        );

        expect(document.querySelectorAll('.curve-fit-field-label')).toHaveLength(0);
        expect(document.querySelectorAll('pre')).toHaveLength(0);
        expect(document.querySelectorAll('.alert')).toHaveLength(1);
    });
});
