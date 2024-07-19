/*
 * Copyright (c) 2008-2018 LabKey Corporation
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

package org.labkey.elispot;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Logger;
import org.labkey.api.assay.plate.PlateUtils;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.dataiterator.AbstractMapDataIterator;
import org.labkey.api.dataiterator.DataIteratorBuilder;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.actions.UploadWizardAction;
import org.labkey.api.assay.AbstractAssayProvider;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayRunUploadContext;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.plate.PlateReader;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.elispot.plate.ColorimetricPlateInfo;
import org.labkey.elispot.plate.FluorescentPlateInfo;
import org.labkey.elispot.plate.PlateInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Karl Lum
 * Date: Jan 8, 2008
 */
public class ElispotDataHandler extends AbstractElispotDataHandler implements TransformDataHandler
{
    public static final String NAMESPACE = "ElispotAssayData";
    private static final AssayDataType ELISPOT_DATA_TYPE = new AssayDataType(NAMESPACE, new FileType(Arrays.asList(".txt", ".xls", ".xlsx"), ".txt"));
    private static final DataType ELISPOT_TRANSFORMED_DATA_TYPE = new DataType("ElispotTransformedData"); // a marker data type

    @Override
    public DataType getDataType()
    {
        return ELISPOT_DATA_TYPE;
    }

    static class ElispotFileParser implements ElispotDataFileParser
    {
        private final ExpData _data;
        private final File _dataFile;
        private final XarContext _context;

        public ElispotFileParser(ExpData data, File dataFile, XarContext context)
        {
            _data = data;
            _dataFile = dataFile;
            _context = context;
        }

        @Override
        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            ExpRun run = _data.getRun();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
            Container container = _data.getContainer();
            ElispotAssayProvider provider = (ElispotAssayProvider)AssayService.get().getProvider(protocol);
            Plate template = provider.getPlate(container, protocol);

            Map<String, DomainProperty> runProperties = new HashMap<>();
            for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
                runProperties.put(column.getName(), column);

            if (runProperties.containsKey(ElispotAssayProvider.READER_PROPERTY_NAME))
            {
                DomainProperty readerProp = runProperties.get(ElispotAssayProvider.READER_PROPERTY_NAME);
                if (_context instanceof AssayUploadXarContext xarContext)
                {
                    Map<DomainProperty, String> runPropValues = xarContext.getContext().getRunProperties();
                    PlateReader reader = provider.getPlateReader(runPropValues.get(readerProp));
                    Map<PlateInfo, Plate> plates = initializePlates(protocol, _dataFile, template, reader);

                    Map<String, ExpMaterial> materialMap = new HashMap<>();
                    for (Map.Entry<? extends ExpMaterial,String> e : run.getMaterialInputs().entrySet())
                        materialMap.put(e.getValue(), e.getKey());

                    // create a map of analyte to cytokine names (third step of the upload wizard)
                    Map<String, Object> analyteToCytokine = new HashMap<>();

                    AssayRunUploadContext<?> assayContext = xarContext.getContext();
                    Map<String, Map<DomainProperty, String>> analyteProperties = ((ElispotRunUploadForm)assayContext).getAnalyteProperties();

                    if (analyteProperties != null)
                    {
                        for (Map.Entry<String, Map<DomainProperty, String>> groupEntry : analyteProperties.entrySet())
                        {
                            String analyteName = groupEntry.getKey();
                            Map<DomainProperty, String> properties = groupEntry.getValue();
                            for (String value : properties.values())
                            {
                                assert !analyteToCytokine.containsKey(analyteName) : "only cytokine to analyte mapping supported";
                                analyteToCytokine.put(analyteName, value);
                            }
                        }
                    }

                    // With multiple plates, different plates can contribute to the same result row (1 filling spotcount, another activity, intensity)
                    // so map them with key <WellLocation>-<WellGroup>-<Analyte> as we fill in, then make a list for the results
                    Map<String, Map<String, Object>> rowMapMap = new HashMap<>();
                    for (Map.Entry<PlateInfo, Plate> entry : plates.entrySet())
                    {
                        Plate plate = entry.getValue();
                        String measurement = entry.getKey().getMeasurement();
                        String analyte = entry.getKey().getAnalyte();
                        for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            ExpMaterial material = materialMap.get(group.getName());
                            if (material != null)
                            {
                                for (Position pos : group.getPositions())
                                {
                                    String mapMapKey = pos.toString() + "-" + group.getName() + "-" + (null != analyte ? analyte : "");
                                    Well well = plate.getWell(pos.getRow(), pos.getColumn());
                                    Map<String, Object> row;
                                    if (!rowMapMap.containsKey(mapMapKey))
                                    {
                                        rowMapMap.put(mapMapKey, new LinkedHashMap<>());
                                    }
                                    row = rowMapMap.get(mapMapKey);

                                    if (SFU_PROPERTY_NAME.equalsIgnoreCase(measurement))
                                        row.put(SFU_PROPERTY_NAME, well.getValue());
                                    else if (INTENSITY_PROPERTY_NAME.equalsIgnoreCase(measurement))
                                        row.put(INTENSITY_PROPERTY_NAME, well.getValue());
                                    else if (ACTIVITY_PROPERTY_NAME.equalsIgnoreCase(measurement))
                                        row.put(ACTIVITY_PROPERTY_NAME, well.getValue());
                                    else if (SPOT_SIZE_PROPERTY_NAME.equalsIgnoreCase(measurement))
                                        row.put(SPOT_SIZE_PROPERTY_NAME, well.getValue());

                                    row.put(WELLGROUP_PROPERTY_NAME, group.getName());
                                    row.put(WELLGROUP_LOCATION_PROPERTY, pos.toString());
                                    row.put(WELL_ROW_PROPERTY, pos.getRow());
                                    row.put(WELL_COLUMN_PROPERTY, pos.getColumn());
                                    row.put(ANALYTE_PROPERTY_NAME, analyte);
                                    if (analyte != null)
                                        row.put(CYTOKINE_PROPERTY_NAME, analyteToCytokine.get(analyte));
                                    row.put(ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                                }
                            }
                        }
                    }

                    return new ArrayList<>(rowMapMap.values());
                }
            }
            throw new ExperimentException("Unable to load data file: Plate reader type not found");
        }
    }

    @Override
    public ElispotDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context)
    {
        return new ElispotFileParser(data, dataFile, context);
    }

    @Override
    public void importTransformDataMap(ExpData data, AssayRunUploadContext<?> context, ExpRun run, DataIteratorBuilder dataMap) throws ExperimentException
    {
        importData(run, dataMap);
    }

    @Override
    public Map<DataType, DataIteratorBuilder> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        ElispotDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, DataIteratorBuilder> datas = new HashMap<>();
        List<Map<String, Object>> rows = parser.getResults();
        datas.put(ELISPOT_TRANSFORMED_DATA_TYPE, AbstractMapDataIterator.builderOf(rows));

        return datas;
    }

    public static Map<PlateInfo, Plate> initializePlates(ExpProtocol protocol, File dataFile, Plate template, PlateReader reader) throws ExperimentException
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);
        Map<PlateInfo, Plate> plateMap = new HashMap<>();

        if (reader != null && provider instanceof ElispotAssayProvider)
        {
            ElispotAssayProvider.DetectionMethodType method = ((ElispotAssayProvider) provider).getDetectionMethod(protocol.getContainer(), protocol);

            if (method == ElispotAssayProvider.DetectionMethodType.COLORIMETRIC)
            {
                double[][] cellValues = reader.loadFile(template, dataFile);
                plateMap.put(new ColorimetricPlateInfo(), PlateService.get().createPlate(template, cellValues, null));
            }
            else if (method == ElispotAssayProvider.DetectionMethodType.FLUORESCENT)
            {
                for (PlateUtils.GridInfo grid : reader.loadMultiGridFile(template, dataFile))
                {
                    // attempt to parse the plate grid annotation into a PlateInfo object
                    FluorescentPlateInfo plateInfo = FluorescentPlateInfo.create(grid.getAnnotations());
                    if (plateInfo != null)
                    {
                        plateMap.put(plateInfo, PlateService.get().createPlate(template, grid.getData(), null));
                    }
                }
            }
            else
            {
                throw new UnsupportedOperationException("ELISpot detection method " + method + " not supported");
            }
        }
        return plateMap;
    }

    /**
     * Adds antigen wellgroup properties to the elispot data table.
     */
    public static void populateAntigenDataProperties(ExpRun run, Plate plate, PlateReader reader, Map<String, Object> propMap) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();

            Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(run.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
            DomainProperty cellWellProp = antigenDomain.getPropertyByName(ElispotAssayProvider.CELLWELL_PROPERTY_NAME);

            List<? extends ExpData> data = run.getOutputDatas(ElispotDataHandler.ELISPOT_DATA_TYPE);
            if (data.size() != 1)
                throw new ExperimentException("Elispot should only upload a single file per run.");

            String dataLsid = data.get(0).getLSID();

            // for each antigen well group, we want to flatten that information to the well level
            for (WellGroup group : plate.getWellGroups(WellGroup.Type.ANTIGEN))
            {
                for (Position pos : group.getPositions())
                {
                    Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(dataLsid, pos);

                    for (RunDataRow runDataRow : ElispotManager.get().getRunDataRows(dataRowLsid.toString(), container))
                    {
                        Map<String, Object> fieldsToUpdate = new HashMap<>();
                        fieldsToUpdate.put("RowId", runDataRow.getRowId());

                        Lsid antigenLsid = getAntigenLsid(group.getName(), runDataRow.getWellgroupName(), run.getRowId(),
                                run.getProtocol().getName(), runDataRow.getAnalyte());
                        fieldsToUpdate.put("AntigenLsid", antigenLsid);
                        fieldsToUpdate.put(ANTIGEN_WELLGROUP_PROPERTY_NAME, group.getName());

                        // calculate a column for normalized spot count
                        Double normalizedSpotCnt = runDataRow.getSpotCount();
                        if (null != normalizedSpotCnt && reader.isWellValueValid(normalizedSpotCnt))
                        {
                            String cellWellKey = UploadWizardAction.getInputName(cellWellProp, group.getName());
                            int cellsPerWell = 0;
                            if (propMap.containsKey(cellWellKey))
                                cellsPerWell = NumberUtils.toInt(ConvertUtils.convert(propMap.get(cellWellKey)), 0);

                            if (cellsPerWell > 0)
                                normalizedSpotCnt = (normalizedSpotCnt * 1000000) / cellsPerWell;

                            fieldsToUpdate.put(NORMALIZED_SFU_PROPERTY_NAME, normalizedSpotCnt);
                        }

                        ElispotManager.get().updateRunDataRow(null, fieldsToUpdate);
                    }
                }
            }
            transaction.commit();
        }
    }

    /**
     * Adds antigen wellgroup statistics to the antigen runs table (one row per sample)
     */
    public static void populateAntigenRunProperties(ExpRun run, Plate plate, PlateReader reader, Map<String, Object> propMap,
                                                    boolean isUpgrade, boolean subtractBackground, boolean updateBackground) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();

            Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(run.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
            List<? extends DomainProperty> antigenProps = antigenDomain.getProperties();
            DomainProperty cellWellProp = antigenDomain.getPropertyByName(ElispotAssayProvider.CELLWELL_PROPERTY_NAME);

            List<? extends ExpData> data = run.getOutputDatas(ElispotDataHandler.ELISPOT_DATA_TYPE);
            if (data.size() != 1)
                throw new ExperimentException("Elispot should only upload a single file per run.");

            String dataLsid = data.get(0).getLSID();

            // calculate antigen statistics on a per sample basis
            Map<String, ExpMaterial> materialMap = new HashMap<>();

            for (Map.Entry<? extends ExpMaterial,String> e : run.getMaterialInputs().entrySet())
                materialMap.put(e.getValue(), e.getKey());

            DomainProperty antigenNameProp = antigenDomain.getPropertyByName(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME);
            Map<String, Map<String, Double>> backgroundValueMap = Collections.emptyMap();

            if (subtractBackground)
                backgroundValueMap = ElispotPlateLayoutHandler.getBackgroundValues(container, plate, reader);

            for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                ExpMaterial material = materialMap.get(group.getName());
                if (material != null)
                {
                    List<Map<String, Object>> antigenRows = new ArrayList<>();

                    Lsid rowLsid = ElispotDataHandler.getAntigenRowLsid(dataLsid, material.getName());

                    // background mean/median values on a per specimen basis
                    double bkMeanValue = 0;
                    double bkMedianValue = 0;

                    // TODO: background subtraction not support for multi-plate yet
                    if (backgroundValueMap.containsKey(group.getName()))
                    {
                        Map<String, Double> values = backgroundValueMap.get(group.getName());

                        if (values.containsKey(ElispotPlateLayoutHandler.MEAN_STAT))
                            bkMeanValue = values.get(ElispotPlateLayoutHandler.MEAN_STAT);

                        if (values.containsKey(ElispotPlateLayoutHandler.MEDIAN_STAT))
                            bkMedianValue = values.get(ElispotPlateLayoutHandler.MEDIAN_STAT);
                    }

                    Set<String> analyteNames = new HashSet<>();
                    for (WellGroup antigenGroup : group.getOverlappingGroups(WellGroup.Type.ANTIGEN))
                    {
                        List<Position> positions = antigenGroup.getPositions();
                        Map<String, List<Double>> statsDataMap = new HashMap<>();       // One per analyte

                        String cellWellKey = UploadWizardAction.getInputName(cellWellProp, antigenGroup.getName());
                        int cellsPerWell = 0;
                        if (propMap.containsKey(cellWellKey))
                            cellsPerWell = NumberUtils.toInt(ConvertUtils.convert(propMap.get(cellWellKey)), 0);

                        for (Position pos : positions)
                        {
                            if (group.contains(pos))
                            {
                                Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(dataLsid, pos);
                                for (RunDataRow runDataRow : ElispotManager.get().getRunDataRows(dataRowLsid.toString(), container))
                                {
//                                    assert material.getName().equals(runDataRow.getWellgroupName());
                                    if (!statsDataMap.containsKey(runDataRow.getAnalyte()))
                                        statsDataMap.put(runDataRow.getAnalyte(), new ArrayList<>());
                                    List<Double> statsData = statsDataMap.get(runDataRow.getAnalyte());

                                    Double value = runDataRow.getSpotCount();
                                    if (null != value && reader.isWellValueValid(value))
                                    {
                                        statsData.add(value);
                                    }
                                }
                            }
                        }

                        for (Map.Entry<String, List<Double>> statsMapEntry : statsDataMap.entrySet())
                        {
                            analyteNames.add(statsMapEntry.getKey());
                            double[] statsData = new double[statsMapEntry.getValue().size()];
                            int i = 0;
                            for (Double doub : statsMapEntry.getValue())
                                statsData[i++] = doub;
                            StatsService service = StatsService.get();
                            MathStat stats = service.getStats(statsData);

                            String key = UploadWizardAction.getInputName(antigenNameProp, antigenGroup.getName());
                            if (propMap.containsKey(key))
                            {
                                // for each antigen group, create two columns for mean and median values
                                String groupName = antigenGroup.getName();

                                Double mean = null;
                                Double median = null;
                                if (!Double.isNaN(stats.getMean()))
                                {
                                    // subtract off the background value, and normalize by 10^6/cellPerWell
                                    double meanValue = stats.getMean() - bkMeanValue;
                                    meanValue = Math.max(meanValue, 0);
                                    if (cellsPerWell > 0)
                                        meanValue = (meanValue * 1000000) / cellsPerWell;
                                    mean = meanValue;
                                }

                                if (!Double.isNaN(stats.getMedian()))
                                {
                                    double medianValue = stats.getMedian() - bkMedianValue;
                                    medianValue = Math.max(medianValue, 0);
                                    if (cellsPerWell > 0)
                                        medianValue = (medianValue * 1000000) / cellsPerWell;
                                    median = medianValue;
                                }

                                String analyte = statsMapEntry.getKey();
                                Map<String, Object> fields = makeAntigenRow(
                                        run.getRowId(),                 // runid
                                        material.getLSID(),             // specimenLSID
                                        run.getMaterialInputs().get(material),  // wellgroupName
                                        (String) propMap.get(key),      // antigenName
                                        mean,
                                        median,
                                        rowLsid.toString(),             // objectUri
                                        run.getProtocol().getName(),    // protocolName
                                        groupName,                      // antigenWellgroupName
                                        analyte,                        // analyte
                                        propMap.get(analyte)            // cytokine
                                );
                                if (propMap.containsKey(cellWellKey))
                                    fields.put(ElispotAssayProvider.CELLWELL_PROPERTY_NAME, propMap.get(cellWellKey));
                                for (DomainProperty antigenProp : antigenProps)
                                {
                                    String antigenPropKey = UploadWizardAction.getInputName(antigenProp, antigenGroup.getName());
                                    if (propMap.containsKey(antigenPropKey))
                                        fields.put(antigenProp.getName(), propMap.get(antigenPropKey));
                                }
                                antigenRows.add(fields);
                            }
                        }
                    }

                    for (String analyteName : analyteNames)
                    {
                        // add the background well values (not normalized)
                        Map<String, Object> fields = makeAntigenRow
                            (
                                run.getRowId(),                         // runid
                                material.getLSID(),                     // specimenLSID
                                run.getMaterialInputs().get(material),  // wellgroupName
                                BACKGROUND_WELL_PROPERTY,               // antigenName
                                bkMeanValue,
                                bkMedianValue,
                                rowLsid.toString(),                     // objectUri
                                run.getProtocol().getName(),            // protocolName
                                BACKGROUND_WELL_PROPERTY,               // antigenWellgroupName
                                analyteName,                            // analyte
                                propMap.get(analyteName)                // cytokine
                            );
                        antigenRows.add(fields);
                    }

/*
                    if (!antigenResults.isEmpty())
                    {
                        if (!dataRow.containsKey(createPropertyLsid(ElispotDataHandler.ELISPOT_ANTIGEN_PROPERTY_LSID_PREFIX, run,ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY).toString()))
                        {
                            ObjectProperty sample = ElispotDataHandler.getAntigenResultObjectProperty(container,
                                    run.getProtocol(),
                                    rowLsid.toString(),
                                    ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY,
                                    material.getLSID(), PropertyType.STRING, null);
                            antigenResults.add(sample);
                        }

                        if (isUpgrade)
                        {
                            // remove any previous properties
                            for (ObjectProperty prop : antigenResults)
                                OntologyManager.deleteProperty(prop.getObjectURI(), prop.getPropertyURI(), container, container);
                        }
                        OntologyManager.ensureObject(container, rowLsid.toString(),  dataLsid);
                        OntologyManager.insertProperties(container, rowLsid.toString(), antigenResults.toArray(new ObjectProperty[antigenResults.size()]));
                    }
*/
                    for (Map<String, Object> antigenRow : antigenRows)
                    {
                        if (updateBackground)
                            ElispotManager.get().updateAntigenRow(null, antigenRow, run.getProtocol());
                        else
                            ElispotManager.get().insertAntigenRow(null, antigenRow, run.getProtocol());
                    }
                }
            }
            transaction.commit();
        }
    }

    private static Map<String, Object> makeAntigenRow(int runId, String specimenLsid, String wellgroupName, String antigenName,
                                                      Double mean, Double median, String objectUri, String protocolName,
                                                      String antigenWellgroupName, String analyte, Object cytokine)
    {
        Map<String, Object> fields = new HashMap<>();
        fields.put(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME, antigenName);
        fields.put("Mean", mean);
        fields.put("Median", median);
        fields.put("RunId", runId);
        fields.put("SpecimenLsid", specimenLsid);
        fields.put(ElispotDataHandler.WELLGROUP_PROPERTY_NAME, wellgroupName);
        fields.put(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME, antigenWellgroupName);
        fields.put("AntigenLsid", getAntigenLsid(antigenWellgroupName, wellgroupName, runId, protocolName, analyte));
        fields.put("ObjectUri", objectUri);
        fields.put(ElispotDataHandler.ANALYTE_PROPERTY_NAME, analyte);
        if (cytokine != null)
            fields.put(ElispotDataHandler.CYTOKINE_PROPERTY_NAME, String.valueOf(cytokine));
        return fields;
    }

}
