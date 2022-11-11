import React from 'react';
import ReactDOM from 'react-dom';
import { App } from '@labkey/api';

import { AppContext, RunDetails } from './RunDetails';

import './RunDetails.scss';

App.registerApp<AppContext>('elisaRunDetails', (target: string, ctx: AppContext) => {
    ReactDOM.render(<RunDetails context={ctx} />, document.getElementById(target));
});
