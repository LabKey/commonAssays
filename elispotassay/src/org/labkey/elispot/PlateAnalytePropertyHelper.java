/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.api.exp.DuplicateMaterialException;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.elispot.plate.FluorescentPlateInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aaronr on 3/23/15.
 * Compare to PlateAntigenPropertyHelper
 */
public class PlateAnalytePropertyHelper extends SamplePropertyHelper<String>
{
    private List<String> _analyteNames;

    public PlateAnalytePropertyHelper(ElispotRunUploadForm form, List<? extends DomainProperty> antigenDomainProperties) throws ExperimentException
    {
        super(antigenDomainProperties);
        _analyteNames = new ArrayList<>();

        Map<String, File> dataFiles = form.getUploadedData();
        if (dataFiles.containsKey(AssayDataCollector.PRIMARY_FILE))
        {
            File file = dataFiles.get(AssayDataCollector.PRIMARY_FILE);
            PlateReader reader;
            PlateTemplate template = form.getProvider().getPlateTemplate(form.getContainer(), form.getProtocol());

            // populate property name to value map
            Map<String, String> runPropMap = new HashMap<>();
            for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
                runPropMap.put(entry.getKey().getName(), entry.getValue());

            if (runPropMap.containsKey(ElispotAssayProvider.READER_PROPERTY_NAME))
            {
                reader = form.getProvider().getPlateReader(runPropMap.get(ElispotAssayProvider.READER_PROPERTY_NAME));
                for (Map.Entry<String, double[][]> entry : reader.loadMultiGridFile(template, file).entrySet())
                {
                    // attempt to parse the plate grid annotation into a PlateInfo object
                    FluorescentPlateInfo plateInfo = FluorescentPlateInfo.create(entry.getKey());
                    if (plateInfo != null)
                    {
                        if (plateInfo.getMeasurement().equals(ElispotDataHandler.SFU_PROPERTY_NAME))
                        {
                            _analyteNames.add(plateInfo.getAnalyte());
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> getSampleNames()
    {
        return _analyteNames;
    }

    @Override
    protected String getObject(int index, Map<DomainProperty, String> sampleProperties) throws DuplicateMaterialException
    {
        String analyteName = _analyteNames.get(index);
        if (analyteName != null)
            return analyteName;

        throw new IndexOutOfBoundsException("Requested #" + index + " but there were only " + _analyteNames.size() + " well group templates");
    }

    @Override
    protected boolean isCopyable(DomainProperty pd)
    {
        return false;
    }
}
