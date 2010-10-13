package org.labkey.nab;

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.FileType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2010 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Aug 27, 2010 11:07:33 AM
 */
public class HighThroughputNabDataHandler extends NabDataHandler
{
    public static final AssayDataType NAB_HIGH_THROUGHPUT_DATA_TYPE = new AssayDataType("HighThroughputAssayRunNabData", new FileType(".csv"));

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_HIGH_THROUGHPUT_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    @Override
    protected String getPreferredDataFileExtension()
    {
        return "csv";
    }

    @Override
    protected DataType getDataType()
    {
        return NAB_HIGH_THROUGHPUT_DATA_TYPE;
    }
    private static final String RESULT_COLUMNN_HEADER = "Measure";
    private static final String ROW_COLUMNN_HEADER = "Row";
    private static final String COLUMN_COLUMNN_HEADER = "Column";

    private int getPlateRow(File dataFile, Map<String, Object> rowData, int line, int max) throws ExperimentException
    {
        Object rowValue = rowData.get(ROW_COLUMNN_HEADER);
        int row;
        if (rowValue instanceof Integer)
        {
            row = (Integer) rowValue;
        }
        else if (rowValue instanceof String && ((String) rowValue).length() == 1)
        {
            char rowchar = ((String) rowValue).charAt(0);
            if (Character.isUpperCase(rowchar))
                row = rowchar - 'A' + 1;
            else
                row = rowchar - 'a' + 1;
        }
        else
        {
            throwParseError(dataFile, "No valid plate row specified for line " + line + ".  Expected an integer " +
                    " or single letter value in a column with header \"" + ROW_COLUMNN_HEADER + "\", found: " + rowValue);
            return -1;
        }
        if (row <= 0 || row > max)
        {
            throwParseError(dataFile, "Row " + row + " is not valid for the current plate template.  " + max +
                    " rows are expected per plate.  Error on line " + line + ".");
        }
        return row;
    }

    @Override
    protected List<Plate> createPlates(File dataFile, PlateTemplate template) throws ExperimentException
    {
        TabLoader loader = null;
        try
        {
            loader = new TabLoader(dataFile, true);
            loader.parseAsCSV();
            int wellsPerPlate = template.getRows() * template.getColumns();

            int wellCount = 0;
            int plateCount = 0;
            double[][] wellValues = new double[template.getRows()][template.getColumns()];
            List<Plate> plates = new ArrayList<Plate>();
            for (Map<String, Object> rowData : loader)
            {
                // Current line in the data file is calculated by the number of wells we've already read,
                // plus one for the current row, plus one for the header row:
                int line = plateCount * wellsPerPlate + wellCount + 2;
                int plateRow = getPlateRow(dataFile, rowData, line, template.getRows());
                Object colValue = rowData.get(COLUMN_COLUMNN_HEADER);
                if (colValue == null || !(colValue instanceof Integer))
                {
                    throwParseError(dataFile, "No valid plate column specified for line " + line + ".  Expected an integer " +
                            "value in a column with header \"" + COLUMN_COLUMNN_HEADER + "\", found: " + colValue);
                }
                int plateCol = (Integer) colValue;
                if (plateCol <= 0 || plateCol > template.getColumns())
                {
                    throwParseError(dataFile, "Column " + plateCol + " is not valid for the current plate template.  " + template.getColumns() +
                            " columns are expected per plate.  Error on line " + line + ".");

                }
                Object dataValue = rowData.get(RESULT_COLUMNN_HEADER);
                if (dataValue == null || !(dataValue instanceof Integer))
                {
                    throwParseError(dataFile, "No valid result value specified for line " + line + ".  Expected an integer " +
                            "value in a column with header \"" + RESULT_COLUMNN_HEADER + "\", found: " + dataValue);
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
    protected void prepareWellGroups(List<WellGroup> groups, ExpMaterial sampleInput, Map<String, DomainProperty> properties)
    {
        List<WellData> wells = new ArrayList<WellData>();
        // All well groups use the same plate template, so it's okay to just check the dilution direction of the first group:
        boolean reverseDirection = Boolean.parseBoolean((String) groups.get(0).getProperty(NabManager.SampleProperty.ReverseDilutionDirection.name()));
        for (WellGroup group : groups)
        {
            for (DomainProperty property : properties.values())
                group.setProperty(property.getName(), sampleInput.getProperty(property));
            wells.addAll(group.getWellData(true));
        }
        applyDilution(wells, sampleInput, properties, reverseDirection);
    }

    @Override
    protected Map<ExpMaterial, List<WellGroup>> getMaterialWellGroupMapping(NabAssayProvider provider, List<Plate> plates, Collection<ExpMaterial> sampleInputs)
    {
        Map<String, ExpMaterial> nameToMaterial = new HashMap<String, ExpMaterial>();
        for (ExpMaterial material : sampleInputs)
            nameToMaterial.put(material.getName(), material);

        Map<ExpMaterial, List<WellGroup>> mapping = new HashMap<ExpMaterial, List<WellGroup>>();
        for (Plate plate : plates)
        {
            List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
            for (WellGroup specimenGroup : specimenGroups)
            {
                String name = specimenGroup.getName();
                ExpMaterial material = nameToMaterial.get(name);
                List<WellGroup> materialWellGroups = mapping.get(material);
                if (materialWellGroups == null)
                {
                    materialWellGroups = new ArrayList<WellGroup>();
                    mapping.put(material, materialWellGroups);
                }
                materialWellGroups.add(specimenGroup);
            }
        }
        return mapping;
    }

    @Override
    protected NabAssayRun createNabAssayRun(NabAssayProvider provider, ExpRun run, List<Plate> plates, User user, List<Integer> sortedCutoffs, DilutionCurve.FitType fit)
    {
        return new HighThroughputNabAssayRun(provider, run, plates, user, sortedCutoffs, fit);
    }
}
