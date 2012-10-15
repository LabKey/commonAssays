/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
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
import org.labkey.api.study.assay.plate.ExcelPlateReader;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.study.assay.plate.PlateReaderService;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.elisa.plate.BioTekPlateReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/6/12
 */
public class ElisaDataHandler extends AbstractAssayTsvDataHandler implements TransformDataHandler
{
    public static final AssayDataType DATA_TYPE;
    public static final String ELISA_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";

    static
    {
        DATA_TYPE = new AssayDataType("ElisaDataType", new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));
    }

    private boolean _allowEmptyData = false;

    @Override
    protected boolean allowEmptyData()
    {
        return _allowEmptyData;
    }

    @Override
    protected boolean shouldAddInputMaterials()
    {
        return true;
    }

    @Override
    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    @Override
    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        try
        {
            importRows(data, context.getUser(), run, context.getProtocol(), context.getProvider(), dataMap);
        }
        catch (ValidationException e)
        {
            throw new ExperimentException(e.toString(), e);
        }
    }

    @Override
    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        ExpProtocol protocol = data.getRun().getProtocol();
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        if (provider instanceof PlateBasedAssayProvider)
        {
            Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
            for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
                runProperties.put(column.getName(), column);

            PlateReader reader = PlateReaderService.getPlateReader((PlateBasedAssayProvider) provider, BioTekPlateReader.TYPE);
            if (reader != null)
            {
                PlateTemplate template = ((PlateBasedAssayProvider)provider).getPlateTemplate(container, protocol);
                double[][] cellValues = reader.loadFile(template, dataFile);
                Plate plate = PlateService.get().createPlate(template, cellValues);

                // collect the standards from the control group so we can calculate the calibration curve
                List<? extends WellGroup> controlGroups = plate.getWellGroups(WellGroup.Type.CONTROL);
                // TODO: add validation in the plate template handler
                assert(controlGroups.size() == 1);

                if (context instanceof AssayUploadXarContext)
                {
                    try {

                        WellGroup controlGroup = controlGroups.get(0);
                        SimpleRegression regression = new SimpleRegression(true);
                        double[] concentrations = {100, 50, 25, 12.5, 6.25, 0};
                        int i = 0;
                        for (WellGroup replicate : controlGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                        {
                            double mean = replicate.getMean();
                            double concentration = concentrations[i++];

                            regression.addData(mean, concentration);
                        }
                        // add the coefficient of determination to the run
                        DomainProperty cod = runProperties.get(ElisaAssayProvider.CORRELATION_COEFFICIENT_PROPERTY_NAME);
                        if (cod != null)
                            data.getRun().setProperty(context.getUser(), cod.getPropertyDescriptor(), regression.getRSquare());

                        for (Position position : controlGroup.getPositions())
                        {
                            Map<String, Object> row = new HashMap<String, Object>();

                            Well well = plate.getWell(position.getRow(), position.getColumn());
                            row.put(ElisaAssayProvider.WELL_PROPERTY_NAME, position.getDescription());
                            row.put(ElisaAssayProvider.WELLGROUP_PROPERTY_NAME, controlGroup.getPositionDescription());
                            row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY_NAME, well.getValue());

                            results.add(row);
                        }

                        for (WellGroup sampleGroup : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            for (WellGroup replicate : sampleGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                            {
                                for (Position position : replicate.getPositions())
                                {
                                    Map<String, Object> row = new HashMap<String, Object>();
                                    Well well = plate.getWell(position.getRow(), position.getColumn());

                                    row.put(ElisaAssayProvider.WELL_PROPERTY_NAME, position.getDescription());
                                    row.put(ElisaAssayProvider.WELLGROUP_PROPERTY_NAME, replicate.getPositionDescription());
                                    row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY_NAME, well.getValue());

                                    // compute the concentration
                                    double concentration = regression.predict(well.getValue());
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
        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        datas.put(DATA_TYPE, results);

        return datas;
    }
}
