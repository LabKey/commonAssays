/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayDataLinkDisplayColumn;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.query.DilutionResultsQueryView;
import org.labkey.api.assay.nab.query.CutoffValueTable;
import org.labkey.api.assay.nab.query.NAbSpecimenTable;
import org.labkey.api.assay.query.ResultsQueryView;
import org.labkey.api.assay.query.RunListDetailsQueryView;
import org.labkey.api.assay.query.RunListQueryView;
import org.labkey.api.cache.BlockingCache;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DatabaseCache;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabAssayController;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;
import org.springframework.validation.BindException;

import java.util.Collections;
import java.util.Set;

/**
 * User: kevink
 * Date: 10/13/12
 */
public class NabProtocolSchema extends AssayProtocolSchema
{
    private static final Cache<String, Set<Double>> CUTOFF_CACHE = new BlockingCache<>(new DatabaseCache<>(NabManager.getSchema().getScope(), 100, "NAbCutoffValues"));
    /*package*/ static final String DATA_ROW_TABLE_NAME = "Data";
    public static final String NAB_DBSCHEMA_NAME = "nab";
    public static final String NAB_VIRUS_SCHEMA_NAME = "nabvirus";
    public static final String CELL_CONTROL_AGGREGATES_TABLE_NAME = "CellControlAggregates";
    public static final String VIRUS_CONTROL_AGGREGATES_TABLE_NAME = "VirusControlAggregates";

    public NabProtocolSchema(User user, Container container, @NotNull NabAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @Override
    public @Nullable TableInfo createDataTable(ContainerFilter cf, boolean includeLinkedToStudyColumns)
    {
        NabRunDataTable table = new NabRunDataTable(this, cf, getProtocol());

        if (includeLinkedToStudyColumns)
        {
            addLinkedToStudyColumns(table, true);
        }
        return table;
    }

    @Override
    public ExpRunTable createRunsTable(ContainerFilter cf)
    {
        final ExpRunTable runTable = super.createRunsTable(cf);
        var nameColumn = runTable.getMutableColumn(ExpRunTable.Column.Name);
        // NAb has two detail type views of a run - the filtered results/data grid, and the run details page that
        // shows the graph. Set the run's name to be a link to the grid instead of the default details page.
        nameColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AssayDataLinkDisplayColumn(colInfo, runTable.getContainerFilter());
            }
        });

        // Add hidden aliased column from Run/RowId to expose the cell control aggregates for this run
        AliasedColumn cellControlAggCol = new AliasedColumn(CELL_CONTROL_AGGREGATES_TABLE_NAME, runTable.getColumn("RowId"));
        cellControlAggCol.setFk(QueryForeignKey.from(this, cf).to(CELL_CONTROL_AGGREGATES_TABLE_NAME, "RunId", "ControlWellgroup"));
        cellControlAggCol.setHidden(true);
        cellControlAggCol.setIsUnselectable(true);
        cellControlAggCol.setKeyField(false); // Issue 42620
        runTable.addColumn(cellControlAggCol);

        // Add hidden aliased column from Run/RowId to expose the virus control aggregates for this run
        AliasedColumn virusControlAggCol = new AliasedColumn(VIRUS_CONTROL_AGGREGATES_TABLE_NAME, runTable.getColumn("RowId"));
        virusControlAggCol.setFk(QueryForeignKey.from(this, cf).to(VIRUS_CONTROL_AGGREGATES_TABLE_NAME, "RunId", "ControlWellgroup"));
        virusControlAggCol.setHidden(true);
        virusControlAggCol.setIsUnselectable(true);
        virusControlAggCol.setKeyField(false); // Issue 42620
        runTable.addColumn(virusControlAggCol);

        return runTable;
    }

    @Nullable
    @Override
    protected ResultsQueryView createDataQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new NabResultsQueryView(getProtocol(), context, settings);
    }

    public Set<Double> getCutoffValues()
    {
        return getCutoffValues(getProtocol());
    }

    /** For databases with a lot of NAb runs, it can be expensive to get the set of unique cutoff values. */
    private static Set<Double> getCutoffValues(final ExpProtocol protocol)
    {
        return CUTOFF_CACHE.get(Integer.toString(protocol.getRowId()), null, new CacheLoader<String, Set<Double>>()
        {
            @Override
            public Set<Double> load(String key, @Nullable Object argument)
            {
                return Collections.unmodifiableSet(DilutionManager.getCutoffValues(protocol));
            }
        });
    }

    public static void clearProtocolFromCutoffCache(int protocolId)
    {
        CUTOFF_CACHE.remove(Integer.toString(protocolId));
    }

    public static class NabResultsQueryView extends DilutionResultsQueryView
    {
        public NabResultsQueryView(ExpProtocol protocol, ViewContext context, QuerySettings settings)
        {
            super(protocol, context, settings);
        }

        @Override
        public ActionURL getGraphSelectedURL()
        {
            return new ActionURL(NabAssayController.NabGraphSelectedAction.class, getContainer());
        }

        @Override
        public ActionURL getRunDetailsURL()
        {
            return new ActionURL(NabAssayController.DetailsAction.class, getContainer());
        }
    }

    public static class NabRunListQueryView extends RunListDetailsQueryView
    {
        public NabRunListQueryView(AssayProtocolSchema schema, QuerySettings settings)
        {
            super(schema, settings, NabAssayController.DetailsAction.class, "rowId", ExpRunTable.Column.RowId.toString());
        }
    }

    @Nullable
    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new NabRunListQueryView(this, settings);
    }

    @Override
    protected TableInfo createProviderTable(String tableType, ContainerFilter cf)
    {
        if(tableType != null)
        {
            if (DilutionManager.CUTOFF_VALUE_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return new CutoffValueTable(this, cf);
            }

            if (DilutionManager.NAB_SPECIMEN_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return new NAbSpecimenTable(this, cf);
            }

            if (DilutionManager.WELL_DATA_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return new NabWellDataTable(this, cf, getProtocol());
            }

            if (DilutionManager.DILUTION_DATA_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return new NabDilutionDataTable(this, cf, getProtocol());
            }

            if (DilutionManager.VIRUS_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                Domain virusDomain = getVirusWellGroupDomain();
                if (virusDomain != null)
                    return new NabVirusDataTable(this, cf, virusDomain);
            }

            if (CELL_CONTROL_AGGREGATES_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return new NabControlAggregatesTable(CELL_CONTROL_AGGREGATES_TABLE_NAME, DilutionManager.CELL_CONTROL_SAMPLE, this, cf, getProtocol(), getProvider());
            }

            if (VIRUS_CONTROL_AGGREGATES_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return new NabControlAggregatesTable(VIRUS_CONTROL_AGGREGATES_TABLE_NAME, DilutionManager.VIRUS_CONTROL_SAMPLE, this, cf, getProtocol(), getProvider());
            }
        }
        return super.createProviderTable(tableType, cf);
    }

    @Override
    public Set<String> getTableNames()
    {
        Set<String> result = super.getTableNames();
        result.add(DilutionManager.CUTOFF_VALUE_TABLE_NAME);
        result.add(DilutionManager.WELL_DATA_TABLE_NAME);
        result.add(DilutionManager.DILUTION_DATA_TABLE_NAME);
        result.add(CELL_CONTROL_AGGREGATES_TABLE_NAME);
        result.add(VIRUS_CONTROL_AGGREGATES_TABLE_NAME);

        if (getVirusWellGroupDomain() != null)
            result.add(DilutionManager.VIRUS_TABLE_NAME);

        return result;
    }

    @Nullable
    private Domain getVirusWellGroupDomain()
    {
        AssayProvider provider = getProvider();
        if ((provider instanceof NabAssayProvider) && ((NabAssayProvider)provider).supportsMultiVirusPlate())
        {
            return ((NabAssayProvider)provider).getVirusWellGroupDomain(getProtocol());
        }
        return null;
    }
}
