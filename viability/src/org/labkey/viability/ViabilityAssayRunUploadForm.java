/*
 * Copyright (c) 2009 LabKey Corporation
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

import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConversionException;

import java.util.*;
import java.io.File;

/**
 * User: kevink
 * Date: Sep 19, 2009
 */
public class ViabilityAssayRunUploadForm extends AssayRunUploadForm<ViabilityAssayProvider>
{
    public static String INPUT_PREFIX = "_pool_";

    private String[] _poolIDs;
    private List<Map<String, Object>> _parsedData;
    private List<Map<String, Object>> _resultProperties;

    public String[] getPoolIds() { return _poolIDs; }
    public void setPoolIds(String[] poolIDs) { _poolIDs = poolIDs; }

    /** Read rows from a posted file. */
    public List<Map<String, Object>> getParsedData() throws ExperimentException
    {
        if (_parsedData == null)
        {
            Map<String, File> uploaded = getUploadedData();
            assert uploaded.size() == 1;
            File file = uploaded.values().iterator().next();

            AssayProvider provider = AssayService.get().getProvider(getProtocol());
            Domain runDomain = provider.getRunDomain(getProtocol());
            Domain resultDomain = provider.getResultsDomain(getProtocol());

            ViabilityAssayDataHandler.Parser parser;
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".tsv") || fileName.endsWith(".txt"))
                parser = new ViabilityTsvDataHandler.Parser(runDomain, resultDomain, file);
            else if (fileName.endsWith(".csv"))
                parser = new GuavaDataHandler.Parser(runDomain, resultDomain, file);
            else
                throw new ExperimentException("Don't know how to parse uploaded file: " + fileName);
            
            List<Map<String, Object>> rows = parser.getResultData();
            ViabilityAssayDataHandler.validateData(rows, false);

            _parsedData = rows;
        }

        return _parsedData;
    }

    /** Get the form posted values and attempt to convert them. */
    public List<Map<String, Object>> getResultProperties() throws ExperimentException
    {
        if (_resultProperties == null)
        {
            if (_poolIDs == null || _poolIDs.length == 0)
                throw new ExperimentException("No rows!");

            Domain resultsDomain = getProvider().getResultsDomain(getProtocol());
            List<DomainProperty> domainProperties = Arrays.asList(resultsDomain.getProperties());

            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(_poolIDs.length);
            for (int rowIndex = 0; rowIndex < _poolIDs.length; rowIndex++)
            {
                String poolID = _poolIDs[rowIndex];
                Map<String, Object> row = getPropertyMapFromRequest(domainProperties, rowIndex, poolID);
                rows.add(row);
            }

            _resultProperties = rows;
        }

        return _resultProperties;
    }

    private Map<String, Object> getPropertyMapFromRequest(List<DomainProperty> columns, int rowIndex, String poolID) throws ExperimentException
    {
        String inputPrefix = INPUT_PREFIX + poolID + "_" + rowIndex;
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        for (DomainProperty dp : columns)
        {
            Object value;
            if (dp.getName().equals(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME))
            {
                value = poolID;
            }
            else
            {
                String paramName = UploadWizardAction.getInputName(dp, inputPrefix);
                String parameter = getRequest().getParameter(paramName);
                if (dp.isRequired() && dp.getPropertyDescriptor().getPropertyType() == PropertyType.BOOLEAN &&
                        (parameter == null || parameter.length() == 0))
                    parameter = Boolean.FALSE.toString();

                PropertyDescriptor pd = dp.getPropertyDescriptor();
                if (dp.getName().equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                {
                    // get SpecimenIDs from request as a String array
                    value = getRequest().getParameterValues(paramName);
                }
                else
                {
                    Class type = pd.getPropertyType().getJavaType();
                    try
                    {
                        value = ConvertUtils.convert(parameter, type);
                    }
                    catch (ConversionException ex)
                    {
                        throw new ExperimentException("Failed to convert property '" + dp.getName() + "' from '" + parameter + "' to a " + type.getSimpleName());
                    }
                }
            }
            properties.put(dp.getName(), value);
        }
        return properties;
    }

}
