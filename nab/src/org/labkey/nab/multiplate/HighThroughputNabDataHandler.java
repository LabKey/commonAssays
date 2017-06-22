/*
 * Copyright (c) 2010-2017 LabKey Corporation
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
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.SampleProperty;
import org.labkey.api.assay.dilution.WellDataRow;
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
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabDataHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    protected static final String LOCATION_COLUMNN_HEADER = "Well Location";

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

            final int expectedRows = template.getRows();
            final int expectedCols = template.getColumns();

            List<double[][]> matrices = parse(dataFile, loader.getColumns(), loader.load(), expectedRows, expectedCols);
            if (matrices == null || matrices.size() == 0)
                throw createParseError(dataFile, "No plate data found");

            int plateNumber = 1;
            List<Plate> plates = new ArrayList<>(matrices.size());
            for (double[][] matrix : matrices)
                plates.add(PlateService.get().createPlate(template, matrix, null, PlateService.NO_RUNID, plateNumber++));

            return plates;
        }
        catch (IOException e)
        {
            throw createParseError(dataFile, null, e);
        }
    }

    @Override
    protected List<Plate> createPlates(ExpRun run, PlateTemplate template, boolean recalcStats) throws ExperimentException
    {
        List<WellDataRow> wellDataRows = DilutionManager.getWellDataRows(run);
        if (wellDataRows.isEmpty())
            throw new ExperimentException("Well data could not be found for run " + run.getName() + ". Run details are not available.");

        Map<Integer, double[][]> matrices = new HashMap<>();
        Map<Integer, boolean[][]> exclusions = new HashMap<>();
        Map<Integer, String> plateToVirusMap = new HashMap<>();
        for (WellDataRow wellDataRow : wellDataRows)
        {
            Integer plateNum = wellDataRow.getPlateNumber();
            if (null == plateNum)
                throw new IllegalStateException("WellData plate number should not be null.");
            if (!matrices.containsKey(plateNum))
                matrices.put(plateNum, new double[template.getRows()][template.getColumns()]);
            if (!exclusions.containsKey(plateNum))
                exclusions.put(plateNum, new boolean[template.getRows()][template.getColumns()]);
            double[][] cellValues = matrices.get(plateNum);
            boolean[][] excluded = exclusions.get(plateNum);

            cellValues[wellDataRow.getRow()][wellDataRow.getColumn()] = wellDataRow.getValue();
            excluded[wellDataRow.getRow()][wellDataRow.getColumn()] = wellDataRow.isExcluded();
            plateToVirusMap.put(plateNum, wellDataRow.getPlateVirusName());
        }

        List<Plate> plates = new ArrayList<>(matrices.size());
        for (Map.Entry<Integer, double[][]> matrix : matrices.entrySet())
        {
            Plate plate = PlateService.get().createPlate(template, matrix.getValue(), exclusions.get(matrix.getKey()), recalcStats ? PlateService.NO_RUNID : run.getRowId(), matrix.getKey());
            plate.setProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME, plateToVirusMap.get(matrix.getKey()));
            plates.add(plate);
        }
        return plates;
    }

    protected double[][] getCellValues(final File dataFile, PlateTemplate nabTemplate) throws ExperimentException
    {
        throw new IllegalStateException("getCellValues should not be called for High Throughput handlers.");
    }

    protected List<double[][]> parse(File dataFile, ColumnDescriptor[] columns, List<Map<String, Object>> rows, int expectedRows, int expectedCols) throws ExperimentException
    {
        // attempt to parse list-style data
        if (columns != null && columns.length > 0)
        {
            // Last column is the results column -- only attempt parsing if it has a non-default name, e.g. "column5"
            String resultColumnHeader = columns[columns.length-1].name;
            if (!resultColumnHeader.equals("column" + (columns.length-1)))
            {
                List<ExperimentException> errors = new ArrayList<>();
                List<double[][]> values = parseList(dataFile, rows, LOCATION_COLUMNN_HEADER, resultColumnHeader, 0, expectedRows, expectedCols, errors);
                if (!errors.isEmpty())
                    throw errors.get(0);
                if (values != null && !values.isEmpty())
                    return values;
            }
        }

        return null;
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
                wellData.sort((Comparator<WellData>) (w1, w2) ->
                {
                    if ((w1 instanceof WellGroupTemplate) && (w2 instanceof WellGroupTemplate))
                    {
                        String order1 = (String) ((WellGroupTemplate) w1).getProperty(REPLICATE_GROUP_ORDER_PROPERTY);
                        String order2 = (String) ((WellGroupTemplate) w2).getProperty(REPLICATE_GROUP_ORDER_PROPERTY);

                        return NumberUtils.toInt(order1, 0) - NumberUtils.toInt(order2, 0);
                    }
                    return 0;
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
        importRows(data, run, context.getProtocol(), dataMap, context.getUser());
    }
}
