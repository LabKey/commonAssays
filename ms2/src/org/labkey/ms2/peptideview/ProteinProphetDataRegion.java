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

import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.AbstractNestableDataRegion;
import org.labkey.ms2.TotalFilteredPeptidesColumn;
import org.labkey.ms2.UniqueFilteredPeptidesColumn;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetDataRegion extends AbstractNestableDataRegion
{
    public ProteinProphetDataRegion(String url)
    {
        super("ProteinGroupId", url);
    }

    @Override
    protected void renderTableRow(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        _groupedRS.previous();
        ResultSet nestedRS = _groupedRS.getNextResultSet();

        try
        {
            int totalFilteredPeptides = 0;
            Set<String> uniqueFilteredPeptides = new HashSet<>();
            // Validate that the inner and outer result sets are sorted the same
            while (nestedRS.next())
            {
                if (!ctx.getRow().get("ProteinGroupId").equals(nestedRS.getInt("ProteinGroupId")))
                {
                    throw new IllegalArgumentException("ProteinGroup ids do not match for the outer and inner queries");
                }
                uniqueFilteredPeptides.add(nestedRS.getString("Peptide"));
                totalFilteredPeptides++;
            }
            nestedRS.beforeFirst();
            ctx.put(TotalFilteredPeptidesColumn.NAME, totalFilteredPeptides);
            ctx.put(UniqueFilteredPeptidesColumn.NAME, uniqueFilteredPeptides.size());

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
}
