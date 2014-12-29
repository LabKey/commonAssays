/*
 * Copyright (c) 2012-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.elisa.plate;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.labkey.api.study.assay.plate.ExcelPlateReader;

/**
 * User: klum
 * Date: 10/14/12
 */
public class BioTekPlateReader extends ExcelPlateReader
{
    public static final String LABEL = "BioTek";

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
