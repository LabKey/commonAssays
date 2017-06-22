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
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.ValidationException;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.Well;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUploadXarContext;
import org.labkey.api.study.assay.PlateBasedAssayProvider;
import org.labkey.api.study.assay.plate.PlateReader;
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
        List<Map<String, Object>> results = new ArrayList<>();
        ExpProtocol protocol = data.getRun().getProtocol();
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        if (provider instanceof PlateBasedAssayProvider)
        {
            Map<String, DomainProperty> runProperties = new HashMap<>();
            for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
                runProperties.put(column.getName(), column);

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
                                regression.addData(conc, mean);
                            }

                            // save the individual well values for the control group
                            for (Position position : replicate.getPositions())
                            {
                                Map<String, Object> row = new HashMap<>();

                                Well well = plate.getWell(position.getRow(), position.getColumn());
                                row.put(ElisaAssayProvider.WELL_PROPERTY_NAME, position.getDescription());
                                row.put(ElisaAssayProvider.WELLGROUP_PROPERTY_NAME, replicate.getPositionDescription());
                                row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY_NAME, well.getValue());
                                if (specimenGroupMap.containsKey(position))
                                {
                                    ExpMaterial material = materialMap.get(specimenGroupMap.get(position));
                                    if (material != null)
                                        row.put(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                                }

                                if (conc != -1)
                                    row.put(ElisaAssayProvider.CONCENTRATION_PROPERTY_NAME, conc);

                                results.add(row);
                            }
                        }

                        // add the coefficient of determination to the run
                        DomainProperty cod = runProperties.get(ElisaAssayProvider.CORRELATION_COEFFICIENT_PROPERTY_NAME);
                        DomainProperty fitParams = runProperties.get(ElisaAssayProvider.CURVE_FIT_PARAMETERS);
                        if (cod != null && fitParams != null && !Double.isNaN(regression.getRSquare()))
                        {
                            data.getRun().setProperty(context.getUser(), cod.getPropertyDescriptor(), regression.getRSquare());

                            String params = String.valueOf(regression.getSlope()) + "&" + String.valueOf(regression.getIntercept());
                            data.getRun().setProperty(context.getUser(), fitParams.getPropertyDescriptor(), params);
                        }

                        for (WellGroup sampleGroup : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            for (WellGroup replicate : sampleGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                            {
                                for (Position position : replicate.getPositions())
                                {
                                    Map<String, Object> row = new HashMap<>();
                                    Well well = plate.getWell(position.getRow(), position.getColumn());

                                    row.put(ElisaAssayProvider.WELL_PROPERTY_NAME, position.getDescription());
                                    row.put(ElisaAssayProvider.WELLGROUP_PROPERTY_NAME, replicate.getPositionDescription());
                                    row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY_NAME, well.getValue());
                                    if (specimenGroupMap.containsKey(position))
                                    {
                                        ExpMaterial material = materialMap.get(specimenGroupMap.get(position));
                                        if (material != null)
                                            row.put(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                                    }

                                    // compute the concentration
                                    double concentration = (well.getValue() - regression.getIntercept()) / regression.getSlope();
                                    row.put(ElisaAssayProvider.CONCENTRATION_PROPERTY_NAME, concentration);

                                    results.add(row);
                                }
                            }
                        }
                    }
                    catch (ValidationException e)
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
                    if (ElisaAssayProvider.CONCENTRATION_PROPERTY_NAME.equals(dp.getName()))
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
