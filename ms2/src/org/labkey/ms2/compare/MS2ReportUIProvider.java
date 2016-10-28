/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
package org.labkey.ms2.compare;

import org.labkey.api.query.QuerySettings;
import org.labkey.api.reports.Report;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.view.DefaultReportUIProvider;
import org.labkey.api.reports.report.view.RReportBean;
import org.labkey.api.reports.report.view.ReportUtil;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.peptideview.SingleMS2RunRReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * User: Karl Lum
 * Date: May 21, 2008
 * Time: 11:53:28 AM
 */

public class MS2ReportUIProvider extends DefaultReportUIProvider
{
    private static final Map<String, String> _typeToIconMap = new HashMap<>();

    static
    {
        _typeToIconMap.put(SpectraCountRReport.TYPE, "/reports/r_logo.svg");
        _typeToIconMap.put(SingleMS2RunRReport.TYPE, "/reports/r_logo.svg");
    }

    public List<ReportService.DesignerInfo> getDesignerInfo(ViewContext context, QuerySettings settings)
    {
        List<ReportService.DesignerInfo> reportDesigners = new ArrayList<>();

        boolean canCreateScript = ReportUtil.canCreateScript(context);
        if (canCreateScript && RReport.isEnabled())
        {
            addDesignerURL(context, settings, reportDesigners, SingleMS2RunRReport.TYPE, SingleMS2RunRReport.PARAMS);

            RReportBean bean = new RReportBean(settings);
            bean.setReportType(SpectraCountRReport.TYPE);
            bean.setRedirectUrl(context.getActionURL().toString());

            reportDesigners.add(new DesignerInfoImpl(SpectraCountRReport.TYPE, "R Report", "MS2 Spectra Count R Report",
                    ReportUtil.getRReportDesignerURL(context, bean), "/reports/r_logo.svg"));
        }
        return reportDesigners;
    }

    public String getIconPath(Report report)
    {
        return report != null ? _typeToIconMap.get(report.getType()) : null;
    }
}