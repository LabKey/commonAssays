/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.flow.reports;

import org.labkey.api.data.Container;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.view.DefaultReportUIProvider;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.FlowModule;

import java.util.ArrayList;
import java.util.List;

public class PositivityFlowReportUIProvider extends DefaultReportUIProvider
{
    @Override
    public List<ReportService.DesignerInfo> getDesignerInfo(ViewContext context)
    {
        List<ReportService.DesignerInfo> designers = new ArrayList<>();
        Container c = context.getContainer();
        if (c.hasActiveModuleByName(FlowModule.NAME))
        {
            ActionURL designerURL = PositivityFlowReport.createURL(c, context.getActionURL(), null);
            DesignerInfoImpl info = new DesignerInfoImpl(PositivityFlowReport.TYPE, "Flow Positivity Report", PositivityFlowReport.DESC, designerURL, null);
            designers.add(info);
        }
        return designers;
    }
}
