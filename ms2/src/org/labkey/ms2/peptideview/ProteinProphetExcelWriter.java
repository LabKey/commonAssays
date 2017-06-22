/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.poi.ss.usermodel.Sheet;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.ms2.TotalFilteredPeptidesColumn;
import org.labkey.ms2.UniqueFilteredPeptidesColumn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProteinProphetExcelWriter extends AbstractProteinExcelWriter
{
    public ProteinProphetExcelWriter()
    {
        super();
    }

    @Override
    protected void renderGridRow(Sheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws SQLException, MaxRowsExceededException
    {

        try (ResultSet nestedRS = _groupedRS.getNextResultSet())
        {
            int totalFilteredPeptides = 0;
            Set<String> uniqueFilteredPeptides = new HashSet<>();

            while (nestedRS.next())
            {
                totalFilteredPeptides++;
                uniqueFilteredPeptides.add(nestedRS.getString("Peptide"));
            }
            nestedRS.beforeFirst();
            ctx.put(TotalFilteredPeptidesColumn.NAME, totalFilteredPeptides);
            ctx.put(UniqueFilteredPeptidesColumn.NAME, uniqueFilteredPeptides.size());

            super.renderGridRow(sheet, ctx, columns);


            // If expanded, output the peptides
            if (_expanded)
            {
                _nestedExcelWriter.setCurrentRow(getCurrentRow());
                _nestedExcelWriter.renderGrid(sheet, nestedRS);
                setCurrentRow(_nestedExcelWriter.getCurrentRow());
            }
            else
            {
                // Burn the rest of the rows
                //noinspection StatementWithEmptyBody
                while (nestedRS.next());
            }
        }
    }
}
