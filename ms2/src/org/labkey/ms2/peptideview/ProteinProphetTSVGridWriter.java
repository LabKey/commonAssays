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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.ms2.TotalFilteredPeptidesColumn;
import org.labkey.ms2.UniqueFilteredPeptidesColumn;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Oct 25, 2007
 */
public class ProteinProphetTSVGridWriter extends ProteinTSVGridWriter
{
    public ProteinProphetTSVGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        super(proteinDisplayColumns, peptideDisplayColumns);
    }

    protected void addCalculatedValues(RenderContext ctx, ResultSet nestedRS) throws SQLException
    {
        int totalFilteredPeptides = 0;
        Set<String> uniqueFilteredPeptides = new HashSet<>();
        while (nestedRS.next())
        {
            totalFilteredPeptides++;
            uniqueFilteredPeptides.add(nestedRS.getString("Peptide"));
        }
        ctx.put(TotalFilteredPeptidesColumn.NAME, totalFilteredPeptides);
        ctx.put(UniqueFilteredPeptidesColumn.NAME, uniqueFilteredPeptides.size());
    }
}
