/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

package org.labkey.elispot.plate;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.reader.ExcelFactory;
import org.labkey.api.study.PlateTemplate;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 23, 2008
 */
public class ExcelPlateReader implements ElispotPlateReaderService.I
{
    public static final String TYPE = "xls";
    
    public String getType()
    {
        return TYPE;
    }

    public double[][] loadFile(PlateTemplate template, File dataFile) throws ExperimentException
    {
        String fileName = dataFile.getName().toLowerCase();
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx"))
            throw new ExperimentException("Unable to load data file: Invalid Format");

        Workbook workbook = null;
        try
        {
            workbook = ExcelFactory.create(dataFile);
        }
        catch (IOException e)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: " + e.getMessage(), e);
        }
        catch (InvalidFormatException e)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: " + e.getMessage(), e);
        }
        double[][] cellValues = new double[template.getRows()][template.getColumns()];

        Sheet plateSheet = workbook.getSheetAt(0);

        int startRow = -1;
        int startCol = -1;

        for (Row row : plateSheet)
        {
            startCol = getStartColumn(row);
            if (startCol != -1)
            {
                startRow = getStartRow(plateSheet, row.getRowNum());
                break;
            }
        }

        if (startRow == -1 || startCol == -1)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: unable to locate spot counts");
        }

        if ((template.getRows() + startRow > (plateSheet.getLastRowNum() + 1)) || (template.getColumns() + startCol > (plateSheet.getRow(startRow).getLastCellNum())))
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: expected " +
                    (template.getRows() + startRow) + " rows and " + (template.getColumns() + startCol) + " columns, but found "+
                    plateSheet.getLastRowNum() + 1 + " rows and " + plateSheet.getRow(startRow).getLastCellNum() + " columns.");
        }

        for (int row = 0; row < template.getRows(); row++)
        {
            for (int col = 0; col < template.getColumns(); col++)
            {
                Row plateRow = plateSheet.getRow(row + startRow);
                Cell cell = plateRow.getCell(col + startCol);
                cellValues[row][col] = cell.getNumericCellValue();
            }
        }
        return cellValues;
    }

    private static int getStartColumn(Row row)
    {
        for (Cell cell : row)
        {
            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && cell.getNumericCellValue() == 1)
            {
                for (int i=1; i < 12; i++)
                {
                    Cell c = row.getCell(cell.getColumnIndex() + i);
                    if (c != null)
                    {
                        if (c.getCellType() != Cell.CELL_TYPE_NUMERIC || c.getNumericCellValue() != (i + 1))
                            return -1;
                    }
                    else
                        return -1;
                }
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private static int getStartRow(Sheet sheet, int row)
    {
        while (row <= sheet.getLastRowNum())
        {
            Row sheetRow = sheet.getRow(row);
            if (sheetRow != null)
            {
                for (Cell cell : sheetRow)
                {
                    if (cell.getCellType() == Cell.CELL_TYPE_STRING && StringUtils.equalsIgnoreCase(cell.getStringCellValue(), "A"))
                    {
                        int col = cell.getColumnIndex();
                        char start = 'B';
                        for (int i=1; i < 8; i++)
                        {
                            String val = String.valueOf(start++);
                            Row r = sheet.getRow(row+i);
                            if (r != null)
                            {
                                Cell c = r.getCell(col);
                                if (c == null || c.getCellType() != Cell.CELL_TYPE_STRING || !StringUtils.equalsIgnoreCase(c.getStringCellValue(), val))
                                    return -1;
                            }
                            else
                                return -1;
                        }
                        return row;
                    }
                }
            }
            row++;
        }
        return -1;
    }
}
