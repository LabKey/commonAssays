/*
 * Copyright (c) 2007 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;

import java.util.List;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class PeptideCountCoverageColumn extends PeptideAggregrationDisplayColumn
{
    public PeptideCountCoverageColumn(ColumnInfo colInfo, ColumnInfo peptideColumn, String caption)
    {
        super(colInfo, peptideColumn, caption);
    }

    public Class getValueClass()
    {
        return Integer.class;
    }

    protected Object calculateValue(RenderContext ctx, List<String> peptides) throws SQLException
    {
        return peptides.size();
    }
}
