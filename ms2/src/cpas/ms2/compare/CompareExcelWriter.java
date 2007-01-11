package cpas.ms2.compare;

import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.DisplayColumn;

import java.util.List;
import java.sql.ResultSet;

import jxl.write.*;

/**
 * User: adam
 * Date: Jul 12, 2006
 * Time: 5:53:56 PM
 */
public class CompareExcelWriter extends ExcelWriter
{
    private List<String> _multiColumnCaptions;
    private int _offset = 0;
    private int _colSpan;

    public CompareExcelWriter(ResultSet rs, List<DisplayColumn> displayColumns)
    {
        super(rs, displayColumns);
    }

    public void setMultiColumnCaptions(List<String> multiColumnCaptions)
    {
        _multiColumnCaptions = multiColumnCaptions;
    }

    public void setColSpan(int colSpan)
    {
        _colSpan = colSpan;
    }

    public void setOffset(int offset)
    {
        _offset = offset;
    }

    @Override
    public void renderColumnCaptions(WritableSheet sheet, List<ExcelColumn> visibleColumns) throws WriteException, MaxRowsExceededException
    {
        int column = _offset;

        for (String caption : _multiColumnCaptions)
        {
            sheet.addCell(new Label(column, getCurrentRow(), caption, getBoldFormat()));
            column += _colSpan;
        }

        incrementRow();

        super.renderColumnCaptions(sheet, visibleColumns);
    }
}


