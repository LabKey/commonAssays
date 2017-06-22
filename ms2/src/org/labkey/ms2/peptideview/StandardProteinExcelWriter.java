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
import org.labkey.api.data.*;
import org.labkey.ms2.Protein;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class StandardProteinExcelWriter extends AbstractProteinExcelWriter
{
    protected int _peptideIndex = -1;

    public StandardProteinExcelWriter()
    {
        super();
    }

    @Override
    protected void renderGridRow(Sheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws SQLException, MaxRowsExceededException
    {
        Protein protein = new Protein();

        protein.setSequence((String) ctx.get("Sequence"));

        List<String> peptides = new ArrayList<>();
        try (ResultSet nestedRS =  _groupedRS.getNextResultSet())
        {
            while (nestedRS.next())
                peptides.add(nestedRS.getString(getPeptideIndex()));

            // If expanded view, back up to the first peptide in this group
            if (_expanded)
                nestedRS.beforeFirst();

            String[] peptideArray = new String[peptides.size()];
            protein.setPeptides(peptides.toArray(peptideArray));

            // Calculate amino acid coverage and add to the rowMap for AACoverageColumn to see
            ctx.put("AACoverage", protein.getAAPercent());

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

    private int getPeptideIndex() throws SQLException
    {
        if (_peptideIndex == -1)
        {
            _peptideIndex = _groupedRS.findColumn("Peptide");
        }
        return _peptideIndex;
    }
}
