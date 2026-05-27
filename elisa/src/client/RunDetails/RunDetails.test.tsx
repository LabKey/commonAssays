/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import { render, waitFor } from '@testing-library/react';

import { RunDetailsImpl } from './RunDetails';
import { TEST_PLOT_DATA } from './utils.test';
import { REQUIRED_COLUMN_NAMES } from './constants';

class MockClass {
    render() {
    }
}

LABKEY.vis = {
    Geom: {
        Path: MockClass,
        Point: MockClass,
    },
    Layer: MockClass,
    Plot: MockClass,
};

describe('RunDetailsImpl', () => {
    test('check initial display', async () => {
        render(
            <RunDetailsImpl
                columnInfo={REQUIRED_COLUMN_NAMES.reduce((columnInfo, name) => {
                    columnInfo[name] = {};
                    return columnInfo;
                }, {})}
                data={TEST_PLOT_DATA}
                getCurveFitXYPairs={jest.fn().mockResolvedValue({})}
                measures={[]}
                protocolId={1}
                runId={2}
                runPropertiesRow={undefined}
            />
        );
        await waitFor(() => {
            expect(document.querySelectorAll('.run-details-left')).toHaveLength(1);
        });

        expect(document.querySelectorAll('.data-selections-panel')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-options-panel')).toHaveLength(1);
        expect(document.querySelectorAll('.curve-fit-panel')).toHaveLength(1);
        expect(document.querySelectorAll('.run-details-right')).toHaveLength(1);
        expect(document.querySelectorAll('.calibration-curve-panel')).toHaveLength(1);
        expect(document.querySelectorAll('.alert')).toHaveLength(0);
    });

    test('missing field keys', async () => {
        render(
            <RunDetailsImpl
                columnInfo={{}}
                data={TEST_PLOT_DATA}
                measures={[]}
                protocolId={1}
                runId={2}
                runPropertiesRow={undefined}
            />
        );
        await waitFor(() => {
            expect(document.querySelectorAll('.alert')).toHaveLength(1);
        });

        expect(document.querySelector('.alert')).toHaveTextContent(REQUIRED_COLUMN_NAMES.join(', '));
    });
});
