package org.labkey.ms2.peptideview;

import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms2.TotalFilteredPeptidesColumn;
import org.labkey.ms2.UniqueFilteredPeptidesColumn;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetDataRegion extends AbstractProteinDataRegion
{
    public ProteinProphetDataRegion(ViewURLHelper urlHelper)
    {
        super("ProteinGroupId", urlHelper);
        setShadeAlternatingRows(true);
    }

    protected void renderTableRow(RenderContext ctx, Writer out, DisplayColumn[] renderers, int rowIndex) throws SQLException, IOException
    {
        _groupedRS.previous();
        ResultSet nestedRS = _groupedRS.getNextResultSet();

        int totalFilteredPeptides = 0;
        Set<String> uniqueFilteredPeptides = new HashSet<String>();
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

        super.renderTableRow(ctx, out, renderers, rowIndex);

        renderNestedGrid(out, ctx, nestedRS, rowIndex);
        nestedRS.close();
    }
}
