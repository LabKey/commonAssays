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
    private String _prefix;
    private String _rowIdColumnName;
    private DataColumn _groupIdColumn;

    public QueryNestingOption(String prefix, String rowIdColumnName)
    {
        _prefix = prefix;
        _rowIdColumnName = rowIdColumnName;
    }

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

    public boolean isNestable(List<DisplayColumn> columns)
    {
        for (DisplayColumn column : columns)
        {
            if (isNested(column))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isNested(DisplayColumn displayColumn)
    {
        ColumnInfo colInfo = displayColumn.getColumnInfo();
        return colInfo != null && colInfo.getName().toLowerCase().startsWith(_prefix.toLowerCase());
    }

    public String getRowIdColumnName()
    {
        return _rowIdColumnName;
    }

    public String getPrefix()
    {
        return _prefix;
    }

    public abstract void calculateValues();

    public QueryPeptideDataRegion createDataRegion(List<DisplayColumn> originalColumns, MS2Run[] runs, ViewURLHelper url, String dataRegionName)
    {
        List<DisplayColumn> innerColumns = new ArrayList<DisplayColumn>();
        List<DisplayColumn> outerColumns = new ArrayList<DisplayColumn>();
        List<DisplayColumn> allColumns = new ArrayList<DisplayColumn>(originalColumns);

        for (DisplayColumn column : originalColumns)
        {
            if (isNested(column))
            {
                setupGroupIdColumn(allColumns, outerColumns, column.getColumnInfo().getParentTable());
                outerColumns.add(column);
            }
            else
            {
                innerColumns.add(column);
            }
        }

        QueryPeptideDataRegion ppRgn = new QueryPeptideDataRegion(allColumns, _groupIdColumn.getColumnInfo().getAlias(), url);
//                ppRgn.setExpanded(_expanded);
        ppRgn.setExpanded(true);
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
