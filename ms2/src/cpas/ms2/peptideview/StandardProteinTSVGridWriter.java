package cpas.ms2.peptideview;

import org.apache.log4j.Logger;
import org.fhcrc.cpas.data.DisplayColumn;
import org.fhcrc.cpas.data.RenderContext;
import org.fhcrc.cpas.ms2.Protein;

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


    protected void writeExpandedRow(PrintWriter out, RenderContext ctx, List<DisplayColumn> displayColumns, ResultSet nestedRS) throws SQLException
    {
        addAACoverage(ctx, nestedRS);
        nestedRS.beforeFirst();

        super.writeExpandedRow(out, ctx, displayColumns, nestedRS);
    }


    protected void writeCollapsedRow(PrintWriter out, RenderContext ctx, List<DisplayColumn> displayColumns) throws SQLException
    {
        ResultSet nestedRS = _groupedRS.getNextResultSet();
        addAACoverage(ctx, nestedRS);
        nestedRS.close();

        super.writeCollapsedRow(out, ctx, displayColumns);
    }


    private void addAACoverage(RenderContext ctx, ResultSet nestedRS) throws SQLException
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
