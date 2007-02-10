package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.ProteinListDisplayColumn;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.CaseInsensitiveHashSet;

import java.util.*;
import java.sql.Types;
import java.io.Writer;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Sep 25, 2006
 */
public class MS2Schema extends UserSchema
{
    public static final String SCHEMA_NAME = "ms2";

    public static final String SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME = "SamplePrepRuns";
    public static final String XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "XTandemSearchRuns";
    public static final String MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "MascotSearchRuns";
    public static final String SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "SequestSearchRuns";
    public static final String GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "MS2SearchRuns";

    public static final String PEPTIDES_TABLE_NAME = "QueryPeptides";
    public static final String PROTEIN_GROUPS_TABLE_NAME = "QueryProteinGroups";
    public static final String SEQUENCES_TABLE_NAME = "QuerySequences";

    private static final String MASCOT_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.Mascot%";
    private static final String SEQUEST_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.Sequest%";
    private static final String XTANDEM_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.XTandem%";
    private static final String SAMPLE_PREP_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.PreSearch.%";

    private static final Set<String> HIDDEN_PEPTIDE_MEMBERSHIPS_COLUMN_NAMES = new CaseInsensitiveHashSet(Arrays.asList("PeptideId"));

    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new MS2Schema(schema.getUser(), schema.getContainer());
            }
        });
    }

    private ExpSchema _expSchema;

    public MS2Schema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, ExperimentService.get().getSchema());
        _expSchema = new ExpSchema(user, container);
    }

    public Set<String> getTableNames()
    {
        return new HashSet<String>(Arrays.asList(
            SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME,
            XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            PEPTIDES_TABLE_NAME,
            PROTEIN_GROUPS_TABLE_NAME,
            SEQUENCES_TABLE_NAME));
    }

    public TableInfo getTable(String name, String alias)
    {
        if (SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            ExpRunTable result = _expSchema.createRunsTable(alias);
            result.setProtocolPatterns(SAMPLE_PREP_PROTOCOL_PATTERN);
            return result;
        }
        else if (XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, XTANDEM_PROTOCOL_PATTERN);
        }
        else if (MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, MASCOT_PROTOCOL_PATTERN);
        }
        else if (SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, SEQUEST_PROTOCOL_PATTERN);
        }
        else if (GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, XTANDEM_PROTOCOL_PATTERN, MASCOT_PROTOCOL_PATTERN,SEQUEST_PROTOCOL_PATTERN);
        }
        else if (PEPTIDES_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createPeptidesTable(alias);
        }
        else if (PROTEIN_GROUPS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createQueryProteinGroupsTable(alias);
        }
        else if (SEQUENCES_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSequencesTable(alias);
        }
        else
        {
            return super.getTable(name, alias);
        }
    }

    private TableInfo createQueryProteinGroupsTable(String alias)
    {
        return new ProteinGroupTableInfo(alias);
    }

    private TableInfo createPeptideMembershipsTable()
    {
        TableInfo info = MS2Manager.getTableInfoPeptideMemberships();
        FilteredTable result = new FilteredTable(info);
        for (ColumnInfo col : info.getColumns())
        {
            ColumnInfo newColumn = result.wrapColumn(col);
            if (HIDDEN_PEPTIDE_MEMBERSHIPS_COLUMN_NAMES.contains(newColumn.getName()))
            {
                newColumn.setIsHidden(true);
            }
        }
        result.getColumn("ProteinGroupId").setFk(new LookupForeignKey("RowId", false)
        {
            public TableInfo getLookupTableInfo()
            {
                return createProteinGroupsTable();
            }
        });
        return result;
    }

    private TableInfo createProteinGroupsTable()
    {
        ProteinGroupTableInfo result = new ProteinGroupTableInfo(null, false);

        ColumnInfo proteinsColumn = result.wrapColumn("Proteins", result.getRealTable().getColumn("RowId"));
        proteinsColumn.setRenderClass(ProteinListDisplayColumn.class);
        proteinsColumn.setKeyField(false);
        result.addColumn(proteinsColumn);

        return result;
    }

    private TableInfo createFractionsTable()
    {
        SqlDialect dialect = MS2Manager.getSqlDialect();
        FilteredTable result = new FilteredTable(MS2Manager.getTableInfoFractions());
        result.wrapAllColumns(true);

        SQLFragment fractionNameSQL = new SQLFragment(dialect.getSubstringFunction(ExprColumn.STR_TABLE_ALIAS + ".FileName", "1", dialect.getStringIndexOfFunction("'.'", ExprColumn.STR_TABLE_ALIAS + ".FileName") + "- 1"));

        ColumnInfo fractionName = new ExprColumn(result, "FractionName", fractionNameSQL, Types.VARCHAR);
        fractionName.setCaption("Name");
        fractionName.setWidth("200");
        result.addColumn(fractionName);

        return result;
    }

    private SequencesTableInfo createSequencesTable(String aliasName)
    {
        return new SequencesTableInfo(aliasName, getContainer());
    }

    private TableInfo createPeptidesTable(final String alias)
    {
        SqlDialect dialect = MS2Manager.getSqlDialect();
        TableInfo info = MS2Manager.getTableInfoPeptidesData();
        final FilteredTable result = new FilteredTable(info)
        {
            public List<FieldKey> getDefaultVisibleColumns()
            {
                List<FieldKey> result = new ArrayList<FieldKey>();
                result.add(FieldKey.fromString("Scan"));
                result.add(FieldKey.fromString("Charge"));
                result.add(FieldKey.fromString("RawScore"));
                result.add(FieldKey.fromString("DiffScore"));
                result.add(FieldKey.fromString("Expect"));
                result.add(FieldKey.fromString("IonPercent"));
                result.add(FieldKey.fromString("Mass"));
                result.add(FieldKey.fromString("DeltaMass"));
                result.add(FieldKey.fromString("PeptideProphet"));
                result.add(FieldKey.fromString("Peptide"));
                result.add(FieldKey.fromString("ProteinHits"));
                result.add(FieldKey.fromString("Protein"));
                return result;
            }
        };
        for (ColumnInfo col : info.getColumns())
        {
            if (!col.getName().toLowerCase().startsWith("score"))
            {
                result.wrapColumn(col);
            }
        }

        SQLFragment precursorMassSQL = new SQLFragment(result.getAliasName() + ".mass + " + result.getAliasName() + ".deltamass");
        ColumnInfo precursorMass = new ExprColumn(result, "PrecursorMass", precursorMassSQL, Types.REAL);
        precursorMass.setFormatString("0.0000");
        precursorMass.setWidth("65");
        precursorMass.setCaption("ObsMH+");
        result.addColumn(precursorMass);

        SQLFragment fractionalDeltaMassSQL = new SQLFragment("ABS(" + result.getAliasName() + ".deltamass - " + dialect.getRoundFunction(result.getAliasName() + ".deltamass") + ")");
        ColumnInfo fractionalDeltaMass = new ExprColumn(result, "FractionalDeltaMass", fractionalDeltaMassSQL, Types.REAL);
        fractionalDeltaMass.setFormatString("0.0000");
        fractionalDeltaMass.setWidth("55");
        fractionalDeltaMass.setCaption("fdMass");
        result.addColumn(fractionalDeltaMass);

        SQLFragment fractionalSQL = new SQLFragment("CASE\n" +
            "            WHEN " + ExprColumn.STR_TABLE_ALIAS + ".mass = 0.0 THEN 0.0\n" +
            "            ELSE abs(1000000.0 * abs(" + ExprColumn.STR_TABLE_ALIAS + ".deltamass - " + dialect.getRoundFunction(ExprColumn.STR_TABLE_ALIAS + ".deltamass") + ") / (" + ExprColumn.STR_TABLE_ALIAS + ".mass + ((" + ExprColumn.STR_TABLE_ALIAS + ".charge - 1.0) * 1.007276)))\n" +
            "        END");
        ColumnInfo fractionalDeltaMassPPM = new ExprColumn(result, "FractionalDeltaMassPPM", fractionalSQL, Types.REAL);
        fractionalDeltaMassPPM.setFormatString("0.0");
        fractionalDeltaMassPPM.setWidth("80");
        fractionalDeltaMassPPM.setCaption("fdMassPPM");
        result.addColumn(fractionalDeltaMassPPM);

        SQLFragment deltaSQL = new SQLFragment("CASE\n" +
            "            WHEN " + ExprColumn.STR_TABLE_ALIAS + ".mass = 0.0 THEN 0.0\n" +
            "            ELSE abs(1000000.0 * " + ExprColumn.STR_TABLE_ALIAS + ".deltamass / (" + ExprColumn.STR_TABLE_ALIAS + ".mass + ((" + ExprColumn.STR_TABLE_ALIAS + ".charge - 1.0) * 1.007276)))\n" +
            "        END");
        ColumnInfo deltaMassPPM = new ExprColumn(result, "DeltaMassPPM", deltaSQL, Types.REAL);
        deltaMassPPM.setFormatString("0.0");
        deltaMassPPM.setWidth("75");
        deltaMassPPM.setCaption("dMassPPM");
        result.addColumn(deltaMassPPM);

        SQLFragment mzSQL = new SQLFragment("CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".Charge = 0.0 THEN 0.0 ELSE (" + ExprColumn.STR_TABLE_ALIAS + ".Mass + " + ExprColumn.STR_TABLE_ALIAS + ".DeltaMass + (" + ExprColumn.STR_TABLE_ALIAS + ".Charge - 1.0) * 1.007276) / " + ExprColumn.STR_TABLE_ALIAS + ".Charge END");
        ColumnInfo mz = new ExprColumn(result, "MZ", mzSQL, Types.REAL);
        mz.setFormatString("0.0000");
        mz.setWidth("55");
        mz.setCaption("ObsMZ");
        result.addColumn(mz);

        SQLFragment strippedPeptideSQL = new SQLFragment("LTRIM(RTRIM(" + ExprColumn.STR_TABLE_ALIAS + ".PrevAA " + dialect.getConcatenationOperator() + " " + ExprColumn.STR_TABLE_ALIAS + ".TrimmedPeptide " + dialect.getConcatenationOperator() + " " + ExprColumn.STR_TABLE_ALIAS + ".NextAA))");
        ColumnInfo strippedPeptide = new ExprColumn(result, "StrippedPeptide", strippedPeptideSQL, Types.VARCHAR);
        strippedPeptide.setWidth("320");
        result.addColumn(strippedPeptide);

        ColumnInfo quantitation = result.wrapColumn("Quantitation", info.getColumn("RowId"));
        quantitation.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return MS2Manager.getTableInfoQuantitation();
            }
        });
        quantitation.setKeyField(false);
        result.addColumn(quantitation);

        ColumnInfo proteinGroup = result.wrapColumn("ProteinProphetData", info.getColumn("RowId"));
        proteinGroup.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createPeptideMembershipsTable();
            }
        });
        proteinGroup.setKeyField(false);
        result.addColumn(proteinGroup);

        ColumnInfo peptideProphetData = result.wrapColumn("PeptideProphetDetails", info.getColumn("RowId"));
        peptideProphetData.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return MS2Manager.getTableInfoPeptideProphetData();
            }
        });
        peptideProphetData.setKeyField(false);
        result.addColumn(peptideProphetData);

        result.getColumn("SeqId").setFk(new LookupForeignKey("SeqId", false)
        {
            public TableInfo getLookupTableInfo()
            {
                SequencesTableInfo sequenceTable = new SequencesTableInfo(null, getContainer());
                SQLFragment fastaNameSQL = new SQLFragment(result.getAliasName() + ".Protein");
                ExprColumn fastaNameColumn = new ExprColumn(result, "Database Sequence Name", fastaNameSQL, Types.VARCHAR);
                sequenceTable.addColumn(fastaNameColumn);

                ViewURLHelper showProteinURL = new ViewURLHelper("MS2", "showProtein.view", getContainer());
                String showProteinURLString = showProteinURL.getLocalURIString() + "peptideId=${RowId}";
                fastaNameColumn.setURL(showProteinURLString);

                return sequenceTable;
            }
        });

        ViewURLHelper showProteinURL = new ViewURLHelper("MS2", "showProtein.view", getContainer());
        String showProteinURLString = showProteinURL.getLocalURIString() + "&peptideId=${RowId}";
        result.getColumn("SeqId").setURL(showProteinURLString);
        result.getColumn("Protein").setURL(showProteinURLString);
        result.getColumn("SeqId").setCaption("Search Engine Protein");

        ViewURLHelper showPeptideURL = new ViewURLHelper("MS2", "showPeptide.view", getContainer());
        String showPeptideURLString = showPeptideURL.getLocalURIString() + "&peptideId=${RowId}";
        result.getColumn("Scan").setURL(showPeptideURLString);
        result.getColumn("Peptide").setURL(showPeptideURLString);

        result.addColumn(result.wrapColumn("RawScore", info.getColumn("score1"))).setCaption("Raw");
        result.addColumn(result.wrapColumn("DiffScore", info.getColumn("score2"))).setCaption("dScore");
        result.addColumn(result.wrapColumn("ZScore", info.getColumn("score3")));

        result.addColumn(result.wrapColumn("SpScore", info.getColumn("score1")));
        result.addColumn(result.wrapColumn("DeltaCN", info.getColumn("score2")));
        result.addColumn(result.wrapColumn("XCorr", info.getColumn("score3")));
        result.addColumn(result.wrapColumn("SpRank", info.getColumn("score4")));

        result.addColumn(result.wrapColumn("Hyper", info.getColumn("score1")));
        result.addColumn(result.wrapColumn("Next", info.getColumn("score2")));
        result.addColumn(result.wrapColumn("B", info.getColumn("score3")));
        result.addColumn(result.wrapColumn("Y", info.getColumn("score4")));
        result.addColumn(result.wrapColumn("Expect", info.getColumn("score5")));

        result.addColumn(result.wrapColumn("Ion", info.getColumn("score1")));
        result.addColumn(result.wrapColumn("Identity", info.getColumn("score2")));
        result.addColumn(result.wrapColumn("Homology", info.getColumn("score3")));

        result.getColumn("Fraction").setFk(new LookupForeignKey("Fraction")
        {
            public TableInfo getLookupTableInfo()
            {
                return createFractionsTable();
            }
        });

        return result;
    }


    private TableInfo createSearchTable(String alias, String... protocolPattern)
    {
        final ExpRunTable result = _expSchema.createRunsTable(alias);
        result.setProtocolPatterns(protocolPattern);

        SQLFragment sql = new SQLFragment("(SELECT MIN(ms2Runs.run)\n" +
                "\nFROM " + MS2Manager.getTableInfoRuns() + " ms2Runs " +
                "\nWHERE ms2Runs.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID)");
        ColumnInfo ms2DetailsColumn = new ExprColumn(result, alias, sql, Types.INTEGER);
        ms2DetailsColumn.setName("MS2Details");
        ViewURLHelper url = new ViewURLHelper("MS2", "showRun.view", getContainer());
        ms2DetailsColumn.setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(MS2Manager.getTableInfoRuns());
                result.addColumn(result.getRealTable().getColumn("Run"));
                result.addColumn(result.getRealTable().getColumn("Description"));
                result.addColumn(result.getRealTable().getColumn("Created"));
                result.addColumn(result.getRealTable().getColumn("Path"));
                result.addColumn(result.getRealTable().getColumn("SearchEngine"));
                result.addColumn(result.getRealTable().getColumn("MassSpecType"));
                result.addColumn(result.getRealTable().getColumn("PeptideCount"));
                result.addColumn(result.getRealTable().getColumn("SpectrumCount"));
                result.addColumn(result.getRealTable().getColumn("SearchEnzyme"));
                result.addColumn(result.getRealTable().getColumn("Filename"));
                result.addColumn(result.getRealTable().getColumn("Status"));
                result.addColumn(result.getRealTable().getColumn("Type"));

                ColumnInfo iconColumn = result.wrapColumn("Links", result.getColumn("Run"));
                iconColumn.setRenderClass(IconLinksDisplayColumn.class);
                result.addColumn(iconColumn);
                return result;
            }
        });
        result.addColumn(ms2DetailsColumn);

        ms2DetailsColumn.setIsHidden(false);
        result.addDefaultVisibleColumn("Flag");
        result.addDefaultVisibleColumn("Links");
        result.addDefaultVisibleColumn("MS2Details/Links");
        result.addDefaultVisibleColumn("Name");
        result.addDefaultVisibleColumn("Protocol");
        result.addDefaultVisibleColumn("MS2Details/Path");
        result.addDefaultVisibleColumn("Input/FASTA");
        result.addDefaultVisibleColumn("Input/mzXML");
        result.addDefaultVisibleColumn("Input/mzXMLTest");

        return result;
    }

    public static class IconLinksDisplayColumn extends DataColumn
    {
        public IconLinksDisplayColumn(ColumnInfo info)
        {
            super(info);
            setCaption("");
            setWidth("18");
        }

        public boolean isFilterable()
        {
            return false;
        }

        public boolean isSortable()
        {
            return false;
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            ViewURLHelper graphURL = new ViewURLHelper("MS2", "showRun.view", ctx.getContainer());
            Object runId = ctx.getRow().get(getColumnInfo().getAlias());
            if (runId != null)
            {
                graphURL.addParameter("run", runId.toString());
                out.write("<a href=\"" + graphURL.getLocalURIString() + "\"><img src=\"" + AppProps.getInstance().getContextPath() + "/MS2/images/runIcon.gif\" height=\"18\" width=\"18\"/></a>");
            }
        }

        public void addQueryColumns(Set<ColumnInfo> columns)
        {
            super.addQueryColumns(columns);
        }
    }


}
