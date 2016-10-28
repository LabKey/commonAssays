/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.query.AliasManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.api.writer.ContainerUser;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.ICSMetadata;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * User: kevink
 * Date: 5/26/11
 */
public class PositivityFlowReport extends FilterFlowReport
{
    public static final String TYPE = "Flow.PositivityReport";
    public static final String SUBSET_PROP = "subset";
    private SubsetSpec _subset;
    private SubsetSpec _parentSubset;

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
    String getScriptResource() throws IOException
    {
        return getScriptResource("positivity.R");
    }

    @Override
    public HttpView getConfigureForm(ViewContext context, ActionURL returnURL)
    {
        return new JspView<>(PositivityFlowReport.class, "editPositivityReport.jsp", Pair.of(this, returnURL));
    }

    SubsetSpec getSubset()
    {
        if (_subset == null)
        {
            ReportDescriptor d = getDescriptor();
            String subset = StringUtils.trimToNull(d.getProperty(SUBSET_PROP));
            if (subset == null)
                throw new IllegalArgumentException("subset required");

            _subset = SubsetSpec.fromUnescapedString(subset);
            _parentSubset = _subset.getParent();
        }
        return _subset;
    }

    SubsetSpec getSubsetParent()
    {
        if (_parentSubset == null)
            getSubset();
        return _parentSubset;
    }

    @Override
    void addScriptProlog(ViewContext context, StringBuffer sb)
    {
        super.addScriptProlog(context, sb);
        //sb.append("report.parameters$subsetDisplay=\"").append(getSubset().getSubset()).append("\"\n");
        //sb.append("report.parameters$subsetParent=\"").append(getSubsetParent()).append("\"\n");
        //sb.append("report.parameters$subsetParentDisplay=\"").append(getSubsetParent()).append("\"\n");
    }

    @Override
    protected void addSelectList(ViewContext context, String tableName, StringBuilder query)
    {
        ICSMetadata metadata = getMetadata(context.getContainer());
        if (metadata == null || !metadata.isComplete())
            throw new NotFoundException("ICS metadata required");

        for (FieldKey fieldKey : getMetadataColumns(metadata))
        {
            String alias = AliasManager.makeLegalName(fieldKey, null, false);
            query.append("  ").append(tableName).append(".").append(toSQL(fieldKey)).append(" AS ").append(alias).append(",\n");
        }

        SubsetSpec subset = getSubset();
        SubsetSpec subsetParent = getSubsetParent();

        String stat = subset + ":Count";
        String parentStat = subsetParent == null ? "Count" : subsetParent.toString() + ":Count";

        query.append("  ").append(tableName).append(".Statistic(").append(toSQL(stat)).append(") AS stat,\n");
        query.append("  ").append(tableName).append(".Background(").append(toSQL(stat)).append(") AS stat_bg,\n");
        query.append("  ").append(tableName).append(".Statistic(").append(toSQL(parentStat)).append(") AS parent,\n");
        query.append("  ").append(tableName).append(".Background(").append(toSQL(parentStat)).append(") AS parent_bg\n");
    }

    @Override
    protected List<Filter> getFilters()
    {
        ICSMetadata metadata = getMetadata(getDescriptor().getResourceContainer());
        if (metadata == null || !metadata.isComplete())
            throw new NotFoundException("ICS metadata required");

        // Filter out any wells with no background statistic
        List<Filter> filters = super.getFilters();
        filters.add(0, new Filter(getSubset() + ":Count", "background", null, CompareType.NONBLANK.getPreferredUrlKey()));
        return filters;
    }

    @Override
    public HttpView renderReport(ViewContext context) throws Exception
    {
        ICSMetadata metadata = getMetadata(context.getContainer());
        if (metadata == null || !metadata.isComplete())
        {
            FlowProtocol protocol = FlowProtocol.getForContainer(context.getContainer());
            ActionURL currentURL = context.getActionURL();
            ActionURL editICSMetadataURL = protocol.urlFor(ProtocolController.EditICSMetadataAction.class);
            editICSMetadataURL.addParameter(ActionURL.Param.returnUrl, currentURL.toString());

            return new HtmlView(
                    "<p class='labkey-error'>Positivity report requires configuring flow experiment metadata for study and background information before running.</p>" +
                    PageFlowUtil.textLink("Edit Metadata", editICSMetadataURL));
        }
        else
        {
            return super.renderReport(context);
        }
    }

    @Override
    public boolean updateProperties(ContainerUser cu, PropertyValues pvs, BindException errors, boolean override)
    {
        Container container = cu.getContainer();
        ICSMetadata metadata = getMetadata(container);
        if (metadata == null || !metadata.isComplete())
        {
            errors.reject(SpringActionController.ERROR_MSG,
                    "Positivity report requires configuring flow experiment metadata for study and background information before running.");
            return false;
        }
        else
        {
            super.updateBaseProperties(cu, pvs, errors, override);
            updateFromPropertyValues(pvs, SUBSET_PROP);
            if (!override)
                updateFilterProperties(pvs);
            return true;
        }
    }

    @Override
    public boolean saveToDomain()
    {
        return true;
    }

    @Override
    public Collection<PropertyDescriptor> getDomainPrototypeProperties()
    {
        Collection<PropertyDescriptor> ret = new ArrayList<>();

        Container shared = ContainerManager.getSharedContainer();
        PropertyDescriptor response = new PropertyDescriptor(null, PropertyType.INTEGER, "Response", shared);
        ret.add(response);

        PropertyDescriptor rawP = new PropertyDescriptor(null, PropertyType.DOUBLE, "Raw P", shared);
        rawP.setImportAliasesSet(Collections.singleton("raw_p"));
        ret.add(rawP);

        PropertyDescriptor adjP = new PropertyDescriptor(null, PropertyType.DOUBLE, "Adjusted P", shared);
        adjP.setImportAliasesSet(Collections.singleton("adj_p"));
        ret.add(adjP);

        return ret;
    }

    @Override
    public boolean hasContentModified(ContainerUser context)
    {
        return hasContentModified(context, SUBSET_PROP);
    }
}
