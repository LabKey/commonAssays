/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

package org.labkey.elisa;

import org.json.JSONObject;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.actions.AssayRunDetailsAction;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.data.statistics.DoublePoint;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.elisa.actions.ElisaUploadWizardAction;
import org.labkey.elisa.query.CurveFitDb;
import org.labkey.elisa.query.ElisaManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElisaController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ElisaController.class,
            ElisaUploadWizardAction.class);

    public ElisaController()
    {
        setActionResolver(_actionResolver);
    }

    public static class RunDetailsForm
    {
        private int _protocolId;
        private int _runId;
        private String _runName;
        private String _schemaName;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getRunName()
        {
            return _runName;
        }

        public void setRunName(String runName)
        {
            _runName = runName;
        }

        public String getSchemaName()
        {
            return _schemaName;
        }

        public void setSchemaName(String schemaName)
        {
            _schemaName = schemaName;
        }

        public int getProtocolId()
        {
            return _protocolId;
        }

        public void setProtocolId(int protocolId)
        {
            _protocolId = protocolId;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetCurveFitXYPairs extends ReadOnlyApiAction<GetCurveFitXYPairsForm>
    {
        ExpRun _run;

        @Override
        public void validateForm(GetCurveFitXYPairsForm form, Errors errors)
        {
            _run = ExperimentService.get().getExpRun(form.getRunId());
            if (_run == null)
                errors.reject(ERROR_MSG, "Unable to find run for ID " + form.getRunId() + ".");

            if (form.getNumberOfPoints() < 2)
                errors.reject(ERROR_MSG, "At least 2 points must be requested.");
            if (form.getxMin() >= form.getxMax())
                errors.reject(ERROR_MSG, "xMin must be less than xMax.");
        }

        @Override
        public Object execute(GetCurveFitXYPairsForm form, BindException errors) throws Exception
        {
            ExpProtocol protocol = form.getProtocol();
            AssayProvider provider = AssayService.get().getProvider(protocol);
            Domain runDomain = provider.getRunDomain(protocol);
            StatsService.CurveFitType curveFitType = ElisaManager.getRunCurveFitType(runDomain, _run);
            List<Map<String, Object>> points = new ArrayList<>();

            CurveFitDb curveFitDb = ElisaManager.getCurveFit(getContainer(), _run, form.getPlateName(), form.getSpot());
            if (curveFitDb != null)
            {
                CurveFit curveFit = StatsService.get().getCurveFit(curveFitType, new DoublePoint[0]);
                curveFit.setParameters(new JSONObject(curveFitDb.getFitParameters()));
                curveFit.setLogXScale(false);

                double stepSize = (form.getxMax() - form.getxMin()) / (form.getNumberOfPoints() - 1);
                double xVal = form.getxMin();
                while (xVal <= form.getxMax())
                {
                    points.add(Map.of("x", xVal, "y", curveFit.fitCurve(xVal)));
                    xVal += stepSize;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("runId", _run.getRowId());
            response.put("curveFitMethod", curveFitType.getLabel());
            response.put("rSquared", curveFitDb != null ? curveFitDb.getrSquared() : "N/A");
            response.put("fitParameters", curveFitDb != null ? curveFitDb.getFitParameters() : "N/A");
            response.put("points", points);

            return new ApiSimpleResponse(response);
        }
    }

    public static class GetCurveFitXYPairsForm extends AssayRunDetailsAction.AssayRunDetailsForm
    {
        private String _plateName = ManualImportHelper.PLACEHOLDER_PLATE_NAME;
        private int spot = 1;
        private double xMin = 0;
        private double xMax = 100;
        private int numberOfPoints = 2;

        public String getPlateName()
        {
            return _plateName;
        }

        public void setPlateName(String plateName)
        {
            _plateName = plateName;
        }

        public int getSpot()
        {
            return spot;
        }

        public void setSpot(int spot)
        {
            this.spot = spot;
        }

        public double getxMin()
        {
            return xMin;
        }

        public void setxMin(double xMin)
        {
            this.xMin = xMin;
        }

        public double getxMax()
        {
            return xMax;
        }

        public void setxMax(double xMax)
        {
            this.xMax = xMax;
        }

        public int getNumberOfPoints()
        {
            return numberOfPoints;
        }

        public void setNumberOfPoints(int numberOfPoints)
        {
            this.numberOfPoints = numberOfPoints;
        }
    }
}