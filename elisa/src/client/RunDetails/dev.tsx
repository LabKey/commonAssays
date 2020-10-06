import React from 'react';
import ReactDOM from 'react-dom';

import { AppContainer } from 'react-hot-loader';
import { App } from '@labkey/api';
import { AppContext, RunDetails } from './RunDetails';

App.registerApp<AppContext>('elisaRunDetails', (target: string, ctx: AppContext) => {
    ReactDOM.render(
        <AppContainer>
            <RunDetails context={ctx} />
        </AppContainer>,
        document.getElementById(target)
    );
}, true /* hot */);

declare const module: any;

