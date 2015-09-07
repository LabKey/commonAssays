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
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.reports.Report;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.reports.report.ReportUrls;
import org.labkey.api.reports.report.view.ReportUtil;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UniqueID;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.WebPartView;
import org.labkey.api.visualization.GenericChartReport;
import org.labkey.api.visualization.GenericChartReportDescriptor;
import org.labkey.elisa.actions.ElisaUploadWizardAction;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public class ElisaController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ElisaController.class,
            ElisaUploadWizardAction.class);

    public ElisaController()
    {
        setActionResolver(_actionResolver);
    }

    public static class GenericReportForm extends ReportUtil.JsonReportForm
    {
        private int _runId;
        private String _renderType;
        private String _dataRegionName;
        private String _jsonData;
        private String _autoColumnYName;
        private String _autoColumnXName;
        private String _runTableName;
        private Double[] _fitParams = new Double[0];
        private String[] _sampleColumns = new String[0];

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getRenderType()
        {
            return _renderType;
        }

        public void setRenderType(String renderType)
        {
            _renderType = renderType;
        }

        public String getDataRegionName()
        {
            return _dataRegionName;
        }

        public void setDataRegionName(String dataRegionName)
        {
            _dataRegionName = dataRegionName;
        }

        public String getJsonData()
        {
            return _jsonData;
        }

        public void setJsonData(String jsonData)
        {
            _jsonData = jsonData;
        }

        public String getAutoColumnYName()
        {
            return _autoColumnYName;
        }

        public void setAutoColumnYName(String autoColumnYName)
        {
            _autoColumnYName = autoColumnYName;
        }

        public String getAutoColumnXName()
        {
            return _autoColumnXName;
        }

        public void setAutoColumnXName(String autoColumnXName)
        {
            _autoColumnXName = autoColumnXName;
        }

        public String getRunTableName()
        {
            return _runTableName;
        }

        public void setRunTableName(String runTableName)
        {
            _runTableName = runTableName;
        }

        public Double[] getFitParams()
        {
            return _fitParams;
        }

        public void setFitParams(Double[] fitParams)
        {
            _fitParams = fitParams;
        }

        public String[] getSampleColumns()
        {
            return _sampleColumns;
        }

        public void setSampleColumns(String[] sampleColumns)
        {
            _sampleColumns = sampleColumns;
        }

        @Override
        public void bindProperties(Map<String, Object> props)
        {
            super.bindProperties(props);

            _renderType = (String)props.get("renderType");
            _dataRegionName = (String)props.get("dataRegionName");

            Object json = props.get("jsonData");
            if (json != null)
                _jsonData = json.toString();
        }

        public static JSONObject toJSON(User user, Container container, Report report)
        {
            JSONObject json = ReportUtil.JsonReportForm.toJSON(user, container, report);
            ReportDescriptor descriptor = report.getDescriptor();

            json.put("renderType", descriptor.getProperty(GenericChartReportDescriptor.Prop.renderType));
            json.put("dataRegionName", descriptor.getProperty(ReportDescriptor.Prop.dataRegionName));
            json.put("jsonData", descriptor.getProperty(ReportDescriptor.Prop.json));

            return json;
        }
    }
}