/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.ms2.metadata;

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.actions.BulkPropertiesUploadForm;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: phussey
 * Date: Sep 17, 2007
 * Time: 12:30:55 AM
 */
public class MassSpecMetadataAssayForm extends BulkPropertiesUploadForm<MassSpecMetadataAssayProvider>
{
    private Map<File, ExpMaterial> _fileFractionMap = new HashMap<>();

    private static final String SAMPLE_COLUMN_PREFIX = "Sample";

    public boolean isFractions()
    {
        return Boolean.parseBoolean(getRequest().getParameter(FractionsDisplayColumn.FRACTIONS_FIELD_NAME));
    }

    public Map<ExpMaterial, String> getStartingMaterials() throws ExperimentException
    {
        Map<ExpMaterial, String> result = new HashMap<>();
        if (isBulkUploadAttempted())
        {
            Map<String, Object> values = getBulkProperties();

            int suffix = 1;
            Object o = values.get(SAMPLE_COLUMN_PREFIX + suffix);
            if (o == null)
            {
                // Allow either "Sample" or "Sample1" for the first sample
                o = values.get(SAMPLE_COLUMN_PREFIX);
            }
            if (o == null)
            {
                throw new ExperimentException("At least one sample is required.");
            }

            do
            {
                ExpMaterial material = resolveSample(o.toString());
                if (result.containsKey(material))
                {
                    throw new ExperimentException("The same material, " + material.getName() + ", cannot be used multiple times for the same mzXML file.");
                }
                result.put(material, "Sample " + suffix);

                suffix++;
                o = values.get(SAMPLE_COLUMN_PREFIX + suffix);
            }
            while (o != null);
        }
        else
        {
            int count = SampleChooserDisplayColumn.getSampleCount(getRequest(), 1);
            for (int i = 0; i < count; i++)
            {
                ExpMaterial material = SampleChooserDisplayColumn.getMaterial(i, getContainer(), getRequest());
                if (!material.getContainer().hasPermission(getUser(), ReadPermission.class))
                {
                    throw new ExperimentException("You do not have permission to reference the sample '" + material.getName() + ".");
                }
                if (result.containsKey(material))
                {
                    throw new ExperimentException("The same material, " + material.getName() + ", cannot be used multiple times for the same mzXML file.");
                }
                result.put(material, "Sample " + (i + 1));
            }
        }
        return result;
    }

    public List<File> getAllFiles()
    {
        List<File> result = new ArrayList<>();
        for (Map<String, File> fileSet : getSelectedDataCollector().getFileQueue(this))
        {
            result.addAll(fileSet.values());
        }
        return result;
    }

    public Map<File, ExpMaterial> getFileFractionMap()
    {
        return _fileFractionMap;
    }

    public Map<String, Object> getBulkProperties() throws ExperimentException
    {
        File file = getUploadedData().get(AssayDataCollector.PRIMARY_FILE);

        // Try both the full file name and without the extension
        return getProperties(file);
    }

    @Override
    public String getHelpPopupHTML()
    {
        return "<p>You may use a set of TSV (tab-separated values) to specify run metadata.</p>" +
                "<p>The Filename column in the TSV is matched with the mzXML file's name. " +
                "The sample name columns (Sample, Sample2, Sample3, etc), " +
                "will be used to look up matching samples by name in all visible sample sets.</p>" +
                "<p>Any additional run level properties may be specified as separate columns.</p>";
    }

    private Map<String, Object> getProperties(File file)
            throws ExperimentException
    {
        return getProperties(file.getName(), FileUtil.getBaseName(file));
    }

    protected String getIdentifierColumnName()
    {
        return "filename";
    }

    public Map<File, Map<DomainProperty, String>> getFractionProperties(ExpSampleSet sampleSet) throws ExperimentException
    {
        List<File> files = getAllFiles();
        if (!isBulkUploadAttempted())
        {
            MsFractionPropertyHelper helper = new MsFractionPropertyHelper(sampleSet, files, getContainer());
            return helper.getSampleProperties(getRequest());
        }
        else
        {
            List<? extends DomainProperty> props = sampleSet.getType().getProperties();
            Map<File, Map<DomainProperty, String>> result = new HashMap<>();
            for (File file : files)
            {
                Map<String, Object> fileRawValues = getProperties(file);
                Map<DomainProperty, String> fileValues = new HashMap<>();
                for (DomainProperty prop : props)
                {
                    fileValues.put(prop, getPropertyValue(fileRawValues, prop));
                }
                result.put(file, fileValues);
            }
            return result;
        }
    }
}
