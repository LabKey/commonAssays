/*
 * Copyright (c) 2006-2015 LabKey Corporation
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
import java.util.Collection;
import java.util.Map;

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
    protected Results selectForDisplay(TableInfo table, Collection<ColumnInfo> columns, Map<String,Object> parameters, SimpleFilter filter, Sort sort, int maxRows, long offset, boolean async) throws SQLException
    {
        // XXX: we're ignoring offset for now
        ResultSet rs = new MultiRunResultSet(_runs, table, columns, parameters, filter, sort, maxRows, getCache());
        return new ResultsImpl(rs, columns);
    }


    public static class MultiRunResultSet extends MS2ResultSet
    {
        private TableInfo _table;
        private Collection<ColumnInfo> _columns;
        private Map<String,Object> _parameters;
        private int _maxRows;
        private boolean _cache;

        MultiRunResultSet(List<MS2Run> runs, TableInfo table, Collection<ColumnInfo> columns, Map<String,Object> parameters, SimpleFilter filter, Sort sort, int maxRows, boolean cache)
        {
            super(runs, filter, sort);
            _table = table;
            _columns = columns;
            _parameters = parameters;
            _maxRows = maxRows;
            _cache = cache;

            init();
        }


        ResultSet getNextResultSet() throws SQLException
        {
            ProteinManager.replaceRunCondition(_filter, null, _iter.next());
            // XXX: we're ignoring offset for now
            //msi: We are using a scrollable resultset here for reasons I am not sure of...
            TableSelector selector = new TableSelector(_table, _columns, _filter, _sort).setForDisplay(true);
            selector.setMaxRows(_maxRows).setNamedParameters(_parameters);

            return selector.getResults(_cache, true);
        }
    }
}
