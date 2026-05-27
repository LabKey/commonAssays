/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@labkey/api';

import { AppContext, RunDetails } from './RunDetails';

import './RunDetails.scss';

const render = (target: string, ctx: AppContext): void => {
    createRoot(document.getElementById(target)).render(<RunDetails context={ctx} />);
};

App.registerApp<AppContext>('elisaRunDetails', render, true);
