/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:43:01 PM
 */

public class NabAssayRun extends Luc5Assay
{
    private ExpProtocol _protocol;
    private AbstractPlateBasedAssayProvider _provider;
    private Map<PropertyDescriptor, Object> _runProperties;
    private Map<PropertyDescriptor, Object> _runDisplayProperties;
    List<SampleResult> _sampleResults;
    private ExpRun _run;
    private User _user;

    public NabAssayRun(AbstractPlateBasedAssayProvider provider, ExpRun run, Plate plate, User user, List<Integer> cutoffs, DilutionCurve.FitType fitType)
    {
        super(plate, cutoffs, fitType);
        _run = run;
        _user = user;
        _protocol = run.getProtocol();
        _provider = provider;
    }

    private Map<FieldKey, PropertyDescriptor> getFieldKeys()
    {
        Map<FieldKey, PropertyDescriptor> fieldKeys = new HashMap<FieldKey, PropertyDescriptor>();
        for (DomainProperty property : _provider.getBatchDomain(_protocol).getProperties())
            fieldKeys.put(FieldKey.fromParts(AssayService.RUN_PROPERTIES_COLUMN_NAME, property.getName()), property.getPropertyDescriptor());
        for (DomainProperty property : _provider.getRunDomain(_protocol).getProperties())
            fieldKeys.put(FieldKey.fromParts(AssayService.RUN_PROPERTIES_COLUMN_NAME, property.getName()), property.getPropertyDescriptor());
        return fieldKeys;
    }

    private Map<PropertyDescriptor, Object> getRunProperties(TableInfo runTable, Map<FieldKey, PropertyDescriptor> fieldKeys, Map<FieldKey, ColumnInfo> selectCols)
    {
        SimpleFilter filter = new SimpleFilter("RowId", _run.getRowId());

        Map<PropertyDescriptor, Object> properties = new LinkedHashMap<PropertyDescriptor, Object>();
        ResultSet rs = null;
        try
        {
            rs = Table.selectForDisplay(runTable, new ArrayList<ColumnInfo>(selectCols.values()), filter, null, 1, 0);
            if (!rs.next())
                HttpView.throwNotFound("Run " + _run.getRowId() + " was not found.");

            for (Map.Entry<FieldKey, ColumnInfo> entry : selectCols.entrySet())
            {
                ColumnInfo column = entry.getValue();
                ColumnInfo displayField = column.getDisplayField();
                if (column.getDisplayField() != null)
                    column = displayField;
                if (fieldKeys.containsKey(entry.getKey()))
                    properties.put(fieldKeys.get(entry.getKey()), column.getValue(rs));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (rs != null)
                try { rs.close(); } catch (SQLException e) { /* do nothing */ }
        }
        return properties;
    }

    public Map<PropertyDescriptor, Object> getRunDisplayProperties(ViewContext context)
    {
        if (_runDisplayProperties == null)
        {
            Map<FieldKey, PropertyDescriptor> fieldKeys = getFieldKeys();
            TableInfo runTable = AssayService.get().createRunTable(_protocol, _provider, _user, _run.getContainer());
            
            CustomView runView = QueryService.get().getCustomView(context.getUser(), context.getContainer(),
                   AssaySchema.NAME, AssayService.get().getRunsTableName(_protocol), NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);

            if (runView != null)
            {
                Map<FieldKey, ColumnInfo> selectCols = QueryService.get().getColumns(runTable, runView.getColumns());
                _runDisplayProperties = getRunProperties(runTable, fieldKeys, selectCols);
            }
            else
                _runDisplayProperties = getRunProperties();
        }
        return Collections.unmodifiableMap(_runDisplayProperties);
    }

    public Map<PropertyDescriptor, Object> getRunProperties()
    {
        if (_runProperties == null)
        {
            Map<FieldKey, PropertyDescriptor> fieldKeys = getFieldKeys();
            TableInfo runTable = AssayService.get().createRunTable(_protocol, _provider, _user, _run.getContainer());
            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(runTable, fieldKeys.keySet());
            _runProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
            _runProperties.putAll(getRunProperties(runTable, fieldKeys, cols));
        }
        return Collections.unmodifiableMap(_runProperties);
    }

    public List<SampleResult> getSampleResults()
    {
        if (_sampleResults == null)
        {
            _sampleResults = new ArrayList<SampleResult>();
            Map<String, Map<PropertyDescriptor, Object>> sampleProperties = getSampleProperties();
            for (DilutionSummary summary : getSummaries())
            {
                String specimenId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
                Double visitId = (Double) summary.getWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
                String participantId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
                Date visitDate = (Date) summary.getWellGroup().getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
                String key = NabAssayController.getMaterialKey(specimenId, participantId, visitId, visitDate);
                Map<PropertyDescriptor, Object> properties = sampleProperties.get(key);
                _sampleResults.add(new SampleResult(summary, key, properties));
            }
        }
        return _sampleResults;
    }

    private Map<String, Map<PropertyDescriptor, Object>> getSampleProperties()
    {
        Map<String, Map<PropertyDescriptor, Object>> samplePropertyMap = new HashMap<String, Map<PropertyDescriptor, Object>>();

        Collection<ExpMaterial> inputs = _run.getMaterialInputs().keySet();
        Domain sampleDomain = _provider.getSampleWellGroupDomain(_protocol);
        DomainProperty[] sampleDomainProperties = sampleDomain.getProperties();

        PropertyDescriptor sampleIdPD = null;
        PropertyDescriptor visitIdPD = null;
        PropertyDescriptor participantIdPD = null;
        PropertyDescriptor datePD = null;
        for (DomainProperty property : sampleDomainProperties)
        {
            if (property.getName().equals(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME))
                sampleIdPD = property.getPropertyDescriptor();
            else if (property.getName().equals(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME))
                participantIdPD = property.getPropertyDescriptor();
            else if (property.getName().equals(AbstractAssayProvider.VISITID_PROPERTY_NAME))
                visitIdPD = property.getPropertyDescriptor();
            else if (property.getName().equals(AbstractAssayProvider.DATE_PROPERTY_NAME))
                datePD = property.getPropertyDescriptor();
        }

        for (ExpMaterial material : inputs)
        {
            Map<PropertyDescriptor, Object> sampleProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
            for (DomainProperty dp : sampleDomainProperties)
            {
                PropertyDescriptor property = dp.getPropertyDescriptor();
                sampleProperties.put(property, material.getProperty(property));
            }
            String key = NabAssayController.getMaterialKey((String) material.getProperty(sampleIdPD),
                    (String) material.getProperty(participantIdPD), (Double) material.getProperty(visitIdPD), (Date) material.getProperty(datePD));
            samplePropertyMap.put(key, sampleProperties);
        }
        return samplePropertyMap;
    }

    public static class SampleResult
    {
        private DilutionSummary _dilutionSummary;
        private String _materialKey;
        private Map<PropertyDescriptor, Object> _properties;

        public SampleResult(DilutionSummary dilutionSummary, String materialKey, Map<PropertyDescriptor, Object> properties)
        {
            _dilutionSummary = dilutionSummary;
            _materialKey = materialKey;
            _properties = sortProperties(properties);
        }
        
        public DilutionSummary getDilutionSummary()
        {
            return _dilutionSummary;
        }

        public String getKey()
        {
            return _materialKey;
        }

        public Map<PropertyDescriptor, Object> getProperties()
        {
            return _properties;
        }

        private Map<PropertyDescriptor, Object> sortProperties(Map<PropertyDescriptor, Object> properties)
        {
            Map<PropertyDescriptor, Object> sortedProperties = new LinkedHashMap<PropertyDescriptor, Object>();
            Map.Entry<PropertyDescriptor, Object> sampleIdEntry =
                    findPropertyDescriptor(properties, AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
            Map.Entry<PropertyDescriptor, Object> ptidEntry =
                    findPropertyDescriptor(properties, AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
            Map.Entry<PropertyDescriptor, Object> visitEntry =
                    findPropertyDescriptor(properties, AbstractAssayProvider.VISITID_PROPERTY_NAME);
            if (sampleIdEntry != null)
                sortedProperties.put(sampleIdEntry.getKey(), sampleIdEntry.getValue());
            if (ptidEntry != null)
                sortedProperties.put(ptidEntry.getKey(), ptidEntry.getValue());
            if (visitEntry != null)
                sortedProperties.put(visitEntry.getKey(), visitEntry.getValue());
            for (Map.Entry<PropertyDescriptor, Object> entry : properties.entrySet())
            {
                if (sampleIdEntry != null && entry.getKey() == sampleIdEntry.getKey() ||
                    ptidEntry != null && entry.getKey() == ptidEntry.getKey() ||
                    visitEntry != null && entry.getKey() == visitEntry.getKey())
                {
                    continue;
                }
                sortedProperties.put(entry.getKey(), entry.getValue());
            }
            return sortedProperties;
        }

        private Map.Entry<PropertyDescriptor, Object> findPropertyDescriptor(Map<PropertyDescriptor, Object> properties, String propertyName)
        {
            for (Map.Entry<PropertyDescriptor, Object> entry : properties.entrySet())
            {
                if (entry.getKey().getName().equals(propertyName))
                    return entry;
            }
            return null;
        }
    }

    private class PropertyDescriptorComparator implements Comparator<PropertyDescriptor>
    {
        public int compare(PropertyDescriptor o1, PropertyDescriptor o2)
        {
            String o1Str = o1.getLabel();
            if (o1Str == null)
                o1Str = o1.getName();
            String o2Str = o2.getLabel();
            if (o2Str == null)
                o2Str = o2.getName();
            return o1Str.compareToIgnoreCase(o2Str);
        }
    }

    public ExpProtocol getProtocol()
    {
        return _protocol;
    }

    public ExpRun getRun()
    {
        return _run;
    }
}