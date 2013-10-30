/*
 * Copyright (c) 2010-2013 LabKey Corporation
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
package org.labkey.nab.multiplate;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.labkey.api.assay.dilution.SampleProperty;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.NabDataHandler;
import org.labkey.nab.NabManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: brittp
 * Date: Aug 27, 2010 11:07:33 AM
 */
public abstract class HighThroughputNabDataHandler extends NabDataHandler implements TransformDataHandler
{
    public static final String REPLICATE_GROUP_ORDER_PROPERTY = "Group Order";

    @Override
    protected String getPreferredDataFileExtension()
    {
        return "csv";
    }

    private static final String LOCATION_COLUMNN_HEADER = "Well Location";

    protected void throwWellLocationParseError(File dataFile, int lineNumber, Object locationValue) throws ExperimentException
    {
        throwParseError(dataFile, "Failed to find valid location in column \"" + LOCATION_COLUMNN_HEADER + "\" on line " + lineNumber +
                    ".  Locations should be identified by a single row letter and column number, such as " +
                    "A1 or P24.  Found \"" + (locationValue != null ? locationValue.toString() : "") + "\".");
    }

    protected Pair<Integer, Integer> getWellLocation(PlateTemplate template, File dataFile, Map<String, Object> line, int lineNumber) throws ExperimentException
    {
        Object locationValue = line.get(LOCATION_COLUMNN_HEADER);
        if (locationValue == null || !(locationValue instanceof String) || ((String) locationValue).length() < 2)
            throwWellLocationParseError(dataFile, lineNumber, locationValue);
        String location = (String) locationValue;
        Character rowChar = location.charAt(0);
        rowChar = Character.toUpperCase(rowChar);
        if (!(rowChar >= 'A' && rowChar <= 'Z'))
            throwWellLocationParseError(dataFile, lineNumber, locationValue);

        Integer col;
        try
        {
            col = Integer.parseInt(location.substring(1));
        }
        catch (NumberFormatException e)
        {
            throwWellLocationParseError(dataFile, lineNumber, locationValue);
            // return to suppress intellij warnings (line will never be reached)
            return null;
        }
        int row = rowChar - 'A' + 1;

        // 1-based row and column indexing:
        if (row > template.getRows())
        {
            throwParseError(dataFile, "Invalid row " + row + " specified on line " + lineNumber +
                    ".  The current plate template defines " + template.getRows() + " rows.");
        }

        // 1-based row and column indexing:
        if (col > template.getColumns())
        {
            throwParseError(dataFile, "Invalid column " + col + " specified on line " + lineNumber +
                    ".  The current plate template defines " + template.getColumns() + " columns.");
        }
        return new Pair<>(row, col);
    }

    @Override
    protected List<Plate> createPlates(File dataFile, PlateTemplate template) throws ExperimentException
    {
        DataLoader loader;
        try
        {
            if (dataFile.getName().toLowerCase().endsWith(".csv"))
            {
                loader = new TabLoader(dataFile, true);
                ((TabLoader) loader).parseAsCSV();
            }
            else
                loader = new ExcelLoader(dataFile, true);

            int wellsPerPlate = template.getRows() * template.getColumns();

            ColumnDescriptor[] columns = loader.getColumns();
            if (columns == null || columns.length == 0)
            {
                throwParseError(dataFile, "No columns found in data file.");
                // return to suppress intellij warnings (line above will always throw):
                return null;
            }

            // The results column is defined as the last column in the file for this file format:
            String resultColumnHeader = columns[columns.length - 1].name;

            int wellCount = 0;
            int plateCount = 0;
            double[][] wellValues = new double[template.getRows()][template.getColumns()];
            List<Plate> plates = new ArrayList<>();
            for (Map<String, Object> rowData : loader)
            {
                // Current line in the data file is calculated by the number of wells we've already read,
                // plus one for the current row, plus one for the header row:
                int line = plateCount * wellsPerPlate + wellCount + 2;
                Pair<Integer, Integer> location = getWellLocation(template, dataFile, rowData, line);
                int plateRow = location.getKey();
                int plateCol = location.getValue();

                Object dataValue = rowData.get(resultColumnHeader);
                if (dataValue == null || !(dataValue instanceof Integer))
                {
                    throwParseError(dataFile, "No valid result value found on line " + line + ".  Expected integer " +
                            "result values in the last data file column (\"" + resultColumnHeader + "\") found: " + dataValue);
                    return null;
                }

                wellValues[plateRow - 1][plateCol - 1] = (Integer) dataValue;
                if (++wellCount == wellsPerPlate)
                {
                    plates.add(PlateService.get().createPlate(template, wellValues));
                    plateCount++;
                    wellCount = 0;
                }
            }
            if (wellCount != 0)
            {
                throwParseError(dataFile, "Expected well data in multiples of " + wellsPerPlate + ".  The file provided included " +
                        plateCount + " complete plates of data, plus " + wellCount + " extra rows.");
            }
            return plates;
        }
        catch (IOException e)
        {
            throwParseError(dataFile, null, e);
            return null;
        }
    }

    @Override
    protected boolean isDilutionDownOrRight()
    {
        return true;
    }

    @Override
    protected void prepareWellGroups(List<WellGroup> groups, ExpMaterial sampleInput, Map<String, DomainProperty> properties) throws ExperimentException
    {
        List<WellData> wells = new ArrayList<>();
        // All well groups use the same plate template, so it's okay to just check the dilution direction of the first group:
        boolean reverseDirection = Boolean.parseBoolean((String) groups.get(0).getProperty(SampleProperty.ReverseDilutionDirection.name()));
        for (WellGroup group : groups)
        {
            for (DomainProperty property : properties.values())
                group.setProperty(property.getName(), sampleInput.getProperty(property));

            boolean hasExplicitOrder = true;
            List<? extends WellData> wellData = group.getWellData(true);
            for (WellData well : wellData)
            {
                if (well instanceof WellGroup)
                {
                    // it's possible to override the natural ordering of the replicate well groups by adding a replicate
                    // well group property : 'Group Order' with a numeric value in the plate template
                    String order = (String)((WellGroupTemplate)well).getProperty(REPLICATE_GROUP_ORDER_PROPERTY);
                    if (!NumberUtils.isDigits(order))
                    {
                        hasExplicitOrder = false;
                        break;
                    }
                }
                else
                {
                    hasExplicitOrder = false;
                    break;
                }
            }

            if (hasExplicitOrder)
            {
                Collections.sort(wellData, new Comparator<WellData>()
                {
                    @Override
                    public int compare(WellData w1, WellData w2)
                    {
                        if ((w1 instanceof WellGroupTemplate) && (w2 instanceof WellGroupTemplate))
                        {
                            String order1 = (String)((WellGroupTemplate)w1).getProperty(REPLICATE_GROUP_ORDER_PROPERTY);
                            String order2 = (String)((WellGroupTemplate)w2).getProperty(REPLICATE_GROUP_ORDER_PROPERTY);

                            return NumberUtils.toInt(order1, 0) - NumberUtils.toInt(order2, 0);
                        }
                        return 0;
                    }
                });

            }
            wells.addAll(wellData);
        }

        applyDilution(wells, sampleInput, properties, reverseDirection);
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        DilutionDataFileParser parser = getDataFileParser(data, dataFile, info);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
        datas.put(NAB_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    @Override
    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importRows(data, run, context.getProtocol(), dataMap);
    }
}
