/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.reports.report.AbstractReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.writer.ContainerUser;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.ReportsController;
import org.labkey.flow.query.FlowTableType;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.labkey.api.action.SpringActionController.ERROR_MSG;

/**
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 5:23:36 PM
 */
abstract public class FlowReport extends AbstractReport
{
    protected String updateFromPropertyValues(PropertyValues pvs, String from, String to)
    {
        PropertyValue pv = pvs.getPropertyValue(from);
        if (null != pv)
        {
            String value = String.valueOf(pv.getValue());
            this.getDescriptor().setProperty(to, value);
        }
        return this.getDescriptor().getProperty(to);
    }

    protected String updateFromPropertyValues(PropertyValues pvs, String from)
    {
        return updateFromPropertyValues(pvs, from, from);
    }

    protected String updateFromPropertyValues(PropertyValues pvs, String from, Enum to)
    {
        return updateFromPropertyValues(pvs, from, to.name());
    }

    protected String updateFromPropertyValues(PropertyValues pvs, Enum to)
    {
        return updateFromPropertyValues(pvs, to.name(), to.name());
    }

    protected void updateBaseProperties(ContainerUser cu, PropertyValues pvs, BindException errors, boolean override)
    {
        if (override)
            return;

        String reportName = null;
        PropertyValue pv = pvs.getPropertyValue(ReportDescriptor.Prop.reportName.toString());
        if (pv != null)
            reportName = String.valueOf(pv.getValue());

        if (reportName == null || reportName.length() == 0)
        {
            errors.rejectValue(ERROR_MSG, "Report name is required");
            return;
        }

        Collection<FlowReport> reports = FlowReportManager.getFlowReports(cu.getContainer(), cu.getUser());
        for (FlowReport report : reports)
        {
            if (this.getReportId() != null && this.getReportId().equals(report.getReportId()))
                continue;

            // check for existing report of the same name
            if (reportName.equals(report.getDescriptor().getReportName()))
            {
                errors.reject(ERROR_MSG, "Report with name already exists");
                return;
            }
        }

        updateFromPropertyValues(pvs, ReportDescriptor.Prop.reportName);
        updateFromPropertyValues(pvs, ReportDescriptor.Prop.reportDescription);
    }

    @Override
    public ActionURL getRunReportURL(ViewContext context)
    {
        Container c = ContainerManager.getForId(getDescriptor().getContainerId());
        ActionURL url = new ActionURL(ReportsController.ExecuteAction.class, c);
        url.addParameter("reportId", getReportId().toString());
        String returnUrl = context.getActionURL().getParameter(ActionURL.Param.returnUrl);
        if (StringUtils.trimToEmpty(returnUrl).isEmpty())
            returnUrl = context.cloneActionURL().getLocalURIString();
        url.addParameter(ActionURL.Param.returnUrl, returnUrl);
        return url;
    }

    @NotNull
    @Override
    public ActionURL getEditReportURL(ViewContext context)
    {
        Container c = ContainerManager.getForId(getDescriptor().getContainerId());
        ActionURL url = new ActionURL(ReportsController.UpdateAction.class, c);
        url.addParameter("reportId", getReportId().toString());
        String returnUrl = context.getActionURL().getParameter(ActionURL.Param.returnUrl);
        if (StringUtils.trimToEmpty(returnUrl).isEmpty())
            returnUrl = context.cloneActionURL().getLocalURIString();
        url.addParameter(ActionURL.Param.returnUrl, returnUrl);
        return url;
    }

    String getScriptResource(String file) throws IOException
    {
        Module m =  ModuleLoader.getInstance().getModule(FlowModule.NAME);

        try (InputStream is = m.getResourceStream("flowReports/" + file))
        {
            return PageFlowUtil.getStreamContentsAsString(is);
        }
    }

    public abstract HttpView getConfigureForm(ViewContext context, ActionURL returnURL);

    /** override=true means only set parameters overrideable via the URL on execute */
    public abstract boolean updateProperties(ContainerUser cu, PropertyValues pvs, BindException errors, boolean override);

    public boolean saveToDomain()
    {
        return false;
    }

    /** Returns the list of PropertyDescriptor prototypes. */
    public Collection<PropertyDescriptor> getDomainPrototypeProperties()
    {
        return null;
    }

    /** Get the domain for the FlowTableType from the Shared container or null if it doesn't exist. */
    public @Nullable Domain getDomain(FlowTableType tableType)
    {
        return FlowReportManager.getDomain(this, tableType);
    }

    /** Get the domain for the FlowTableType from the Shared container or null if it wasn't created. */
    public @Nullable Domain ensureDomain(User user, FlowTableType tableType)
    {
        return FlowReportManager.ensureDomain(this, user, tableType);
    }

    /**
     * The exp.object in the Container for this FlowReport instance.
     * The report results will use this as the owner id.
     */
    public @Nullable Integer getOntologyObjectId(Container container)
    {
        return FlowReportManager.getReportOntologyObjectId(this, container);
    }

    /**
     * The exp.object in the Container for this FlowReport instance.
     * The report results will use this as the owner id.
     */
    public @Nullable Integer ensureOntologyObjectId(Container container)
    {
        return FlowReportManager.ensureReportOntologyObjectId(this, container);
    }

    /**
     * Delete all previous saved results in the Container for all FlowTableType Domains.
     * @param container
     */
    public void deleteSavedResults(Container container)
    {
        FlowReportManager.deleteReportResults(this, container);
    }

    @Override
    public void beforeDelete(ContainerUser context)
    {
        super.beforeDelete(context);
        deleteSavedResults(context.getContainer());
    }
}
