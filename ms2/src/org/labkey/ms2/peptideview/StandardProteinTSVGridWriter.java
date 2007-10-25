package org.labkey.ms2.peptideview;

import org.apache.log4j.Logger;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.ms2.Protein;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: adam
 * Date: Sep 6, 2006
 */
public class StandardProteinTSVGridWriter extends ProteinTSVGridWriter
{
    private static Logger _log = Logger.getLogger(StandardProteinTSVGridWriter.class);

    protected int _peptideIndex = -1;

    public StandardProteinTSVGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        super(proteinDisplayColumns, peptideDisplayColumns);
    }

    protected void addCalculatedValues(RenderContext ctx, ResultSet nestedRS) throws SQLException
    {
        Protein protein = new Protein();

        protein.setSequence((String) ctx.get("Sequence"));

        List<String> peptides = new ArrayList<String>();

        while (nestedRS.next())
            peptides.add(nestedRS.getString(getPeptideIndex()));

        String[] peptideArray = new String[peptides.size()];
        protein.setPeptides(peptides.toArray(peptideArray));

        // Calculate amino acid coverage and add to the context for AACoverageColumn to see
        ctx.put("AACoverage", protein.getAAPercent());
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
