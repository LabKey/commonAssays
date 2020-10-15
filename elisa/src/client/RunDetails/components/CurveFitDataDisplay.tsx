import React, { PureComponent } from 'react';
import { Alert } from "@labkey/components";

import { CurveFitData } from "../models";

interface Props {
    curveFitData: CurveFitData
}

export class CurveFitDataDisplay extends PureComponent<Props> {
    render() {
        const { curveFitData } = this.props;
        const hasFitParams = curveFitData.fitParameters?.startsWith('{');

        return (
            <>
                {curveFitData.error &&
                    <Alert>{curveFitData.error}</Alert>
                }
                {curveFitData.rSquared !== undefined &&
                    <div className={'curve-fit-field-label'}>R Squared: {curveFitData.rSquared}</div>
                }
                {curveFitData.fitParameters !== undefined &&
                    <div className={'curve-fit-field-label'}>Fit Parameters: {!hasFitParams && curveFitData.fitParameters}</div>
                }
                {hasFitParams &&
                    <pre>{JSON.stringify(JSON.parse(curveFitData.fitParameters), null, 2)}</pre>
                }
            </>
        )
    }
}