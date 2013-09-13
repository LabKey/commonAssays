/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.ms2.Protein;

import java.util.Set;
import java.util.List;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class QueryAACoverageColumn extends PeptideAggregrationDisplayColumn
{
    private final ColumnInfo _sequenceColumn;

    public QueryAACoverageColumn(ColumnInfo sequenceColumn, ColumnInfo seqIdColumn, ColumnInfo peptideColumn)
    {
        super(seqIdColumn, peptideColumn, "AA Coverage");

        _sequenceColumn = sequenceColumn;

        // Don't set a format if we don't have all of the columns or we'll blow up trying to format the error string
        if (sequenceColumn != null && seqIdColumn != null && peptideColumn != null)
        {
            setFormatString("0.0%");
            setTsvFormatString("0.00");
        }
        setWidth("90");
        setTextAlign("right");
    }


    public ColumnInfo getColumnInfo()
    {
        return _sequenceColumn;
    }

    public Class getValueClass()
    {
        return Double.class;
    }

    protected Object calculateValue(RenderContext ctx, List<String> peptides)
            throws SQLException
    {
        Protein protein = new Protein();
        protein.setSequence(ctx.getResults().getString(_sequenceColumn.getAlias()));
        protein.setPeptides(peptides.toArray(new String[peptides.size()]));
        return protein.getAAPercent();
    }


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        super.addQueryColumns(set);
        set.add(_sequenceColumn);
    }
}
