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
package org.labkey.ms2.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.IconDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Module;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class MassSpecMetadataProtocolSchema extends AssayProtocolSchema
{
    public static final String SEARCH_COUNT_COLUMN = "MS2SearchCount";
    public static final String SEARCHES_COLUMN = "MS2Searches";

    public MassSpecMetadataProtocolSchema(User user, Container container, @NotNull MassSpecMetadataAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        ExpRunTable result = super.createRunsTable();
        SQLFragment searchCountSQL = new SQLFragment();
        searchCountSQL.append(getSearchRunSQL(getContainer(), result.getContainerFilter(), ExprColumn.STR_TABLE_ALIAS + ".RowId", "COUNT(DISTINCT(er.RowId))"));
        ExprColumn searchCountCol = new ExprColumn(result, SEARCH_COUNT_COLUMN, searchCountSQL, JdbcType.INTEGER);
        searchCountCol.setLabel("MS2 Search Count");
        result.addColumn(searchCountCol);

        ColumnInfo searchLinkCol = result.addColumn(SEARCHES_COLUMN, ExpRunTable.Column.RowId);
        searchLinkCol.setHidden(false);
        searchLinkCol.setLabel("MS2 Search Results");
        searchLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new LinkDisplayColumn(colInfo, getContainer());
            }
        });

        List<FieldKey> defaultCols = new ArrayList<>(result.getDefaultVisibleColumns());
        defaultCols.add(2, FieldKey.fromParts(searchLinkCol.getName()));
        result.setDefaultVisibleColumns(defaultCols);

        return result;
    }

    private class LinkDisplayColumn extends IconDisplayColumn
    {
        private ColumnInfo _searchCountColumn;

        public LinkDisplayColumn(ColumnInfo runIdColumn, Container container)
        {
            super(runIdColumn, 18, 18, new ActionURL(MassSpecMetadataController.SearchLinkAction.class, container), "runId", AppProps.getInstance().getContextPath() + "/MS2/images/runIcon.gif");
        }

        @Override
        public void addQueryColumns(Set<ColumnInfo> columns)
        {
            super.addQueryColumns(columns);
            FieldKey fk = new FieldKey(getBoundColumn().getFieldKey().getParent(), SEARCH_COUNT_COLUMN);
            _searchCountColumn = QueryService.get().getColumns(getBoundColumn().getParentTable(), Collections.singletonList(fk)).get(fk);
            if (_searchCountColumn != null)
            {
                columns.add(_searchCountColumn);
            }
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            if (_searchCountColumn == null || ((Number)_searchCountColumn.getValue(ctx)).intValue() > 0)
            {
                super.renderGridCellContents(ctx, out);
            }
            else
            {
                out.write("&nbsp;");
            }
        }
    }

    public static SQLFragment getSearchRunSQL(Container container, ContainerFilter containerFilter, String runIdSQL, String selectSQL)
    {
        SQLFragment searchCountSQL = new SQLFragment("(SELECT " + selectSQL + " FROM " +
                MS2Manager.getTableInfoRuns() + " r, " +
                ExperimentService.get().getTinfoExperimentRun() + " er, " +
                ExperimentService.get().getTinfoData() + " d, " +
                ExperimentService.get().getTinfoDataInput() + " di, " +
                ExperimentService.get().getTinfoProtocolApplication() + " pa " +
                " WHERE di.TargetApplicationId = pa.RowId AND pa.RunId = er.RowId AND r.ExperimentRunLSID = er.LSID " +
                " AND r.deleted = ? AND d.RunId = " + runIdSQL + " AND d.RowId = di.DataId AND (" );
        searchCountSQL.add(Boolean.FALSE);
        String separator = "";
        for (String prefix : MS2Module.SEARCH_RUN_TYPE.getProtocolPrefixes())
        {
            searchCountSQL.append(separator);
            searchCountSQL.append("er.ProtocolLSID LIKE ");
            searchCountSQL.append(ExperimentService.get().getSchema().getSqlDialect().concatenate("'%'", "?", "'%'"));
            searchCountSQL.add(prefix);
            separator = " OR ";
        }
        searchCountSQL.append(") AND ");
        searchCountSQL.append(containerFilter.getSQLFragment(ExperimentService.get().getSchema(), new SQLFragment("er.Container"), container));

        searchCountSQL.append(")");
        return searchCountSQL;
    }

    @Override
    public FilteredTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        final ExpDataTable result = new ExpSchema(getUser(), getContainer()).getDatasTable();
        SQLFragment runConditionSQL = new SQLFragment("RunId IN (SELECT RowId FROM " +
                ExperimentService.get().getTinfoExperimentRun() + " WHERE ProtocolLSID = ?)");
        runConditionSQL.add(getProtocol().getLSID());
        result.addCondition(runConditionSQL, FieldKey.fromParts("RunId"));
        result.getColumn(ExpDataTable.Column.Run).setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable expRunTable = createRunsTable();
                expRunTable.setContainerFilter(result.getContainerFilter());
                return expRunTable;
            }
        });

        List<FieldKey> cols = new ArrayList<>(result.getDefaultVisibleColumns());
        cols.remove(FieldKey.fromParts(ExpDataTable.Column.DataFileUrl));
        Domain fractionDomain = ((MassSpecMetadataAssayProvider)getProvider()).getFractionDomain(getProtocol());
        if (fractionDomain != null)
        {
            for (DomainProperty fractionProperty : fractionDomain.getProperties())
            {
                cols.add(getDataFractionPropertyFieldKey(fractionProperty));
            }
        }
        for (DomainProperty runProperty : getProvider().getRunDomain(getProtocol()).getProperties())
        {
            cols.add(FieldKey.fromParts(ExpDataTable.Column.Run.toString(), runProperty.getName()));
        }
        cols.add(0, FieldKey.fromParts(ExpDataTable.Column.Run.toString(), MassSpecMetadataProtocolSchema.SEARCHES_COLUMN));
        result.setDefaultVisibleColumns(cols);

        return (FilteredTable)result;
    }

    private FieldKey getDataFractionPropertyFieldKey(DomainProperty fractionProperty)
    {
        return FieldKey.fromParts(
                        ExpDataTable.Column.Run.toString(),
                        ExpRunTable.Column.Input.toString(),
                        MassSpecRunCreator.FRACTION_INPUT_ROLE,
                        ExpMaterialTable.Column.Property.toString(),
                        fractionProperty.getName());
    }
}
