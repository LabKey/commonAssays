/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import { render } from '@testing-library/react';
import { CalibrationCurvePanel } from './CalibrationCurvePanel';
import { getDefaultPlotOptions } from '../utils';

describe('CalibrationCurvePanel', () => {
    test('default props', () => {
        render(
            <CalibrationCurvePanel
                columnInfo={{}}
                curveFitData={undefined}
                data={[]}
                plotOptions={getDefaultPlotOptions([], [])}
                protocolId={1}
                runId={2}
                runPropertiesRow={{}}
            />
        );

        expect(document.querySelectorAll('.calibration-curve-panel')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-panel-display')).toHaveLength(1);
        expect(document.querySelectorAll('.plot-button-bar')).toHaveLength(1);
    });
});
