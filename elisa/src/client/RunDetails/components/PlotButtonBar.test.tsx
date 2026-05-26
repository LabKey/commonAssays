/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import { render } from '@testing-library/react';

import { PlotButtonBar } from './PlotButtonBar';
import { PlotOptions } from '../models';

describe('PlotButtonBar', () => {
    test('default props', () => {
        render(
            <PlotButtonBar
                plotElement={undefined}
                plotOptions={{ showAllSamples: true, showAllControls: true } as PlotOptions}
                protocolId={1}
                runId={1}
                runPropertiesRow={undefined}
            />
        );

        expect(document.querySelectorAll('.plot-button-bar')).toHaveLength(1);
        const buttons = document.querySelectorAll('.labkey-button');
        expect(buttons).toHaveLength(3);
        expect(buttons[0]).toHaveProperty('target', '_blank');
        expect(buttons[0]).toHaveProperty(
            'href',
            'http://localhost/assay-assayResults.view?rowId=1&Data.Run%2FRowId~eq=1'
        );
    });
});
