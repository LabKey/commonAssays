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

import org.labkey.api.query.QueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.reports.report.CustomRReport;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.model.Feature;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.view.PeaksView;

/*
* User: Dave
* Date: Sep 5, 2008
* Time: 1:31:13 PM
*/
public class PeaksRReport extends CustomRReport
{
    public static final String TYPE = "MS1.R.Peaks";
    public static final String[] PARAMS = new String[]{
            MS1Controller.PeaksViewForm.ParamNames.featureId.name(),
            MS1Controller.PeaksViewForm.ParamNames.scanFirst.name(),
            MS1Controller.PeaksViewForm.ParamNames.scanLast.name()
    };

    public PeaksRReport()
    {
        super(PARAMS, TYPE);
    }

    protected QueryView getQueryView(ViewContext context) throws Exception
    {
        ActionURL url = context.getActionURL();
        Feature feature = MS1Manager.get().getFeature(Integer.valueOf(url.getParameter(MS1Controller.PeaksViewForm.ParamNames.featureId.name())).intValue());
        if(null == feature)
            throw new Exception("Unable to locate feature id " + url.getParameter(MS1Controller.PeaksViewForm.ParamNames.featureId.name()));
        
        int scanFirst = feature.getScanFirst().intValue();
        int scanLast = feature.getScanLast().intValue();
        if(null != url.getParameter(MS1Controller.PeaksViewForm.ParamNames.scanFirst.name()))
            scanFirst = Integer.valueOf(url.getParameter(MS1Controller.PeaksViewForm.ParamNames.scanFirst.name())).intValue();
        if(null != url.getParameter(MS1Controller.PeaksViewForm.ParamNames.scanLast.name()))
            scanLast = Integer.valueOf(url.getParameter(MS1Controller.PeaksViewForm.ParamNames.scanLast.name())).intValue();
        
        return new PeaksView(context, new MS1Schema(context.getUser(), context.getContainer()), feature.getExpRun(), feature, scanFirst, scanLast);
    }

    protected boolean hasRequiredParams(ViewContext context)
    {
        return null != context.getActionURL().getParameter(MS1Controller.PeaksViewForm.ParamNames.featureId.name());
    }
}