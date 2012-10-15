package org.labkey.elisa.plate;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.labkey.api.study.assay.plate.ExcelPlateReader;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/14/12
 */
public class BioTekPlateReader extends ExcelPlateReader
{
    @Override
    protected boolean isValidStartRow(Sheet sheet, int row)
    {
        boolean valid = super.isValidStartRow(sheet, row);

        if (valid)
        {
            // skip over any plate layout information, make sure cell values are numeric
            Row sheetRow = sheet.getRow(row);
            if (sheetRow != null)
            {
                for (Cell cell : sheetRow)
                {
                    if (cell.getCellType() == Cell.CELL_TYPE_STRING && StringUtils.equalsIgnoreCase(cell.getStringCellValue(), "A"))
                        continue;
                    else if (cell.getCellType() != Cell.CELL_TYPE_NUMERIC)
                        return false;
                }
            }
        }
        return valid;
    }
}
