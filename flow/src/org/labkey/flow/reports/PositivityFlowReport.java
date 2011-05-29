/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

/**
 * User: kevink
 * Date: 5/26/11
 */
public class PositivityFlowReport extends FlowReport
{
    public static final String TYPE = "Flow.PositivityReport";
    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getTypeDescription()
    {
        return "Flow Positivity Call";
    }

    @Override
    public HttpView getConfigureForm()
    {
        return new JspView<PositivityFlowReport>(PositivityFlowReport.class, "editPositivityReport.jsp", this);
    }

    @Override
    public boolean updateProperties(PropertyValues pvs, BindException errors, boolean override)
    {
        super.updateBaseProperties(pvs, errors, override);
        //updateFormPropertyValues(pvs, "statistic");
        if (!override)
            ;//updateFilterProperties(pvs);
        return true;
    }

    @Override
    public HttpView renderReport(ViewContext context) throws Exception
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
