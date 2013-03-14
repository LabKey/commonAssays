/*
 * Copyright (c) 2008-2013 LabKey Corporation
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

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.data.*;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.security.User;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.query.NabProviderSchema;
import org.labkey.nab.query.NabRunDataTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:43:01 PM
 */

public abstract class NabAssayRun extends Luc5Assay
{
    private ExpProtocol _protocol;
    private NabAssayProvider _provider;
    private Map<PropertyDescriptor, Object> _runProperties;
    private Map<PropertyDescriptor, Object> _runDisplayProperties;
    List<SampleResult> _sampleResults;
    private ExpRun _run;
    // Be extremely careful to not leak this user out in any objects (e.g, via schemas or tables) as it may have elevated permissions.
    private User _user;
    private DilutionCurve.FitType _savedCurveFitType = null;
    private Map<ExpMaterial, List<WellGroup>> _materialWellGroupMapping;
    private Map<WellGroup, ExpMaterial> _wellGroupMaterialMapping;

    public NabAssayRun(NabAssayProvider provider, ExpRun run,
                       User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        super(run.getRowId(), cutoffs, renderCurveFitType);
        _run = run;
        _user = user;
        _protocol = run.getProtocol();
        _provider = provider;

        for (Map.Entry<PropertyDescriptor, Object> property : getRunProperties().entrySet())
        {
            if (NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME.equals(property.getKey().getName()))
            {
                String fitTypeLabel = (String) property.getValue();
                _savedCurveFitType = DilutionCurve.FitType.fromLabel(fitTypeLabel);
            }
        }
    }

    public NabDataHandler getDataHandler()
    {
        return _provider.getDataHandler();
    }

    @Override
    public String getRunName()
    {
        return _run.getName();
    }

    private Map<FieldKey, PropertyDescriptor> getFieldKeys()
    {
        Map<FieldKey, PropertyDescriptor> fieldKeys = new HashMap<FieldKey, PropertyDescriptor>();
        for (DomainProperty property : _provider.getBatchDomain(_protocol).getProperties())
            fieldKeys.put(FieldKey.fromParts(AssayService.BATCH_COLUMN_NAME, property.getName()), property.getPropertyDescriptor());
        for (DomainProperty property : _provider.getRunDomain(_protocol).getProperties())
            fieldKeys.put(FieldKey.fromParts(property.getName()), property.getPropertyDescriptor());

        // Add all of the hard columns to the set of properties we can show
        TableInfo runTableInfo = AssayService.get().createRunTable(_protocol, _provider, _user, _run.getContainer());
        for (ColumnInfo runColumn : runTableInfo.getColumns())
        {
            // These columns cause an UnauthorizedException if the user has permission to see the dataset
            // this run has been copied to, but not the run folder, because the column joins to the exp.Data query
            // which doesn't know anything about the extra permission the user has been granted by the copy to study linkage.
            // We don't need to show it in the details view, so just skip it.
            if (!ExpRunTable.Column.DataOutputs.name().equalsIgnoreCase(runColumn.getName()) &&
                !ExpRunTable.Column.JobId.name().equalsIgnoreCase(runColumn.getName()) &&
                !ExpRunTable.Column.RunGroups.name().equalsIgnoreCase(runColumn.getName()))
            {
                // Fake up a property descriptor. Currently only name and label are actually used for rendering the page,
                // but set a few more so that toString() works for debugging purposes
                PropertyDescriptor pd = new PropertyDescriptor();
                pd.setName(runColumn.getName());
                pd.setLabel(runColumn.getLabel());
                pd.setPropertyURI(runColumn.getPropertyURI());
                pd.setContainer(_protocol.getContainer());
                pd.setProject(_protocol.getContainer().getProject());
                fieldKeys.put(FieldKey.fromParts(runColumn.getName()), pd);
            }
        }

        return fieldKeys;
    }

    public DilutionCurve.FitType getSavedCurveFitType()
    {
        return _savedCurveFitType;
    }

    private Map<PropertyDescriptor, Object> getRunProperties(TableInfo runTable, Map<FieldKey, PropertyDescriptor> fieldKeys, Map<FieldKey, ColumnInfo> selectCols)
    {
        SimpleFilter filter = new SimpleFilter("RowId", _run.getRowId());

        Map<PropertyDescriptor, Object> properties = new LinkedHashMap<PropertyDescriptor, Object>();
        ResultSet rs = null;
        try
        {
            rs = Table.selectForDisplay(runTable, new ArrayList<ColumnInfo>(selectCols.values()), null, filter, null, 1, Table.NO_OFFSET);
            if (!rs.next())
            {
                throw new NotFoundException("Run " + _run.getRowId() + " was not found.");
            }

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
                SchemaKey.fromParts("assay", _provider.getResourceName(), _protocol.getName()).toString(), AssayProtocolSchema.RUNS_TABLE_NAME, NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);

            if (runView == null)
            {
                // Try with the old schema/query name
                runView = QueryService.get().getCustomView(context.getUser(), context.getContainer(),
                        AssaySchema.NAME, AssaySchema.getLegacyProtocolTableName(_protocol, AssayProtocolSchema.RUNS_TABLE_NAME), NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);
            }

            Collection<FieldKey> fieldKeysToShow;
            if (runView != null)
            {
                // If we have a saved view to use for the column list, use it
                fieldKeysToShow = new ArrayList<FieldKey>(runView.getColumns());
            }
            else
            {
                // Otherwise, use the default list of columns
                fieldKeysToShow = new ArrayList<FieldKey>(runTable.getDefaultVisibleColumns());
            }
            // The list of available columns is reduced from the default set because the user may not have
            // permission to join to all of the lookups. Remove any columns that aren't part of the acceptable set,
            // which is built up by getFieldKeys()
            for (Iterator<FieldKey> i = fieldKeysToShow.iterator(); i.hasNext();)
            {
                if (!fieldKeys.containsKey(i.next()))
                {
                    i.remove();
                }
            }

            Map<FieldKey, ColumnInfo> selectCols = QueryService.get().getColumns(runTable, fieldKeysToShow);
            _runDisplayProperties = getRunProperties(runTable, fieldKeys, selectCols);
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
            List<SampleResult> sampleResults = new ArrayList<SampleResult>();

            NabDataHandler handler = _provider.getDataHandler();
            ExpData[] outputDatas = _run.getOutputDatas(handler.getDataType());
            if (outputDatas.length != 1)
                throw new IllegalStateException("Expected a single data file output for this NAb run.  Found " + outputDatas.length);
            ExpData outputObject = outputDatas[0];

            Map<String, NabResultProperties> allProperties = getSampleProperties(outputObject);
            Set<String> captions = new HashSet<String>();
            boolean longCaptions = false;

            for (DilutionSummary summary : getSummaries())
            {
                if (!summary.isBlank())
                {
                    NabMaterialKey key = summary.getMaterialKey();
                    String shortCaption = key.getDisplayString(false);
                    if (captions.contains(shortCaption))
                        longCaptions = true;
                    captions.add(shortCaption);

                    NabResultProperties props = allProperties.get(getSampleKey(summary));
                    sampleResults.add(new SampleResult(_provider, outputObject, summary, key, props.getSampleProperties(), props.getDataProperties()));
                }
            }

            if (longCaptions)
            {
                for (SampleResult result : sampleResults)
                    result.setLongCaptions(true);
            }

            _sampleResults = sampleResults;
        }
        return _sampleResults;
    }

    private static class NabResultProperties
    {
        private Map<PropertyDescriptor, Object> _sampleProperties;
        private Map<PropertyDescriptor, Object> _dataProperties;

        public NabResultProperties(Map<PropertyDescriptor, Object> sampleProperties, Map<PropertyDescriptor, Object> dataProperties)
        {
            _sampleProperties = sampleProperties;
            _dataProperties = dataProperties;
        }

        public Map<PropertyDescriptor, Object> getSampleProperties()
        {
            return _sampleProperties;
        }

        public Map<PropertyDescriptor, Object> getDataProperties()
        {
            return _dataProperties;
        }
    }

    private Map<String, NabResultProperties> getSampleProperties(ExpData outputData)
    {
        Map<String, NabResultProperties> samplePropertyMap = new HashMap<String, NabResultProperties>();

        Collection<ExpMaterial> inputs = _run.getMaterialInputs().keySet();
        Domain sampleDomain = _provider.getSampleWellGroupDomain(_protocol);
        DomainProperty[] sampleDomainProperties = sampleDomain.getProperties();

        NabProviderSchema nabProviderSchema = (NabProviderSchema)_provider.createProviderSchema(_user, _run.getContainer(), null);
        NabRunDataTable nabRunDataTable = nabProviderSchema.createDataRowTable(_protocol);

        for (ExpMaterial material : inputs)
        {
            Map<PropertyDescriptor, Object> sampleProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
            for (DomainProperty dp : sampleDomainProperties)
            {
                PropertyDescriptor property = dp.getPropertyDescriptor();
                sampleProperties.put(property, material.getProperty(property));
            }

            // in addition to the properties saved on the sample object, we'll add the properties associated with each sample's
            // "output" data object.
            Map<PropertyDescriptor, Object> dataProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
            String wellGroupName = getWellGroupName(material);
            String dataRowLsid = getDataHandler().getDataRowLSID(outputData, wellGroupName, sampleProperties).toString();
            if (!NabManager.useNewNab)
            {
                Map<String, ObjectProperty> outputProperties = OntologyManager.getPropertyObjects(_run.getContainer(), dataRowLsid);
                for (ObjectProperty prop : outputProperties.values())
                {
                    PropertyDescriptor pd = OntologyManager.getPropertyDescriptor(prop.getPropertyURI(), prop.getContainer());
                    dataProperties.put(pd, prop.value());
                }
            }
            else
            {
                PropertyDescriptor[] propertyDescriptors = NabProviderSchema.getExistingDataProperties(_protocol);
                NabManager.get().getDataPropertiesFromNabRunData(nabRunDataTable, dataRowLsid, _run.getContainer(), propertyDescriptors, dataProperties);
            }
            samplePropertyMap.put(getSampleKey(material), new NabResultProperties(sampleProperties,  dataProperties));
        }
        return samplePropertyMap;
    }

    public static class SampleResult
    {
        private String _dataRowLsid;
        private Container _dataContainer;
        private Integer _objectId;
        private DilutionSummary _dilutionSummary;
        private NabMaterialKey _materialKey;
        private Map<PropertyDescriptor, Object> _sampleProperties;
        private Map<PropertyDescriptor, Object> _dataProperties;
        private boolean _longCaptions = false;

        public SampleResult(NabAssayProvider provider, ExpData data, DilutionSummary dilutionSummary, NabMaterialKey materialKey,
                            Map<PropertyDescriptor, Object> sampleProperties, Map<PropertyDescriptor, Object> dataProperties)
        {
            _dilutionSummary = dilutionSummary;
            _materialKey = materialKey;
            _sampleProperties = sortProperties(sampleProperties);
            _dataProperties = sortProperties(dataProperties);
            _dataRowLsid = provider.getDataHandler().getDataRowLSID(data, dilutionSummary.getFirstWellGroup().getName(), sampleProperties).toString();
            _dataContainer = data.getContainer();
        }

        public Integer getObjectId()
        {
            if (!NabManager.useNewNab)
            {
            if (_objectId == null)
            {
                try
                {
                    _objectId = Table.executeSingleton(OntologyManager.getExpSchema(), "SELECT ObjectId FROM " +
                            OntologyManager.getTinfoObject() + " WHERE ObjectURI = ? AND Container = ?",
                            new Object[] {_dataRowLsid, _dataContainer.getId()}, Integer.class);
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }

            return _objectId;
            }
            else
            {
                if (null == _objectId)
                {
                    NabSpecimen nabSpecimen = NabManager.get().getNabSpecimen(_dataRowLsid, _dataContainer);
                    if (null != nabSpecimen)
                        _objectId = nabSpecimen.getRowId();
                }
                return _objectId;
            }

        }

        public DilutionSummary getDilutionSummary()
        {
            return _dilutionSummary;
        }

        public String getCaption()
        {
            return _materialKey.getDisplayString(_longCaptions);
        }

        public Map<PropertyDescriptor, Object> getSampleProperties()
        {
            return _sampleProperties;
        }

        public Map<PropertyDescriptor, Object> getDataProperties()
        {
            return _dataProperties;
        }

        public String getDataRowLsid()
        {
            return _dataRowLsid;
        }

        public void setLongCaptions(boolean longCaptions)
        {
            _longCaptions = longCaptions;
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

    public void setMaterialWellGroupMapping(Map<ExpMaterial, List<WellGroup>> materialWellGroupMapping)
    {
        _materialWellGroupMapping = materialWellGroupMapping;
        _wellGroupMaterialMapping = new HashMap<WellGroup, ExpMaterial>();
        for (Map.Entry<ExpMaterial, List<WellGroup>> entry : materialWellGroupMapping.entrySet())
        {
            for (WellGroup wellGroup : entry.getValue())
            {
                _wellGroupMaterialMapping.put(wellGroup, entry.getKey());
            }
        }
    }

    public ExpMaterial getMaterial(WellGroup wellgroup)
    {
        return _wellGroupMaterialMapping.get(wellgroup);
    }

    public List<WellGroup> getWellGroups(ExpMaterial material)
    {
        return _materialWellGroupMapping.get(material);
    }

    protected String getWellGroupName(ExpMaterial material)
    {
        List<WellGroup> groups = getWellGroups(material);
        // All current NAb assay types don't mix well groups for a single sample- there may be muliple
        // instances of the same well group on different plates, but they'll all have the same name.
        return groups != null ? groups.get(0).getName() : null;
    }

    /**
     * Generate a key for the sample level property map
     * @param material
     * @return
     */
    protected String getSampleKey(ExpMaterial material)
    {
        return getWellGroupName(material);
    }

    /**
     * Generate a key for the sample level property map
     * @param summary
     * @return
     */
    protected String getSampleKey(DilutionSummary summary)
    {
        return summary.getFirstWellGroup().getName();
    }
}
