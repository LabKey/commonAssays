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
package org.labkey.ms1.report;

import org.labkey.api.query.QuerySettings;
import org.labkey.api.reports.Report;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.view.DefaultReportUIProvider;
import org.labkey.api.view.ViewContext;
import org.labkey.ms1.query.MS1Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* User: Dave
* Date: Sep 5, 2008
* Time: 10:50:55 AM
*/
public class MS1ReportUIProvider extends DefaultReportUIProvider
{
    private static final Map<String, String> _typeToIconMap = new HashMap<>();

    static
    {
        _typeToIconMap.put(FeaturesRReport.TYPE, "/reports/r_logo.svg");
        _typeToIconMap.put(PeaksRReport.TYPE, "/reports/r_logo.svg");
    }

    public List<ReportService.DesignerInfo> getDesignerInfo(ViewContext context, QuerySettings settings)
    {
        List<ReportService.DesignerInfo> reportDesigners = new ArrayList<>();

        if (MS1Schema.SCHEMA_NAME.equals(settings.getSchemaName()))
        {
            if (MS1Schema.TABLE_FEATURES.equals(settings.getQueryName()))
                addDesignerURL(context, settings, reportDesigners, FeaturesRReport.TYPE, FeaturesRReport.PARAMS);
            if (MS1Schema.TABLE_PEAKS.equals(settings.getQueryName()))
                addDesignerURL(context, settings, reportDesigners, PeaksRReport.TYPE, PeaksRReport.PARAMS);
        }
        return reportDesigners;
    }

    public String getIconPath(Report report)
    {
        return report != null ? _typeToIconMap.get(report.getType()) : null;
    }
}