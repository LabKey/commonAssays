package org.labkey.issue.query;

import org.labkey.issue.IssuesController;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.issues.IssuesSchema;
import org.labkey.api.query.*;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.issue.model.IssueManager;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IssuesTable extends FilteredTable
{
    private static Logger _log = Logger.getLogger("org.labkey.api." + IssuesTable.class);

    private static final String DEFAULT_LIST_COLUMNS = "IssueId,Type,Area,Title,AssignedTo,Priority,Status,Milestone";
    private IssuesQuerySchema _schema;

    public IssuesTable(IssuesQuerySchema schema)
    {
        super(IssuesSchema.getInstance().getTableInfoIssues(), schema.getContainer());
        _schema = schema;

        for (ColumnInfo col : getAvailableColumns())
            addColumn(col);

        setDefaultVisibleColumns(getDefaultColumns());
    }

    private ColumnInfo[] getAvailableColumns()
    {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

        final ColumnInfo col = getRealTable().getColumn("IssueId");
        ColumnInfo issueColumn = new AliasedColumn(this, col.getName(), col);
        issueColumn.setFk(new QueryForeignKey(_schema, "Issue", "IssueId", "IssueId")
        {
            public StringExpressionFactory.StringExpression getURL(ColumnInfo parent) {
                ViewURLHelper base = new ViewURLHelper("Issues", "details", _schema.getContainer());
                return new LookupURLExpression(base, Collections.singletonMap("issueId", parent));
            }
        });
        columns.add(issueColumn);
        
        addColumn(columns, "Type");
        addColumn(columns, "Area");
        addColumn(columns, "Title");

        // Set a custom renderer for the AssignedTo column
        final ColumnInfo info = getRealTable().getColumn("AssignedTo");
        if (info != null)
        {
            info.setRenderClass(DisplayColumnAssignedTo.class);
            columns.add(new AliasedColumn(this, info.getName(), info));
        }
        addColumn(columns, "Priority");
        addColumn(columns, "Status");
        addColumn(columns, "Milestone");

        addColumn(columns, "BuildFound");
        addColumn(columns, "ModifiedBy");
        addColumn(columns, "Modified");
        addColumn(columns, "CreatedBy");
        addColumn(columns, "Created");
        addColumn(columns, "ResolvedBy");
        addColumn(columns, "Resolved");
        addColumn(columns, "Duplicate");
        addColumn(columns, "ClosedBy");
        addColumn(columns, "Closed");
        addColumn(columns, "NotifyList");

        // add any custom columns
        Map<String, String> customColumnCaptions = getCustomColumnCaptions(_schema.getContainer());
        for (Map.Entry<String, String> cce : customColumnCaptions.entrySet())
        {
            ColumnInfo realColumn = getRealTable().getColumn(cce.getKey());
            if (realColumn != null)
            {
                ColumnInfo column = new AliasedColumn(this, cce.getValue(), realColumn);
                column.setAlias(cce.getKey());
                columns.add(column);
            }
        }

        return columns.toArray(new ColumnInfo[0]);
    }

    private void addColumn(List<ColumnInfo> columns, String columnName)
    {
        final ColumnInfo info = getRealTable().getColumn(columnName);
        if (info != null)
        {
            columns.add(new AliasedColumn(this, info.getName(), info));
        }
        else
            _log.error("Unable to add issue column name: " + columnName);
    }

    /**
     * Returns the default list of visible columns for this table.
     */
    private List<FieldKey> getDefaultColumns()
    {
        List<FieldKey> visibleColumns = new ArrayList<FieldKey>();

        for (String name : getDefaultColumnNames(_schema.getContainer()).split(","))
        {
            visibleColumns.add(FieldKey.fromString(name));
        }
        return visibleColumns;
    }

    public static String getDefaultColumnNames(Container container)
    {
        Map map = PropertyManager.getProperties(container.getId(), IssuesController.ISSUES_COLUMNS_LOOKUP, false);
        if (null != map)
        {
            String listColumns = (String)map.get("ListColumns");

            if (null != listColumns)
                return listColumns;
        }

        StringBuffer columnNames = new StringBuffer(DEFAULT_LIST_COLUMNS);
        Map<String, String> columnCaptions = getCustomColumnCaptions(container);

        for (String columnName : columnCaptions.values())
            columnNames.append(',').append(columnName);
        return columnNames.toString();
    }

    public static Map<String, String> getCustomColumnCaptions(Container container)
    {
        try {
            return IssueManager.getCustomColumnConfiguration(container).getColumnCaptions();
        }
        catch (Exception e)
        {
            _log.error(e);
        }
        return Collections.emptyMap();
    }

    /**
     * Don't display any name for UserId "0" (Unassigned)
     */
    public static class DisplayColumnAssignedTo extends DataColumn
    {
        public DisplayColumnAssignedTo(ColumnInfo col)
        {
            super(col);
        }

        public Object getValue(RenderContext ctx)
        {
            Map rowMap = ctx.getRow();
            String displayName = (String)rowMap.get("assignedTo$displayName");
            return (null != displayName ? displayName : "&nbsp;");
        }

        public Class getValueClass()
        {
            return String.class;
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException {
            out.write(getValue(ctx).toString());
        }
    }
}
