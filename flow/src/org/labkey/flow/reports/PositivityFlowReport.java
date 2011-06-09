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

import org.labkey.api.gwt.client.util.StringUtils;
import org.labkey.api.query.AliasManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.data.ICSMetadata;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

import java.io.IOException;

/**
 * User: kevink
 * Date: 5/26/11
 */
public class PositivityFlowReport extends FilterFlowReport
{
    public static final String TYPE = "Flow.PositivityReport";
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
    public HttpView getConfigureForm(ViewContext context)
    {
        ICSMetadata metadata = getMetadata(context.getContainer());
        if (metadata == null)
            throw new NotFoundException("ICS metadata required");

        return new JspView<PositivityFlowReport>(PositivityFlowReport.class, "editPositivityReport.jsp", this);
    }

    SubsetSpec getSubset()
    {
        if (_subset == null)
        {
            ReportDescriptor d = getDescriptor();
            String subset = StringUtils.trimToNull(d.getProperty("subset"));
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
        sb.append("report.parameters$subsetDisplay=\"").append(getSubset().getSubset()).append("\"\n");
        sb.append("report.parameters$subsetParent=\"").append(getSubsetParent()).append("\"\n");
        sb.append("report.parameters$subsetParentDisplay=\"").append(getSubsetParent()).append("\"\n");
    }

    @Override
    protected void addSelectList(ViewContext context, String tableName, StringBuilder query)
    {
        ICSMetadata metadata = getMetadata(context.getContainer());
        if (metadata == null)
            throw new NotFoundException("ICS metadata required");

        SubsetSpec subset = getSubset();
        SubsetSpec subsetParent = getSubsetParent();

        String stat = subset + ":Count";
        String parentStat = subsetParent == null ? "Count" : subsetParent.toString() + ":Count";

        for (FieldKey fieldKey : getMatchColumns(metadata))
        {
            String alias = AliasManager.makeLegalName(fieldKey, null);
            query.append("  ").append(tableName).append(".").append(toSQL(fieldKey)).append(" AS ").append(alias).append(",\n");
        }

        query.append("  ").append(tableName).append(".Statistic(").append(toSQL(stat)).append(") AS stat,\n");
        query.append("  ").append(tableName).append(".Background(").append(toSQL(stat)).append(") AS stat_bg,\n");
        query.append("  ").append(tableName).append(".Statistic(").append(toSQL(parentStat)).append(") AS parent,\n");
        query.append("  ").append(tableName).append(".Background(").append(toSQL(parentStat)).append(") AS parent_bg\n");
    }

    @Override
    public boolean updateProperties(PropertyValues pvs, BindException errors, boolean override)
    {
        super.updateBaseProperties(pvs, errors, override);
        updateFromPropertyValues(pvs, "subset");
        if (!override)
            updateFilterProperties(pvs);
        return true;
    }

}
