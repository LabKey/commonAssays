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

package org.labkey.luminex;

import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.study.assay.AssayDataCollector;

import java.util.*;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Aug 8, 2007
 */
public class LuminexRunUploadForm extends AssayRunUploadForm<LuminexAssayProvider>
{
    private String[] _analyteNames;
    private LuminexDataHandler.LuminexDataFileParser _parser;

    public String[] getAnalyteNames()
    {
        return _analyteNames;
    }

    public void setAnalyteNames(String[] analyteNames)
    {
        _analyteNames = analyteNames;
    }

    protected Map<DomainProperty, String> getAnalytePropertyMapFromRequest(List<DomainProperty> columns, String analyteName)
    {
        Map<DomainProperty, String> properties = new LinkedHashMap<DomainProperty, String>();
        for (DomainProperty dp : columns)
        {
            String propName = UploadWizardAction.getInputName(dp);
            String value = getRequest().getParameter("_analyte_" + ColumnInfo.propNameFromName(analyteName) + "_" + propName);
            if (dp.isRequired() && dp.getPropertyDescriptor().getPropertyType() == PropertyType.BOOLEAN &&
                    (value == null || value.length() == 0))
                value = Boolean.FALSE.toString();
            properties.put(dp, value);
        }
        return properties;
    }

    public Map<DomainProperty, String> getAnalyteProperties(String analyteName)
    {
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        List<DomainProperty> domainProperties = Arrays.asList(analyteDomain.getProperties());
        return getAnalytePropertyMapFromRequest(domainProperties, analyteName);
    }

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain, String disambiguationId) throws ExperimentException
    {
        if (!isResetDefaultValues() && LuminexUploadWizardAction.AnalyteStepHandler.NAME.equals(getUploadStep()))
        {
            Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
            DomainProperty[] analyteColumns = analyteDomain.getProperties();
            try
            {
                SQLFragment sql = new SQLFragment("SELECT ObjectURI FROM " + OntologyManager.getTinfoObject() + " WHERE Container = ? AND ObjectId = (SELECT MAX(ObjectId) FROM " + OntologyManager.getTinfoObject() + " o, " + LuminexSchema.getTableInfoAnalytes() + " a WHERE o.ObjectURI = a.LSID AND a.Name = ?)");
                sql.add(getContainer().getId());
                sql.add(disambiguationId);
                String objectURI = Table.executeSingleton(LuminexSchema.getSchema(), sql.getSQL(), sql.getParamsArray(), String.class);
                if (objectURI != null)
                {
                    Map<String, ObjectProperty> values = OntologyManager.getPropertyObjects(getContainer(), objectURI);
                    Map<DomainProperty, Object> ret = new HashMap<DomainProperty, Object>();
                    for (DomainProperty analyteDP : analyteColumns)
                    {
                        ObjectProperty objectProp = values.get(analyteDP.getPropertyURI());
                        if (objectProp != null)
                        {
                            ret.put(analyteDP, objectProp.value());
                        }
                    }
                    return ret;
                }
                else
                    return Collections.emptyMap();

            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        else
        {
            return super.getDefaultValues(domain, disambiguationId);
        }
    }

    public LuminexDataHandler.LuminexDataFileParser getParser() throws ExperimentException
    {
        if (_parser == null)
        {
            _parser = new LuminexExcelDataHandler.LuminexExcelParser(getProtocol(), getUploadedData().get(AssayDataCollector.PRIMARY_FILE));            
        }
        return _parser;
    }

    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException
    {
        // TODO - Make this look at what the user POSTED to figure out which analytes have been mapped to which titrations

        // For now, say that all analytes use all of the titrations identified in the data file
        return getParser().getTitrations();
    }
}
