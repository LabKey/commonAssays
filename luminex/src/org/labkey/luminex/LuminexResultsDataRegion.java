/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.luminex.query.LuminexDataTable;

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

    public LuminexResultsDataRegion(AssayProvider provider, ExpProtocol protocol)
    {
        super(provider, protocol);
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
