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
        Set<String> uniqueFilteredPeptides = new HashSet<String>();
        while (nestedRS.next())
        {
            totalFilteredPeptides++;
            uniqueFilteredPeptides.add(nestedRS.getString("Peptide"));
        }
        ctx.put(TotalFilteredPeptidesColumn.NAME, totalFilteredPeptides);
        ctx.put(UniqueFilteredPeptidesColumn.NAME, uniqueFilteredPeptides.size());
    }
}
