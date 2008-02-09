package org.labkey.microarray;

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.AppProps;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.study.assay.AssayDataLinkDisplayColumn;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.util.Set;
import java.util.List;
import java.sql.Types;
import java.io.Writer;
import java.io.IOException;

public class MicroarraySchema extends UserSchema
{
    public static final String SCHEMA_NAME = "Microarray";
    public static final String TABLE_RUNS = "MicroarrayRuns";

    private ExpSchema _expSchema;
    public static final String QC_REPORT_COLUMN_NAME = "QCReport";
    public static final String THUMBNAIL_IMAGE_COLUMN_NAME = "ThumbnailImage";

    public MicroarraySchema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, getSchema());
        _expSchema = new ExpSchema(user, container);
    }


    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new MicroarraySchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public Set<String> getTableNames()
    {
        return PageFlowUtil.set(TABLE_RUNS);
    }

    public TableInfo getTable(String name, String alias)
    {
        if (TABLE_RUNS.equalsIgnoreCase(name))
        {
            return createRunsTable(alias);
        }
        return super.getTable(name, alias);
    }

    public ExpRunTable createRunsTable(String alias)
    {
        ExpRunTable result = _expSchema.createRunsTable(alias);
        result.getColumn(ExpRunTable.Column.Name).setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AssayDataLinkDisplayColumn(colInfo);
            }
        });
        result.setProtocolPatterns("urn:lsid:%:" + MicroarrayAssayProvider.PROTOCOL_PREFIX + ".%");

        SQLFragment thumbnailSQL = new SQLFragment("(SELECT MIN(d.RowId)\n" +
                "\nFROM " + ExperimentService.get().getTinfoData() + " d " +
                "\nWHERE d.RunId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId AND d.LSID LIKE '%:" + MicroarrayModule.IMAGE_DATA_TYPE.getNamespacePrefix() + "%')");
        ColumnInfo thumbnailColumn = new ExprColumn(result, THUMBNAIL_IMAGE_COLUMN_NAME, thumbnailSQL, Types.INTEGER);
        thumbnailColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ActionURL url = new ActionURL("experiment", "showFile.view", getContainer());
                return new IconDisplayColumn(colInfo, 18, 18, url, "rowId", AppProps.getInstance().getContextPath() + "/microarray/images/microarrayThumbnailIcon.png");
            }
        });
        result.addColumn(thumbnailColumn);

        SQLFragment qcReportSQL = new SQLFragment("(SELECT MIN(d.RowId)\n" +
                "\nFROM " + ExperimentService.get().getTinfoData() + " d " +
                "\nWHERE d.RunId = " + ExprColumn.STR_TABLE_ALIAS + ".RowId AND d.LSID LIKE '%:" + MicroarrayModule.QC_REPORT_DATA_TYPE.getNamespacePrefix() + "%')");
        ColumnInfo qcReportColumn = new ExprColumn(result, QC_REPORT_COLUMN_NAME, qcReportSQL, Types.INTEGER);
        qcReportColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ActionURL url = new ActionURL("experiment", "showFile.view", getContainer());
                url.addParameter("inline", "true");
                return new IconDisplayColumn(colInfo, 18, 18, url, "rowId", AppProps.getInstance().getContextPath() + "/microarray/images/qcReportIcon.png");
            }
        });
        result.addColumn(qcReportColumn);

        List<FieldKey> defaultCols = result.getDefaultVisibleColumns();
        defaultCols.remove(FieldKey.fromParts(qcReportColumn.getName()));
        defaultCols.remove(FieldKey.fromParts(thumbnailColumn.getName()));
        defaultCols.add(2, FieldKey.fromParts(qcReportColumn.getName()));
        defaultCols.add(2, FieldKey.fromParts(thumbnailColumn.getName()));
        result.setDefaultVisibleColumns(defaultCols);

        return result;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("microarray");
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static class IconLinksDisplayColumn extends DataColumn
    {
        public IconLinksDisplayColumn(ColumnInfo info)
        {
            super(info);
            setCaption("");
            setWidth("18");
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            ActionURL graphURL = new ActionURL("experiment", "showData.view", ctx.getContainer());
            Object runId = ctx.getRow().get(getColumnInfo().getAlias());
            if (runId != null)
            {
                graphURL.addParameter("rowId", runId.toString());
                out.write("<a href=\"" + graphURL.getLocalURIString() + "\" title=\"Thumbnail image\"><img src=\"" + AppProps.getInstance().getContextPath() + "/MS2/images/runIcon.gif\" height=\"18\" width=\"18\"/></a>");
            }
        }
    }
}
