package org.labkey.ms2.peptideview;

import jxl.write.WritableSheet;
import jxl.write.WriteException;
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
    protected void renderGridRow(WritableSheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws SQLException, WriteException, MaxRowsExceededException
    {
        ResultSet nestedRS = _groupedRS.getNextResultSet();
        try
        {
            int totalFilteredPeptides = 0;
            Set<String> uniqueFilteredPeptides = new HashSet<String>();

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
                while (nestedRS.next());
            }
        }
        finally
        {
            if (nestedRS != null) { try { nestedRS.close(); } catch (SQLException e) {} }
        }
    }
}
