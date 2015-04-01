/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyType;
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
import org.labkey.api.query.ValidationException;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.Well;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUploadXarContext;
import org.labkey.api.study.assay.plate.PlateReader;
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

    public static final String ACTIVITY_PROPERTY_NAME = "Activity";
    public static final String INTENSITY_PROPERTY_NAME = "Intensity";

    @Override
    public DataType getDataType()
    {
        return ELISPOT_DATA_TYPE;
    }

    class ElispotFileParser implements ElispotDataFileParser
    {
        private ExpData _data;
        private File _dataFile;
        private ViewBackgroundInfo _info;
        private Logger _log;
        private XarContext _context;

        public ElispotFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context)
        {
            _data = data;
            _dataFile = dataFile;
            _info = info;
            _log = log;
            _context = context;
        }

        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            ExpRun run = _data.getRun();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
            Container container = _data.getContainer();
            ElispotAssayProvider provider = (ElispotAssayProvider)AssayService.get().getProvider(protocol);
            PlateTemplate template = provider.getPlateTemplate(container, protocol);

            Map<String, DomainProperty> runProperties = new HashMap<>();
            for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
                runProperties.put(column.getName(), column);

            if (runProperties.containsKey(ElispotAssayProvider.READER_PROPERTY_NAME))
            {
                DomainProperty readerProp = runProperties.get(ElispotAssayProvider.READER_PROPERTY_NAME);
                if (_context instanceof AssayUploadXarContext)
                {
                    Map<DomainProperty, String> runPropValues = ((AssayUploadXarContext)_context).getContext().getRunProperties();
                    PlateReader reader = provider.getPlateReader(runPropValues.get(readerProp));
                    Map<PlateInfo, Plate> plates = initializePlates(protocol, _dataFile, template, reader);

                    List<Map<String, Object>> results = new ArrayList<>();
                    Map<String, ExpMaterial> materialMap = new HashMap<>();
                    for (ExpMaterial material : run.getMaterialInputs().keySet())
                        materialMap.put(material.getName(), material);

                    // create a map of analyte to cytokine names (third step of the upload wizard)
                    Map<String, Object> analyteToCytokine = new HashMap<>();

                    if (_context instanceof AssayUploadXarContext)
                    {
                        AssayRunUploadContext assayContext = ((AssayUploadXarContext) _context).getContext();
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
                    }

                    for (Map.Entry<PlateInfo, Plate> entry : plates.entrySet())
                    {
                        // TODO : remove this assert when populating fluorospot data is working
                        assert (plates.size() == 1) : "fluorospot run data population not yet implemented";

                        Plate plate = entry.getValue();
                        for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            ExpMaterial material = materialMap.get(group.getName());
                            if (material != null)
                            {
                                for (Position pos : group.getPositions())
                                {
                                    Well well = plate.getWell(pos.getRow(), pos.getColumn());
                                    Map<String, Object> row = new LinkedHashMap<>();

                                    row.put(ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                                    row.put(SFU_PROPERTY_NAME, well.getValue());
                                    row.put(WELLGROUP_PROPERTY_NAME, group.getName());
                                    row.put(WELLGROUP_LOCATION_PROPERTY, pos.toString());
                                    row.put(WELL_ROW_PROPERTY, pos.getRow());
                                    row.put(WELL_COLUMN_PROPERTY, pos.getColumn());

                                    results.add(row);
                                }
                            }
                        }
                    }
                    return results;
                }
            }
            throw new ExperimentException("Unable to load data file: Plate reader type not found");
        }
    }

    public ElispotDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        return new ElispotFileParser(data, dataFile, info, log, context);
    }

    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importData(data, run, context.getProtocol(), dataMap);
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        ElispotDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
        datas.put(ELISPOT_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    public static Map<PlateInfo, Plate> initializePlates(ExpProtocol protocol, File dataFile, PlateTemplate template, PlateReader reader) throws ExperimentException
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);
        Map<PlateInfo, Plate> plateMap = new HashMap<>();

        if (reader != null && provider instanceof ElispotAssayProvider)
        {
            ElispotAssayProvider.DetectionMethodType method = ((ElispotAssayProvider) provider).getDetectionMethod(protocol.getContainer(), protocol);

            if (method == ElispotAssayProvider.DetectionMethodType.COLORIMETRIC)
            {
                double[][] cellValues = reader.loadFile(template, dataFile);
                plateMap.put(new ColorimetricPlateInfo(), PlateService.get().createPlate(template, cellValues));
            }
            else if (method == ElispotAssayProvider.DetectionMethodType.FLUORESCENT)
            {
                for (Map.Entry<String, double[][]> entry : reader.loadMultiGridFile(template, dataFile).entrySet())
                {
                    // attempt to parse the plate grid annotation into a PlateInfo object
                    FluorescentPlateInfo plateInfo = FluorescentPlateInfo.create(entry.getKey());
                    if (plateInfo != null)
                    {
                        plateMap.put(plateInfo, PlateService.get().createPlate(template, entry.getValue()));
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
    
    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (ELISPOT_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    /**
     * Adds antigen wellgroup properties to the elispot data table.
     */
    public static void populateAntigenDataProperties(ExpRun run, Plate plate, PlateReader reader, Map<String, Object> propMap,
                                                     boolean isUpgrade, boolean subtractBackground) throws ValidationException, ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();

            // get the URI for the spot count
            String spotCountURI = createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, ElispotDataHandler.SFU_PROPERTY_NAME).toString();

            Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(run.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
            DomainProperty cellWellProp = antigenDomain.getPropertyByName(ElispotAssayProvider.CELLWELL_PROPERTY_NAME);
            List<? extends DomainProperty> antigenProps = antigenDomain.getProperties();

            List<? extends ExpData> data = run.getOutputDatas(ElispotDataHandler.ELISPOT_DATA_TYPE);
            if (data.size() != 1)
                throw new ExperimentException("Elispot should only upload a single file per run.");

            String dataLsid = data.get(0).getLSID();

            // for each antigen well group, we want to flatten that information to the well level
            List<ObjectProperty> results = new ArrayList<>();
            for (WellGroup group : plate.getWellGroups(WellGroup.Type.ANTIGEN))
            {
                for (Position pos : group.getPositions())
                {
                    results.clear();
                    Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(dataLsid, pos);

                    // get the current row map for assay data
                    Map<String, ObjectProperty> dataRow = OntologyManager.getPropertyObjects(container, dataRowLsid.toString());

                    for (DomainProperty dp : antigenProps)
                    {
                        String key = UploadWizardAction.getInputName(dp, group.getName());
                        if (propMap.containsKey(key) && !dataRow.containsKey(createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, dp).toString()))
                        {
                            ObjectProperty op = ElispotDataHandler.getResultObjectProperty(container,
                                    run.getProtocol(),
                                    dataRowLsid.toString(),
                                    dp.getName(),
                                    propMap.get(key),
                                    dp.getPropertyDescriptor().getPropertyType(),
                                    dp.getPropertyDescriptor().getFormat());

                            results.add(op);
                        }
                    }

                    if (!results.isEmpty() && !dataRow.containsKey(createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME).toString()))
                    {
                        // include the antigen wellgroup name
                        ObjectProperty op = ElispotDataHandler.getResultObjectProperty(container,
                                run.getProtocol(), dataRowLsid.toString(),
                                ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME,
                                group.getName(), PropertyType.STRING);
                        results.add(op);
                    }

                    // calculate a column for normalized spot count
                    if (dataRow.containsKey(spotCountURI) && !dataRow.containsKey(createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, ElispotDataHandler.NORMALIZED_SFU_PROPERTY_NAME).toString()))
                    {
                        ObjectProperty o = dataRow.get(spotCountURI);

                        double normalizedSpotCnt = o.getFloatValue();

                        if (reader.isWellValueValid(normalizedSpotCnt))
                        {
                            String cellWellKey = UploadWizardAction.getInputName(cellWellProp, group.getName());
                            int cellsPerWell = 0;
                            if (propMap.containsKey(cellWellKey))
                                cellsPerWell = NumberUtils.toInt(ConvertUtils.convert(propMap.get(cellWellKey)), 0);

                            if (cellsPerWell > 0)
                                normalizedSpotCnt = (normalizedSpotCnt * 1000000) / cellsPerWell;

                            ObjectProperty sfuOp = ElispotDataHandler.getResultObjectProperty(container,
                                    run.getProtocol(), dataRowLsid.toString(),
                                    ElispotDataHandler.NORMALIZED_SFU_PROPERTY_NAME,
                                    normalizedSpotCnt, PropertyType.DOUBLE);

                            results.add(sfuOp);
                        }
                    }

                    if (!results.isEmpty())
                    {
                        OntologyManager.ensureObject(container, dataRowLsid.toString(),  dataLsid);

                        if (isUpgrade)
                        {
                            // remove any previous properties
                            for (ObjectProperty prop : results)
                                OntologyManager.deleteProperty(prop.getObjectURI(), prop.getPropertyURI(), container, container);
                        }
                        OntologyManager.insertProperties(container, dataRowLsid.toString(), results.toArray(new ObjectProperty[results.size()]));
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
                                                    boolean isUpgrade, boolean subtractBackground) throws ValidationException, ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();

            // get the URI for the spot count
            String spotCountURI = createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, ElispotDataHandler.SFU_PROPERTY_NAME).toString();

            Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(run.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
            DomainProperty cellWellProp = antigenDomain.getPropertyByName(ElispotAssayProvider.CELLWELL_PROPERTY_NAME);

            List<? extends ExpData> data = run.getOutputDatas(ElispotDataHandler.ELISPOT_DATA_TYPE);
            if (data.size() != 1)
                throw new ExperimentException("Elispot should only upload a single file per run.");

            String dataLsid = data.get(0).getLSID();

            // calculate antigen statistics on a per sample basis
            Map<String, ExpMaterial> materialMap = new HashMap<>();

            for (ExpMaterial material : run.getMaterialInputs().keySet())
                materialMap.put(material.getName(), material);

            DomainProperty antigenNameProp = antigenDomain.getPropertyByName(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME);
            Map<String, Map<String, Double>> backgroundValueMap = Collections.emptyMap();

            if (subtractBackground)
                backgroundValueMap = ElispotPlateTypeHandler.getBackgroundValues(container, plate, reader);

            for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                ExpMaterial material = materialMap.get(group.getName());

                if (material != null)
                {
                    List<ObjectProperty> antigenResults = new ArrayList<>();
                    Set<String> antigenNames = new HashSet<>();
                    Lsid rowLsid = ElispotDataHandler.getAntigenRowLsid(dataLsid, material.getName());
                    Map<String, ObjectProperty> dataRow = OntologyManager.getPropertyObjects(container, rowLsid.toString());

                    // background mean/median values on a per specimen basis
                    double bkMeanValue = 0;
                    double bkMedianValue = 0;

                    if (backgroundValueMap.containsKey(group.getName()))
                    {
                        Map<String, Double> values = backgroundValueMap.get(group.getName());

                        if (values.containsKey(ElispotPlateTypeHandler.MEAN_STAT))
                            bkMeanValue = values.get(ElispotPlateTypeHandler.MEAN_STAT);

                        if (values.containsKey(ElispotPlateTypeHandler.MEDIAN_STAT))
                            bkMedianValue = values.get(ElispotPlateTypeHandler.MEDIAN_STAT);
                    }

                    for (WellGroup antigenGroup : group.getOverlappingGroups(WellGroup.Type.ANTIGEN))
                    {
                        List<Position> positions = antigenGroup.getPositions();
                        double[] statsData = new double[positions.size()];
                        int i = 0;

                        String cellWellKey = UploadWizardAction.getInputName(cellWellProp, antigenGroup.getName());
                        int cellsPerWell = 0;
                        if (propMap.containsKey(cellWellKey))
                            cellsPerWell = NumberUtils.toInt(ConvertUtils.convert(propMap.get(cellWellKey)), 0);

                        for (Position pos : positions)
                        {
                            if (group.contains(pos))
                            {
                                Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(dataLsid, pos);
                                Map<String, ObjectProperty> props = OntologyManager.getPropertyObjects(container, dataRowLsid.toString());

                                if (props.containsKey(spotCountURI))
                                {
                                    ObjectProperty o = props.get(spotCountURI);

                                    double value = o.getFloatValue();

                                    if (reader.isWellValueValid(value))
                                    {
                                        statsData[i++] = value;
                                    }
                                }
                            }
                        }
                        statsData = Arrays.copyOf(statsData, i);
                        StatsService service = ServiceRegistry.get().getService(StatsService.class);
                        MathStat stats = service.getStats(statsData);

                        String key = UploadWizardAction.getInputName(antigenNameProp, antigenGroup.getName());
                        if (propMap.containsKey(key))
                        {
                            // for each antigen group, create two columns for mean and median values
                            String antigenName = StringUtils.defaultString(ConvertUtils.convert(propMap.get(key)), "");
                            String groupName = antigenGroup.getName();

                            if (StringUtils.isEmpty(antigenName))
                                antigenName = groupName;

                            if (antigenNames.contains(antigenName))
                                antigenName = antigenName + "_" + groupName;

                            if (!antigenNames.contains(antigenName))
                            {
                                antigenNames.add(antigenName);
                                if (!Double.isNaN(stats.getMean()))
                                {
                                    // subtract off the background value, and normalize by 10^6/cellPerWell
                                    double meanValue = stats.getMean() - bkMeanValue;
                                    meanValue = Math.max(meanValue, 0);

                                    if (cellsPerWell > 0)
                                        meanValue = (meanValue * 1000000) / cellsPerWell;

                                    ObjectProperty mean = ElispotDataHandler.getAntigenResultObjectProperty(container,
                                            run.getProtocol(),
                                            rowLsid.toString(),
                                            antigenName + "_Mean",
                                            meanValue,
                                            PropertyType.DOUBLE, "0.0");
                                    antigenResults.add(mean);
                                }

                                if (!Double.isNaN(stats.getMedian()))
                                {
                                    double medianValue = stats.getMedian() - bkMedianValue;
                                    medianValue = Math.max(medianValue, 0);

                                    if (cellsPerWell > 0)
                                        medianValue = (medianValue * 1000000) / cellsPerWell;

                                    ObjectProperty median = ElispotDataHandler.getAntigenResultObjectProperty(container,
                                            run.getProtocol(),
                                            rowLsid.toString(),
                                            antigenName + "_Median",
                                            medianValue,
                                            PropertyType.DOUBLE, "0.0");
                                    antigenResults.add(median);
                                }
                            }
                        }
                    }

                    // add the background well values (not normalized)
                    ObjectProperty backgroundMeanValue = ElispotDataHandler.getAntigenResultObjectProperty(container,
                            run.getProtocol(),
                            rowLsid.toString(),
                            ElispotDataHandler.BACKGROUND_WELL_PROPERTY + "_Mean",
                            bkMeanValue,
                            PropertyType.DOUBLE, "0.0");

                    ObjectProperty backgroundMedianValue = ElispotDataHandler.getAntigenResultObjectProperty(container,
                            run.getProtocol(),
                            rowLsid.toString(),
                            ElispotDataHandler.BACKGROUND_WELL_PROPERTY + "_Median",
                            bkMedianValue,
                            PropertyType.DOUBLE, "0.0");

                    antigenResults.add(backgroundMeanValue);
                    antigenResults.add(backgroundMedianValue);

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
                }
            }
            transaction.commit();
        }
    }

    public static boolean isSpotCountValid(double value)
    {
        // negative sfu values are error codes
        return value >= 0;
    }

    public static Lsid createPropertyLsid(String prefix, ExpRun run, DomainProperty prop)
    {
        return new Lsid(prefix, run.getProtocol().getName(), prop.getName());
    }

    public static Lsid createPropertyLsid(String prefix, ExpRun run, String propName)
    {
        return new Lsid(prefix, run.getProtocol().getName(), propName);
    }
}
