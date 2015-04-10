/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

package org.labkey.elispot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.RunListDetailsQueryView;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.elispot.query.ElispotAntigenCrosstabTable;
import org.labkey.elispot.query.ElispotRunAntigenTable;
import org.labkey.elispot.query.ElispotRunDataTable;
import org.springframework.validation.BindException;

import java.util.Set;

public class ElispotProtocolSchema extends AssayProtocolSchema
{
    public static final String ELISPOT_DBSCHEMA_NAME = "elispot";
    public static final String ELISPOT_ANTIGEN_SCHEMA_NAME = "elispotantigen";
    public static final String ANTIGEN_STATS_TABLE_NAME = "AntigenStats";   // table as CrossTab
    public static final String ANTIGEN_TABLE_NAME = "Antigen";      // raw table

    public ElispotProtocolSchema(User user, Container container, @NotNull ElispotAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @NotNull
    @Override
    public ElispotAssayProvider getProvider()
    {
        return (ElispotAssayProvider)super.getProvider();
    }

    public Set<String> getTableNames()
    {
        Set<String> names = super.getTableNames();
        names.add(ANTIGEN_TABLE_NAME);
        names.add(ANTIGEN_STATS_TABLE_NAME);
        return names;
    }

    public TableInfo createProviderTable(String name)
    {
        if (name.equalsIgnoreCase(ANTIGEN_TABLE_NAME))
        {
            Domain domain = AbstractAssayProvider.getDomainByPrefix(getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
            if (null != domain)
                return new ElispotRunAntigenTable(this, domain, getProtocol());
        }
        else if (name.equalsIgnoreCase(ANTIGEN_STATS_TABLE_NAME))
        {
            return ElispotAntigenCrosstabTable.create((ElispotRunAntigenTable) createProviderTable(ANTIGEN_TABLE_NAME), getProtocol());
        }

        return super.createProviderTable(name);
    }

    @Override
    public ElispotRunDataTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        ElispotRunDataTable table = new ElispotRunDataTable(this, getProtocol());
        if (includeCopiedToStudyColumns)
        {
            addCopiedToStudyColumns(table, true);
        }
        return table;
    }

    @Nullable
    @Override
    protected ResultsQueryView createDataQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new ElispotResultsQueryView(getProtocol(), context, settings);
    }

    private static final class ElispotResultsQueryView extends ResultsQueryView
    {
        public ElispotResultsQueryView(ExpProtocol protocol, ViewContext context, QuerySettings settings)
        {
            super(protocol, context, settings);
        }

        public DataView createDataView()
        {
            DataView view = super.createDataView();
            view.getDataRegion().setRecordSelectorValueColumns("RowId");
            return view;
        }
    }

    @Nullable
    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new RunListDetailsQueryView(this, settings,
                ElispotController.RunDetailRedirectAction.class, "rowId", ExpRunTable.Column.RowId.toString())
        {
            @Override
            protected void populateButtonBar(DataView view, ButtonBar bar)
            {
                super.populateButtonBar(view, bar);

                ActionURL url = new ActionURL(ElispotController.BackgroundSubtractionAction.class, getContainer());
                ActionButton btn = new ActionButton(url, "Subtract Background");

                btn.setRequiresSelection(true);
                btn.setDisplayPermission(InsertPermission.class);
                btn.setActionType(ActionButton.Action.POST);

                bar.add(btn);
            }
        };
    }
}
