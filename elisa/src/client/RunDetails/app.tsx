import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@labkey/api';

import { AppContext, RunDetails } from './RunDetails';

import './RunDetails.scss';

App.registerApp<AppContext>('elisaRunDetails', (target: string, ctx: AppContext) => {
    createRoot(document.getElementById(target)).render(<RunDetails context={ctx} />);
});
