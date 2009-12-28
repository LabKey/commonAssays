/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
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
    public static final AssayDataType ELISPOT_DATA_TYPE = new AssayDataType("ElispotAssayData", new FileType(Arrays.asList(".txt", ".xls"), ".txt"));

    public static final String ELISPOT_DATA_LSID_PREFIX = "ElispotAssayData";
    public static final String ELISPOT_DATA_ROW_LSID_PREFIX = "ElispotAssayDataRow";
    public static final String ELISPOT_PROPERTY_LSID_PREFIX = "ElispotProperty";
    public static final String ELISPOT_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";

    public static final String SFU_PROPERTY_NAME = "SpotCount";
    public static final String WELLGROUP_PROPERTY_NAME = "WellgroupName";

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

            for (ObjectProperty property : run.getObjectProperties().values())
            {
                if (ElispotAssayProvider.READER_PROPERTY_NAME.equals(property.getName()))
                {
                    ElispotPlateReaderService.I reader = ElispotPlateReaderService.getPlateReaderFromName(property.getStringValue(), _info.getContainer());
                    Plate plate = initializePlate(_dataFile, template, reader);

                    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
                    Map<String, ExpMaterial> materialMap = new HashMap<String, ExpMaterial>();
                    for (ExpMaterial material : run.getMaterialInputs().keySet())
                        materialMap.put(material.getName(), material);

                    for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                    {
                        for (Position pos : group.getPositions())
                        {
                            Well well = plate.getWell(pos.getRow(), pos.getColumn());
                            ExpMaterial material = materialMap.get(group.getName());
                            if (material != null)
                            {
                                Map<String, Object> row = new LinkedHashMap<String, Object>();

                                Lsid dataRowLsid = getDataRowLsid(_data.getLSID(), pos);

                                row.put(DATA_ROW_LSID_PROPERTY, dataRowLsid.toString());
                                row.put(ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID().toString());
                                row.put(SFU_PROPERTY_NAME, well.getValue());
                                row.put(WELLGROUP_PROPERTY_NAME, group.getName());
                                row.put(WELLGROUP_LOCATION_PROPERTY, pos.toString());

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

    public void importTransformDataMap(ExpData data, User user, ExpRun run, ExpProtocol protocol, AssayProvider provider, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importData(data, run, protocol, dataMap);
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ElispotDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        datas.put(ELISPOT_DATA_TYPE, parser.getResults());

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
