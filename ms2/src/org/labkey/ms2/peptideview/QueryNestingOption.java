/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.view.ActionURL;

import java.io.IOException;
import java.io.Writer;
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
    private String _aggregateRowIdColumnName;
    private DataColumn _groupIdColumn;
    protected boolean _allowNesting;

    public QueryNestingOption(String rowIdColumnName, boolean allowNesting)
    {
        this(rowIdColumnName, rowIdColumnName, allowNesting);
    }

    public QueryNestingOption(String rowIdColumnName, String aggregateRowIdColumnName, boolean allowNesting)
    {
        _rowIdColumnName = rowIdColumnName;
        _aggregateRowIdColumnName = aggregateRowIdColumnName;
        _allowNesting = allowNesting;
    }

    public void setupGroupIdColumn(List<DisplayColumn> allColumns, List<DisplayColumn> outerColumns, TableInfo parentTable)
    {
        if (_groupIdColumn != null)
        {
            return;
        }
        Map<FieldKey, ColumnInfo> infos = QueryService.get().getColumns(parentTable, Collections.singleton(FieldKey.fromString(_rowIdColumnName)));
        assert infos.size() == 1;

        ColumnInfo info = infos.values().iterator().next();
        _groupIdColumn = new DataColumn(info);
        _groupIdColumn.setVisible(false);
        allColumns.add(_groupIdColumn);
        outerColumns.add(_groupIdColumn);
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

    public String getAggregateRowIdColumnName()
    {
        return _aggregateRowIdColumnName;
    }

    public QueryPeptideDataRegion createDataRegion(List<DisplayColumn> originalColumns, ActionURL url, String dataRegionName, boolean expanded)
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

        QueryPeptideDataRegion dataRegion = new QueryPeptideDataRegion(allColumns, _groupIdColumn.getColumnInfo().getAlias(), url);
        // Set the nested button bar as not visible so that we don't render a bunch of nested <form>s which mess up IE.
        dataRegion.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY);
        dataRegion.setExpanded(expanded);
        dataRegion.setRecordSelectorValueColumns(_groupIdColumn.getColumnInfo().getAlias());
        DataRegion nestedRgn = new DataRegion()
        {
            @Override
            protected void renderHeaderScript(RenderContext ctx, Writer out,  Map<String, String> messages, boolean showRecordSelectors) throws IOException
            {
                // Issue 11405: customized grid does not work MS2 query based views.
                // Nested DataRegions don't need to re-render the "new LABKEY.DataRegion(...)" script.
            }
        };

        nestedRgn.setName(dataRegionName);
        ButtonBar bar = new ButtonBar();
        bar.setVisible(false);
        nestedRgn.setShowFilterDescription(false);
        nestedRgn.setButtonBar(bar);
        nestedRgn.setDisplayColumns(innerColumns);
        dataRegion.setNestedRegion(nestedRgn);
        for (DisplayColumn column : outerColumns)
        {
            column.setCaption(column.getColumnInfo().getLabel());
        }
        dataRegion.setDisplayColumns(outerColumns);

        return dataRegion;
    }
}
