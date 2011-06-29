package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.query.ResultsQueryView;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Jun 28, 2011
 */
public class LuminexResultsDataRegion extends ResultsQueryView.ResultsDataRegion
{
    private ColumnInfo _excludedColumn;

    public LuminexResultsDataRegion(AssayProvider provider)
    {
        super(provider);
    }

    @Override
    protected boolean isErrorRow(RenderContext ctx, int rowIndex)
    {
        // Check if it's been excluded
        return super.isErrorRow(ctx, rowIndex) ||
                _excludedColumn != null && Boolean.TRUE.equals(_excludedColumn.getValue(ctx));
    }

    @Override
    public void addQueryColumns(Set<ColumnInfo> columns)
    {
        super.addQueryColumns(columns);
        FieldKey fk = new FieldKey(null, LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME);
        Map<FieldKey, ColumnInfo> newColumns = QueryService.get().getColumns(getTable(), Collections.singleton(fk), columns);
        _excludedColumn = newColumns.get(fk);
        if (_excludedColumn != null)
        {
            // Add the column that indicates if the well is excluded
            columns.add(_excludedColumn);
        }
    }
    
}
