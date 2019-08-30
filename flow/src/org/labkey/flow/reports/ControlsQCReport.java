/*
 * Copyright (c) 2009-2018 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.util.Tuple3;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.writer.ContainerUser;
import org.labkey.flow.controllers.ReportsController;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

import java.io.IOException;

/**
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 3:30:40 PM
 */
public class ControlsQCReport extends FilterFlowReport
{
    public static String TYPE = "Flow.QCControlReport";
    public static String DESC = "Flow Controls Statistics over Time";
    public static final String STATISTIC_PROP = "statistic";

    public static ActionURL createURL(Container c, @Nullable ActionURL returnURL, @Nullable ActionURL cancelURL)
    {
        ActionURL url = new ActionURL(ReportsController.CreateAction.class, c).addParameter(ReportDescriptor.Prop.reportType, TYPE);
        if (returnURL != null)
            url.addReturnURL(returnURL);
        if (cancelURL != null)
            url.addCancelURL(cancelURL);
        return url;
    }

    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getTypeDescription()
    {
        return DESC;
    }

    @Override
    String getScriptResource() throws IOException
    {
        return getScriptResource("qc.R");
    }

    @Override
    public HttpView getConfigureForm(ViewContext context, ActionURL returnURL, ActionURL cancelURL)
    {
        return new JspView<>("/org/labkey/flow/reports/editQCReport.jsp", Tuple3.of(this, returnURL, cancelURL));
    }

    @Override
    protected void addSelectList(ViewContext context, String tableName, StringBuilder query)
    {
        ReportDescriptor d = getDescriptor();
        String statistic = d.getProperty(STATISTIC_PROP);

        query.append("  ").append(tableName).append(".Statistic(").append(toSQL(statistic)).append(") AS value,\n");
        query.append("  ").append(toSQL(statistic)).append(" AS statistic\n");
    }

    @Override
    public boolean updateProperties(ContainerUser cu, PropertyValues pvs, BindException errors, boolean override)
    {
        super.updateBaseProperties(cu, pvs, errors, override);
        updateFromPropertyValues(pvs, STATISTIC_PROP);
        if (!override)
            updateFilterProperties(pvs);
        return true;
    }

    @Override
    public boolean hasContentModified(ContainerUser context)
    {
        return hasContentModified(context, STATISTIC_PROP);
    }

    @Override
    public boolean isSandboxed()
    {
        return true;
    }
}
