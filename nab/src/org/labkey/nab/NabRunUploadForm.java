/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.api.*;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.study.actions.PlateUploadForm;
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
public class NabRunUploadForm extends PlateUploadForm<NabAssayProvider> implements AssayRunUploadContext
{
    private ExpRun _reRun;
    private Integer _reRunId;
    private Map<String, Map<DomainProperty, String>> _sampleProperties;

    public Integer getReRunId()
    {
        return _reRunId;
    }

    public void setReRunId(Integer reRunId)
    {
        _reRunId = reRunId;
    }

    public Map<String, Map<DomainProperty, String>> getSampleProperties()
    {
        return _sampleProperties;
    }

    public void setSampleProperties(Map<String, Map<DomainProperty, String>> sampleProperties)
    {
        _sampleProperties = sampleProperties;
    }

    public ExpRun getReRun()
    {
        if (_reRunId != null)
        {
            _reRun = ExperimentService.get().getExpRun(_reRunId);
            if (_reRun == null)
            {
                throw new NotFoundException("NAb run " + _reRunId + " could not be found.");
            }
        }
        return _reRun;
    }

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain, String scope) throws ExperimentException
    {
        ExpRun reRun = getReRun();
        if (reRun != null)
        {
            try
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
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        else
            return super.getDefaultValues(domain, scope);
    }

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain) throws ExperimentException
    {
        ExpRun reRun = getReRun();
        if (reRun != null)
        {
            try
            {
                Map<DomainProperty, Object> ret = new HashMap<DomainProperty, Object>();
                String batchDomainURI = NabAssayProvider.getDomainURIForPrefix(getProtocol(), ExpProtocol.ASSAY_DOMAIN_BATCH);
                if (batchDomainURI.equals(domain.getTypeURI()))
                {
                    // we're getting batch values
                    ExpExperiment batch = AssayService.get().findBatch(reRun);
                    if (batch != null)
                    {
                        Map<String, ObjectProperty> batchProperties = batch.getObjectProperties();
                        for (DomainProperty property : domain.getProperties())
                        {
                            ObjectProperty value = batchProperties.get(property.getPropertyURI());
                            ret.put(property, value != null ? value.value() : null);
                        }
                    }
                }
                else
                {
                    // we're getting run values
                    Map<String, Object> values = OntologyManager.getProperties(getContainer(), reRun.getLSID());
                    for (DomainProperty property : domain.getProperties())
                        ret.put(property, values.get(property.getPropertyURI()));

                    // bad hack here to temporarily create domain properties for name and comments.  These need to be
                    // repopulated just like the rest of the domain properties, but they aren't actually part of the
                    // domain- they're hard columns on the ExperimentRun table.  Since the DomainProperty objects are
                    // just used to calculate the input form element names, this hack works to pre-populate the values.
                    DomainProperty nameProperty = domain.addProperty();
                    nameProperty.setName("Name");
                    ret.put(nameProperty, reRun.getName());
                    nameProperty.delete();

                    DomainProperty commentsProperty = domain.addProperty();
                    commentsProperty.setName("Comments");
                    ret.put(commentsProperty, reRun.getComments());
                    commentsProperty.delete();
                }
                return ret;
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        else
            return super.getDefaultValues(domain);
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
