/*
 * Copyright (c) 2009-2014 LabKey Corporation
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

package org.labkey.viability;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayFileWriter;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.springframework.validation.BindException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.api.action.SpringActionController.ERROR_MSG;

/**
 * User: kevink
 * Date: Sep 19, 2009
 */
public class ViabilityAssayRunUploadForm extends AssayRunUploadForm<ViabilityAssayProvider>
{
    public static String INPUT_PREFIX = "_pool_";
    private static String FRONTIER_SCIENCE_BARCODE_PREFIX = "\u221fFSQ";

    private boolean _delete;
    
    private String[] _poolIDs;
    private ViabilityAssayDataHandler.Parser _parser;
    private List<Map<String, Object>> _resultProperties;

    public String[] getPoolIds() { return _poolIDs; }
    public void setPoolIds(String[] poolIDs) { _poolIDs = poolIDs; }

    public boolean isDelete() { return _delete; }
    public void setDelete(boolean delete) { _delete = delete; }

    /** Read rows from a posted file. */
    public List<Map<String, Object>> getParsedResultData() throws ExperimentException
    {
        parseUploadedFile();
        return _parser.getResultData();
    }

    public Map<DomainProperty, Object> getParsedRunData() throws ExperimentException
    {
        parseUploadedFile();
        return _parser.getRunData();
    }

    public File getUploadedFile() throws ExperimentException
    {
        Map<String, File> uploaded = getUploadedData();
        assert uploaded.containsKey(AssayDataCollector.PRIMARY_FILE);
        return uploaded.get(AssayDataCollector.PRIMARY_FILE);
    }

    private void parseUploadedFile() throws ExperimentException
    {
        if (_parser == null)
        {
            File file = getUploadedFile();

            ViabilityAssayDataHandler.Parser parser = ViabilityAssayDataHandler.createParser(file, getProtocol());
            List<Map<String, Object>> rows = parser.getResultData();
            ViabilityAssayDataHandler.validateData(rows, false);
            _parser = parser;
        }
    }

    /** Get the form posted values and attempt to convert them. */
    public List<Map<String, Object>> getResultProperties(BindException errors) throws ExperimentException
    {
        if (_resultProperties == null)
        {
            if (_poolIDs == null || _poolIDs.length == 0)
                throw new ExperimentException("No rows!");

            Domain resultsDomain = getProvider().getResultsDomain(getProtocol());
            List<? extends DomainProperty> domainProperties = resultsDomain.getProperties();

            List<Map<String, Object>> rows = new ArrayList<>(_poolIDs.length);
            for (int rowIndex = 0; rowIndex < _poolIDs.length; rowIndex++)
            {
                String poolID = _poolIDs[rowIndex];
                Map<String, Object> row = getPropertyMapFromRequest(domainProperties, rowIndex, poolID, errors);
                rows.add(row);
            }

            _resultProperties = rows;
        }

        return _resultProperties;
    }

    private Map<String, Object> getPropertyMapFromRequest(List<? extends DomainProperty> columns, int rowIndex, String poolID, BindException errors) throws ExperimentException
    {
        String inputPrefix = INPUT_PREFIX + poolID + "_" + rowIndex;
        Map<String, Object> properties = new LinkedHashMap<>();
        for (DomainProperty dp : columns)
        {
            Object value = null;
            if (dp.getName().equals(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME))
            {
                value = poolID;
            }
            else
            {
                String label = dp.getPropertyDescriptor().getNonBlankCaption();
                String paramName = UploadWizardAction.getInputName(dp, inputPrefix);
                String parameter = getRequest().getParameter(paramName);
                PropertyDescriptor pd = dp.getPropertyDescriptor();
                Class type = pd.getPropertyType().getJavaType();

                if (dp.isRequired() && dp.getPropertyDescriptor().getPropertyType() == PropertyType.BOOLEAN &&
                        (parameter == null || parameter.length() == 0))
                    parameter = Boolean.FALSE.toString();

                if (dp.isRequired() && (parameter == null || parameter.length() == 0))
                {
                    String msg = label + " is required and must be of type " + ColumnInfo.getFriendlyTypeName(type) + ".";
                    if (errors == null)
                        throw new ExperimentException(msg);
                    errors.reject(ERROR_MSG, msg);
                }

                if (dp.getName().equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                {
                    // get SpecimenIDs from request as a List<String>
                    String[] values = getRequest().getParameterValues(paramName);
                    List<String> specimenIDs = new ArrayList<>(values.length);
                    for (String specimenID : values)
                    {
                        if (specimenID != null)
                        {
                            specimenID = specimenID.trim();
                            if (specimenID.startsWith(FRONTIER_SCIENCE_BARCODE_PREFIX))
                                specimenID = specimenID.substring(FRONTIER_SCIENCE_BARCODE_PREFIX.length());
                            if (specimenID.length() > 0)
                                specimenIDs.add(specimenID);
                        }
                    }
                    value = specimenIDs;
                }
                else
                {
                    try
                    {
                        value = ConvertUtils.convert(parameter, type);
                    }
                    catch (ConversionException e)
                    {
                        String message = label + " must be of type " + ColumnInfo.getFriendlyTypeName(type) + ".";
                        message +=  "  Value \"" + parameter + "\" could not be converted";
                        if (e.getCause() instanceof ArithmeticException)
                            message +=  ": " + e.getCause().getLocalizedMessage();
                        else
                            message += ".";

                        if (errors == null)
                            throw new ExperimentException(message);
                        errors.reject(ERROR_MSG, message);
                    }
                }
            }
            properties.put(dp.getName(), value);
        }
        return properties;
    }

    /**
     * Returns a Map of PoolId to a Map of property names and values.
     */
    public Map<String, Map<String, Object>> getReRunResults() throws ExperimentException
    {
        Map<String, Map<String, Object>> ret = Collections.emptyMap();

        ExpRun reRun = getReRun();
        if (reRun != null)
        {
            ret = new HashMap<>();
            ViabilityResult[] results = ViabilityManager.getResults(reRun, reRun.getContainer());
            for (ViabilityResult result : results)
            {
                String poolID = result.getPoolID();
                String lowerPoolID = poolID.replaceAll(" ", "").toLowerCase();
                // XXX: there can be more than one pool id in a run
                if (!ret.containsKey(lowerPoolID))
                    ret.put(lowerPoolID, result.toMap());
            }
        }

        return ret;
    }

    @Override @NotNull
    public Map<String, File> getUploadedData() throws ExperimentException
    {
        // We don't want to re-populate the upload form with the re-run file if this is a reshow due to error during
        // a re-upload process:
        Map<String, File> currentUpload = super.getUploadedData();
        if (currentUpload.isEmpty())
        {
            ExpRun reRun = getReRun();
            if (reRun != null)
            {
                List<ExpData> inputs = reRun.getDataOutputs();
                if (inputs.size() > 0)
                {
                    File dataFile = inputs.get(0).getFile();
                    if (dataFile.exists())
                    {
                        AssayFileWriter writer = new AssayFileWriter();
                        File dup = writer.safeDuplicate(getViewContext(), dataFile);
                        return Collections.singletonMap(AssayDataCollector.PRIMARY_FILE, dup);
                    }
                }
            }
        }
        return currentUpload;
    }
}
