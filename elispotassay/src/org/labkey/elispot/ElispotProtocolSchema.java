/*
 * Copyright (c) 2008-2012 LabKey Corporation
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
import org.labkey.api.data.*;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.RunListDetailsQueryView;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.elispot.query.ElispotRunAntigenTable;
import org.labkey.elispot.query.ElispotRunDataTable;
import org.springframework.validation.BindException;

import java.sql.SQLException;
import java.util.Set;

public class ElispotProtocolSchema extends AssayProtocolSchema
{
    public static final String ANTIGEN_STATS_TABLE_NAME = "AntigenStats";

    public ElispotProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @NotNull ElispotAssayProvider provider, @Nullable Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
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
        names.add(ANTIGEN_STATS_TABLE_NAME);
        return names;
    }

    public TableInfo createProviderTable(String name)
    {
        TableInfo table = super.createProviderTable(name);
        if (table != null)
            return table;
        
        if (name.equalsIgnoreCase(ANTIGEN_STATS_TABLE_NAME))
            return new ElispotRunAntigenTable(this, getProtocol());

        return super.createProviderTable(name);
    }

    public static PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol, String propertyPrefix) throws SQLException
    {
        String propPrefix = new Lsid(propertyPrefix, protocol.getName(), "").toString();
        SimpleFilter propertyFilter = new SimpleFilter();
        propertyFilter.addCondition("PropertyURI", propPrefix, CompareType.STARTS_WITH);

        PropertyDescriptor[] result = Table.select(OntologyManager.getTinfoPropertyDescriptor(), Table.ALL_COLUMNS,
                propertyFilter, null, PropertyDescriptor.class);

        // Merge measure/dimension properties from well group domain into ElispotProperties domain
        // This needs to be removed and instead base the properties on a single PropertyDescriptor/DomainProperty
        Domain domain = PropertyService.get().getDomain(protocol.getContainer(), new Lsid(ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP, "Folder-" + protocol.getContainer().getRowId(), protocol.getName()).toString());
        if (domain != null)
        {
            for (PropertyDescriptor dataProperty : result)
            {
                DomainProperty domainProp = domain.getPropertyByName(dataProperty.getName());
                if (domainProp != null)
                {
                    dataProperty.setMeasure(domainProp.isMeasure());
                    dataProperty.setDimension(domainProp.isDimension());
                }
            }
        }
        return result;
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
            view.getDataRegion().setRecordSelectorValueColumns("ObjectId");
            return view;
        }
    }

    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new RunListDetailsQueryView(getProtocol(), context,
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
