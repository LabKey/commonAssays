package org.labkey.issue.query;

import org.apache.log4j.Logger;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.issues.IssuesSchema;
import org.labkey.api.query.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.issue.IssuesController;
import org.labkey.issue.model.IssueManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IssuesTable extends FilteredTable
{
    private static Logger _log = Logger.getLogger(IssuesTable.class);

    private static final String DEFAULT_LIST_COLUMNS = "IssueId,Type,Area,Title,AssignedTo,Priority,Status,Milestone";
    private IssuesQuerySchema _schema;

    public IssuesTable(IssuesQuerySchema schema)
    {
        super(IssuesSchema.getInstance().getTableInfoIssues(), schema.getContainer());
        _schema = schema;

        addAllColumns();

        setDefaultVisibleColumns(getDefaultColumns());
        ViewURLHelper base = new ViewURLHelper("Issues", "details", _schema.getContainer());
        setDetailsURL(new DetailsURL(base, Collections.singletonMap("issueId", "IssueId")));
        setTitleColumn("Title");
    }

    private void addAllColumns()
    {
        ColumnInfo issueIdColumn = wrapColumn(_rootTable.getColumn("IssueId"));
        issueIdColumn.setFk(new RowIdForeignKey(issueIdColumn)
        {
            public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
            {
                if (displayField == null)
                    return null;
                return super.createLookupColumn(parent, displayField);
            }
        });

        issueIdColumn.setKeyField(true);
        addColumn(issueIdColumn);
        addWrapColumn(_rootTable.getColumn("Type"));
        addWrapColumn(_rootTable.getColumn("Area"));
        addWrapColumn(_rootTable.getColumn("Title"));
        ColumnInfo assignedTo = wrapColumn("AssignedTo", _rootTable.getColumn("AssignedTo"));
        assignedTo.setFk(new UserIdForeignKey());
        assignedTo.setRenderClass(UserIdRenderer.GuestAsBlank.class);
        addColumn(assignedTo);
        addWrapColumn(_rootTable.getColumn("Priority"));
        addWrapColumn(_rootTable.getColumn("Status"));
        addWrapColumn(_rootTable.getColumn("Milestone"));

        addWrapColumn(_rootTable.getColumn("BuildFound"));
        ColumnInfo modifiedBy = wrapColumn(_rootTable.getColumn("ModifiedBy"));
        UserIdForeignKey.initColumn(modifiedBy);
        addColumn(modifiedBy);
        addWrapColumn(_rootTable.getColumn("Modified"));
        ColumnInfo createdBy = wrapColumn(_rootTable.getColumn("CreatedBy"));
        UserIdForeignKey.initColumn(createdBy);
        addColumn(createdBy);
        addWrapColumn(_rootTable.getColumn("Created"));
        ColumnInfo resolvedBy = wrapColumn(_rootTable.getColumn("ResolvedBy"));
        UserIdForeignKey.initColumn(resolvedBy);
        addColumn(resolvedBy);
        addWrapColumn(_rootTable.getColumn("Resolved"));
        addWrapColumn(_rootTable.getColumn("Resolution"));
        addWrapColumn(_rootTable.getColumn("Duplicate"));
        ColumnInfo closedBy = wrapColumn(_rootTable.getColumn("ClosedBy"));
        UserIdForeignKey.initColumn(closedBy);
        addColumn(closedBy);
        addWrapColumn(_rootTable.getColumn("Closed"));
        addWrapColumn(_rootTable.getColumn("NotifyList"));
        // add any custom columns
        Map<String, String> customColumnCaptions = getCustomColumnCaptions(_schema.getContainer());
        for (Map.Entry<String, String> cce : customColumnCaptions.entrySet())
        {
            ColumnInfo realColumn = getRealTable().getColumn(cce.getKey());
            if (realColumn != null)
            {
                ColumnInfo column = new AliasedColumn(this, cce.getValue(), realColumn);
                column.setAlias(cce.getKey());
                if (getColumn(column.getName()) == null)
                    addColumn(column);
            }
        }
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
}
