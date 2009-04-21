/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.api.security.ACL;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;

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
public class XarAssayForm extends AssayRunUploadForm<XarAssayProvider>
{
    private Map<File, ExpMaterial> _fileFractionMap = new HashMap<File, ExpMaterial>();

    public boolean isFractions()
    {
        return Boolean.parseBoolean(getRequest().getParameter(FractionsDisplayColumn.FRACTIONS_FIELD_NAME));
    }

    @Override
    public XarAssayDataCollector getSelectedDataCollector()
    {
        return (XarAssayDataCollector)super.getSelectedDataCollector();
    }

    public Map<ExpMaterial, String> getInputMaterials() throws ExperimentException
    {
        Map<ExpMaterial, String> result = new HashMap<ExpMaterial, String>();
        int count = SampleChooserDisplayColumn.getSampleCount(getRequest(), 1);
        for (int i = 0; i < count; i++)
        {
            ExpMaterial material = SampleChooserDisplayColumn.getMaterial(i, getContainer(), getRequest());
            if (!material.getContainer().hasPermission(getUser(), ACL.PERM_READ))
            {
                throw new ExperimentException("You do not have permission to reference the sample '" + material.getName() + ".");
            }
            if (result.containsKey(material))
            {
                throw new ExperimentException("The same material cannot be used multiple times");
            }
            result.put(material, "Sample " + (i + 1));
        }
        return result;
    }

    public List<File> getAllFiles()
    {
        List<File> result = new ArrayList<File>();
        for (Map<String, File> fileSet : getSelectedDataCollector().getFileCollection(this))
        {
            result.addAll(fileSet.values());
        }
        return result;
    }

    public Map<File, ExpMaterial> getFileFractionMap()
    {
        return _fileFractionMap;
    }
}
