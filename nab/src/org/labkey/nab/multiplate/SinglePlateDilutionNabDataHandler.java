/*
 * Copyright (c) 2013-2018 LabKey Corporation
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

import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.labkey.api.util.Pair;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: klum
 * Date: 2/24/13
 */
public class SinglePlateDilutionNabDataHandler extends HighThroughputNabDataHandler
{
    public static final AssayDataType SINGLE_PLATE_DILUTION_DATA_TYPE = new AssayDataType("SinglePlateDilutionAssayRunNabData", new FileType(Arrays.asList(".xls", ".xlsx", ".csv"), ".csv"));

    @Override
    public DataType getDataType()
    {
        return SINGLE_PLATE_DILUTION_DATA_TYPE;
    }

    @Override
    protected List<Plate> createPlates(File dataFile, Plate template) throws ExperimentException
    {
        DataLoader loader = null;
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
                throw createParseError(dataFile, "No columns found in data file.");
            }

            // TODO: Refactor to remove code duplication with NabDataHandler.parseList()

            // The results column is defined as the last column in the file for this file format:
            String resultColumnHeader = columns[columns.length - 1].name;
            String virusNameColumnHeader = NabAssayProvider.VIRUS_NAME_PROPERTY_NAME;

            int wellCount = 0;
            int plateCount = 0;
            double[][] wellValues = new double[template.getRows()][template.getColumns()];
            List<Plate> plates = new ArrayList<>();
            Map<Integer, String> plateToVirusMap = new HashMap<>();

            for (Map<String, Object> rowData : loader)
            {
                // Current line in the data file is calculated by the number of wells we've already read,
                // plus one for the current row, plus one for the header row:
                int line = plateCount * wellsPerPlate + wellCount + 2;
                Pair<Integer, Integer> location = getWellLocation(dataFile, LOCATION_COLUMNN_HEADER, template.getRows(), template.getColumns(), rowData, line);
                if (location == null)
                {
                    throw createParseError(dataFile, "No well location information was found in the datafile");
                }
                int plateRow = location.getKey();
                int plateCol = location.getValue();

                Object dataValue = rowData.get(resultColumnHeader);
                if (!(dataValue instanceof Integer))
                {
                    throw createParseError(dataFile, "No valid result value found on line " + line + ".  Expected integer " +
                            "result values in the last data file column (\"" + resultColumnHeader + "\") found: " + dataValue);
                }

                Object virusName = rowData.get(virusNameColumnHeader);
                if (!(virusName instanceof String))
                {
                    throw createParseError(dataFile, "No valid virus name value found on line " + line + ".  Expected string " +
                            "result values  (\"" + virusNameColumnHeader + "\") found: " + virusName);
                }

                if (!plateToVirusMap.containsKey(plateCount))
                    plateToVirusMap.put(plateCount, virusName.toString());
                else
                {
                    if (!plateToVirusMap.get(plateCount).equals(virusName.toString()))
                        throw createParseError(dataFile, "more than one virus name on a plate found for : plate " + plateCount +
                                " and virus name : " + virusName);
                }

                wellValues[plateRow - 1][plateCol - 1] = (Integer) dataValue;
                if (++wellCount == wellsPerPlate)
                {
                    Plate plate = PlateService.get().createPlate(template, wellValues, null, PlateService.NO_RUNID, plateCount + 1);
                    plate.setProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME, plateToVirusMap.get(plateCount));

                    plates.add(plate);
                    plateCount++;
                    wellCount = 0;
                }
            }
            if (wellCount != 0)
            {
                throw createParseError(dataFile, "Expected well data in multiples of " + wellsPerPlate + ".  The file provided included " +
                        plateCount + " complete plates of data, plus " + wellCount + " extra rows.");
            }
            return plates;
        }
        catch (IOException e)
        {
            throw createParseError(dataFile, null, e);
        }
        finally
        {
            if (loader != null)
            {
                loader.close();
            }
        }
    }

    @Override
    protected Map<ExpMaterial, List<WellGroup>> getMaterialWellGroupMapping(DilutionAssayProvider<?> provider, List<Plate> plates, Map<? extends ExpMaterial,String> sampleInputs) throws ExperimentException
    {
        Map<String, ExpMaterial> nameToMaterial = new HashMap<>();
        for (Map.Entry<? extends ExpMaterial,String> e : sampleInputs.entrySet())
            nameToMaterial.put(e.getValue(), e.getKey());

        Map<ExpMaterial, List<WellGroup>> mapping = new HashMap<>();
        for (Plate plate : plates)
        {
            List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
            for (WellGroup specimenGroup : specimenGroups)
            {
                String name = specimenGroup.getName();
                String virusName = plate.getProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME).toString();
                String key = SinglePlateDilutionSamplePropertyHelper.getKey(virusName, name);

                ExpMaterial material = nameToMaterial.get(key);
                if (material == null)
                {
                    throw new ExperimentException("Unable to find sample metadata for sample well group \"" + name +
                            "\": your sample metadata file may contain incorrect well group names, or it may not list all required samples.");
                }
                List<WellGroup> materialWellGroups = mapping.computeIfAbsent(material, k -> new ArrayList<>());
                materialWellGroups.add(specimenGroup);
            }
        }
        return mapping;
    }

    @Override
    protected DilutionAssayRun createDilutionAssayRun(DilutionAssayProvider<?> provider, ExpRun run, List<Plate> plates, User user, List<Integer> sortedCutoffs, StatsService.CurveFitType fit)
    {
        return new SinglePlateDilutionNabAssayRun(provider, run, plates, user, sortedCutoffs, fit);
    }

    @Override
    public Map<DilutionSummary, DilutionAssayRun> getDilutionSummaries(User user, StatsService.CurveFitType fit, int... dataObjectIds) throws ExperimentException
    {
        Map<DilutionSummary, DilutionAssayRun> summaries = new LinkedHashMap<>();
        if (dataObjectIds == null || dataObjectIds.length == 0)
            return summaries;

        Map<Integer, DilutionAssayRun> dataToAssay = new HashMap<>();
        List<Integer> nabSpecimenIds = new ArrayList<>(dataObjectIds.length);
        for (int nabSpecimenId : dataObjectIds)
            nabSpecimenIds.add(nabSpecimenId);
        List<NabSpecimen> nabSpecimens = NabManager.get().getNabSpecimens(nabSpecimenIds);

        Set<String> specimenLsids = new HashSet<>();
        for (NabSpecimen nabSpecimen : nabSpecimens)
        {
            String wellgroupName = nabSpecimen.getWellgroupName();
            String specimenLsid = nabSpecimen.getSpecimenLsid();

            if (wellgroupName != null || specimenLsid != null)
                specimenLsids.add(specimenLsid);
        }

        Map<String, ExpMaterial> specimenMaterials = ExperimentService.get().getExpMaterialsByLsid(specimenLsids)
                .stream().collect(Collectors.toMap(ExpMaterial::getLSID, material -> material));
        for (NabSpecimen nabSpecimen : nabSpecimens)
        {
            String wellgroupName = nabSpecimen.getWellgroupName();
            String specimenLsid = nabSpecimen.getSpecimenLsid();
            String virusName = null;

            if (wellgroupName == null || specimenLsid == null)
                continue;

            // get the virus name from the material input
            ExpMaterial inputMaterial = specimenMaterials.get(specimenLsid);
            if (inputMaterial != null)
            {
                for (Map.Entry<PropertyDescriptor, Object> entry : inputMaterial.getPropertyValues().entrySet())
                {
                    if (NabAssayProvider.VIRUS_NAME_PROPERTY_NAME.equals(entry.getKey().getName()))
                    {
                        virusName = entry.getValue().toString();
                        break;
                    }
                }
            }

            if (virusName == null)
                continue;

            int runId = nabSpecimen.getRunId();
            DilutionAssayRun assay = dataToAssay.get(runId);
            if (assay == null)
            {
                ExpRun run = ExperimentService.get().getExpRun(runId);
                if (null == run)
                    continue;
                assay = getAssayResults(run, user, fit);
                if (null == assay)
                    continue;
                dataToAssay.put(runId, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                WellGroup group = summary.getFirstWellGroup();
                Object virusProp = group.getProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);

                if (wellgroupName.equals(group.getName()) && (virusProp != null && virusProp.toString().equals(virusName)))
                {
                    summaries.put(summary, assay);
                }
            }
        }

        return summaries;
    }
}
