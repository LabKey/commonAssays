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

package org.labkey.ms2.query;

import org.apache.commons.lang.ObjectUtils;
import org.labkey.api.data.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public abstract class PeptideAggregrationDisplayColumn extends SimpleDisplayColumn
{
    private ColumnInfo _groupingColumn;
    private ColumnInfo _peptideColumn;

    public PeptideAggregrationDisplayColumn(ColumnInfo groupingColumn, ColumnInfo peptideColumn, String caption)
    {
        _groupingColumn = groupingColumn;
        _peptideColumn = peptideColumn;
        setCaption(caption);
    }

    public ColumnInfo getColumnInfo()
    {
        return _groupingColumn;
    }

    public Object getValue(RenderContext ctx)
    {
        ResultSet originalRS = ctx.getResultSet();
        ResultSet rs = originalRS;

        boolean closeRS = false;
        try
        {
            if (originalRS instanceof GroupedResultSet)
            {
                rs = ((GroupedResultSet)originalRS).getNextResultSet();
                closeRS = true;
            }
            Object groupingValue = originalRS.getObject(_groupingColumn.getAlias());
            List<String> peptides = (List<String>)ctx.get("PeptideList");
            Object cachedGroupingValue = ctx.get("PeptideListGroupingValue");
            if (peptides == null || cachedGroupingValue == null || !cachedGroupingValue.equals(groupingValue))
            {
                peptides = new ArrayList<String>();

                peptides.add(originalRS.getString(_peptideColumn.getAlias()));
                int originalRow = originalRS.getRow();

                while (rs.next())
                {
                    if (ObjectUtils.equals(rs.getObject(_groupingColumn.getAlias()), groupingValue))
                    {
                        peptides.add(rs.getString(_peptideColumn.getAlias()));
                    }
                    else
                    {
                        break;
                    }
                }
                originalRS.absolute(originalRow);

                ctx.put("PeptideList", peptides);
                ctx.put("PeptideListGroupingValue", groupingValue);
            }

            return calculateValue(ctx, peptides);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (closeRS) { try { rs.close(); } catch (SQLException e) {} }
        }
    }

    protected abstract Object calculateValue(RenderContext ctx, List<String> peptides)
        throws SQLException;


    public boolean isFilterable()
    {
        return false;
    }

    public boolean isSortable()
    {
        return false;
    }

    public void addQueryColumns(Set<ColumnInfo> set)
    {
        super.addQueryColumns(set);
        set.add(_groupingColumn);
        set.add(_peptideColumn);
    }

    public abstract Class getValueClass();
}
