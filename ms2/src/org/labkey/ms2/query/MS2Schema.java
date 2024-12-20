/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.ms2.query;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.action.BaseViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.AggregateColumnInfo;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.CrosstabDimension;
import org.labkey.api.data.CrosstabMeasure;
import org.labkey.api.data.CrosstabMember;
import org.labkey.api.data.CrosstabSettings;
import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.CrosstabTableInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.FilterInfo;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.IconDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TempTableTracker;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.module.Module;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.protein.query.SequencesTableInfo;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.CustomViewInfo;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.StringExpression;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Fraction;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.PeptideManager;
import org.labkey.ms2.ProteinGroupProteins;
import org.labkey.ms2.RunListCache;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.protein.ProteinViewBean;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class MS2Schema extends UserSchema
{
    public static final String SCHEMA_NAME = "ms2";
    public static final SchemaKey SCHEMA_KEY = SchemaKey.fromParts("ms2");
    public static final String SCHEMA_DESCR = "Contains data about MS2 runs, including detected peptides and proteins";

    private static final String PROTOCOL_PATTERN_PREFIX = "urn:lsid:%:Protocol.%:";

    public static final String MASCOT_PROTOCOL_OBJECT_PREFIX = "MS2.Mascot";
    public static final String COMET_PROTOCOL_OBJECT_PREFIX = "MS2.Comet";
    public static final String SEQUEST_PROTOCOL_OBJECT_PREFIX = "MS2.Sequest";
    public static final String FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX = "MS2.FractionRollup";
    public static final String XTANDEM_PROTOCOL_OBJECT_PREFIX = "MS2.XTandem";
    public static final String IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX = "MS2.ImportedSearch";
    public static final String SAMPLE_PREP_PROTOCOL_OBJECT_PREFIX = "MS2.PreSearch.";

    private final ProteinGroupProteins _proteinGroupProteins = new ProteinGroupProteins();
    private List<MS2Run> _runs;

    private static final Set<String> TABLE_NAMES;

    static
    {
        Set<String> names = new HashSet<>();
        for (TableType tableType : TableType.values())
        {
            names.add(tableType.toString());
        }
        for (MS2RunType runType : MS2RunType.values())
        {
            if (!runType.isPeptideTableHidden())
            {
                names.add(runType.getPeptideTableName());
            }
        }
        TABLE_NAMES = Collections.unmodifiableSet(names);
    }

    public static void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                // Publish schema if any ProteomicsModule is active in the container
                for (Module m : schema.getContainer().getActiveModules(schema.getUser()))
                {
                    if (m instanceof ProteomicsModule)
                        return true;
                }
                return false;
            }

            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new MS2Schema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public MS2Schema(User user, Container container)
    {
        super(SCHEMA_KEY, SCHEMA_DESCR, user, container, ExperimentService.get().getSchema(), null);
    }

    public enum TableType
    {
        ImportedSearchRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable searchTable = ms2Schema.createSearchTable(ImportedSearchRuns.toString(), cf, IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX);
                searchTable.setDescription("Contains one row per externally-generated MS2 search result imported in this folder.");
                return searchTable;
            }
        },
        XTandemSearchRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable searchTable = ms2Schema.createSearchTable(XTandemSearchRuns.toString(), cf, XTANDEM_PROTOCOL_OBJECT_PREFIX);
                searchTable.setDescription("Contains one row per X!Tandem search result loaded in this folder.");
                return searchTable;
            }
        },
        MascotSearchRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable searchTable = ms2Schema.createSearchTable(MascotSearchRuns.toString(), cf, MASCOT_PROTOCOL_OBJECT_PREFIX);
                searchTable.setDescription("Contains one row per Mascot search results loaded in this folder.");
                return searchTable;
            }
        },
        CometSearchRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable searchTable = ms2Schema.createSearchTable(CometSearchRuns.toString(), cf, COMET_PROTOCOL_OBJECT_PREFIX);
                searchTable.setDescription("Contains one row per Comet search results loaded in this folder.");
                return searchTable;
            }
        },
        SequestSearchRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable searchTable = ms2Schema.createSearchTable(SequestSearchRuns.toString(), cf, SEQUEST_PROTOCOL_OBJECT_PREFIX);
                searchTable.setDescription("Contains one row per Sequest search result loaded in this folder.");
                return searchTable;
            }
        },
        FractionRollupsRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable searchTable = ms2Schema.createSearchTable(FractionRollupsRuns.toString(), cf, FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX);
                searchTable.setDescription("Contains one row per fraction rollup analysis result loaded in this folder.");
                return searchTable;
            }
        },
        MS2SearchRuns
        {
            @Override
            public ExpRunTable createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ExpRunTable runsTable = ms2Schema.createRunsTable(MS2SearchRuns.toString(), cf);
                runsTable.setDescription("Contains one row per MS2 search result, regardless of source, loaded in this folder.");
                return runsTable;
            }
        },
        MS2RunDetails
        {
            @Override
            public TableInfo createTable(MS2Schema ms2, ContainerFilter cf)
            {
                FilteredTable<?> result = new FilteredTable<>(MS2Manager.getTableInfoRuns(), ms2, cf);
                result.setName(MS2RunDetails.name());
                result.addWrapColumn(result.getRealTable().getColumn("Run"));
                result.addWrapColumn(result.getRealTable().getColumn("Description"));
                result.addWrapColumn(result.getRealTable().getColumn("Created"));
                result.addWrapColumn(result.getRealTable().getColumn("Path"));
                result.addWrapColumn(result.getRealTable().getColumn("SearchEngine"));
                result.addWrapColumn(result.getRealTable().getColumn("MassSpecType"));
                result.addWrapColumn(result.getRealTable().getColumn("PeptideCount"));

                // Add count columns for MS1-MS4 data
                result.addColumn(new ExprColumn(result, "MS1ScanCount", getScanCountSqlFragment(1), JdbcType.INTEGER));

                // SpectrumCount is an old column that was populated based on the number of spectra imported into the
                // ms2.spectradata table. Use its value if available, otherwise defer to the fractions' MS2ScanCount values
                SQLFragment ms2SQL = new SQLFragment("CASE WHEN SpectrumCount > 0 THEN SpectrumCount ELSE ");
                ms2SQL.append(getScanCountSqlFragment(2));
                ms2SQL.append(" END");
                result.addColumn(new ExprColumn(result, "MS2ScanCount", ms2SQL, JdbcType.INTEGER));
                result.addColumn(new ExprColumn(result, "SpectrumCount", ms2SQL, JdbcType.INTEGER)).setHidden(true);

                result.addColumn(new ExprColumn(result, "MS3ScanCount", getScanCountSqlFragment(3), JdbcType.INTEGER));
                result.addColumn(new ExprColumn(result, "MS4ScanCount", getScanCountSqlFragment(4), JdbcType.INTEGER));

                var fastaColumnInfo = result.wrapColumn("FastaId", result.getRealTable().getColumn("Run"));
                fastaColumnInfo.setKeyField(false);
                fastaColumnInfo.setFk(new MultiValuedForeignKey(QueryForeignKey.from(ms2,cf).to(FastaRunMapping.name(), "Run", null).build(), "FastaId"));
                fastaColumnInfo.setLabel("FASTA");
                result.addColumn(fastaColumnInfo);
                result.addWrapColumn(result.getRealTable().getColumn("SearchEnzyme"));
                result.addWrapColumn(result.getRealTable().getColumn("Filename"));
                result.addWrapColumn(result.getRealTable().getColumn("Status"));
                result.addWrapColumn(result.getRealTable().getColumn("StatusId")).setHidden(true);
                result.addWrapColumn(result.getRealTable().getColumn("Type"));
                result.addWrapColumn(result.getRealTable().getColumn("MascotFile"));
                result.addWrapColumn(result.getRealTable().getColumn("DistillerRawFile"));

                var iconColumn = result.wrapColumn("Links", result.getRealTable().getColumn("Run"));
                iconColumn.setDisplayColumnFactory(colInfo -> {
                    ActionURL linkURL = MS2Controller.getShowRunURL(ms2.getUser(), ms2.getContainer());
                    return new IconDisplayColumn(colInfo, 18, 18, linkURL, "run", AppProps.getInstance().getContextPath() + "/MS2/images/runIcon.gif");
                });
                result.addColumn(iconColumn);
                return result;
            }

            @NotNull
            public SQLFragment getScanCountSqlFragment(int msLevel)
            {
                SQLFragment sql = new SQLFragment("(SELECT SUM(MS");
                sql.appendValue(msLevel);
                sql.append("ScanCount) FROM ");
                sql.append(MS2Manager.getTableInfoFractions(), "f");
                sql.append(" WHERE f.Run = ");
                sql.append(ExprColumn.STR_TABLE_ALIAS);
                sql.append(".Run)");
                return sql;
            }
        },
        Peptides
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createPeptidesTable(cf, MS2RunType.values());
            }
        },
        Fractions
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createFractionsTable(cf);
            }
        },
        ProteinGroups
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                ProteinGroupTableInfo result = new ProteinGroupTableInfo(ms2Schema, cf);
                result.setRunFilter(null);
                return result;
            }
        },
        Sequences
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {

                return ms2Schema.createSequencesTable(cf);
            }
        },
        FastaRunMapping
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createFastaRunMappingTable(cf);
            }
        };

        public abstract TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf);
    }

    public enum HiddenTableType
    {
        PeptidesFilter
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                PeptidesTableInfo peptidesTable = (PeptidesTableInfo)ms2Schema.createPeptidesTable(cf, MS2RunType.values());
                peptidesTable.setName(this.toString());
                return peptidesTable;
            }
        },
        ProteinGroupsFilter
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createProteinGroupsForRunTable(cf, null);
            }
        },
        ProteinGroupsForSearch
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createProteinGroupsForSearchTable(cf);
            }
        },
        ProteinGroupsForRun
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createProteinGroupsForRunTable(cf, false);
            }
        },
        CompareProteinProphet
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createProteinProphetCompareTable(null, null);
            }
        },
        ComparePeptides
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createPeptidesCompareTable(false, null, null);
            }
        },
        ProteinProphetCrosstab
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createProteinProphetCrosstabTable(null, null);
            }
        },
        ProteinProphetNormalizedCrosstab
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createNormalizedProteinProphetComparisonTable(null, null);
            }
        },
        PeptideCrosstab
        {
            @Override
            public TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf)
            {
                return ms2Schema.createPeptideCrosstabTable(null, null);
            }
        } ;

        public abstract TableInfo createTable(MS2Schema ms2Schema, ContainerFilter cf);

    }

    @Override
    public Set<String> getTableNames()
    {
        return TABLE_NAMES;
    }

    public ProteinGroupProteins getProteinGroupProteins()
    {
        return _proteinGroupProteins;
    }

    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        for (TableType tableType : TableType.values())
        {
            if (tableType.toString().equalsIgnoreCase(name))
            {
                return tableType.createTable(this, cf);
            }
        }
        for (HiddenTableType tableType : HiddenTableType.values())
        {
            if (tableType.toString().equalsIgnoreCase(name))
            {
                return tableType.createTable(this, cf);
            }
        }
        for (MS2RunType runType : MS2RunType.values())
        {
            if (runType.getPeptideTableName().equalsIgnoreCase(name))
            {
                return createPeptidesTable(cf, runType);
            }
        }

        SpectraCountConfiguration config = SpectraCountConfiguration.findByTableName(name);
        if (config != null)
        {
            MS2Controller.SpectraCountForm form = null;
            ViewContext context = null;
            if (HttpView.hasCurrentView())
            {
                form = new MS2Controller.SpectraCountForm();
                context = HttpView.currentContext();
                BaseViewAction.defaultBindParameters(form, null, HttpView.getBindPropertyValues());

                _runs = Collections.emptyList();

                if (form.getRunList() != null)
                {
                    try
                    {
                        _runs = RunListCache.getCachedRuns(form.getRunList(), false, context);
                    }
                    catch (RunListException ignored) {}
                }
            }
            return createSpectraCountTable(config, context, form);
        }

        return null;
    }

    public ComparePeptideTableInfo createPeptidesCompareTable(boolean forExport, HttpServletRequest request, String peptideViewName)
    {
        return new ComparePeptideTableInfo(this, _runs, forExport, request, peptideViewName);
    }

    public CompareProteinProphetTableInfo createProteinProphetCompareTable(HttpServletRequest request, String peptideViewName)
    {
        return new CompareProteinProphetTableInfo(this, _runs, false, request, peptideViewName);
    }

    public ExpRunTable createRunsTable(String name, ContainerFilter filter)
    {
        return createSearchTable(name, filter, XTANDEM_PROTOCOL_OBJECT_PREFIX, MASCOT_PROTOCOL_OBJECT_PREFIX, COMET_PROTOCOL_OBJECT_PREFIX, SEQUEST_PROTOCOL_OBJECT_PREFIX , IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX, FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX);
    }

    public SpectraCountTableInfo createSpectraCountTable(SpectraCountConfiguration config, ViewContext context, MS2Controller.SpectraCountForm form)
    {
        return new SpectraCountTableInfo(this, config, context, form);
    }

    public ProteinGroupTableInfo createProteinGroupsForSearchTable(ContainerFilter cf)
    {
        ProteinGroupTableInfo result = new ProteinGroupTableInfo(this, cf);
        List<FieldKey> defaultColumns = new ArrayList<>(result.getDefaultVisibleColumns());
        defaultColumns.add(0, FieldKey.fromParts("ProteinProphet","Run"));
        defaultColumns.add(0, FieldKey.fromParts("ProteinProphet", "Run", "Folder"));
        result.setDefaultVisibleColumns(defaultColumns);
        return result;
    }

    public ProteinGroupTableInfo createProteinGroupsForRunTable(ContainerFilter cf, String alias)
    {
        return createProteinGroupsForRunTable(cf, true);
    }

    public ProteinGroupTableInfo createProteinGroupsForRunTable(ContainerFilter cf, boolean includeFirstProteinColumn)
    {
        ProteinGroupTableInfo result = new ProteinGroupTableInfo(this, cf, includeFirstProteinColumn);
        result.addProteinsColumn(cf);
        List<FieldKey> defaultColumns = new ArrayList<>(result.getDefaultVisibleColumns());
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein"));
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein", "BestGeneName"));
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein", "Mass"));
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein", "Description"));
        result.setDefaultVisibleColumns(defaultColumns);
        return result;
    }

    protected FilteredTable<?> createProteinGroupMembershipTable(final MS2Controller.PeptideFilteringComparisonForm form, final ViewContext context, boolean filterByRuns)
    {
        FilteredTable<?> result = new FilteredTable<>(MS2Manager.getTableInfoProteinGroupMemberships(), this);
        result.wrapAllColumns(true);

        result.getMutableColumn("ProteinGroupId").setFk(new LookupForeignKey(result.getContainerFilter(), "RowId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ProteinGroupTableInfo result = createProteinGroupsForRunTable(getLookupContainerFilter(), null);

                result.removeColumn(result.getColumn("Proteins"));
                result.removeColumn(result.getColumn("FirstProtein"));

                SQLFragment totalSQL;
                SQLFragment uniqueSQL;

                if (form != null && form.isPeptideProphetFilter() && form.getPeptideProphetProbability() != null)
                {
                    totalSQL = new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".RowId IS NULL THEN NULL ELSE (SELECT COUNT(pd.RowId) FROM " + MS2Manager.getTableInfoPeptidesData() + " pd, ");
                    totalSQL.append(MS2Manager.getTableInfoPeptideMemberships() + " pm WHERE pd.RowId = pm.PeptideId AND pd.PeptideProphet >= " + form.getPeptideProphetProbability() + " AND pm.ProteinGroupId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId) END)");

                    uniqueSQL = new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".RowId IS NULL THEN NULL ELSE (SELECT COUNT(DISTINCT TrimmedPeptide) FROM " + MS2Manager.getTableInfoPeptidesData() + " pd, ");
                    uniqueSQL.append(MS2Manager.getTableInfoPeptideMemberships() + " pm WHERE pd.RowId = pm.PeptideId AND pd.PeptideProphet >= " + form.getPeptideProphetProbability() + " AND pm.ProteinGroupId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId) END)");
                }
                else if (form != null && form.isCustomViewPeptideFilter())
                {
                    totalSQL = new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".RowId IS NULL THEN NULL ELSE (SELECT COUNT(p.RowId) FROM " + MS2Manager.getTableInfoPeptideMemberships() + " pm, ");
                    totalSQL.append("(");
                    totalSQL.append(getPeptideSelectSQL(context.getRequest(), form.getPeptideCustomViewName(context), Arrays.asList(FieldKey.fromParts("RowId"), FieldKey.fromParts("TrimmedPeptide")), null));
                    totalSQL.append(") p ");
                    totalSQL.append("WHERE p.RowId = pm.PeptideId AND pm.ProteinGroupId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId) END)");

                    uniqueSQL = new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".RowId IS NULL THEN NULL ELSE (SELECT COUNT(DISTINCT p.TrimmedPeptide) FROM " + MS2Manager.getTableInfoPeptideMemberships() + " pm, ");
                    uniqueSQL.append("(");
                    uniqueSQL.append(getPeptideSelectSQL(context.getRequest(), form.getPeptideCustomViewName(context), Arrays.asList(FieldKey.fromParts("RowId"), FieldKey.fromParts("TrimmedPeptide")), null));
                    uniqueSQL.append(") p ");
                    uniqueSQL.append("WHERE p.RowId = pm.PeptideId AND pm.ProteinGroupId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId) END)");
                }
                else
                {
                    totalSQL = new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".RowId IS NULL THEN NULL ELSE (SELECT COUNT(pd.RowId) FROM " + MS2Manager.getTableInfoPeptidesData() + " pd, ");
                    totalSQL.append(MS2Manager.getTableInfoPeptideMemberships() + " pm WHERE pd.RowId = pm.PeptideId AND pm.ProteinGroupId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId) END)");

                    uniqueSQL = new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".RowId IS NULL THEN NULL ELSE (SELECT COUNT(DISTINCT TrimmedPeptide) FROM " + MS2Manager.getTableInfoPeptidesData() + " pd, ");
                    uniqueSQL.append(MS2Manager.getTableInfoPeptideMemberships() + " pm WHERE pd.RowId = pm.PeptideId AND pm.ProteinGroupId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId) END)");
                }

                result.addColumn(new ExprColumn(result, "TotalFilteredPeptides", totalSQL, JdbcType.BIGINT));
                result.addColumn(new ExprColumn(result, "UniqueFilteredPeptides", uniqueSQL, JdbcType.BIGINT));

                return result;
            }
        });

        result.getMutableColumn("SeqId").setLabel("Protein");
        result.getMutableColumn("SeqId").setFk(createSequencesLookup());

        if (_runs != null && filterByRuns)
        {
            SQLFragment sql = new SQLFragment("ProteinGroupId IN (SELECT pg.RowId FROM ");
            sql.append(MS2Manager.getTableInfoProteinGroups() + " pg, " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf ");
            sql.append(" WHERE pg.ProteinProphetFileId = ppf.RowId AND ppf.Run IN ");
            appendRunInClause(sql);
            sql.append(")");
            result.addCondition(sql, FieldKey.fromParts("ProteinGroupId"));
        }

        if (form != null)
        {
            if (form.isProteinProphetFilter() && form.getProteinProphetProbability() != null)
            {
                SQLFragment sql = new SQLFragment("ProteinGroupID IN (SELECT pg.RowId FROM ");
                sql.append(MS2Manager.getTableInfoProteinGroups() + " pg ");
                sql.append("WHERE pg.GroupProbability >= ");
                sql.appendValue(form.getProteinProphetProbability());
                sql.append(")");
                result.addCondition(sql, FieldKey.fromParts("ProteinGroupId"));
            }
            else if (form.isCustomViewProteinGroupFilter())
            {
                SQLFragment sql = new SQLFragment("ProteinGroupID IN (");
                getProteinGroupSelectSQL(form, context, sql);
                sql.append(")");
                result.addCondition(sql, FieldKey.fromParts("ProteinGroupId"));
            }
        }

        var peptideMembershipsColumn = result.wrapColumn("PeptideMemberships", result.getRealTable().getColumn("ProteinGroupId"));
        peptideMembershipsColumn.setFk(new LookupForeignKey(result.getContainerFilter(), "ProteinGroupId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return createPeptideMembershipsTable(getLookupContainerFilter());
            }
        });
        result.addColumn(peptideMembershipsColumn);

        return result;
    }

    public void getProteinGroupSelectSQL(MS2Controller.PeptideFilteringComparisonForm form, ViewContext context, SQLFragment sql)
    {
        QueryDefinition queryDef = QueryService.get().createQueryDefForTable(this, HiddenTableType.ProteinGroupsFilter.toString());
        SimpleFilter filter = new SimpleFilter();
        CustomView view = queryDef.getCustomView(getUser(), context.getRequest(), form.getProteinGroupCustomViewName(context));
        if (view != null)
        {
            ActionURL url = new ActionURL();
            view.applyFilterAndSortToURL(url, "InternalName");
            filter.addUrlFilters(url, "InternalName");
        }
        ProteinGroupTableInfo tableInfo = new ProteinGroupTableInfo(this, null, false);
        tableInfo.setContainerFilter(ContainerFilter.EVERYTHING);
        sql.append(getSelectSQL(tableInfo, filter, Collections.singleton(FieldKey.fromParts("RowId"))));
    }

    public void appendRunInClause(SQLFragment sql)
    {
        sql.append("(");
        if (_runs == null || _runs.isEmpty())
        {
            sql.append("-1");
        }
        else
        {
            String separator = "";
            for (MS2Run run : _runs)
            {
                sql.append(separator);
                separator = ", ";
                sql.appendValue(run.getRun());
            }
        }
        sql.append(")");
    }

    protected TableInfo createPeptideMembershipsTable(ContainerFilter cf)
    {
        FilteredTable result = new FilteredTable<>(MS2Manager.getTableInfoPeptideMemberships(), this);
        result.wrapAllColumns(false);

        LookupForeignKey fk = new LookupForeignKey(cf, "RowId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ProteinGroupTableInfo result = new ProteinGroupTableInfo(MS2Schema.this, getLookupContainerFilter());
                result.getMutableColumn("ProteinProphet").setHidden(true);
                result.addProteinDetailColumns();

                return result;
            }
        };
        fk.setPrefixColumnCaption(false);
        result.getMutableColumn("ProteinGroupId").setFk(fk);

        result.getMutableColumn("PeptideId").setHidden(true);
        result.getMutableColumn("PeptideId").setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return createPeptidesTable(ContainerFilter.EVERYTHING, MS2RunType.values());
            }
        });

        return result;
    }

    protected TableInfo createFractionsTable(ContainerFilter cf)
    {
        SqlDialect dialect = MS2Manager.getSqlDialect();
        FilteredTable<?> result = new FilteredTable<>(MS2Manager.getTableInfoFractions(), this, cf)
        {
            @Override
            protected void applyContainerFilter(ContainerFilter filter)
            {
                FieldKey containerFieldKey = FieldKey.fromParts("Container");
                clearConditions(containerFieldKey);
                SQLFragment sql = new SQLFragment("Run IN (SELECT r.Run FROM ");
                sql.append(MS2Manager.getTableInfoRuns(), "r");
                sql.append(" WHERE ");
                sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("r.Container")));
                sql.append(")");
                addCondition(sql, containerFieldKey);
            }
    };
        result.wrapAllColumns(true);
        result.setTitleColumn("FileName");

        SQLFragment notDeletedSQL = new SQLFragment("Run IN (SELECT r.Run FROM ");
        notDeletedSQL.append(MS2Manager.getTableInfoRuns(), "r");
        notDeletedSQL.append(" WHERE r.Deleted = ?)");
        notDeletedSQL.add(false);
        result.addCondition(notDeletedSQL, FieldKey.fromParts("Run"));

        SQLFragment fractionNameSQL = new SQLFragment("CASE")
                .append(" WHEN ").append(dialect.getStringIndexOfFunction(new SQLFragment("'.'"), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FileName"))).append(" > 0")
                .append(" THEN ").append(dialect.getSubstringFunction(new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FileName"), new SQLFragment("1"), dialect.getStringIndexOfFunction(new SQLFragment("'.'"), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FileName")).append("- 1")))
                .append(" ELSE " + ExprColumn.STR_TABLE_ALIAS + ".FileName END");

        var fractionName = new ExprColumn(result, "FractionName", fractionNameSQL, JdbcType.VARCHAR);
        fractionName.setLabel("Name");
        fractionName.setWidth("200");
        result.addColumn(fractionName);

        // Add a column that links directly to the data object
        ExprColumn dataColumn = new ExprColumn(result, "Data",
                new SQLFragment("(SELECT MIN(d.RowId) FROM " + ExperimentService.get().getTinfoData() + " d, " +
                        MS2Manager.getTableInfoRuns() + " r WHERE d.Container = r.Container AND r.Run = " +
                        ExprColumn.STR_TABLE_ALIAS + ".Run AND d.DataFileURL = " + ExprColumn.STR_TABLE_ALIAS + ".mzxmlURL)"), JdbcType.INTEGER);
        dataColumn.setFk(new ExpSchema(getUser(), getContainer()).getDataIdForeignKey(cf));
        result.addColumn(dataColumn);

        ActionURL url = MS2Controller.getShowRunURL(getUser(), getContainer());
        result.getMutableColumn("Run").setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return new RunTableInfo(MS2Schema.this, cf);
            }
        });

        return result;
    }

    // Customizes the SequencesTableInfo, adding "BestName" column and ShowProteinAction details URL
    public static Consumer<SequencesTableInfo<?>> STANDARD_MODIFIER = (SequencesTableInfo<?> tableInfo) ->
    {
        ActionURL url = new ActionURL(MS2Controller.ShowProteinAction.class, tableInfo.getContainer());
        url.addParameter("seqId", "${SeqId}");
        var bnColumn = tableInfo.getMutableColumn("BestName");
        bnColumn.setURL(StringExpressionFactory.createURL(url));
        bnColumn.setURLTargetWindow("prot");
        tableInfo.setDetailsURL(new DetailsURL(url));
    };

    public SequencesTableInfo<MS2Schema> createSequencesTable(ContainerFilter cf)
    {
        return new SequencesTableInfo<>(this, cf);
    }

    private TableInfo createFastaRunMappingTable(ContainerFilter cf)
    {
        return new FastaRunMappingTable(this, cf);
    }

    public TableInfo createPeptidesTable(ContainerFilter containerFilter, MS2RunType... runTypes)
    {
        return new PeptidesTableInfo(this, containerFilter, runTypes);
    }

    private ExpRunTable createSearchTable(String name, ContainerFilter filter, String... protocolObjectPrefix)
    {
        final ExpRunTable result = ExperimentService.get().createRunTable(name, this, filter);
        result.populate();
        String[] protocolPatterns = new String[protocolObjectPrefix.length];
        for (int i = 0; i < protocolObjectPrefix.length; i++)
        {
            protocolPatterns[i] = PROTOCOL_PATTERN_PREFIX + protocolObjectPrefix[i] + "%";
        }
        result.setProtocolPatterns(protocolPatterns);

        SQLFragment sql = new SQLFragment("(SELECT MIN(ms2Runs.run)\n" +
                "\nFROM " + MS2Manager.getTableInfoRuns() + " ms2Runs " +
                "\nWHERE ms2Runs.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID AND ms2Runs.Deleted = ?)");
        sql.add(Boolean.FALSE);
        var ms2DetailsColumn = new ExprColumn(result, "MS2Details", sql, JdbcType.INTEGER);
        ActionURL url = MS2Controller.getShowRunURL(getUser(), getContainer());
        DetailsURL detailsURL = new DetailsURL(url, Collections.singletonMap("Run", ms2DetailsColumn.getFieldKey()));
        ms2DetailsColumn.setURL(detailsURL);
        ms2DetailsColumn.setFk( QueryForeignKey.from(this,filter).to(TableType.MS2RunDetails.name(), "Run", "Description" ) );
        result.addColumn(ms2DetailsColumn);

        result.getMutableColumn(ExpRunTable.Column.Name).setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DataColumn(colInfo)
                {
                    private ColumnInfo _runCol;

                    @Override
                    public String renderURL(RenderContext ctx)
                    {
                        if (_runCol != null && _runCol.getValue(ctx) != null)
                        {
                            // In rare cases we'll have something that qualifies as an MS2 run
                            // based on its protocol LSID but that doesn't actually have a MS2 run
                            // attached to it in the database, so don't show links to it
                            return super.renderURL(ctx);
                        }
                        else
                        {
                            return null;
                        }
                    }

                    @Override
                    public void addQueryColumns(Set<ColumnInfo> columns)
                    {
                        super.addQueryColumns(columns);
                        FieldKey key = new FieldKey(FieldKey.fromString(getBoundColumn().getName()).getParent(),  "MS2Details");
                        Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(getBoundColumn().getParentTable(), Collections.singleton(key));
                        _runCol = cols.get(key);
                        if (_runCol != null)
                        {
                            columns.add(_runCol);
                            ActionURL url = MS2Controller.getShowRunURL(getUser(), getContainer());
                            setURLExpression(new DetailsURL(url, Collections.singletonMap("run", _runCol.getFieldKey())));
                        }
                    }
                };
            }
        });

        ms2DetailsColumn.setHidden(false);

        //adjust the default visible columns
        List<FieldKey> columns = new ArrayList<>(result.getDefaultVisibleColumns());
        columns.remove(FieldKey.fromParts("MS2Details"));
        columns.remove(FieldKey.fromParts("Protocol"));
        columns.remove(FieldKey.fromParts("CreatedBy"));

        columns.add(2, FieldKey.fromParts("MS2Details", "Links"));
        columns.add(FieldKey.fromParts("MS2Details", "FASTAId", "ShortName"));

        result.setDefaultVisibleColumns(columns);
        return result;
    }

    public void setRuns(MS2Run[] runs)
    {
        setRuns(Arrays.asList(runs));
    }

    public void setRuns(List<MS2Run> runs)
    {
        _runs = runs;
        _proteinGroupProteins.setRuns(_runs);
        _runs.sort((run1, run2) ->
        {
            if (run1.getDescription() == null && run2.getDescription() == null)
            {
                return 0;
            }
            if (run1.getDescription() == null)
            {
                return 1;
            }
            if (run2.getDescription() == null)
            {
                return -1;
            }
            return run1.getDescription().compareTo(run2.getDescription());
        });
    }

    public List<MS2Run> getRuns()
    {
        return _runs;
    }

    protected SQLFragment getPeptideSelectSQL(SimpleFilter filter, Collection<FieldKey> fieldKeys)
    {
        TableInfo tiFiltered = getTable(HiddenTableType.PeptidesFilter.name(), ContainerFilter.EVERYTHING, true, false);
        return getSelectSQL(tiFiltered, filter, fieldKeys);
    }

    protected SQLFragment getSelectSQL(TableInfo tableInfo, SimpleFilter filter, Collection<FieldKey> fieldKeys)
    {
        Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(tableInfo, fieldKeys);

        Collection<ColumnInfo> reqCols = new ArrayList<>(columnMap.values());
        Set<FieldKey> unresolvedColumns = new HashSet<>();
        reqCols = QueryService.get().ensureRequiredColumns(tableInfo, reqCols, filter, null, unresolvedColumns);

        SQLFragment innerSelect = Table.getSelectSQL(tableInfo, reqCols, null, null);

        Map<FieldKey, ColumnInfo> map = new HashMap<>(reqCols.size());
        for(ColumnInfo col : reqCols)
        {
            map.put(col.getFieldKey(), col);
        }

        SQLFragment sql = new SQLFragment();
        sql.append("SELECT ");
        String separator = "";
        for (FieldKey fieldKey : fieldKeys)
        {
            sql.append(separator);
            separator = ", ";
            sql.append(columnMap.get(fieldKey).getAlias());
        }
        sql.append(" FROM (\n");
        sql.append(innerSelect);
        sql.append("\n) AS InnerSubquery ");

        sql.append(filter.getSQLFragment(getDbSchema().getSqlDialect(), "InnerSubquery", map));
        return sql;
    }

    public SQLFragment getPeptideSelectSQL(HttpServletRequest request, String viewName, Collection<FieldKey> fieldKeys, List<Integer> targetSeqIds)
    {
        QueryDefinition queryDef = QueryService.get().createQueryDefForTable(this, HiddenTableType.PeptidesFilter.toString());
        SimpleFilter filter = new SimpleFilter();

        if (targetSeqIds != null && targetSeqIds.size() > 0)
        {
            filter.addCondition(PeptideManager.getSequencesFilter(targetSeqIds));
        }
        CustomView view = queryDef.getCustomView(getUser(), request, viewName);
        if (view != null)
        {
            ActionURL url = new ActionURL();
            view.applyFilterAndSortToURL(url, "InternalName");
            filter.addUrlFilters(url, "InternalName");
        }
        return getPeptideSelectSQL(filter, fieldKeys);
    }

    public CrosstabTableInfo createNormalizedProteinProphetComparisonTable(final MS2Controller.PeptideFilteringComparisonForm form, final ViewContext context)
    {
        VirtualTable rawTable;

        var normalizedIdCol = new BaseColumnInfo("NormalizedId", JdbcType.INTEGER);
        normalizedIdCol.setHidden(true);

        var proteinGroupIdCol = new BaseColumnInfo("ProteinGroupId", JdbcType.INTEGER);
        proteinGroupIdCol.setFk(new LookupForeignKey((ContainerFilter)null, "RowId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return new ProteinGroupTableInfo(MS2Schema.this, getLookupContainerFilter(), true);
            }

            @Override
            public StringExpression getURL(ColumnInfo parent)
            {
                return getURL(parent, true);
            }
        });

        final String name;

        if (form != null && form.getRunList() != null)
        {
            try
            {
                name = ensureNormalizedProteinGroups(form.getRunList().intValue());
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        else
        {
            name = "bogusTable";
        }

        rawTable = new VirtualTable(getDbSchema(), name, this)
        {
            @NotNull
            @Override
            public SQLFragment getFromSQL()
            {
                return new SQLFragment("SELECT * FROM " + name);
            }
        };

        rawTable.addColumn(normalizedIdCol);
        normalizedIdCol.setParentTable(rawTable);
        rawTable.addColumn(proteinGroupIdCol);
        proteinGroupIdCol.setParentTable(rawTable);

        FilteredTable baseTable = new FilteredTable<>(rawTable, this, null);
        baseTable.wrapAllColumns(true);
        var peptideMembershipsColumn = baseTable.wrapColumn("PeptideMemberships", rawTable.getColumn("ProteinGroupId"));
        peptideMembershipsColumn.setFk(new LookupForeignKey((ContainerFilter)null, "ProteinGroupId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return createPeptideMembershipsTable(getLookupContainerFilter());
            }
        });
        baseTable.addColumn(peptideMembershipsColumn);

        SimpleFilter filter = new SimpleFilter();
        addPeptideFilters(form, context, baseTable, filter);

        ExprColumn normalizedProteinCountCol = new ExprColumn(baseTable, "ProteinCount", new SQLFragment("(SELECT COUNT (DISTINCT SeqId) FROM " + name + " n,  " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm WHERE n.ProteinGroupId = pgm.ProteinGroupId and n.NormalizedId = " + ExprColumn.STR_TABLE_ALIAS + ".NormalizedId)"), JdbcType.INTEGER);
        baseTable.addColumn(normalizedProteinCountCol);

        var proteinsCol = baseTable.wrapColumn("Proteins", rawTable.getColumn("NormalizedId"));
        proteinsCol.setHidden(false);
        baseTable.addColumn(proteinsCol);
        LookupForeignKey proteinsFK = new LookupForeignKey("NormalizedId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                // Create a junction query that connects normalized group ids with protein identifications
                VirtualTable result = new VirtualTable(getDbSchema(), "InnerTable", MS2Schema.this)
                {
                    @NotNull
                    @Override
                    public SQLFragment getFromSQL()
                    {
                        return new SQLFragment("SELECT DISTINCT NormalizedId, SeqId FROM " + name + " n, " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm WHERE n.ProteinGroupId = pgm.ProteinGroupId");
                    }
                };
                var normalizedIdCol = new BaseColumnInfo("NormalizedId", result, JdbcType.INTEGER);
                result.addColumn(normalizedIdCol);
                var seqIdCol = new BaseColumnInfo("SeqId", result, JdbcType.INTEGER);
                LookupForeignKey seqFK = new LookupForeignKey("SeqId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return MS2Schema.this.createSequencesTable(getLookupContainerFilter());
                    }
                };
                seqFK.setPrefixColumnCaption(false);
                seqIdCol.setFk(seqFK);
                result.addColumn(seqIdCol);

                return result;
            }

            @Override
            public StringExpression getURL(ColumnInfo parent)
            {
                return super.getURL(parent, true);
            }
        };
        proteinsCol.setFk(new MultiValuedForeignKey(proteinsFK, "SeqId"));

        TableInfo proteinGroupMembershipTable = createProteinGroupMembershipTable(form, context, false);
        ColumnInfo proteinGroupColumn = proteinGroupMembershipTable.getColumn("ProteinGroupId");

        SQLFragment selectSQL = QueryService.get().getSelectSQL(proteinGroupMembershipTable, Collections.singleton(proteinGroupColumn), null, null, Table.ALL_ROWS, Table.NO_OFFSET, false);
        SQLFragment filterSQL = new SQLFragment("ProteinGroupId IN (SELECT " + proteinGroupColumn.getAlias() + " FROM (");
        filterSQL.append(selectSQL);
        filterSQL.append(") x)");

        baseTable.addCondition(filterSQL, FieldKey.fromParts("ProteinGroupId"));

        CrosstabTable result;
        CrosstabSettings settings = new CrosstabSettings(baseTable);
        CrosstabMeasure firstProteinGroupMeasure = settings.addMeasure(proteinGroupIdCol.getFieldKey(), CrosstabMeasure.AggregateFunction.MIN, "Run First Protein Group");
        CrosstabMeasure groupCountMeasure = settings.addMeasure(proteinGroupIdCol.getFieldKey(), CrosstabMeasure.AggregateFunction.COUNT, "Run Protein Group Count");

        settings.getRowAxis().setCaption("Normalized Protein Group");

        CrosstabDimension colDim;
        if (form != null && form.getPivotTypeEnum() == MS2Controller.PivotType.fraction)
        {
            settings.setInstanceCountCaption("Num Fractions");
            settings.getColumnAxis().setCaption("Fractions");
            colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts("PeptideMemberships", "PeptideId", "Fraction"));
            colDim.setUrl(MS2Controller.getShowRunURL(getUser(), getContainer())
                .addParameter("fraction", CrosstabMember.VALUE_TOKEN)
                .addParameter(MS2Manager.getDataRegionNamePeptides() + ".Fraction~eq", CrosstabMember.VALUE_TOKEN));
        }
        else
        {
            settings.setInstanceCountCaption("Found In Runs");
            settings.getColumnAxis().setCaption("Runs");
            colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts("ProteinGroupId", "ProteinProphetFileId", "Run"));
            colDim.setUrl(MS2Controller.getShowRunURL(getUser(), getContainer())
                .addParameter("run", CrosstabMember.VALUE_TOKEN));
        }

        settings.getRowAxis().addDimension(normalizedIdCol.getFieldKey());
        settings.getRowAxis().addDimension(normalizedProteinCountCol.getFieldKey());
        settings.getRowAxis().addDimension(proteinsCol.getFieldKey());

        settings.setSourceTableFilter(filter);

        if(null != _runs)
        {
            ArrayList<CrosstabMember> members = new ArrayList<>();
            //build up the list of column members
            for (MS2Run run : _runs)
            {
                if (form != null && form.getPivotTypeEnum() == MS2Controller.PivotType.fraction)
                {
                    for (MS2Fraction fraction : run.getFractions())
                    {
                        members.add(new CrosstabMember(fraction.getFraction(), colDim.getFieldKey(), fraction.getFileName()));
                    }
                }
                else
                {
                    members.add(new CrosstabMember(run.getRun(), colDim.getFieldKey(), run.getDescription()));
                }
            }
            result = new CrosstabTable(settings, members);
        }
        else
        {
            result = new CrosstabTable(settings);
        }
        if (form != null)
        {
            result.setOrAggFitlers(form.isOrCriteriaForEachRun());
        }
        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts(CrosstabTable.COL_INSTANCE_COUNT));
        defaultCols.add(FieldKey.fromParts(AggregateColumnInfo.getColumnName(null, groupCountMeasure)));
        defaultCols.add(FieldKey.fromParts(AggregateColumnInfo.getColumnName(null, firstProteinGroupMeasure), "Group"));
        defaultCols.add(FieldKey.fromParts(AggregateColumnInfo.getColumnName(null, firstProteinGroupMeasure), "GroupProbability"));
        defaultCols.add(FieldKey.fromParts(proteinsCol.getName(), "BestName"));
        result.setDefaultVisibleColumns(defaultCols);
        return result;
    }

    private static class NormalizedProteinGroupsTracker
    {
        private String _name;

        public NormalizedProteinGroupsTracker(String name)
        {
            _name = name;
        }

        public String getName()
        {
            return _name;
        }
    }

    private final static Map<Integer, NormalizedProteinGroupsTracker> NORMALIZED_PROTEIN_GROUP_CACHE = new HashMap<>();

    private String ensureNormalizedProteinGroups(int runListId) throws SQLException
    {
        synchronized (NORMALIZED_PROTEIN_GROUP_CACHE)
        {
            if (NORMALIZED_PROTEIN_GROUP_CACHE.containsKey(runListId))
            {
                // See if we've already built a temp table
                return NORMALIZED_PROTEIN_GROUP_CACHE.get(runListId).getName();
            }

            DbScope scope = MS2Manager.getSchema().getScope();

            try (Connection connection = scope.getConnection(); var ignore = SpringActionController.ignoreSqlUpdates())
            {
                String shortName = "RunList" + runListId;
                String tempTableName = getDbSchema().getSqlDialect().getGlobalTempTablePrefix() + shortName;
                NormalizedProteinGroupsTracker tracker = new NormalizedProteinGroupsTracker(tempTableName);
                TempTableTracker.track(shortName, tracker);
                // Working with a temp table, so use the same connection for all inserts/updates
                SqlExecutor executor = new SqlExecutor(scope, connection);

                // Populate the temp table with all of the protein groups from the selected runs
                SQLFragment insertSQL = new SQLFragment("SELECT x.RowId AS ProteinGroupId, x.RowId as NormalizedId INTO " + tempTableName +
                        " FROM (SELECT pg.RowId FROM " + MS2Manager.getTableInfoProteinGroups() + " pg, " +
                        MS2Manager.getTableInfoProteinProphetFiles() + " ppf WHERE pg.ProteinProphetFileId = ppf.RowId AND ppf.Run IN (");
                String separator = "";
                for (MS2Run run : getRuns())
                {
                    insertSQL.append(separator);
                    separator = ", ";
                    insertSQL.appendValue(run.getRun());
                }
                insertSQL.append(") ) AS x");

                executor.execute(insertSQL);

                // Create indices on the two columns
                executor.execute("CREATE INDEX IDX_" + shortName + "_NormalizedId ON " + tempTableName + "(NormalizedId)");
                executor.execute("CREATE INDEX IDX_" + shortName + "_ProteinGroupId ON " + tempTableName + "(ProteinGroupId)");

                // Figure out the minimum group id that contains a protein (SeqId) that's also in this group
                String updateSubQuery = "SELECT MIN(MinNormalizedId) AS NewNormalizedId, GroupId FROM \n" +
                        "(SELECT CASE WHEN g1.NormalizedId < g2.NormalizedId THEN g1.NormalizedId ELSE g2.NormalizedId END AS MinNormalizedId, g1.ProteinGroupId AS GroupId FROM " +
                        tempTableName + " g1, " + tempTableName + " g2, " + MS2Manager.getTableInfoProteinGroupMemberships() + " pg1, " +
                        MS2Manager.getTableInfoProteinGroupMemberships() + " pg2 WHERE pg1.SeqId = pg2.SeqId AND " +
                        "g2.ProteinGroupId = pg1.ProteinGroupId AND g1.ProteinGroupId = pg2.ProteinGroupId) Innermost GROUP BY GroupId)";

                String updateSQL = "UPDATE " + tempTableName + " SET NormalizedId = \n" +
                        "(SELECT NewNormalizedId FROM (" + updateSubQuery + " x \n" +
                        "WHERE GroupId = ProteinGroupId) WHERE NormalizedId != (SELECT NewNormalizedId FROM (" + updateSubQuery + " x WHERE GroupId = ProteinGroupId)";

                int rowsUpdated;
                do
                {
                    // Set the normalized group id to be the minimum id from all the groups that share the same proteins
                    rowsUpdated = executor.execute(updateSQL);
                }
                // Keep going while any value changed. When we're done, we've found the transitive closure and any
                // groups that share proteins (including transitively) are lumped into the same normalized group
                while (rowsUpdated > 0);

                // We don't have a good way of knowing when the temp table can be cleaned up. Since users
                // may sort/filter/export the report that's backed by the temp table and we don't know when they're
                // finished. Our normal pattern is to wait for a tracker object to be garbage collected and then
                // delete it, but we also clean them up at startup. That's good enough here, because there won't
                // be thousands of different reports using a table like this.
                NORMALIZED_PROTEIN_GROUP_CACHE.put(runListId, tracker);

                return tempTableName;
            }
        }
    }

    public CrosstabTableInfo createProteinProphetCrosstabTable(MS2Controller.PeptideFilteringComparisonForm form, ViewContext context)
    {
        // Don't need to filter by run since we'll add a filter, post-join
        FilteredTable baseTable = createProteinGroupMembershipTable(form, context, false);

        CrosstabSettings settings = new CrosstabSettings(baseTable);
        SimpleFilter filter = new SimpleFilter();

        //todo: can we support column ordering that matches the MS2 Runs grid on the dashboard?
        
        List<Integer> runIds = new ArrayList<>();
        if (_runs != null)
        {
            for (MS2Run run : _runs)
            {
                runIds.add(run.getRun());
            }
        }
        addPeptideFilters(form, context, baseTable, filter);
        filter.addClause(new SimpleFilter.InClause(FieldKey.fromParts("ProteinGroupId", "ProteinProphetFileId", "Run"), runIds, false));
        settings.setSourceTableFilter(filter);

        CrosstabDimension rowDim = settings.getRowAxis().addDimension(FieldKey.fromParts("SeqId"));
        ActionURL showProteinURL = new ActionURL(MS2Controller.ShowProteinAction.class, getContainer());
        rowDim.setUrl(showProteinURL.addParameter("seqId", "${SeqId}"));
        ((BaseColumnInfo)rowDim.getSourceColumn()).setDisplayColumnFactory(colInfo -> {
            DisplayColumn dc = new DataColumn(colInfo);
            dc.setLinkTarget("prot");
            return dc;
        });

        CrosstabMeasure proteinGroupMeasure = settings.addMeasure(FieldKey.fromParts("ProteinGroupId"), CrosstabMeasure.AggregateFunction.MIN, "Protein Group");

        settings.getRowAxis().setCaption("Protein Information");

        CrosstabDimension colDim;
        if (form != null && form.getPivotTypeEnum() == MS2Controller.PivotType.fraction)
        {
            settings.setInstanceCountCaption("Num Fractions");
            settings.getColumnAxis().setCaption("Fractions");
            colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts("PeptideMemberships", "PeptideId", "Fraction"));
            colDim.setUrl(MS2Controller.getShowRunURL(getUser(), getContainer())
                .addParameter("fraction", CrosstabMember.VALUE_TOKEN)
                .addParameter(MS2Manager.getDataRegionNamePeptides() + ".Fraction~eq", CrosstabMember.VALUE_TOKEN));
        }
        else
        {
            settings.setInstanceCountCaption("Num Runs");
            settings.getColumnAxis().setCaption("Runs");
            colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts("ProteinGroupId", "ProteinProphetFileId", "Run"));
            colDim.setUrl(MS2Controller.getShowRunURL(getUser(), getContainer())
                .addParameter("run", CrosstabMember.VALUE_TOKEN));
        }

        CrosstabTable result;

        if(null != _runs)
        {
            ArrayList<CrosstabMember> members = new ArrayList<>();
            //build up the list of column members
            for (MS2Run run : _runs)
            {
                if (form != null && form.getPivotTypeEnum() == MS2Controller.PivotType.fraction)
                {
                    for (MS2Fraction fraction : run.getFractions())
                    {
                        members.add(new CrosstabMember(fraction.getFraction(), colDim, fraction.getFileName()));
                    }
                }
                else
                {
                    members.add(new CrosstabMember(run.getRun(), colDim, run.getDescription()));
                }
            }
            result = new CrosstabTable(settings, members);
        }
        else
        {
            result = new CrosstabTable(settings);
        }
        if (form != null)
        {
            result.setOrAggFitlers(form.isOrCriteriaForEachRun());
        }
        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("SeqId"));
        defaultCols.add(FieldKey.fromParts(CrosstabTable.COL_INSTANCE_COUNT));
        defaultCols.add(FieldKey.fromParts(proteinGroupMeasure.getName(), "Group"));
        defaultCols.add(FieldKey.fromParts(proteinGroupMeasure.getName(), "GroupProbability"));
        result.setDefaultVisibleColumns(defaultCols);
        return result;
    }

    private void addPeptideFilters(MS2Controller.PeptideFilteringComparisonForm form, ViewContext context, FilteredTable baseTable, SimpleFilter filter)
    {
        if (form != null)
        {
            if (form.isPeptideProphetFilter() && form.getPeptideProphetProbability() != null)
            {
                filter.addClause(new CompareType.CompareClause(FieldKey.fromParts("PeptideMemberships", "PeptideId", "PeptideProphet"), CompareType.GTE, form.getPeptideProphetProbability()));
            }
            else if (form.isCustomViewPeptideFilter())
            {
                String viewName = form.getPeptideCustomViewName(context);
                CustomView view = QueryService.get().getCustomView(context.getUser(), context.getContainer(), context.getUser(), MS2Schema.SCHEMA_NAME, HiddenTableType.PeptidesFilter.toString(), viewName);
                if (view != null)
                {
                    try
                    {
                        CustomViewInfo.FilterAndSort filterAndSort = CustomViewInfo.FilterAndSort.fromString(view.getFilterAndSort());
                        FieldKey baseFieldKey = FieldKey.fromParts("PeptideMemberships", "PeptideId");
                        for (FilterInfo filterInfo : filterAndSort.getFilter())
                        {
                            FieldKey fieldKey = FieldKey.fromParts(baseFieldKey, filterInfo.getField());
                            Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(baseTable, Collections.singleton(fieldKey));
                            ColumnInfo col = columns.get(fieldKey);
                            Object value = filterInfo.getValue();
                            if (col != null)
                            {
                                value = CompareType.convertParamValue(col, value);
                            }
                            filter.addClause(new CompareType.CompareClause(fieldKey, filterInfo.getOp(), value));
                        }
                    }
                    catch (URISyntaxException e)
                    {
                        throw new UnexpectedException(e);
                    }
                }
            }
        }
    }

    public CrosstabTableInfo createPeptideCrosstabTable(MS2Controller.PeptideFilteringComparisonForm form, ViewContext context)
    {
        FilteredTable baseTable = createPeptidesTableForQueryCrosstab(form, context);

        CrosstabSettings settings = new CrosstabSettings(baseTable);
        SimpleFilter filter = new SimpleFilter();
        List<Integer> runIds = new ArrayList<>();
        if (_runs != null)
        {
            for (MS2Run run : _runs)
            {
                runIds.add(run.getRun());
            }
        }
        filter.addClause(new SimpleFilter.InClause(FieldKey.fromParts("Run"), runIds, false));
        settings.setSourceTableFilter(filter);
        // add link from peptide to peptide details ?
        CrosstabDimension rowDim = settings.getRowAxis().addDimension(FieldKey.fromParts("Peptide"));
        CrosstabDimension rowDimTrimmed = settings.getRowAxis().addDimension(FieldKey.fromParts("TrimmedPeptide"));
        CrosstabDimension searchEngineProteinMeasure = settings.getRowAxis().addDimension(FieldKey.fromParts("seqId"));

        CrosstabDimension colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts( "Run"));
        ActionURL linkUrlOnRunColuumn;
        // if matching on a single target protein, go straight to the Protein Details page in "all peptides" mode
        if ((form != null) && (form.getTargetSeqIds()!=null) && form.getTargetSeqIds().size() == 1)
        {
            linkUrlOnRunColuumn =new ActionURL(MS2Controller.ShowProteinAction.class,getContainer());
            linkUrlOnRunColuumn.addParameter("seqId", form.getTargetSeqIds().get(0));
            linkUrlOnRunColuumn.addParameter(ProteinViewBean.ALL_PEPTIDES_URL_PARAM, "true");
            linkUrlOnRunColuumn.addParameter("protein", form.getTargetProtein());
            if (form.isCustomViewPeptideFilter()  && form.getPeptideCustomViewName(context) != null)
            {
                linkUrlOnRunColuumn.addParameter(MS2Manager.getDataRegionNamePeptides() + ".viewName", form.getPeptideCustomViewName(context));
            }
            else if (form.isPeptideProphetFilter() && form.getPeptideProphetProbability() != null)
            {
                linkUrlOnRunColuumn.addParameter(MS2Manager.getDataRegionNamePeptides() + ".peptideProphet~gte", String.valueOf(form.getPeptideProphetProbability()));
            }

        }
        else
        {
            // in all other cases do the usual thing of linking to the MS2 show run page
            linkUrlOnRunColuumn = MS2Controller.getShowRunURL(getUser(), getContainer());
        }
        colDim.setUrl(linkUrlOnRunColuumn.addParameter("run", CrosstabMember.VALUE_TOKEN));

        CrosstabMeasure scansMeasure = settings.addMeasure(FieldKey.fromParts("RowId"), CrosstabMeasure.AggregateFunction.COUNT, "Total");
        CrosstabMeasure avgPepProphMeasure = settings.addMeasure(FieldKey.fromParts("PeptideProphet"), CrosstabMeasure.AggregateFunction.AVG, "Avg PepProphet");
        settings.addMeasure(FieldKey.fromParts("Charge"), CrosstabMeasure.AggregateFunction.MIN, "Min Charge");
        settings.addMeasure(FieldKey.fromParts("Charge"), CrosstabMeasure.AggregateFunction.MAX, "Max Charge");
        settings.addMeasure(FieldKey.fromParts("Charge"), CrosstabMeasure.AggregateFunction.COUNT, "Charge States");

        settings.addMeasure(FieldKey.fromParts("PeptideProphet"), CrosstabMeasure.AggregateFunction.MAX, "Max PepProphet");
        settings.addMeasure(FieldKey.fromParts("XCorr"), CrosstabMeasure.AggregateFunction.AVG, "Avg XCorr");
        settings.addMeasure(FieldKey.fromParts("XCorr"), CrosstabMeasure.AggregateFunction.MAX, "Max XCorr");
        settings.addMeasure(FieldKey.fromParts("DeltaCn"), CrosstabMeasure.AggregateFunction.AVG, "Avg DeltaCn");
        settings.addMeasure(FieldKey.fromParts("DeltaCn"), CrosstabMeasure.AggregateFunction.MAX, "Max DeltaCn");
        settings.addMeasure(FieldKey.fromParts("PeptideProphetErrorRate"), CrosstabMeasure.AggregateFunction.AVG, "Avg PepProph Error");
        settings.addMeasure(FieldKey.fromParts("PeptideProphetErrorRate"), CrosstabMeasure.AggregateFunction.MAX, "Max PepProph Error");
        settings.addMeasure(FieldKey.fromParts("LightArea"), CrosstabMeasure.AggregateFunction.SUM, "Total light area");
        settings.addMeasure(FieldKey.fromParts("HeavyArea"), CrosstabMeasure.AggregateFunction.SUM, "Total heavy area");
        settings.addMeasure(FieldKey.fromParts("DecimalRatio"), CrosstabMeasure.AggregateFunction.AVG, "Avg decimal ratio");
        settings.addMeasure(FieldKey.fromParts("DecimalRatio"), CrosstabMeasure.AggregateFunction.MAX, "Max decimal ratio");
        settings.addMeasure(FieldKey.fromParts("DecimalRatio"), CrosstabMeasure.AggregateFunction.MIN, "Min decimal ratio");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("run", CrosstabMember.VALUE_NAME);
        parameters.put(MS2Manager.getDataRegionNamePeptides() + ".Peptide~eq", FieldKey.fromParts("peptide"));
        scansMeasure.setUrl(new DetailsURL(new ActionURL(MS2Controller.ShowPeptidePopupAction.class, getContainer()), parameters));
        ((BaseColumnInfo)scansMeasure.getSourceColumn()).setDisplayColumnFactory(new DisplayColumnFactory(){
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    DisplayColumn dc = new DataColumn(colInfo);
                    dc.setLinkTarget("pep");
                    return dc;
                }
            });

        settings.setInstanceCountCaption("Run Count");
        settings.getRowAxis().setCaption("Peptides identified");
        settings.getColumnAxis().setCaption("Runs");

        CrosstabTableInfo result;

        if (null != _runs)
        {
            ArrayList<CrosstabMember> members = new ArrayList<>();
            //build up the list of column members
            for (MS2Run run : _runs)
            {
                members.add(new CrosstabMember(run.getRun(), colDim, run.getDescription()));
            }
            result = new CrosstabTable(settings, members);
        }
        else
        {
            result = new CrosstabTable(settings);
        }

        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts(rowDim.getName()));
        defaultCols.add(FieldKey.fromParts(CrosstabTable.COL_INSTANCE_COUNT));
        defaultCols.add(FieldKey.fromParts(scansMeasure.getName()));
        defaultCols.add(FieldKey.fromParts(avgPepProphMeasure.getName()));
        result.setDefaultVisibleColumns(defaultCols);
        return result;
    }

    protected FilteredTable createPeptidesTableForQueryCrosstab(final MS2Controller.PeptideFilteringComparisonForm form, final ViewContext context)
    {
        FilteredTable baseTable = new FilteredTable<>(MS2Manager.getTableInfoPeptides(), this);
        baseTable.wrapAllColumns(true);
        //apply PeptideFilteringComparisonForm
        SQLFragment whereSQL;
        SimpleFilter.FilterClause filt;

        if (form != null && form.isPeptideProphetFilter() && form.getPeptideProphetProbability() != null)
        {
            baseTable.addCondition(new SimpleFilter(FieldKey.fromParts("PeptideProphet"), form.getPeptideProphetProbability(), CompareType.GTE));
        }
        else if (form != null && form.isCustomViewPeptideFilter())
        {
            whereSQL = new SQLFragment( "( RowId IN ( ");
            whereSQL.append(getPeptideSelectSQL(context.getRequest(), form.getPeptideCustomViewName(context), Arrays.asList(FieldKey.fromParts("RowId")), null));
            whereSQL.append(") ) ");
            baseTable.addCondition(whereSQL, FieldKey.fromParts("RowId"));
        }

        if (form != null && form.hasTargetSeqIds())
        {
            filt = PeptideManager.getSequencesFilter(form.getTargetSeqIds());
            baseTable.addCondition(filt.toSQLFragment(null, this.getDbSchema().getSqlDialect()));
        }
        baseTable.getMutableColumn("SeqId").setLabel("Search Engine Protein");
        baseTable.getMutableColumn("SeqId").setFk(createSequencesLookup());

        return baseTable;
    }

    private ForeignKey createSequencesLookup()
    {
        return new LookupForeignKey("SeqId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                SequencesTableInfo result = createSequencesTable(getLookupContainerFilter());
                // This is a horrible hack to try to deal with https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=5237
                // Performance on a SQLServer installation with a large number of runs and sequences is much better with
                // this condition because it causes the query plan to flip to something that does a much more efficient
                // join with the sequences tables. However, adding it significantly degrades performance on my admittedly
                // small (though not tiny) Postgres dev database
                if (_runs != null && MS2Manager.getSchema().getSqlDialect().isSqlServer())
                {
                    SQLFragment sql = new SQLFragment();
                    sql.append("(SeqId IN (SELECT SeqId FROM " + ProteinSchema.getTableInfoFastaSequences() + " WHERE FastaId IN (SELECT FastaId FROM ");
                    sql.append(MS2Manager.getTableInfoFastaRunMapping() + " WHERE Run IN ");
                    appendRunInClause(sql);
                    sql.append(")))");

                    result.addCondition(sql, FieldKey.fromParts("SeqId"));
                }
                return result;
            }
        };
    }

    public TableInfo createPeptidesTableInfo()
    {
        return createPeptidesTableInfo(true, ContainerFilter.current(getContainer()), null, null);
    }

    public TableInfo createPeptidesTableInfo(boolean includeFeatureFk, ContainerFilter containerFilter, SimpleFilter filter, Iterable<FieldKey> defaultColumns)
    {
        // Go through the schema so we get metadata applied correctly
        PeptidesTableInfo table = (PeptidesTableInfo)getTable(MS2Schema.TableType.Peptides.name(), containerFilter, true, true);
        if (null != filter)
            table.addCondition(filter);
        if (null != defaultColumns)
            table.setDefaultVisibleColumns(defaultColumns);
        table.setLocked(true);
        return table;
    }
}
