/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

package org.labkey.elisa;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayRunUploadContext;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateReader;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.data.statistics.DoublePoint;
import org.labkey.api.data.statistics.FitFailedException;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ProvenanceService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.elisa.actions.ElisaRunUploadForm;
import org.labkey.elisa.plate.BioTekPlateReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: klum
 * Date: 10/6/12
 */
public class ElisaDataHandler extends AbstractAssayTsvDataHandler implements TransformDataHandler
{
    public static final String NAMESPACE = "ElisaDataType";
    public static final AssayDataType DATA_TYPE;
    public static final String ELISA_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";

    static
    {
        DATA_TYPE = new AssayDataType(NAMESPACE, new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));
    }

    @Override
    public DataType getDataType()
    {
        return DATA_TYPE;
    }

    @Override
    protected boolean allowEmptyData()
    {
        return false;
    }

    @Override
    protected boolean shouldAddInputMaterials()
    {
        return true;
    }

    @Override
    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        final ProvenanceService pvs = ProvenanceService.get();
        List<Map<String, Object>> results = new ArrayList<>();
        ExpProtocol protocol = data.getRun().getProtocol();
        ExpRun run = data.getRun();
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        if (provider instanceof PlateBasedAssayProvider)
        {
            PlateBasedAssayProvider plateProvider = (PlateBasedAssayProvider)provider;
            Map<String, DomainProperty> runProperties = new HashMap<>();
            for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
                runProperties.put(column.getName(), column);

            Map<String, DomainProperty> sampleProperties = new CaseInsensitiveHashMap<>();
            for (DomainProperty prop : ((PlateBasedAssayProvider) provider).getSampleWellGroupDomain(protocol).getProperties())
                sampleProperties.put(prop.getName(), prop);

            if (plateProvider.getMetadataInputFormat(protocol).equals(SampleMetadataInputFormat.MANUAL))
            {
            }

            PlateReader reader = ((PlateBasedAssayProvider)provider).getPlateReader(BioTekPlateReader.LABEL);
            if (reader != null)
            {
                PlateTemplate template = ((PlateBasedAssayProvider)provider).getPlateTemplate(container, protocol);
                double[][] cellValues = reader.loadFile(template, dataFile);
                Plate plate = PlateService.get().createPlate(template, cellValues, null);

                // collect the standards from the control group so we can calculate the calibration curve
                List<? extends WellGroup> controlGroups = plate.getWellGroups(WellGroup.Type.CONTROL);
                // TODO: add validation in the plate template handler
                assert(controlGroups.size() == 1);

                if (context instanceof AssayUploadXarContext)
                {
                    try {
                        // collect the standard concentration values
                        AssayRunUploadContext runContext = ((AssayUploadXarContext)context).getContext();
                        Map<String, Double> concentrations = getStandardConcentrations(runContext);

                        WellGroup controlGroup = controlGroups.get(0);
                        SimpleRegression regression = new SimpleRegression(true);
                        List<DoublePoint> pointData = new ArrayList<>();

                        Map<String, ExpMaterial> materialMap = new HashMap<>();
                        for (Map.Entry<ExpMaterial,String> e : data.getRun().getMaterialInputs().entrySet())
                            materialMap.put(e.getValue(), e.getKey());

                        Map<Position, String> specimenGroupMap = new HashMap<>();
                        for (WellGroup sample : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            for (Position pos : sample.getPositions())
                                specimenGroupMap.put(pos, sample.getName());
                        }

                        for (WellGroup replicate : controlGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                        {
                            double mean = replicate.getMean();
                            double conc = -1;

                            String key = replicate.getPositionDescription();
                            if (concentrations.containsKey(key))
                            {
                                conc = concentrations.get(key);
                                pointData.add(new DoublePoint(conc, mean));
                                regression.addData(conc, mean);
                            }

                            // save the individual well values for the control group
                            for (Position position : replicate.getPositions())
                            {
                                Map<String, Object> row = new HashMap<>();

                                Well well = plate.getWell(position.getRow(), position.getColumn());
                                row.put(ElisaAssayProvider.WELL_LOCATION_PROPERTY, position.getDescription());
                                row.put(ElisaAssayProvider.WELLGROUP_PROPERTY, replicate.getPositionDescription());
                                row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY, well.getValue());
                                if (specimenGroupMap.containsKey(position))
                                {
                                    ExpMaterial material = materialMap.get(specimenGroupMap.get(position));
                                    if (material != null)
                                    {
                                        row.put(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                                        // TODO: Support adding the material to existing provenance inputs on the row, if any
                                        if (pvs != null)
                                            row.put(ProvenanceService.PROVENANCE_INPUT_PROPERTY, List.of(material.getLSID()));
                                    }
                                }

                                if (conc != -1)
                                    row.put(ElisaAssayProvider.CONCENTRATION_PROPERTY, conc);

                                results.add(row);
                            }
                        }

                        // Compute curve fit parameters based on the selected curve fit (default to linear for legacy assay designs)
                        StatsService.CurveFitType curveFitType = StatsService.CurveFitType.LINEAR;
                        DomainProperty curveFitPd = runProperties.get(DilutionAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
                        if (curveFitPd != null)
                        {
                            Object value = run.getProperty(curveFitPd);
                            if (value != null)
                                curveFitType = StatsService.CurveFitType.fromLabel(String.valueOf(value));
                        }

                        DomainProperty cod = runProperties.get(ElisaAssayProvider.CORRELATION_COEFFICIENT_PROPERTY);
                        DomainProperty fitParams = runProperties.get(ElisaAssayProvider.CURVE_FIT_PARAMETERS_PROPERTY);
                        CurveFit standardCurveFit = StatsService.get().getCurveFit(curveFitType, pointData.toArray(DoublePoint[]::new));
                        standardCurveFit.setLogXScale(false);

                        if (cod != null && fitParams != null && !Double.isNaN(standardCurveFit.getFitError()))
                        {
                            data.getRun().setProperty(context.getUser(), cod.getPropertyDescriptor(), regression.getRSquare());
                            if (standardCurveFit.getParameters() != null)
                            {
                                Map<String, Object> params = standardCurveFit.getParameters().toMap();

                                // TODO : will need a standard way to serialize parameters
                                var serializedParams = params.get("slope") + "&" + params.get("intercept");
                                data.getRun().setProperty(context.getUser(), fitParams.getPropertyDescriptor(), serializedParams);
                            }
                        }

                        for (WellGroup sampleGroup : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            List<DoublePoint> samplePoints = new ArrayList<>();
                            for (WellGroup replicate : sampleGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                            {
                                for (Position position : replicate.getPositions())
                                {
                                    Map<String, Object> row = new HashMap<>();
                                    Well well = plate.getWell(position.getRow(), position.getColumn());

                                    row.put(ElisaAssayProvider.WELL_LOCATION_PROPERTY, position.getDescription());
                                    row.put(ElisaAssayProvider.WELLGROUP_PROPERTY, replicate.getPositionDescription());
                                    row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY, well.getValue());
                                    if (specimenGroupMap.containsKey(position))
                                    {
                                        ExpMaterial material = materialMap.get(specimenGroupMap.get(position));
                                        if (material != null)
                                        {
                                            row.put(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                                            // TODO: Support adding the material to existing provenance inputs on the row, if any
                                            if (pvs != null)
                                                row.put(ProvenanceService.PROVENANCE_INPUT_PROPERTY, List.of(material.getLSID()));
                                        }
                                    }

                                    // compute the concentration
                                    double concentration = standardCurveFit.fitCurveY(well.getValue());
                                    row.put(ElisaAssayProvider.CONCENTRATION_PROPERTY, concentration);

                                    samplePoints.add(new DoublePoint(concentration, well.getValue()));
                                    results.add(row);
                                }
                            }

                            // compute sample scoped statistics
                            calculateSampleStats(context.getUser(), materialMap.get(sampleGroup.getName()), sampleProperties, curveFitType, samplePoints);
                        }
                    }
                    catch (FitFailedException | ValidationException e)
                    {
                        throw new ExperimentException(e);
                    }
                }
            }
        }
        Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
        datas.put(getDataType(), results);

        return datas;
    }

    /**
     * Generate sample scoped stats for this run
     * @param material the sample to compute stats for
     * @param sampleProps map of sample domain properties
     * @param curveFitType curve fit selected for this run
     * @param sampleValues the list of x,y pairs (concentration, absorption) for the sample wells
     */
    private void calculateSampleStats(User user, @Nullable ExpMaterial material, Map<String, DomainProperty> sampleProps,
                                      StatsService.CurveFitType curveFitType, List<DoublePoint> sampleValues) throws FitFailedException, ValidationException
    {
        if (material != null)
        {
            if (sampleProps.containsKey(ElisaAssayProvider.AUC_PROPERTY))
            {
                CurveFit sampleCurveFit = StatsService.get().getCurveFit(curveFitType, sampleValues.toArray(DoublePoint[]::new));
                sampleCurveFit.setLogXScale(false);

                material.setProperty(user, sampleProps.get(ElisaAssayProvider.AUC_PROPERTY).getPropertyDescriptor(), sampleCurveFit.calculateAUC(StatsService.AUCType.NORMAL));
            }

            List<Double> absorption = sampleValues.stream()
                    .map(dp -> dp.second)
                    .collect(Collectors.toList());
            MathStat absStat = StatsService.get().getStats(absorption);
            if (sampleProps.containsKey(ElisaAssayProvider.MEAN_ABSORPTION_PROPERTY))
            {
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.MEAN_ABSORPTION_PROPERTY).getPropertyDescriptor(), absStat.getMean());
            }

            if (sampleProps.containsKey(ElisaAssayProvider.CV_ABSORPTION_PROPERTY))
            {
                double cv = absStat.getStdDev() / absStat.getMean();
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.CV_ABSORPTION_PROPERTY).getPropertyDescriptor(), cv);
            }

            List<Double> concentration = sampleValues.stream()
                    .map(dp -> dp.first)
                    .collect(Collectors.toList());
            MathStat concStat = StatsService.get().getStats(concentration);
            if (sampleProps.containsKey(ElisaAssayProvider.MEAN_CONCENTRATION_PROPERTY))
            {
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.MEAN_CONCENTRATION_PROPERTY).getPropertyDescriptor(), concStat.getMean());
            }

            if (sampleProps.containsKey(ElisaAssayProvider.CV_CONCENTRATION_PROPERTY))
            {
                double cv = concStat.getStdDev() / concStat.getMean();
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.CV_CONCENTRATION_PROPERTY).getPropertyDescriptor(), cv);
            }
        }
    }

    private Map<String, Double> getStandardConcentrations(AssayRunUploadContext context) throws ExperimentException
    {
        Map<String, Double> concentrations = new HashMap<>();
        if (context instanceof ElisaRunUploadForm)
        {
            Map<String, Map<DomainProperty, String>> props = ((ElisaRunUploadForm)context).getConcentrationProperties();

            for (Map.Entry<String, Map<DomainProperty, String>> entry : props.entrySet())
            {
                for (DomainProperty dp : entry.getValue().keySet())
                {
                    double conc = 0;
                    if (ElisaAssayProvider.CONCENTRATION_PROPERTY.equals(dp.getName()))
                    {
                        conc = NumberUtils.toDouble(entry.getValue().get(dp), 0);
                    }
                    concentrations.put(entry.getKey(), conc);
                }
            }

            return concentrations;
        }
        else
            throw new ExperimentException("The form is not an instance of ElisaRunUploadForm, concentration values were not accessible.");
    }
}
