/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.query.AbstractNestableDataRegion;
import org.labkey.ms2.Protein;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class StandardProteinDataRegion extends AbstractNestableDataRegion
{
    private int _peptideIndex = -1;

    public StandardProteinDataRegion(String url)
    {
        super("Protein", url);
    }

    @Override
    protected void renderTableRow(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        Protein protein = new Protein();

        protein.setSequence((String) ctx.get("Sequence"));
        Integer outerSeqId = (Integer)ctx.getRow().get("SeqId");
        ResultSet nestedRS = null;
        try
        {
            nestedRS = _groupedRS.getNextResultSet();

            if (outerSeqId != null)
            {
                List<String> peptides = new ArrayList<>();
                while (nestedRS.next())
                {
                    peptides.add(nestedRS.getString(getPeptideIndex()));
                }

                // Back up to the first peptide in this group
                nestedRS.beforeFirst();

                String[] peptideArray = new String[peptides.size()];
                protein.setPeptides(peptides.toArray(peptideArray));

                // Calculate amino acid coverage and add to the rowMap for AACoverageColumn to see
                ctx.put("AACoverage", protein.getAAPercent());
            }
            else
            {
                ctx.put("AACoverage", -1.0);
            }

            super.renderTableRow(ctx, out, showRecordSelectors, renderers, rowIndex);

            renderNestedGrid(out, ctx, nestedRS, rowIndex);
        }
        finally
        {
            if (nestedRS != null)
            {
                try { nestedRS.close(); } catch (SQLException e) {}
            }
        }
    }

    private int getPeptideIndex() throws SQLException
    {
        if (_peptideIndex == -1)
        {
            _peptideIndex = _groupedRS.findColumn("Peptide");   // Cache peptide column index
        }
        return _peptideIndex;
    }
}
