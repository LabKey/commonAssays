import React, { PureComponent } from 'react';
import { formatFitParams, formatNumber } from "../utils";

interface Props {
    row: {[key: string]: any}
}

export class RunProperties extends PureComponent<Props> {
    renderRow(label, value) {
        return (
            <tr>
                <td>{label}</td>
                <td>{value}</td>
            </tr>
        )
    }

    render() {
        const { row } = this.props;

        return (
            <table className={'table table-responsive table-condensed detail-component--table__fixed'}>
                <tbody>
                    {this.renderRow('Name', row['Name'])}
                    {this.renderRow('Curve Fit Type', row['CurveFitMethod'] || 'Linear')}
                    {this.renderRow('Curve Fit Parameters', formatFitParams(row))}
                    {this.renderRow('Coefficient of Determination', formatNumber(row['RSquared']))}
                    {this.renderRow('Created', row['Created'])}
                    {this.renderRow('CreatedBy', row['CreatedBy/DisplayName'])}
                </tbody>
            </table>
        )
    }
}