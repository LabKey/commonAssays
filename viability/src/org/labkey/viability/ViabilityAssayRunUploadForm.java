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
import org.labkey.api.data.ColumnInfo;
import static org.labkey.api.action.SpringActionController.ERROR_MSG;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConversionException;
import org.springframework.validation.BindException;

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
    private ViabilityAssayDataHandler.Parser _parser;
    private List<Map<String, Object>> _resultProperties;

    public String[] getPoolIds() { return _poolIDs; }
    public void setPoolIds(String[] poolIDs) { _poolIDs = poolIDs; }

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

    private void parseUploadedFile() throws ExperimentException
    {
        if (_parser == null)
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
            List<DomainProperty> domainProperties = Arrays.asList(resultsDomain.getProperties());

            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(_poolIDs.length);
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

    private Map<String, Object> getPropertyMapFromRequest(List<DomainProperty> columns, int rowIndex, String poolID, BindException errors) throws ExperimentException
    {
        String inputPrefix = INPUT_PREFIX + poolID + "_" + rowIndex;
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
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
                    errors.reject(ERROR_MSG, label + " is required and must be of type " + ColumnInfo.getFriendlyTypeName(type) + ".");

                if (dp.getName().equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                {
                    // get SpecimenIDs from request as a List<String>
                    String[] values = getRequest().getParameterValues(paramName);
                    List<String> specimenIDs = new ArrayList<String>(values.length);
                    for (String specimenID : values)
                    {
                        if (specimenID != null && specimenID.trim().length() > 0)
                            specimenIDs.add(specimenID.trim());
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

                        errors.reject(ERROR_MSG, message);
                    }
                }
            }
            properties.put(dp.getName(), value);
        }
        return properties;
    }

}
