/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.microarray.assay;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.api.study.assay.AssayRunType;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.microarray.query.MicroarrayUserSchema;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class MicroarrayProtocolSchema extends AssayProtocolSchema
{
    public MicroarrayProtocolSchema(User user, Container container, @NotNull MicroarrayAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        ExpRunTable result = super.createRunsTable();

        new MicroarrayUserSchema(getUser(), getContainer()).configureRunsTable(result);
        if (getProvider().isEditableRuns(getProtocol()))
        {
            result.addAllowablePermission(UpdatePermission.class);
        }

        return result;
    }

    @Override
    public AssayResultTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        AssayResultTable result = new AssayResultTable(this, includeCopiedToStudyColumns);
        if (!AbstractAssayProvider.getDomainByPrefix(getProtocol(), ExpProtocol.ASSAY_DOMAIN_DATA).getProperties().isEmpty())
        {
            List<FieldKey> cols = new ArrayList<>(result.getDefaultVisibleColumns());
            Iterator<FieldKey> iterator = cols.iterator();
            while (iterator.hasNext())
            {
                FieldKey key = iterator.next();
                if ("Run".equals(key.getParts().get(0)))
                {
                    iterator.remove();
                }
            }
            result.setDefaultVisibleColumns(cols);
        }

        return result;
    }

    @Nullable
    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        MicroarrayRunListQueryView queryView = new MicroarrayRunListQueryView(this, settings, new AssayRunType(getProtocol(), getContainer()));

        queryView.setShowUpdateColumn(true);
        queryView.setShowAddToRunGroupButton(true);

        return queryView;
    }

    private class MicroarrayRunListQueryView extends RunListQueryView
    {
        public MicroarrayRunListQueryView(MicroarrayProtocolSchema schema, QuerySettings settings, AssayRunType type)
        {
            super(schema, settings, type);
        }

        protected void populateButtonBar(DataView view, ButtonBar bar)
        {
            super.populateButtonBar(view, bar);

            ActionURL url = new ActionURL();
            url.setPath("/microarray/geo_export.view");

            ViewContext context = HttpView.currentContext();
            url.setContainer(context.getContainer());
            ActionButton btn = new ActionButton(url, "Create GEO Export");
            bar.add(btn);
        }
    }
}
