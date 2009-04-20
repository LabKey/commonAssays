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

package org.labkey.nab;

import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.RuntimeSQLException;

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
public class NabRunUploadForm extends AssayRunUploadForm<NabAssayProvider>
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

    private ExpRun getReRun()
    {
        if (_reRunId != null)
        {
            _reRun = ExperimentService.get().getExpRun(_reRunId);
            if (_reRun == null)
                HttpView.throwNotFound("NAb run " + _reRunId + " could not be found.");
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
                    HttpView.throwNotFound("NAb run input " + scope + " could not be found for run " + _reRunId + ".");
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
                Map<String, Object> values = OntologyManager.getProperties(getContainer(), reRun.getLSID());
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
            return super.getDefaultValues(domain);
    }

    @Override
    public Map<String, File> getUploadedData() throws ExperimentException
    {
        // we don't want to re-populate the upload form with the re-run file if this is a reshow due to error during
        // a re-upload process:
        Map<String, File> currentUpload = super.getUploadedData();
        if (currentUpload == null || currentUpload.isEmpty())
        {
            ExpRun reRun = getReRun();
            if (reRun != null)
            {
                List<ExpData> inputs = reRun.getDataOutputs();
                if (inputs.size() > 1)
                    throw new IllegalStateException("NAb runs are expected to produce one output.");
                File dataFile = inputs.get(0).getDataFile();
                if (dataFile.exists())
                {
                    AssayFileWriter writer = new AssayFileWriter();
                    File dup = writer.safeDuplicate(getViewContext(), dataFile);
                    return Collections.singletonMap(inputs.get(0).getName(), dup);
                }
            }
        }
        return currentUpload;
    }
}
