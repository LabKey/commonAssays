/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
import org.labkey.ms2.MS2Run;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User: adam
 * Date: Aug 8, 2006
 * Time: 4:27:44 PM
 */
public class MultiRunRenderContext extends RenderContext
{
    List<MS2Run> _runs;

    public MultiRunRenderContext(ViewContext ctx, List<MS2Run> runs)
    {
        super(ctx);
        _runs = runs;
    }

    @Override
    protected ResultSet selectForDisplay(TableInfo table, List<ColumnInfo> columns, SimpleFilter filter, Sort sort, int maxRows, long offset, boolean async) throws SQLException
    {
        // XXX: we're ignoring offset for now
        return new MultiRunResultSet(_runs, table, columns, filter, sort, maxRows, getCache());
    }


    public static class MultiRunResultSet extends MS2ResultSet
    {
        private TableInfo _table;
        private List<ColumnInfo> _columns;
        private int _maxRows;
        private boolean _cache;

        MultiRunResultSet(List<MS2Run> runs, TableInfo table, List<ColumnInfo> columns, SimpleFilter filter, Sort sort, int maxRows, boolean cache)
        {
            super(runs, filter, sort);
            _table = table;
            _columns = columns;
            _maxRows = maxRows;
            _cache = cache;
        }


        ResultSet getNextResultSet() throws SQLException
        {
            ProteinManager.replaceRunCondition(_filter, null, _iter.next());
            // XXX: we're ignoring offset for now
            return Table.selectForDisplay(_table, _columns, _filter, _sort, _maxRows, 0, _cache);
        }
    }
}
