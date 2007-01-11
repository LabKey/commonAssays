package cpas.ms2.peptideview;

import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.ExcelColumn;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractProteinExcelWriter extends ExcelWriter
{
    protected boolean _expanded = false;
    protected ExcelWriter _nestedExcelWriter = null;
    protected GroupedResultSet _groupedRS = null;

    @Override
    public void renderGrid(WritableSheet sheet, List<ExcelColumn> columns, ResultSet rs) throws SQLException, WriteException, MaxRowsExceededException
    {
        super.renderGrid(sheet, columns, rs);

        if (null != _groupedRS)
            _groupedRS.close();
    }

    public void setExpanded(boolean expanded)
    {
        _expanded = expanded;
    }

    public void setExcelWriter(ExcelWriter nestedExcelWriter)
    {
        _nestedExcelWriter = nestedExcelWriter;
    }

    public void setGroupedResultSet(GroupedResultSet groupedRS) throws SQLException
    {
        _groupedRS = groupedRS;
    }
}
