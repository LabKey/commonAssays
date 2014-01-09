/*
 * Copyright (c) 2009-2014 LabKey Corporation
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

package org.labkey.viability;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MVDisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UrlColumn;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.PropertyColumn;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.SpecimenForeignKey;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ViabilityAssaySchema extends AssayProtocolSchema
{
    public enum UserTables
    {
        ResultSpecimens
    }

    public ViabilityAssaySchema(User user, Container container, ExpProtocol protocol, Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
        assert protocol != null;
    }

    public static DbSchema getSchema()
    {
        return ViabilitySchema.getSchema();
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public Set<String> getTableNames()
    {
        Set<String> result = super.getTableNames();
        result.add(UserTables.ResultSpecimens.name());
        return result;
    }

    public TableInfo createProviderTable(String name)
    {
        if (name.equalsIgnoreCase(UserTables.ResultSpecimens.name()))
            return createResultSpecimensTable();

        return super.createProviderTable(name);
    }

    @Override
    public FilteredTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        // UNDONE: add copy to study columns when copy to study is implemented
        //addCopiedToStudyColumns(table, protocol, schema.getUser(), "rowId", true);
        return new ResultsTable();
    }

    public ResultSpecimensTable createResultSpecimensTable()
    {
        return new ResultSpecimensTable();
    }

    public ExpDataTable createDataFileTable()
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(ExpSchema.TableType.Data.toString(), this);
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.addColumn(ExpDataTable.Column.SourceProtocolApplication).setHidden(true);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setHidden(true);
        ret.setPublicSchemaName(ExpSchema.SCHEMA_NAME);
        return ret;
    }

    class ViabilityAssayTable extends FilteredTable<ViabilityAssaySchema>
    {
        ViabilityAssayProvider _provider;

        protected ViabilityAssayTable(TableInfo table)
        {
            super(table, ViabilityAssaySchema.this);
            _provider = ViabilityManager.get().getProvider();
            _defaultVisibleColumns = new ArrayList<>();
            setPublicSchemaName(ViabilityAssaySchema.this.getSchemaName());
        }

        protected ColumnInfo addVisible(ColumnInfo col)
        {
            ColumnInfo ret = addColumn(col);
            addDefaultVisible(col.getName());
            return ret;
        }

        protected void addDefaultVisible(String... key)
        {
            addDefaultVisible(FieldKey.fromParts(key));
        }

        protected void addDefaultVisible(FieldKey fieldKey)
        {
            ((List<FieldKey>)_defaultVisibleColumns).add(fieldKey);
        }

    }

    protected static ColumnInfo copyProperties(ColumnInfo column, DomainProperty dp)
    {
        if (dp != null)
        {
            PropertyDescriptor pd = dp.getPropertyDescriptor();
            column.setLabel(pd.getLabel() == null ? column.getName() : pd.getLabel());
            column.setDescription(pd.getDescription());
            column.setFormat(pd.getFormat());
            column.setDisplayWidth(pd.getDisplayWidth());
            column.setHidden(pd.isHidden());
            column.setNullable(!pd.isRequired());
        }
        return column;
    }

    public class ResultsTable extends ViabilityAssayTable
    {
        protected Domain _resultsDomain;

        public ResultsTable()
        {
            super(ViabilitySchema.getTableInfoResults());
            setName(AssayProtocolSchema.DATA_TABLE_NAME);

            _resultsDomain = _provider.getResultsDomain(getProtocol());
            DomainProperty[] resultDomainProperties = _resultsDomain.getProperties();
            Map<String, DomainProperty> propertyMap = new LinkedHashMap<>(resultDomainProperties.length);
            for (DomainProperty property : resultDomainProperties)
                propertyMap.put(property.getName(), property);

            ColumnInfo rowId = addColumn(wrapColumn(getRealTable().getColumn("RowId")));
            rowId.setHidden(true);
            rowId.setKeyField(true);

            ColumnInfo dataColumn = addColumn(wrapColumn("Data", getRealTable().getColumn("DataId")));
            dataColumn.setHidden(true);
            dataColumn.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    ExpDataTable dataTable = ViabilityAssaySchema.this.createDataFileTable();
                    dataTable.setContainerFilter(getContainerFilter());
                    return dataTable;
                }
            });

            ColumnInfo objectIdCol = wrapColumn(_rootTable.getColumn("ObjectId"));
            for (DomainProperty dp : resultDomainProperties)
            {
                ColumnInfo col;

                // UNDONE: OOR columns?

                if (getRealTable().getColumn(dp.getName()) != null)
                {
                    // Add the property from the hard-table.
                    col = addColumn(copyProperties(wrapColumn(getRealTable().getColumn(dp.getName())), dp));
                    if (!ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME.equals(dp.getName()))
                        addDefaultVisible(col.getName());
                }
                else if (ViabilityAssayProvider.VIABILITY_PROPERTY_NAME.equals(dp.getName()))
                {
                    col = new ExprColumn(this, "Viability", new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".TotalCells > 0 THEN CAST(" + ExprColumn.STR_TABLE_ALIAS + ".ViableCells AS FLOAT) / " + ExprColumn.STR_TABLE_ALIAS + ".TotalCells ELSE 0.0 END)"), JdbcType.DOUBLE);
                    copyProperties(col, propertyMap.get(ViabilityAssayProvider.VIABILITY_PROPERTY_NAME));
                    addVisible(col);
                }
                else if (ViabilityAssayProvider.RECOVERY_PROPERTY_NAME.equals(dp.getName()))
                {
                    col = new SpecimenAggregateColumn(this, "Recovery",
                            new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + "$z.OriginalCells IS NULL OR " +
                                    ExprColumn.STR_TABLE_ALIAS + "$z.OriginalCells = 0 THEN NULL " +
                                    "ELSE " + ExprColumn.STR_TABLE_ALIAS + ".ViableCells / " + ExprColumn.STR_TABLE_ALIAS + "$z.OriginalCells END)"),
                            JdbcType.DOUBLE);
                    copyProperties(col, _resultsDomain.getPropertyByName(ViabilityAssayProvider.RECOVERY_PROPERTY_NAME));
                    addColumn(col);
                }
                else if (ViabilityAssayProvider.ORIGINAL_CELLS_PROPERTY_NAME.equals(dp.getName()))
                {
                    col = new SpecimenAggregateColumn(this, "OriginalCells", JdbcType.DOUBLE);
                    copyProperties(col, propertyMap.get(ViabilityAssayProvider.ORIGINAL_CELLS_PROPERTY_NAME));
                    addVisible(col);
                }
                else if (ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME.equals(dp.getName()))
                {
                    SQLFragment specimenIDs = new SQLFragment();
                    if (getDbSchema().getSqlDialect().isSqlServer())
                    {
                        specimenIDs.append("(REPLACE(");
                        specimenIDs.append("(SELECT rs.SpecimenID AS [data()]");
                        specimenIDs.append(" FROM " + ViabilitySchema.getTableInfoResultSpecimens() + " rs");
                        specimenIDs.append(" WHERE rs.ResultID = " + ExprColumn.STR_TABLE_ALIAS + ".RowID");
                        specimenIDs.append(" ORDER BY rs.SpecimenIndex");
                        specimenIDs.append(" FOR XML PATH ('')),' ', ','))");
                    }
                    else if (getDbSchema().getSqlDialect().isPostgreSQL())
                    {
                        specimenIDs.append("(SELECT array_to_string(viability.array_accum(rs1.SpecimenID), ',')");
                        specimenIDs.append("  FROM (SELECT rs2.SpecimenID, rs2.ResultID FROM viability.ResultSpecimens rs2");
                        specimenIDs.append("    WHERE rs2.ResultID = " + ExprColumn.STR_TABLE_ALIAS + ".RowID");
                        specimenIDs.append("    ORDER BY rs2.SpecimenIndex) AS rs1");
                        specimenIDs.append("  GROUP BY rs1.ResultID");
                        specimenIDs.append(")");
                    }
                    else
                    {
                        throw new UnsupportedOperationException("SqlDialect not supported: " + getDbSchema().getSqlDialect().getClass().getSimpleName());
                    }
                    col = new ExprColumn(this, "SpecimenIDs", specimenIDs, JdbcType.VARCHAR);
                    copyProperties(col, propertyMap.get(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME));
                    col.setDisplayColumnFactory(new MissingSpecimenPopupFactory());
                    addVisible(col);
                }
                else
                {
                    // Add the user's property column
                    PropertyDescriptor pd = dp.getPropertyDescriptor();
                    col = new PropertyColumn(pd, objectIdCol, _container, _user, true);
                    ((PropertyColumn)col).setParentIsObjectId(true);
                    copyProperties(col, dp);
                    if (!dp.isHidden())
                        addDefaultVisible(dp.getName());
                    addColumn(col);
                }

                if (col.getMvColumnName() != null)
                {
                    MVDisplayColumnFactory.addMvColumns(this, col, dp, objectIdCol, _container, _user);
                }
            }

            SQLFragment runSql = new SQLFragment("(SELECT d.RunID FROM exp.data d WHERE d.RowID = " + ExprColumn.STR_TABLE_ALIAS + ".DataID)");
            ExprColumn runColumn = new ExprColumn(this, "Run", runSql, JdbcType.INTEGER);
            runColumn.setFk(new LookupForeignKey("RowID")
            {
                public TableInfo getLookupTableInfo()
                {
                    ExpRunTable expRunTable = AssayService.get().createRunTable(getProtocol(), _provider, ViabilityAssaySchema.this.getUser(), ViabilityAssaySchema.this.getContainer());
                    expRunTable.setContainerFilter(getContainerFilter());
                    return expRunTable;
                }
            });
            addColumn(runColumn);

            ExprColumn specimenCount = new ExprColumn(this, "SpecimenCount",
                    new SQLFragment("(SELECT COUNT(RS.specimenid) FROM viability.resultspecimens RS WHERE " + ExprColumn.STR_TABLE_ALIAS + ".RowID = RS.ResultID)"), JdbcType.INTEGER);
            addVisible(specimenCount);

            ExprColumn specimenMatchCount = new SpecimenAggregateColumn(this, "SpecimenMatchCount", JdbcType.INTEGER);
            specimenMatchCount.setHidden(true);
            addColumn(specimenMatchCount);

            ExprColumn specimenMatches = new SpecimenAggregateColumn(this, "SpecimenMatches", JdbcType.VARCHAR);
            specimenMatches.setHidden(true);
            addColumn(specimenMatches);

            SQLFragment protocolIDFilter = new SQLFragment("ProtocolID = ?");
            protocolIDFilter.add(getProtocol().getRowId());
            addCondition(protocolIDFilter, FieldKey.fromParts("ProtocolID"));
        }

        @Override
        public Domain getDomain()
        {
            return _resultsDomain;
        }

        @Override
        protected ColumnInfo resolveColumn(String name)
        {
            ColumnInfo result = super.resolveColumn(name);

            if ("Properties".equalsIgnoreCase(name))
            {
                // Hook up a column that joins back to this table so that the columns formerly under the Properties
                // node can still be queried there.
                result = wrapColumn("Properties", getRealTable().getColumn("RowId"));
                result.setIsUnselectable(true);
                LookupForeignKey fk = new LookupForeignKey("RowId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return new ResultsTable();
                    }
                };
                fk.setPrefixColumnCaption(false);
                result.setFk(fk);
            }

            return result;
        }

        public class SpecimenAggregateColumn extends ExprColumn
        {
            public SpecimenAggregateColumn(TableInfo parent, String name, SQLFragment frag, JdbcType type, ColumnInfo... dependentColumns)
            {
                super(parent, name, frag, type, dependentColumns);
            }

            public SpecimenAggregateColumn(TableInfo parent, String name, JdbcType type, ColumnInfo... dependentColumns)
            {
                super(parent, name, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + "$z." + name), type, dependentColumns);
            }

            @Override
            public void declareJoins(String parentAlias, Map<String, SQLFragment> map)
            {
                ResultSpecimensTable rs = new ResultSpecimensTable();
                // 9024: propogate container filter
                rs.setContainerFilter(getContainerFilter());
                List<FieldKey> fields = new ArrayList<>();
                FieldKey resultId = FieldKey.fromParts("ResultID");
                FieldKey volume = FieldKey.fromParts("SpecimenID", "Volume");
                FieldKey globalUniqueId = FieldKey.fromParts("SpecimenID", "GlobalUniqueId");
                fields.add(resultId);
                fields.add(FieldKey.fromParts("SpecimenID"));
                fields.add(volume);
                fields.add(globalUniqueId);

                // TargetStudy could be on the Results, Run, or Batch table.
                fields.add(FieldKey.fromParts("ResultID", "TargetStudy"));
                fields.add(FieldKey.fromParts("ResultID", "Run", "TargetStudy"));
                fields.add(FieldKey.fromParts("ResultID", "Run", "Batch", "TargetStudy"));

                Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(rs, fields);

                SQLFragment sub = QueryService.get().getSelectSQL(rs, columnMap.values(), null, null, Table.ALL_ROWS, Table.NO_OFFSET, false);
                SQLFragment groupFrag = new SQLFragment();
                groupFrag.append("SELECT\n");
                groupFrag.append("  " + columnMap.get(resultId).getAlias() + " as VolumeResultID,\n");
                groupFrag.append("  SUM(" + columnMap.get(volume).getAlias() + ") as OriginalCells,\n");
                groupFrag.append("  COUNT(" + columnMap.get(globalUniqueId).getAlias() + ") as SpecimenMatchCount,\n");

                if (getDbSchema().getSqlDialect().supportsGroupConcat())
                {
                    SQLFragment guid = new SQLFragment(columnMap.get(globalUniqueId).getAlias());
                    SQLFragment specimenMatches = getDbSchema().getSqlDialect().getGroupConcat(guid, true, true);
                    groupFrag.append("  ").append(specimenMatches).append(" as SpecimenMatches\n");
                }
                else
                {
                    groupFrag.append(" NULL as SpecimenMatches\n");
                }

                groupFrag.append("FROM (\n");
                groupFrag.append(sub);
                groupFrag.append(") y \nGROUP BY " + columnMap.get(resultId).getAlias());

                SQLFragment frag = new SQLFragment();
                frag.append("\nLEFT OUTER JOIN (\n");
                frag.append(groupFrag);
                String name = parentAlias + "$z";
                frag.append(") AS ").append(name).append(" ON ").append(parentAlias).append(".RowId = ").append(name).append(".VolumeResultID");

                map.put(name, frag);
            }
        }
    }

    public class MissingSpecimenPopupFactory implements DisplayColumnFactory
    {
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            final FieldKey specimenIDs = FieldKey.fromParts("SpecimenIDs");
            final FieldKey specimenMatches = FieldKey.fromParts("SpecimenMatches");
            final FieldKey specimenCount = FieldKey.fromParts("SpecimenCount");
            final FieldKey specimenMatchCount = FieldKey.fromParts("SpecimenMatchCount");

            return new DataColumn(colInfo)
            {
                @Override
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    super.renderGridCellContents(ctx, out);

                    String popupText = "Some specimen IDs were not found in the target study.";

                    Number count = (Number)ctx.get(specimenCount);
                    Number matchCount = (Number)ctx.get(specimenMatchCount);

                    if (count != null && count.intValue() > 0 && !count.equals(matchCount))
                    {
                        String id = (String)ctx.get(specimenIDs);
                        String match = (String)ctx.get(specimenMatches);

                        // XXX: SpecimenMatches column isn't supported on SQLServer yet
                        if (ViabilityAssaySchema.this.getSqlDialect().isPostgreSQL())
                        {
                            String[] ids = id != null ? id.split(",") : new String[0];
                            String[] matches = match != null ? match.split(",") : new String[0];

                            HashSet<String> s = new LinkedHashSet<>(Arrays.asList(ids));
                            s.removeAll(Arrays.asList(matches));

                            popupText += "<p>" + PageFlowUtil.filter(StringUtils.join(s, ", ")) + "</p>";
                        }

                        String imgHtml = "<img align=\"top\" src=\"" +
                                HttpView.currentContext().getContextPath() +
                                "/_images/mv_indicator.gif\" class=\"labkey-mv-indicator\">";

                        out.write(PageFlowUtil.helpPopup("Matched Specimen IDs", popupText, true, imgHtml, 0));
                    }
                }

                @Override
                public void addQueryFieldKeys(Set<FieldKey> keys)
                {
                    super.addQueryFieldKeys(keys);
                    keys.add(specimenIDs);
                    keys.add(specimenMatches);
                    keys.add(specimenCount);
                    keys.add(specimenMatchCount);
                }
            };
        }
    }

    public class ResultSpecimensTable extends ViabilityAssayTable
    {
        public ResultSpecimensTable()
        {
            super(ViabilitySchema.getTableInfoResultSpecimens());

            ColumnInfo resultIDCol = addVisible(wrapColumn(getRealTable().getColumn("ResultID")));
            resultIDCol.setLabel("Result");
            resultIDCol.setKeyField(true);
            resultIDCol.setFk(new LookupForeignKey("RowID")
            {
                public TableInfo getLookupTableInfo()
                {
                    ResultsTable results = new ResultsTable();
                    results.setContainerFilter(getContainerFilter());
                    return results;
                }
            });

            ColumnInfo specimenID = addVisible(wrapColumn(getRealTable().getColumn("SpecimenID")));
            specimenID.setLabel("Specimen");
            specimenID.setKeyField(true);
            AssayTableMetadata metadata = new ViabilityAssayProvider.ResultsSpecimensAssayTableMetadata(_provider, getProtocol());
//            SpecimenForeignKey fk = new SpecimenForeignKey(ViabilityAssaySchema.this, _provider, getProtocol(), metadata);
            SpecimenForeignKey fk = new SpecimenForeignKey(ViabilityAssaySchema.this, this, metadata);
            SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("volumeunits"), "CEL");
            fk.addSpecimenFilter(filter);
            specimenID.setFk(fk);

            ColumnInfo indexCol = addVisible(wrapColumn(getRealTable().getColumn("SpecimenIndex")));
            indexCol.setKeyField(true);
        }

        @Override
        protected void applyContainerFilter(ContainerFilter containerFilter)
        {
            FieldKey resultIdFieldKey = FieldKey.fromParts("ResultID");
            getFilter().deleteConditions(resultIdFieldKey);

            SQLFragment filter = new SQLFragment(
                    "ResultID IN (" +
                        "SELECT result.RowId FROM " +
                            ViabilitySchema.getTableInfoResults() + " result " +
                        "WHERE result.ProtocolID = ? AND ");
            filter.add(getProtocol().getRowId());

            filter.append(containerFilter.getSQLFragment(getSchema(), "result.Container", getContainer()));

            filter.append(")");
            addCondition(filter, resultIdFieldKey);
        }
    }

    @Nullable
    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new ViabilityRunListQueryView(this, settings);
    }

    private class ViabilityRunListQueryView extends RunListQueryView
    {
        public ViabilityRunListQueryView(ViabilityAssaySchema schema, QuerySettings settings)
        {
            super(schema, settings);
        }

        @Override
        public List<DisplayColumn> getDisplayColumns()
        {
            ActionURL reRunURL = getProvider().getImportURL(getContainer(), _schema.getProtocol());
            reRunURL.addParameter("reRunId", "${RowId}");

            DisplayColumn reRunDisplayCol = new UrlColumn(StringExpressionFactory.createURL(reRunURL), "rerun");
            reRunDisplayCol.setNoWrap(true);

            List<DisplayColumn> cols = super.getDisplayColumns();
            cols.add(0, reRunDisplayCol);
            return cols;
        }
    }

}
