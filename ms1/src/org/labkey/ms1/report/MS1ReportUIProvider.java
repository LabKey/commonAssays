/*
 * Copyright (c) 2008 LabKey Corporation
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
import org.labkey.api.reports.report.view.DefaultReportUIProvider;
import org.labkey.api.reports.report.view.RReportBean;
import org.labkey.api.reports.report.view.ReportUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;

import java.util.Map;

/*
* User: Dave
* Date: Sep 5, 2008
* Time: 10:50:55 AM
*/
public class MS1ReportUIProvider extends DefaultReportUIProvider
{
    public void getReportDesignURL(ViewContext context, QuerySettings settings, Map<String, String> designers)
    {
        addDesignerURL(context, settings, designers, FeaturesRReport.TYPE, FeaturesRReport.PARAMS);
        addDesignerURL(context, settings, designers, PeaksRReport.TYPE, PeaksRReport.PARAMS);
    }

    public String getReportIcon(ViewContext context, String reportType)
    {
        if (FeaturesRReport.TYPE.equals(reportType) || PeaksRReport.TYPE.equals(reportType))
            return context.getContextPath() + "/reports/r.gif";
        return super.getReportIcon(context, reportType);
    }
}