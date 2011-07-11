/*
 * Copyright (c) 2009-2011 LabKey Corporation
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

import org.apache.commons.io.IOUtils;
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

/**
 * Created by IntelliJ IDEA.
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
    

    protected void updateBaseProperties(PropertyValues pvs, BindException errors, boolean override)
    {
        if (override)
            return;
        updateFromPropertyValues(pvs, ReportDescriptor.Prop.reportName);
        updateFromPropertyValues(pvs, ReportDescriptor.Prop.reportDescription);
    }


    @Override
    public ActionURL getRunReportURL(ViewContext context)
    {
        Container c = ContainerManager.getForId(getDescriptor().getContainerId());
        ActionURL url = new ActionURL(ReportsController.ExecuteAction.class, c);
        url.addParameter("reportId", getReportId().toString());
        return url;
    }

    @Override
    public ActionURL getEditReportURL(ViewContext context)
    {
        Container c = ContainerManager.getForId(getDescriptor().getContainerId());
        ActionURL url = new ActionURL(ReportsController.UpdateAction.class, c);
        url.addParameter("reportId", getReportId().toString());
        return url;
    }


    String getScriptResource(String file) throws IOException
    {
        InputStream is = null;
        try
        {
            Module m =  ModuleLoader.getInstance().getModule(FlowModule.NAME);
            is = m.getResourceStream("/META-INF/" + file);
            return IOUtils.toString(is);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
    }


    public abstract HttpView getConfigureForm(ViewContext context);
    /** override=true means only set parameters overrideable via the URL on execute */
    public abstract boolean updateProperties(PropertyValues pvs, BindException errors, boolean override);

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

