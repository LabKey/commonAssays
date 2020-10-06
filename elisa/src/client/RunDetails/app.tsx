import React from 'react';
import ReactDOM from 'react-dom';

import { AppContext, RunDetails } from './RunDetails';
import { App } from '@labkey/api';

App.registerApp<AppContext>('elisaRunDetails', (target: string, ctx: AppContext) => {
    ReactDOM.render(<RunDetails context={ctx} />, document.getElementById(target));
});
