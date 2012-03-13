/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.nab;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.reader.ExcelFactory;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.util.FileType;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 3:21:18 PM
 */
public class SinglePlateNabDataHandler extends NabDataHandler implements TransformDataHandler
{
    public static final DataType NAB_TRANSFORMED_DATA_TYPE = new DataType("AssayRunNabTransformedData"); // a marker data type

    public static final AssayDataType NAB_DATA_TYPE = new AssayDataType("AssayRunNabData", new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));
    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;

    @Override
    protected String getPreferredDataFileExtension()
    {
        return "xls";
    }

    @Override
    protected DataType getDataType()
    {
        return NAB_DATA_TYPE;
    }

    @Override
    protected List<Plate> createPlates(File dataFile, PlateTemplate template) throws ExperimentException
    {
        double[][] cellValues = getCellValues(dataFile, template);
        Plate plate = PlateService.get().createPlate(template, cellValues);
        return Collections.singletonList(plate);
    }

    @Override
    protected boolean isDilutionDownOrRight()
    {
        return false;
    }

    @Override
    protected Map<ExpMaterial, List<WellGroup>> getMaterialWellGroupMapping(NabAssayProvider provider, List<Plate> plates, Collection<ExpMaterial> sampleInputs)
    {
        Plate plate = plates.get(0);
        List<? extends WellGroup> wellgroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
        Map<String, ExpMaterial> nameToMaterial = new HashMap<String, ExpMaterial>();
        for (ExpMaterial material : sampleInputs)
            nameToMaterial.put(material.getName(), material);

        Map<ExpMaterial, List<WellGroup>> mapping = new HashMap<ExpMaterial, List<WellGroup>>();
        for (WellGroup wellgroup : wellgroups)
        {
            ExpMaterial material = nameToMaterial.get(wellgroup.getName());
            if (material == null)
                throw new IllegalStateException("Each wellgroup should have a matching input material.");
            mapping.put(material, Collections.singletonList(wellgroup));
        }
        return mapping;
    }

    @Override
    protected NabAssayRun createNabAssayRun(NabAssayProvider provider, ExpRun run, List<Plate> plates, User user, List<Integer> sortedCutoffs, DilutionCurve.FitType fit)
    {
        return new SinglePlateNabAssayRun(provider, run, plates.get(0), user, sortedCutoffs, fit);
    }

    private double[][] getCellValues(File dataFile, PlateTemplate nabTemplate) throws ExperimentException
    {
        Workbook workbook = null;

        try
        {
            workbook = ExcelFactory.create(dataFile);
        }
        catch (IOException e)
        {
            throwParseError(dataFile, null, e);
        }
        catch (InvalidFormatException e)
        {
            throwParseError(dataFile, null, e);
        }
        double[][] cellValues = new double[nabTemplate.getRows()][nabTemplate.getColumns()];

        Sheet plateSheet = null;
        Pair<Integer, Integer> dataLocation = null;

        // search the workbook for a region that contains 96 cells of data labeled with A-H rows and 1-12 cols:
        for (int sheet = 0; sheet < workbook.getNumberOfSheets() && dataLocation == null; sheet++)
        {
            plateSheet = workbook.getSheetAt(sheet);
            dataLocation = getPlateDataLocation(plateSheet, nabTemplate.getRows(), nabTemplate.getColumns());
        }

        int startRow;
        int startColumn;
        if (dataLocation == null)
        {
            // if we couldn't find a labeled grid of plate data, we'll assume the default location at START_ROW/START_COL
            // within the second worksheet:
            startRow = START_ROW;
            startColumn = START_COL;
            if (workbook.getNumberOfSheets() < 2)
                throwParseError(dataFile, dataFile.getName() + " does not appear to be a valid data file: no plate data was found.");
            plateSheet = workbook.getSheetAt(1);
        }
        else
        {
            startRow = dataLocation.getKey().intValue();
            startColumn = dataLocation.getValue().intValue();
        }

        if ((nabTemplate.getRows() + startRow > (plateSheet.getLastRowNum() + 1)) || (nabTemplate.getColumns() + startColumn > (plateSheet.getRow(startRow).getLastCellNum() + 1)))
        {
            Row firstRow = plateSheet.getRow(startRow);
            int colCount = firstRow != null ? firstRow.getLastCellNum() : -1;
            throwParseError(dataFile, dataFile.getName() + " does not appear to be a valid data file: expected " +
                    (nabTemplate.getRows() + startRow) + " rows and " + (nabTemplate.getColumns() + startColumn) + " columns, but found "+
                    (plateSheet.getLastRowNum() + 1) + " rows and " + (colCount > 0 ? colCount : "no populated") + " columns.");
        }

        for (int row = 0; row < nabTemplate.getRows(); row++)
        {
            for (int col = 0; col < nabTemplate.getColumns(); col++)
            {
                Row currentRow = plateSheet.getRow(row + startRow);
                Cell cell = currentRow.getCell(col + startColumn);
                try
                {
                    if (ExcelFactory.isCellNumeric(cell))
                        cellValues[row][col] = cell.getNumericCellValue();
                    else
                        cellValues[row][col] = 0.0;
                }
                catch (NumberFormatException e)
                {
                    throwParseError(dataFile, dataFile.getName() + " does not appear to be a valid data file: could not parse '" +
                            cell.getStringCellValue() + "' as a number.", e);
                }
            }
        }
        return cellValues;
    }

    @Override
    protected void prepareWellGroups(List<WellGroup> groups, ExpMaterial sampleInput, Map<String, DomainProperty> properties)
    {
        if (groups.size() != 1)
            throw new IllegalStateException("Expected exactly 1 well group per material for single-plate NAb runs.  Found " + groups.size());
        WellGroup group = groups.get(0);
        for (DomainProperty property : properties.values())
            group.setProperty(property.getName(), sampleInput.getProperty(property));

        List<? extends WellData> wells = group.getWellData(true);
        boolean reverseDirection = Boolean.parseBoolean((String) group.getProperty(NabManager.SampleProperty.ReverseDilutionDirection.name()));
        applyDilution(wells, sampleInput, properties, reverseDirection);
    }

    private Pair<Integer, Integer> getPlateDataLocation(Sheet plateSheet, int plateHeight, int plateWidth)
    {
        for (Row currentRow : plateSheet)
        {
            if (currentRow.getRowNum() + plateHeight > plateSheet.getLastRowNum())
                return null;

            for (Cell cell : currentRow)
            {
                if (isPlateMatrix(plateSheet, currentRow.getRowNum(), cell.getColumnIndex(), plateHeight, plateWidth))
                {
                    // add one to row and col, since (row,col) is the index of the data grid
                    // where the first row is column labels and the first column is row labels.
                    return new Pair<Integer, Integer>(currentRow.getRowNum() + 1, cell.getColumnIndex() + 1);
                }
            }
        }
        return null;
    }

    private boolean isPlateMatrix(Sheet plateSheet, int startRow, int startCol, int plateHeight, int plateWidth)
    {
        Row row = plateSheet.getRow(startRow);
        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startCol + plateWidth > row.getLastCellNum())
            return false;

        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startRow + plateHeight > plateSheet.getLastRowNum())
            return false;

        // check for 1-12 in the row:
        for (int colIndex = startCol + 1; colIndex < startCol + plateWidth + 1; colIndex++)
        {
            Cell current = row.getCell(colIndex);
            if (current != null)
            {
                String indexString = String.valueOf(colIndex - startCol);
                if (!StringUtils.equals(ExcelFactory.getCellStringValue(current), indexString))
                    return false;
            }
        }

        char start = 'A';
        for (int rowIndex = startRow + 1; rowIndex < startRow + plateHeight + 1; rowIndex++)
        {
            Row currentRow = plateSheet.getRow(rowIndex);
            if (currentRow != null)
            {
                Cell current = currentRow.getCell(startCol);
                if (current != null)
                {
                    String indexString = String.valueOf(start++);
                    if (!StringUtils.equals(ExcelFactory.getCellStringValue(current), indexString))
                        return false;
                }
            }
        }
        return true;
    }

    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importRows(data, run, context.getProtocol(), dataMap);
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        NabDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        datas.put(NAB_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
