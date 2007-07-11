package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms2.MS2Run;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;

/**
 * User: jeckels
* Date: Apr 9, 2007
*/
public abstract class QueryNestingOption
{
    private String _rowIdColumnName;
    private DataColumn _groupIdColumn;
    protected boolean _allowNesting;

    public QueryNestingOption(String rowIdColumnName, boolean allowNesting)
    {
        _rowIdColumnName = rowIdColumnName;
        _allowNesting = allowNesting;
    }

    public abstract int getResultSetRowLimit();
    public abstract int getOuterGroupLimit();

    public void setupGroupIdColumn(List<DisplayColumn> allColumns, List<DisplayColumn> outerColumns, TableInfo parentTable)
    {
        if (_groupIdColumn != null)
        {
            return;
        }
        Map<FieldKey, ColumnInfo> infos = QueryService.get().getColumns(parentTable, Collections.singleton(FieldKey.fromString(_rowIdColumnName)));
        for (ColumnInfo info : infos.values())
        {
            _groupIdColumn = new DataColumn(info);
            _groupIdColumn.setVisible(false);
            allColumns.add(_groupIdColumn);
            outerColumns.add(_groupIdColumn);
        }
    }

    public boolean isNested(List<DisplayColumn> columns)
    {
        boolean foundInner = false;
        boolean foundOuter = false;
        for (DisplayColumn column : columns)
        {
            if (isOuter(column))
            {
                foundOuter = true;
            }
            else
            {
                foundInner = true;
            }
        }
        return foundOuter && foundInner;
    }

    private boolean isOuter(DisplayColumn column)
    {
        ColumnInfo colInfo = column.getColumnInfo();
        return colInfo != null && isOuter(colInfo.getName());
    }

    public abstract boolean isOuter(String columnName);

    public String getRowIdColumnName()
    {
        return _rowIdColumnName;
    }

    public QueryPeptideDataRegion createDataRegion(List<DisplayColumn> originalColumns, MS2Run[] runs, ViewURLHelper url, String dataRegionName, boolean expanded)
    {
        List<DisplayColumn> innerColumns = new ArrayList<DisplayColumn>();
        List<DisplayColumn> outerColumns = new ArrayList<DisplayColumn>();
        List<DisplayColumn> allColumns = new ArrayList<DisplayColumn>(originalColumns);

        for (DisplayColumn column : originalColumns)
        {
            if (isOuter(column))
            {
                setupGroupIdColumn(allColumns, outerColumns, column.getColumnInfo().getParentTable());
                outerColumns.add(column);
            }
            else
            {
                innerColumns.add(column);
            }
        }

        QueryPeptideDataRegion ppRgn = new QueryPeptideDataRegion(allColumns, _groupIdColumn.getColumnInfo().getAlias(), url, getResultSetRowLimit(), getOuterGroupLimit());
        ppRgn.setExpanded(expanded);
        ppRgn.setRecordSelectorValueColumns(_groupIdColumn.getColumnInfo().getAlias());
        DataRegion nestedRgn = new DataRegion();
        nestedRgn.setName(dataRegionName);
        nestedRgn.setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);
        nestedRgn.setDisplayColumnList(innerColumns);
        ppRgn.setNestedRegion(nestedRgn);
        for (DisplayColumn column : outerColumns)
        {
            column.setCaption(column.getColumnInfo().getCaption());
        }
        ppRgn.setDisplayColumnList(outerColumns);

        return ppRgn;
    }
}
