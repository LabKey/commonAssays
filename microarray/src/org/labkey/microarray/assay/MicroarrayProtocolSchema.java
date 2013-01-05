/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.actions.AssayRunDetailsAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.microarray.MicroarraySchema;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class MicroarrayProtocolSchema extends AssayProtocolSchema
{
    public MicroarrayProtocolSchema(User user, Container container, ExpProtocol protocol, Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        ExpRunTable result = super.createRunsTable();

        new MicroarraySchema(getUser(), getContainer()).configureRunsTable(result);
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
        if (AbstractAssayProvider.getDomainByPrefix(getProtocol(), ExpProtocol.ASSAY_DOMAIN_DATA).getProperties().length > 0)
        {
            List<FieldKey> cols = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
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
        MicroarrayRunListQueryView queryView = new MicroarrayRunListQueryView(context, getProtocol());

        queryView.setShowUpdateColumn(true);
        queryView.setShowAddToRunGroupButton(true);

        return queryView;
    }

    private class MicroarrayRunListQueryView extends RunListQueryView
    {
        public MicroarrayRunListQueryView(ViewContext context, ExpProtocol protocol)
        {
            super(protocol, context);
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
