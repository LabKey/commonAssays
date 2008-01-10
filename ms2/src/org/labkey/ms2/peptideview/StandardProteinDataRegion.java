package org.labkey.ms2.peptideview;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.ms2.Protein;
import org.labkey.api.view.ActionURL;

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
public class StandardProteinDataRegion extends AbstractProteinDataRegion
{
    private int _peptideIndex = -1;

    public StandardProteinDataRegion(ActionURL url)
    {
        super("Protein", url);
        setShadeAlternatingRows(true);
    }

    protected void renderTableRow(RenderContext ctx, Writer out, DisplayColumn[] renderers, int rowIndex) throws SQLException, IOException
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
                List<String> peptides = new ArrayList<String>();
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

            super.renderTableRow(ctx, out, renderers, rowIndex);

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
