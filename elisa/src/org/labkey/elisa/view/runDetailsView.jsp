<%
/*
 * Copyright (c) 2012-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.elisa.ElisaController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext4"); // TODO remove all remaining usages
        dependencies.add("vischart");
    }
%>
<%
    JspView<ElisaController.RunDetailsForm> me = (JspView<ElisaController.RunDetailsForm>) HttpView.currentView();
    ElisaController.RunDetailsForm form = me.getModelBean();
    ObjectMapper jsonMapper = new ObjectMapper();

    String runPropsId = "run-properties-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
    String plotId = "plot-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>

<style type="text/css">
    .field-label {
        padding-right: 10px;
        font-weight: bold;
    }
</style>

<script type="text/javascript">
    var runPropsId = <%=q(runPropsId)%>;
    var plotId = <%=q(plotId)%>;
    var protocolId = <%=form.getProtocolId()%>;
    var schemaName = <%=q(form.getSchemaName())%>;
    var runId = <%=form.getRunId()%>;
    var runName = <%=q(form.getRunName())%>;
    var params = <%=text(jsonMapper.writeValueAsString(form.getFitParams()))%>;

    LABKEY.Utils.onReady(function(){
        _togglePropertiesLoading(true);
        LABKEY.Query.selectRows({
            schemaName: schemaName,
            queryName: 'Runs',
            columns: 'Name,Created,CreatedBy/DisplayName,CurveFitMethod,RSquared,CurveFitParams',
            filterArray: [LABKEY.Filter.create('RowId', runId)],
            scope: this,
            success: function(data) {
                _togglePropertiesLoading(false);
                var row = data.rows[0];
                _renderRunProperties(row);
            }
        });

        _togglePlotLoading(true);
        _toggleExportOptions(false);
        LABKEY.Query.selectRows({
            schemaName: schemaName,
            queryName: 'Data',
            columns: 'SpecimenLsid/Property/SpecimenId,WellLocation,Absorption,Concentration',
            filterArray: [
                LABKEY.Filter.create('Run/RowId', runId),
                // LABKEY.Filter.create('PlateName', 'Plate_1LK05A1071'),
                // LABKEY.Filter.create('Spot', 1),
            ],
            success: function(data) {
                _togglePlotLoading(false);
                _toggleExportOptions(true);
                _renderPlot(data);
            }
        })
    });

    function _renderRunProperties(row) {
        document.getElementById(runPropsId).innerHTML =
            '<div>' +
                '<table>' +
                    '<tr><td class="field-label">Name</td><td>' + row.Name + '</td></tr>' +
                    '<tr><td class="field-label">Curve Fit Type</td><td>' + (row.CurveFitMethod || 'Linear') + '</td></tr>' +
                    '<tr><td class="field-label">Curve Fit Parameters</td><td>' + _formatFitParams(row) + '</td></tr>' +
                    '<tr><td class="field-label">Coefficient of Determination</td><td>' + _formatNumber(row.RSquared, "0.00000") + '</td></tr>' +
                    '<tr><td class="field-label">Created</td><td>' + row.Created + '</td></tr>' +
                    '<tr><td class="field-label">Created By</td><td>' + row['CreatedBy/DisplayName'] + '</td></tr>' +
                '</table>' +
            '</div>';
    }

    function _renderPlot(data) {
        // TODO add point hover details

        new LABKEY.vis.Plot({
            renderTo: plotId,
            width: 1095,
            height: 406,
            labels: {
                main: {
                    value: runName
                },
                x: {
                    value: "Concentration"
                },
                y: {
                    value:"Absorption"
                }
            },
            layers: [
                new LABKEY.vis.Layer({
                    data: data.rows,
                    geom: new LABKEY.vis.Geom.Point({
                        size: 5,
                        opacity: .5,
                        color: '#116596'
                    }),
                    aes: {
                        x: 'Concentration',
                        y: 'Absorption'
                    }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Path({
                        size: 2,
                        color: '#555'
                    }),
                    aes: {x: 'x', y: 'y'},
                    data: LABKEY.vis.Stat.fn(_linearCurveFitFn(params), 2, 0, 100)
                })
            ],
            scales: {
                x: {scaleType: 'continuous', trans: 'linear', tickFormat: _tickFormatFn},
                y: {scaleType: 'continuous', trans: 'linear', tickFormat: _tickFormatFn}
            }
        }).render();
    }

    var valExponentialDigits = 6;
    function _tickFormatFn(value) {
        if (LABKEY.Utils.isNumber(value) && Math.abs(Math.round(value)).toString().length >= valExponentialDigits) {
            return value.toExponential();
        }
        return value;
    }

    function _linearCurveFitFn(params){
        if (params && params.length >= 2) {
            return function(x){return x * params[0] + params[1];}
        }
        return function(x) {return x;}
    }

    function _formatFitParams(row) {
        if (row.CurveFitParams) {
            var parts = row.CurveFitParams.split('&');
            return 'Slope : ' + _formatNumber(parts[0], "0.00") + ', Intercept : ' + _formatNumber(parts[1], "0.00");
        }
        return 'There was an error loading the curve fit parameters.';
    }

    function _formatNumber(value, format) {
        return Ext4.util.Format.number(value, format)
    }

    function _exportSVGToFile(format) {
        var svgEl = document.getElementById(plotId).children[0];
        var title = 'Calibration Curve';
        LABKEY.vis.SVGConverter.convert(svgEl, format, title);
    }

    function _toggleExportOptions(show) {
        document.getElementById('plot-export-options').style.display = show ? 'block' : 'none';
    }

    function _togglePlotLoading(show) {
        document.getElementById(plotId).innerHTML = show ? '<div><i class="fa fa-spinner fa-pulse"></i> loading plot...</div>' : '';
    }

    function _togglePropertiesLoading(show) {
        document.getElementById(runPropsId).innerHTML = show ? '<div><i class="fa fa-spinner fa-pulse"></i> loading properties...</div>' : '';
    }

    function _goToRunResults() {
        window.location = LABKEY.ActionURL.buildURL('assay', 'assayResults', undefined, {rowId: protocolId, 'Data.Run/RowId~eq': runId})
    }
</script>

<div class="panel panel-default">
    <div class="panel-heading">
        <div class="panel-title">
            Run Properties
        </div>
        <div class="clearfix"></div>
    </div>
    <div class="panel-body">
        <div id=<%=q(runPropsId)%>></div>
    </div>
</div>
<div class="panel panel-default">
    <div class="panel-heading">
        <div class="panel-title">
            Calibration Curve
        </div>
        <div class="clearfix"></div>
    </div>
    <div class="panel-body">
        <div id=<%=q(plotId)%>></div>
        <div id="plot-export-options" style="display: none;">
            <button class="labkey-button" onclick="_exportSVGToFile(LABKEY.vis.SVGConverter.FORMAT_PNG)">Export to PNG</button>
            <button class="labkey-button" onclick="_exportSVGToFile(LABKEY.vis.SVGConverter.FORMAT_PDF)">Export to PDF</button>
            <button class="labkey-button primary" onclick="_goToRunResults()">View Results</button>
        </div>
    </div>
</div>

