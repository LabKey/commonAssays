package cpas.ms2.peptideview;

import org.labkey.api.data.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jxl.write.WritableSheet;
import jxl.write.WriteException;

public class ProteinProphetExcelWriter extends AbstractProteinExcelWriter
{
    public ProteinProphetExcelWriter()
    {
        super();
    }

    @Override
    protected void renderGridRow(WritableSheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws SQLException, WriteException, MaxRowsExceededException
    {
        super.renderGridRow(sheet, ctx, columns);

        // If expanded, output the peptides
        if (_expanded)
        {
            ResultSet nestedRS = _groupedRS.getNextResultSet();
            _nestedExcelWriter.setCurrentRow(getCurrentRow());
            _nestedExcelWriter.renderGrid(sheet, nestedRS);
            setCurrentRow(_nestedExcelWriter.getCurrentRow());
        }
    }
}
