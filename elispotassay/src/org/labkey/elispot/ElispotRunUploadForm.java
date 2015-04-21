/*
 * Copyright (c) 2008-2015 LabKey Corporation
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

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.PlateUploadForm;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: Karl Lum
 * Date: Jan 9, 2008
 */
public class ElispotRunUploadForm extends AssayRunUploadForm<ElispotAssayProvider> implements PlateUploadForm<ElispotAssayProvider>
{
    private Map<String, Map<DomainProperty, String>> _antigenProperties;
    private Map<String, Map<DomainProperty, String>> _analyteProperties;
    private Map<String, Map<DomainProperty, String>> _sampleProperties;
    private PlateSamplePropertyHelper _samplePropertyHelper;

    public PlateSamplePropertyHelper getSamplePropertyHelper()
    {
        return _samplePropertyHelper;
    }

    public void setSamplePropertyHelper(PlateSamplePropertyHelper helper)
    {
        _samplePropertyHelper = helper;
    }

    public Map<String, Map<DomainProperty, String>> getSampleProperties()
    {
        return _sampleProperties;
    }

    public void setSampleProperties(Map<String, Map<DomainProperty, String>> sampleProperties)
    {
        _sampleProperties = sampleProperties;
    }

    public Map<String, Map<DomainProperty, String>> getAntigenProperties()
    {
        return _antigenProperties;
    }

    public void setAntigenProperties(Map<String, Map<DomainProperty, String>> antigenProperties)
    {
        _antigenProperties = antigenProperties;
    }

    public Map<String, Map<DomainProperty, String>> getAnalyteProperties()
    {
        return _analyteProperties;
    }

    public void setAnalyteProperties(Map<String, Map<DomainProperty, String>> analyteProperties)
    {
        _analyteProperties = analyteProperties;
    }

    @Override
    public Map<DomainProperty, String> getRunProperties() throws ExperimentException
    {
        ElispotAssayProvider.DetectionMethodType method = getProvider().getDetectionMethod(getContainer(), getProtocol());
        if (method == ElispotAssayProvider.DetectionMethodType.FLUORESCENT)
        {
            Map<DomainProperty, String> runProperties = new LinkedHashMap<>();
            for (Map.Entry<DomainProperty, String> entry : super.getRunProperties().entrySet())
            {
                // don't include the background subtraction property
                if (!entry.getKey().getName().equalsIgnoreCase(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME))
                    runProperties.put(entry.getKey(), entry.getValue());
            }
            return runProperties;
        }
        else
            return super.getRunProperties();
    }
}
