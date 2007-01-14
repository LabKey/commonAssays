package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentManager;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.AppProps;

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

    private static final String MASCOT_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.Mascot%";
    private static final String SEQUEST_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.Sequest%";
    private static final String XTANDEM_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.XTandem%";
    private static final String SAMPLE_PREP_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.PreSearch.%";

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
        super(SCHEMA_NAME, user, container, ExperimentManager.get().getExpSchema());
        _expSchema = new ExpSchema(user, container);
    }

    public Set<String> getTableNames()
    {
        Set<String> result = new HashSet<String>();
        result.add(SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME);
        result.add(XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME);
        result.add(MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME);
        result.add(SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME);
        result.add(GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME);
        return result;
    }

    public TableInfo getTable(String name, String alias)
    {
        if (SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            ExpRunTable result = _expSchema.createRunsTable(alias);
            result.setProtocolPatterns(SAMPLE_PREP_PROTOCOL_PATTERN);
            return result;
        }
        if (XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, XTANDEM_PROTOCOL_PATTERN);
        }
        if (MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, MASCOT_PROTOCOL_PATTERN);
        }
        if (SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, SEQUEST_PROTOCOL_PATTERN);
        }
        if (GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, XTANDEM_PROTOCOL_PATTERN, MASCOT_PROTOCOL_PATTERN,SEQUEST_PROTOCOL_PATTERN);
        }
        return super.getTable(name, alias);
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
