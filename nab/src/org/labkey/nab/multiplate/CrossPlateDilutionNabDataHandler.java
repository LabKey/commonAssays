/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.FileType;
import org.labkey.api.util.Pair;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabAssayRun;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/24/13
 */
public class CrossPlateDilutionNabDataHandler extends HighThroughputNabDataHandler
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
    public DataType getDataType()
    {
        return NAB_HIGH_THROUGHPUT_DATA_TYPE;
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
            List<Plate> plates = new ArrayList<Plate>();
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
    protected Map<ExpMaterial, List<WellGroup>> getMaterialWellGroupMapping(NabAssayProvider provider, List<Plate> plates, Collection<ExpMaterial> sampleInputs) throws ExperimentException
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
                if (material == null)
                {
                    throw new ExperimentException("Unable to find sample metadata for sample well group \"" + name +
                            "\": your sample metadata file may contain incorrect well group names, or it may not list all required samples.");
                }
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
        return new CrossPlateDilutionNabAssayRun(provider, run, plates, user, sortedCutoffs, fit);
    }
}
