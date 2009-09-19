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
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.pipeline.PipelineJob;
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
            fieldKeys.put(FieldKey.fromParts(AssayService.BATCH_COLUMN_NAME, AssayService.BATCH_PROPERTIES_COLUMN_NAME, property.getName()), property.getPropertyDescriptor());
        for (DomainProperty property : _provider.getRunDomain(_protocol).getProperties())
            fieldKeys.put(FieldKey.fromParts(AssayService.RUN_PROPERTIES_COLUMN_NAME, property.getName()), property.getPropertyDescriptor());

        // Add all of the hard columns to the set of properties we can show
        TableInfo runTableInfo = AssayService.get().createRunTable(_protocol, _provider, _user, _run.getContainer());
        for (ColumnInfo runColumn : runTableInfo.getColumns())
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

            Collection<FieldKey> fieldKeysToShow;
            if (runView != null)
            {
                // If we have a saved view to use for the column list, use it
                fieldKeysToShow = runView.getColumns();
            }
            else
            {
                // Otherwise, use the default list of columns
                fieldKeysToShow = new ArrayList<FieldKey>(runTable.getDefaultVisibleColumns());
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
            _sampleResults = new ArrayList<SampleResult>();
            Map<NabMaterialKey, Map<PropertyDescriptor, Object>> sampleProperties = getSampleProperties();
            Set<String> captions = new HashSet<String>();
            boolean longCaptions = false;
            for (NabMaterialKey key : sampleProperties.keySet())
            {
                String shortCaption = key.getDisplayString(false);
                if (captions.contains(shortCaption))
                    longCaptions = true;
                captions.add(shortCaption);
            }

            List<ExpData> outputs = _run.getDataOutputs();
            if (outputs.size() != 1)
                throw new IllegalStateException("Expected a single data file output for this NAb run.  Found " + outputs.size());
            ExpData outputObject = outputs.get(0);

            for (DilutionSummary summary : getSummaries())
            {
                String specimenId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
                Double visitId = (Double) summary.getWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
                String participantId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
                Date visitDate = (Date) summary.getWellGroup().getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
                NabMaterialKey key = new NabMaterialKey(specimenId, participantId, visitId, visitDate);
                Map<PropertyDescriptor, Object> properties = sampleProperties.get(key);
                _sampleResults.add(new SampleResult(outputObject, summary, key, properties, longCaptions));
            }
        }
        return _sampleResults;
    }

    private Map<NabMaterialKey, Map<PropertyDescriptor, Object>> getSampleProperties()
    {
        Map<NabMaterialKey, Map<PropertyDescriptor, Object>> samplePropertyMap =
                new HashMap<NabMaterialKey, Map<PropertyDescriptor, Object>>();

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
            NabMaterialKey key = new NabMaterialKey((String) material.getProperty(sampleIdPD),
                    (String) material.getProperty(participantIdPD), (Double) material.getProperty(visitIdPD), (Date) material.getProperty(datePD));
            samplePropertyMap.put(key, sampleProperties);
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
        private Map<PropertyDescriptor, Object> _properties;
        private boolean _longCaptions = false;

        public SampleResult(ExpData data, DilutionSummary dilutionSummary, NabMaterialKey materialKey, Map<PropertyDescriptor, Object> properties, boolean longCaptions)
        {
            _dilutionSummary = dilutionSummary;
            _materialKey = materialKey;
            _longCaptions = longCaptions;
            _properties = sortProperties(properties);
            _dataRowLsid = NabDataHandler.getDataRowLSID(data, dilutionSummary.getWellGroup()).toString();
            _dataContainer = data.getContainer();
        }

        public Integer getObjectId()
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

        public DilutionSummary getDilutionSummary()
        {
            return _dilutionSummary;
        }

        public String getCaption()
        {
            return _materialKey.getDisplayString(_longCaptions);
        }

        public Map<PropertyDescriptor, Object> getProperties()
        {
            return _properties;
        }

        public String getDataRowLsid()
        {
            return _dataRowLsid;
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

    private static final String INSERT_AUC_COLUMNS = "Inserting new AUC and IC values for curve fit type: %s";
    private static final String DELETE_PREV_COLUMN = "Deleting previous IC column : %s";

    public void upgradeAUCValues(PipelineJob job) throws Exception
    {
        Container container = getRun().getContainer();

        for (SampleResult result : getSampleResults())
        {
            Map<String, ObjectProperty> rowMap = OntologyManager.getPropertyObjects(container, result.getDataRowLsid());

            for (DilutionCurve.FitType type : DilutionCurve.FitType.values())
            {
                // if we don't find an AUC colunmn for that fit type, lets go ahead and recalculate AUC and IC values and remove the old columns
                Lsid propertyURI = new Lsid(NabDataHandler.NAB_PROPERTY_LSID_PREFIX, getProtocol().getName(), NabDataHandler.getPropertyName(NabDataHandler.AUC_PREFIX, type));

                if (!rowMap.containsKey(propertyURI.toString()))
                {
                    job.info(String.format(INSERT_AUC_COLUMNS, type.getLabel()));
                    List<ObjectProperty> results = new ArrayList<ObjectProperty>();
                    DilutionSummary dilution = result.getDilutionSummary();
                    Map<Integer, String> cutoffFormats = NabDataHandler.getCutoffFormats(getProtocol(), getRun());
                    Map<String, Object> props = new HashMap<String, Object>();
                    Lsid dataRowLsid = new Lsid(result.getDataRowLsid());

                    double auc = dilution.getAUC(type);
                    if (!Double.isNaN(auc))
                    {
                        props.put(NabDataHandler.getPropertyName(NabDataHandler.AUC_PREFIX, type), auc);
                        if (getCurveFitType() == type)
                            props.put(NabDataHandler.AUC_PREFIX, auc);
                    }
                    
                    for (Integer cutoff : getCutoffs())
                    {
                        NabDataHandler.saveICValue(NabDataHandler.getPropertyName(NabDataHandler.CURVE_IC_PREFIX, cutoff, type),
                                dilution.getCutoffDilution(cutoff / 100.0, type),
                                dilution, dataRowLsid, getProtocol(), container, cutoffFormats, props, type);
                    }

                    // convert to object properties
                    for (Map.Entry<String, Object> entry : props.entrySet())
                    {
                        results.add(NabDataHandler.getObjectProperty(container, getProtocol(), result.getDataRowLsid(),
                                entry.getKey(), entry.getValue(), cutoffFormats));
                    }
                    OntologyManager.insertProperties(container, result.getDataRowLsid(), results.toArray(new ObjectProperty[results.size()]));
                }
            }
        }
        // delete old IC columns (point and curve plus OOR indicators)
/*
        for (Integer cutoff : getCutoffs())
        {
            List<String> prevColumns = new ArrayList<String>();

            prevColumns.add(new Lsid(NabDataHandler.NAB_PROPERTY_LSID_PREFIX, getProtocol().getName(), NabDataHandler.CURVE_IC_PREFIX + cutoff).toString());
            prevColumns.add(new Lsid(NabDataHandler.NAB_PROPERTY_LSID_PREFIX, getProtocol().getName(), NabDataHandler.CURVE_IC_PREFIX + cutoff + NabDataHandler.OORINDICATOR_SUFFIX).toString());

            for (String uri : prevColumns)
            {
                PropertyDescriptor pd = OntologyManager.getPropertyDescriptor(uri, container);
                if (pd != null)
                {
                    job.info(String.format(DELETE_PREV_COLUMN, pd.getName()));
                    OntologyManager.deletePropertyDescriptor(pd);
                }
            }
        }
*/
    }
}