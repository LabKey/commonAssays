/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.nab;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.exp.*;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.api.*;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.study.actions.PlateUploadForm;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.view.NotFoundException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.sql.SQLException;
import java.io.File;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 4:00:02 PM
 */
public class NabRunUploadForm extends AssayRunUploadForm<NabAssayProvider> implements PlateUploadForm<NabAssayProvider>
{
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

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain, String scope) throws ExperimentException
    {
        ExpRun reRun = getReRun();
        if (reRun != null)
        {
            ExpMaterial selected = null;
            for (Map.Entry<ExpMaterial, String> entry : reRun.getMaterialInputs().entrySet())
            {
                if (entry.getValue().equals(scope))
                {
                    selected = entry.getKey();
                    break;
                }
            }
            if (selected == null)
                throw new NotFoundException("NAb run input " + scope + " could not be found for run " + getReRunId() + ".");
            Map<String, Object> values = OntologyManager.getProperties(getContainer(), selected.getLSID());
            Map<DomainProperty, Object> ret = new HashMap<DomainProperty, Object>();
            for (DomainProperty property : domain.getProperties())
                ret.put(property, values.get(property.getPropertyURI()));
            return ret;
        }
        else
            return super.getDefaultValues(domain, scope);
    }

    @Override @NotNull
    public Map<String, File> getUploadedData() throws ExperimentException
    {
        // we don't want to re-populate the upload form with the re-run file if this is a reshow due to error during
        // a re-upload process:
        Map<String, File> currentUpload = super.getUploadedData();
        if (currentUpload.isEmpty())
        {
            ExpRun reRun = getReRun();
            if (reRun != null)
            {
                List<ExpData> outputs = reRun.getDataOutputs();
                File dataFile = null;
                for (ExpData data : outputs)
                {
                    File possibleFile = data.getFile();
                    String dataLsid = data.getLSID();
                    if (possibleFile != null && dataLsid != null && getProvider().getDataType().matches(new Lsid(dataLsid)))
                    {
                        if (dataFile != null)
                        {
                            throw new IllegalStateException("NAb runs are expected to produce a single file output. " +
                                    dataFile.getPath() + " and " + possibleFile.getPath() + " are both associated with run " + reRun.getRowId());
                        }
                        dataFile = possibleFile;
                    }
                }
                if (dataFile == null)
                    throw new IllegalStateException("NAb runs are expected to produce a file output.");

                if (dataFile.exists())
                {
                    AssayFileWriter writer = new AssayFileWriter();
                    File dup = writer.safeDuplicate(getViewContext(), dataFile);
                    return Collections.singletonMap(AssayDataCollector.PRIMARY_FILE, dup);
                }
            }
        }
        return currentUpload;
    }
}
