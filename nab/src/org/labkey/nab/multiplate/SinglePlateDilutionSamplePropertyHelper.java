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

import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.PlateSampleFilePropertyHelper;
import org.labkey.nab.NabAssayProvider;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/24/13
 */
public class SinglePlateDilutionSamplePropertyHelper extends PlateSampleFilePropertyHelper
{
    public SinglePlateDilutionSamplePropertyHelper(Container c, ExpProtocol protocol, DomainProperty[] sampleProperties, PlateTemplate template)
    {
        super(c, protocol, sampleProperties, template);
    }

    @Override
    public Map<String, Map<DomainProperty, String>> getSampleProperties(HttpServletRequest request) throws ExperimentException
    {
        if (_sampleProperties != null)
            return _sampleProperties;

        File metadataFile = getSampleMetadata(request);
        if (metadataFile == null)
            return null;

        Map<String, Map<DomainProperty, String>> allProperties = new HashMap<String, Map<DomainProperty, String>>();
        try
        {
            List<WellGroupTemplate> sampleGroups = getSampleWellGroups();
            Map<String, WellGroupTemplate> sampleGroupNames = new HashMap<String, WellGroupTemplate>(sampleGroups.size());
            for (WellGroupTemplate sampleGroup : sampleGroups)
                sampleGroupNames.put(sampleGroup.getName(), sampleGroup);

            ExcelLoader loader = new ExcelLoader(metadataFile, true);
            boolean hasSampleNameCol = false;
            boolean hasVirusIdCol = false;
            for (ColumnDescriptor col : loader.getColumns())
            {
                hasSampleNameCol = WELLGROUP_COLUMN.equals(col.name) || hasSampleNameCol;
                hasVirusIdCol = NabAssayProvider.VIRUS_NAME_PROPERTY_NAME.equals(col.name) || hasVirusIdCol;
            }

            if (!hasSampleNameCol)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + WELLGROUP_COLUMN + "\".");
            if (!hasVirusIdCol)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + NabAssayProvider.VIRUS_NAME_PROPERTY_NAME + "\".");

            for (Map<String, Object> row : loader)
            {
                String wellGroupName = (String) row.get(WELLGROUP_COLUMN);
                WellGroupTemplate wellgroup = wellGroupName != null ? sampleGroupNames.get(wellGroupName) : null;

                if (wellgroup != null)
                {
                    String virusId = (String)row.get(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);

                    if (virusId == null)
                        throw new ExperimentException("Every row in the metadata file must contain a value for the column \"" + NabAssayProvider.VIRUS_NAME_PROPERTY_NAME + "\".");

                    String key = getKey(virusId, wellGroupName);
                    Map<DomainProperty, String> sampleProperties = allProperties.get(key);
                    if (sampleProperties == null)
                    {
                        sampleProperties = new HashMap<DomainProperty, String>();
                        allProperties.put(key, sampleProperties);

                        for (DomainProperty property : _domainProperties)
                        {
                            Object value = getValue(row, property);
                            sampleProperties.put(property, value != null ? value.toString() : null);
                        }
                    }
                    else
                    {
                        // if there are duplicates for the same virusid/wellgroup combination, they should
                        // be the same

                        for (DomainProperty property : _domainProperties)
                        {
                            Object value = getValue(row, property);
                            Object existing = sampleProperties.get(property);

                            if (value != null && !value.toString().equals(existing.toString()))
                                throw new ExperimentException("If there are duplicate virusID/wellGroup combinations in the metadata file, they must contain exactly the same field values.");
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException("Unable to parse sample properties file.  Please verify that the file is a valid Excel workbook.", e);
        }
        _sampleProperties = allProperties;
        return _sampleProperties;
    }

    public static String getKey(String virusName, String wellGroup)
    {
        return virusName + "-" + wellGroup;
    }
}
