/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayUploadXarContext;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.util.FileType;
import org.labkey.elispot.plate.ElispotPlateReaderService;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 8, 2008
 */
public class ElispotDataHandler extends AbstractElispotDataHandler implements TransformDataHandler
{
    public static final AssayDataType ELISPOT_DATA_TYPE = new AssayDataType("ElispotAssayData", new FileType(Arrays.asList(".txt", ".xls", ".xlsx"), ".txt"));
    public static final DataType ELISPOT_TRANSFORMED_DATA_TYPE = new DataType("ElispotTransformedData"); // a marker data type

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

            Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
            for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
                runProperties.put(column.getName(), column);

            if (runProperties.containsKey(ElispotAssayProvider.READER_PROPERTY_NAME))
            {
                DomainProperty readerProp = runProperties.get(ElispotAssayProvider.READER_PROPERTY_NAME);
                if (_context instanceof AssayUploadXarContext)
                {
                    Map<DomainProperty, String> runPropValues = ((AssayUploadXarContext)_context).getContext().getRunProperties();
                    ElispotPlateReaderService.I reader = ElispotPlateReaderService.getPlateReaderFromName(runPropValues.get(readerProp), _info.getContainer());
                    Plate plate = initializePlate(_dataFile, template, reader);

                    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
                    Map<String, ExpMaterial> materialMap = new HashMap<String, ExpMaterial>();
                    for (ExpMaterial material : run.getMaterialInputs().keySet())
                        materialMap.put(material.getName(), material);

                    for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                    {
                        ExpMaterial material = materialMap.get(group.getName());
                        if (material != null)
                        {
                            for (Position pos : group.getPositions())
                            {
                                Well well = plate.getWell(pos.getRow(), pos.getColumn());
                                Map<String, Object> row = new LinkedHashMap<String, Object>();

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

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ElispotDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        datas.put(ELISPOT_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    public static Plate initializePlate(File dataFile, PlateTemplate template, ElispotPlateReaderService.I reader) throws ExperimentException
    {
        if (reader != null)
        {
            double[][] cellValues = reader.loadFile(template, dataFile);
            return PlateService.get().createPlate(template, cellValues);
        }
        return null;
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
}
