/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.defaults.DefaultValueService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Aug 8, 2007
 */
public class LuminexRunUploadForm extends AssayRunUploadForm<LuminexAssayProvider> implements LuminexRunContext
{
    private String[] _analyteNames;
    private LuminexExcelParser _parser;

    public String[] getAnalyteNames()
    {
        return _analyteNames;
    }

    public void setAnalyteNames(String[] analyteNames)
    {
        _analyteNames = analyteNames;
    }

    protected Map<DomainProperty, String> getAnalytePropertyMapFromRequest(List<? extends DomainProperty> columns, String analyteName)
    {
        Map<DomainProperty, String> properties = new LinkedHashMap<>();
        for (DomainProperty dp : columns)
        {
            String value = getRequest().getParameter(LuminexUploadWizardAction.getAnalytePropertyName(analyteName, dp));
            if (dp.isRequired() && dp.getPropertyDescriptor().getPropertyType() == PropertyType.BOOLEAN &&
                    (value == null || value.length() == 0))
                value = Boolean.FALSE.toString();
            value = StringUtils.trimToNull(value);
            properties.put(dp, value);
        }
        return properties;
    }

    public Map<DomainProperty, String> getAnalyteProperties(String analyteName)
    {
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        List<? extends DomainProperty> domainProperties = analyteDomain.getProperties();
        return getAnalytePropertyMapFromRequest(domainProperties, analyteName);
    }

    public Map<ColumnInfo, String> getAnalyteColumnProperties(String analyteName)
    {
        Map<ColumnInfo, String> properties = new HashMap<>();
        ColumnInfo col = LuminexProtocolSchema.getTableInfoAnalytes().getColumn(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
        String value = getRequest().getParameter(LuminexUploadWizardAction.getAnalytePropertyName(analyteName, col.getName()));
        value = StringUtils.trimToNull(value);
        properties.put(col, value);
        return properties;
    }

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain, String disambiguationId) throws ExperimentException
    {
        //  Issue 14851: added check for isMultiRunUpload to if statement for case when "Save and Import Another Run" gets default values for the Run properties
        if (!isMultiRunUpload() && !isResetDefaultValues() && domain.getDomainKind() instanceof LuminexAnalyteDomainKind)
        {
            List<? extends DomainProperty> analyteColumns = domain.getProperties();
            Map<DomainProperty, Object> analyteDefaultValues = DefaultValueService.get().getDefaultValues(getContainer(), domain);
            Map<DomainProperty, Object> userDefaultValues = new HashMap<>();

            String objectURI = null;
            if (getReRunId() != null)
            {
                // In the re-run case we want to reuse the analyte values from the original run (if the analyte
                // was in that version of the run)
                SQLFragment sql = new SQLFragment("SELECT MIN(a.LSID) FROM ");
                sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
                sql.append(" WHERE LOWER(Name) = LOWER(?) AND a.DataId IN (SELECT d.RowId FROM ");
                sql.add(disambiguationId);
                sql.append(ExperimentService.get().getTinfoData(), "d");
                sql.append(" WHERE d.RunId = ?)");
                sql.add(getReRunId());
                objectURI = new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getObject(String.class);
            }

            if (objectURI == null)
            {
                // If we don't have one yet, do a lookup against the last uploaded set of values
                SQLFragment sql = new SQLFragment("SELECT ObjectURI FROM " + OntologyManager.getTinfoObject() + " WHERE Container = ? AND ObjectId = (SELECT MAX(ObjectId) FROM " + OntologyManager.getTinfoObject() + " o, " + LuminexProtocolSchema.getTableInfoAnalytes() + " a WHERE o.ObjectURI = a.LSID AND LOWER(a.Name) = LOWER(?))");
                sql.add(getContainer().getId());
                sql.add(disambiguationId);
                objectURI = new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getObject(String.class);
            }

            if (objectURI != null)
            {
                // Look up the values for that ObjectURI
                Map<String, ObjectProperty> values = OntologyManager.getPropertyObjects(getContainer(), objectURI);
                for (DomainProperty analyteDP : analyteColumns)
                {
                    ObjectProperty objectProp = values.get(analyteDP.getPropertyURI());
                    if (objectProp != null)
                    {
                        userDefaultValues.put(analyteDP, objectProp.value());
                    }
                }
            }

            // Issue 19913: return the previous run values for re-run case instead of merging with the default values
            if (getReRun() != null)
                return userDefaultValues;
            else
                return DefaultValueService.get().getMergedValues(domain, userDefaultValues, analyteDefaultValues);
        }
        else
        {
            return super.getDefaultValues(domain, disambiguationId);
        }
    }

    public Map<String, String> getAnalyteColumnDefaultValues(ExpProtocol protocol)
    {
        if (getReRunId() != null)
        {
            SQLFragment sql = new SQLFragment("SELECT * FROM ");
            sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
            sql.append(" WHERE a.DataId IN (SELECT d.RowId FROM ");
            sql.append(ExperimentService.get().getTinfoData(), "d");
            sql.append(" WHERE d.RunId = ?)");
            sql.add(getReRunId());
            List<Analyte> analyteRows = new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getArrayList(Analyte.class);

            Map<String, String> values = new HashMap<>();
            for (Analyte analyte : analyteRows)
            {
                String inputName = LuminexUploadWizardAction.getAnalytePropertyName(analyte.getName(), LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
                values.put(inputName, analyte.getPositivityThreshold() != null ? analyte.getPositivityThreshold().toString() : null);
            }
            return values;
        }
        else
            return PropertyManager.getProperties(getUser(), getContainer(), protocol.getName() + ": Analyte Column");
    }

    public LuminexExcelParser getParser() throws ExperimentException
    {
        if (_parser == null)
        {
            _parser = new LuminexExcelParser(getProtocol(), getUploadedData().values());
        }
        return _parser;
    }

    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException
    {
        Set<String> result = new HashSet<>();
        for (String titration : getParser().getTitrations())
        {
            if (getViewContext().getRequest().getParameter(LuminexUploadWizardAction.getTitrationCheckboxName(titration, analyteName)) != null)
            {
                result.add(titration);
            }
        }
        return result;
    }

    public List<Titration> getTitrations() throws ExperimentException
    {
        List<Titration> result = new ArrayList<>();
        for (String titrationName : getParser().getTitrations())
        {
            Titration titration = new Titration();
            titration.setName(titrationName);
            for (Titration.Type type : Titration.Type.values())
            {
                String propertyName = LuminexUploadWizardAction.getTitrationTypeCheckboxName(type, titration);
                if (getViewContext().getRequest().getParameter(propertyName) != null)
                {
                    String hiddenValue = getViewContext().getRequest().getParameter(propertyName);
                    type.setEnabled(titration, hiddenValue.equals("true"));
                }
            }

            // issue 17728: only add titrations that are set as one of the specified types
            if (titration.hasRole())
            {
                result.add(titration);
            }
        }
        return result;
    }

    public List<SinglePointControl> getSinglePointControls() throws ExperimentException
    {
        List<SinglePointControl> result = new ArrayList<>();
        for (String singlePointControlName : getParser().getSinglePointControls())
        {
            SinglePointControl singlePointControl = new SinglePointControl();
            singlePointControl.setName(singlePointControlName);
            String propertyName = LuminexUploadWizardAction.getSinglePointControlCheckboxName(singlePointControlName);
            if (getViewContext().getRequest().getParameter(propertyName) != "")
            {
                result.add(singlePointControl);
            }

        }
        return result;
    }

}
