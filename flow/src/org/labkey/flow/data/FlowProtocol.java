/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.data;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowSchema;

import javax.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class FlowProtocol extends FlowObject<ExpProtocol>
{
    static private final Logger _log = Logger.getLogger(FlowProtocol.class);
    static private final String DEFAULT_PROTOCOL_NAME = "Flow";
    static final private String SAMPLESET_NAME = "Samples";
    static public FlowProtocol ensureForContainer(User user, Container container) throws Exception
    {
        FlowProtocol ret = getForContainer(container);
        if (ret != null)
        {
            if (ret.getProtocol().getImplementation() == null)
            {
                ret.setProperty(user, ExperimentProperty.PROTOCOLIMPLEMENTATION.getPropertyDescriptor(), FlowProtocolImplementation.NAME);
            }
            FlowProtocolStep.initProtocol(user, ret);
            return ret;
        }
        ExpProtocol protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ExperimentRun, DEFAULT_PROTOCOL_NAME);
        protocol.save(user);
        ret = new FlowProtocol(protocol);
        FlowProtocolStep.initProtocol(user, ret);
        return ret;
    }

    static public FlowProtocol getForContainer(Container container)
    {
        return getForContainer(container, DEFAULT_PROTOCOL_NAME);
    }

    static public FlowProtocol fromURL(User user, ActionURL url, HttpServletRequest request) throws UnauthorizedException
    {
        FlowProtocol ret = fromProtocolId(getIntParam(url, request, FlowParam.experimentId));
        if (ret == null)
        {
            ret = FlowProtocol.getForContainer(ContainerManager.getForPath(url.getExtraPath()));
        }
        if (ret == null)
            return null;
        if (!ret.getContainer().hasPermission(user, ACL.PERM_READ))
        {
            HttpView.throwUnauthorized();
        }
        return ret;
    }

    static public FlowProtocol fromProtocolId(int id)
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(id);
        if (protocol == null)
            return null;
        return new FlowProtocol(protocol);
    }

    static public FlowProtocol getForContainer(Container container, String name)
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(container, name);
        if (protocol != null)
            return new FlowProtocol(protocol);
        return null;
    }

    public FlowProtocol(ExpProtocol protocol)
    {
        super(protocol);
    }

    public ExpProtocol getProtocol()
    {
        return getExpObject();
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        switch (getProtocol().getApplicationType())
        {
            case ExperimentRun:
                map.put(FlowParam.protocolId, getProtocol().getRowId());
                break;
            case ProtocolApplication:
                FlowProtocolStep step = getStep();
                if (step != null)
                    map.put(FlowParam.actionSequence, step.getDefaultActionSequence());
                break;
        }
    }

    public FlowObject getParent()
    {
        return null;
    }

    public ActionURL urlShow()
    {
        return urlFor(ProtocolController.Action.showProtocol);
    }

    public FlowProtocolStep getStep()
    {
        return FlowProtocolStep.fromLSID(getContainer(), getLSID());
    }

    public ExpSampleSet getSampleSet()
    {
        return ExperimentService.get().getSampleSet(getContainer(), SAMPLESET_NAME);
    }

    public Map<String, FieldKey> getSampleSetJoinFields()
    {
        String prop;
        try
        {
            prop = (String) getProperty(FlowProperty.SampleSetJoin.getPropertyDescriptor());
        }
        catch (SQLException e)
        {
            return Collections.EMPTY_MAP;
        }
        if (prop == null)
            return Collections.EMPTY_MAP;
        String[] values = StringUtils.split(prop, "&");
        Map<String, FieldKey> ret = new LinkedHashMap();
        for (String value : values)
        {
            int ichEquals = value.indexOf("=");
            String left = PageFlowUtil.decode(value.substring(0, ichEquals));
            String right = PageFlowUtil.decode(value.substring(ichEquals + 1));
            ret.put(left, FieldKey.fromString(right));
        }
        return ret;
    }

    public String getSampleSetLSID()
    {
        try
        {
            String propValue = (String) getProperty(ExperimentProperty.SampleSetLSID.getPropertyDescriptor());
            if (propValue != null)
            {
                return propValue;
            }
        }
        catch (SQLException e)
        {
            _log.error("Error", e);
        }
        return ExperimentService.get().generateLSID(getContainer(), ExpSampleSet.class, SAMPLESET_NAME);
    }

    public void setSampleSetJoinFields(User user, Map<String, FieldKey> values) throws Exception
    {
        List<String> strings = new ArrayList();
        for (Map.Entry<String, FieldKey> entry : values.entrySet())
        {
            strings.add(PageFlowUtil.encode(entry.getKey()) + "=" + PageFlowUtil.encode(entry.getValue().toString()));
        }
        String value = StringUtils.join(strings.iterator(), "&");
        setProperty(user, FlowProperty.SampleSetJoin.getPropertyDescriptor(), value);
        setProperty(user, ExperimentProperty.SampleSetLSID.getPropertyDescriptor(), getSampleSetLSID());
    }

    public ActionURL urlUploadSamples(boolean importMoreSamples)
    {
        ActionURL ret = new ActionURL("Experiment", "showUploadMaterials", getContainer());
        ret.addParameter("name", SAMPLESET_NAME);
        ret.addParameter("nameReadOnly", "true");
        if (importMoreSamples)
        {
            ret.addParameter("importMoreSamples", "true");
        }
        return ret;
    }

    public Map<SampleKey, ExpMaterial> getSampleMap() throws SQLException
    {
        ExpSampleSet ss = getSampleSet();
        if (ss == null)
            return Collections.emptyMap();
        Set<String> propertyNames = getSampleSetJoinFields().keySet();
        if (propertyNames.size() == 0)
            return Collections.emptyMap();
        SamplesSchema schema = new SamplesSchema(null, getContainer());

        ExpMaterialTable sampleTable = schema.getSampleTable("samples", ss);
        List<ColumnInfo> selectedColumns = new ArrayList<ColumnInfo>();
        ColumnInfo colProperty = sampleTable.getColumn(ExpMaterialTable.Column.Property.toString());
        ColumnInfo colRowId = sampleTable.getColumn(ExpMaterialTable.Column.RowId.toString());
        selectedColumns.add(colRowId);
        for (String propertyName : propertyNames)
        {
            ColumnInfo lookupColumn = colProperty.getFk().createLookupColumn(colProperty, propertyName);
            if (lookupColumn != null)
                selectedColumns.add(lookupColumn);
        }
        Map<SampleKey, ExpMaterial> ret = new HashMap<SampleKey, ExpMaterial>();
        ResultSet rsSamples = Table.select(sampleTable, selectedColumns, null, null);
        try
        {
            while (rsSamples.next())
            {
                int rowId = ((Number) colRowId.getValue(rsSamples)).intValue();
                ExpMaterial sample = ExperimentService.get().getExpMaterial(rowId);
                if (sample == null)
                    continue;
                SampleKey key = new SampleKey();
                for (int i = 1; i < selectedColumns.size(); i ++)
                {
                    ColumnInfo column = selectedColumns.get(i);
                    key.addValue(column.getValue(rsSamples));
                }
                ret.put(key, sample);
            }
        }
        finally
        {
            rsSamples.close();
        }
        return ret;
    }

    public int updateSampleIds(User user) throws Exception
    {
        ExperimentService.Interface svc = ExperimentService.get();
        Map<String, FieldKey> joinFields = getSampleSetJoinFields();
        Map<SampleKey, ExpMaterial> sampleMap = getSampleMap();
        ExpSampleSet ss = getSampleSet();

        FlowSchema schema = new FlowSchema(user, getContainer());
        TableInfo fcsFilesTable = schema.getTable("FCSFiles", "FCSFiles");
        List<FieldKey> fields = new ArrayList();
        FieldKey fieldRowId = new FieldKey(null, "RowId");
        FieldKey fieldSampleRowId = new FieldKey(null, "Sample");
        fields.add(fieldRowId);
        fields.add(fieldSampleRowId);
        fields.addAll(joinFields.values());
        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(fcsFilesTable, fields);
        ColumnInfo colRowId = columns.get(fieldRowId);
        ColumnInfo colSampleId = columns.get(fieldSampleRowId);
        int ret = 0;
        ResultSet rs = Table.select(fcsFilesTable, new ArrayList<ColumnInfo>(columns.values()), null, null);
        try
        {
            boolean fTransaction = false;
            try
            {
                if (!svc.isTransactionActive())
                {
                    svc.beginTransaction();
                    fTransaction = true;
                }
                while (rs.next())
                {
                    int fcsFileId = ((Number) colRowId.getValue(rs)).intValue();
                    ExpData fcsFile = svc.getExpData(fcsFileId);
                    if (fcsFile == null)
                        continue;
                    SampleKey key = new SampleKey();
                    for (FieldKey fieldKey : joinFields.values())
                    {
                        ColumnInfo column = columns.get(fieldKey);
                        Object value = null;
                        if (column != null)
                        {
                            value = column.getValue(rs);
                        }
                        key.addValue(value);
                    }
                    ExpMaterial sample = sampleMap.get(key);
                    Integer newSampleId = sample == null ? null : sample.getRowId();
                    Object oldSampleId = colSampleId.getValue(rs);
                    if (ObjectUtils.equals(newSampleId, oldSampleId))
                        continue;
                    ExpProtocolApplication app = fcsFile.getSourceApplication();
                    if (app == null)
                    {
                        // This will happen for orphaned FCSFiles (where the ExperimentRun has been deleted).
                        continue;
                    }

                    boolean found = false;
                    for (ExpMaterial material : app.getInputMaterials())
                    {
                        if (material.getSampleSet() == null || material.getSampleSet().getRowId() != ss.getRowId())
                            continue;
                        if (sample != null)
                        {
                            if (material.equals(sample))
                            {
                                found = true;
                                ret ++;
                                break;
                            }
                        }
                        app.removeMaterialInput(user, material);
                    }
                    if (!found && sample != null)
                    {
                        app.addMaterialInput(user, sample, null);
                        ret ++;
                    }
                }
            }
            catch (Exception e)
            {
                if (fTransaction)
                {
                    svc.rollbackTransaction();
                    fTransaction = false;
                }
                throw e;
            }
            finally
            {
                if (fTransaction)
                    svc.commitTransaction();
            }
        }
        finally
        {
            rs.close();
        }
        return ret;
    }

    public SampleKey makeSampleKey(String runName, String fileName, AttributeSet attrs)
    {
        Collection<FieldKey> fields = getSampleSetJoinFields().values();
        if (fields.size() == 0)
            return null;
        FieldKey tableRun = FieldKey.fromParts("Run");
        FieldKey tableKeyword = FieldKey.fromParts("Keyword");
        SampleKey ret = new SampleKey();
        for (FieldKey field : fields)
        {
            if (field.getTable() == null)
            {
                if ("Name".equals(field.getName()))
                {
                    ret.addValue(fileName);
                }
                else
                {
                    return null;
                }
            }
            else if (tableRun.equals(field.getTable()))
            {
                if ("Name".equals(field.getName()))
                {
                    ret.addValue(runName);
                }
                else
                {
                    return null;
                }
            }
            else if (tableKeyword.equals(field.getTable()))
            {
                ret.addValue(attrs.getKeywords().get(field.getName()));
            }
        }
        return ret;
    }

    static public FieldSubstitution getDefaultFCSAnalysisNameExpr()
    {
        return new FieldSubstitution(new Object[] {new FieldKey(null, "Name")});
    }

    public FieldSubstitution getFCSAnalysisNameExpr()
    {
        String ret = (String) getProperty(FlowProperty.FCSAnalysisName);
        if (ret == null)
        {
            return null;
        }
        return FieldSubstitution.fromString(ret);
    }

    public void setFCSAnalysisNameExpr(User user, FieldSubstitution fs) throws Exception
    {
        String value = null;
        if (fs != null)
            value = fs.toString();
        if (StringUtils.isEmpty(value))
        {
            value = null;
        }
        setProperty(user, FlowProperty.FCSAnalysisName.getPropertyDescriptor(), value);
    }

    public void updateFCSAnalysisName(User user) throws Exception
    {
        ExperimentService.Interface expService = ExperimentService.get();
        FieldSubstitution fs = getFCSAnalysisNameExpr();
        if (fs == null)
        {
            fs = FlowProtocol.getDefaultFCSAnalysisNameExpr();
        }
        fs.insertParent(FieldKey.fromParts("FCSFile"));
        FlowSchema schema = new FlowSchema(user, getContainer());
        ExpDataTable table = schema.createFCSAnalysisTable("FCSAnalysis", FlowDataType.FCSAnalysis);
        Map<FieldKey, ColumnInfo> columns = new HashMap<FieldKey, ColumnInfo>();
        ColumnInfo colRowId = table.getColumn(ExpDataTable.Column.RowId);
        columns.put(new FieldKey(null, "RowId"), colRowId);
        columns.putAll(QueryService.get().getColumns(table, Arrays.asList(fs.getFieldKeys())));
        boolean fTrans = false;
        ResultSet rs = Table.select(table, new ArrayList<ColumnInfo>(columns.values()), null, null);
        try
        {
            if (!expService.isTransactionActive())
            {
                expService.beginTransaction();
                fTrans = true;
            }
            while (rs.next())
            {
                int rowid = ((Number) colRowId.getValue(rs)).intValue();
                FlowObject obj = FlowDataObject.fromRowId(rowid);
                if (obj instanceof FlowFCSAnalysis)
                {
                    ExpData data = ((FlowFCSAnalysis) obj).getData();
                    String name = fs.eval(columns, rs);
                    if (!ObjectUtils.equals(name, data.getName()))
                    {
                        data.setName(name);
                        data.save(user);
                    }
                }
            }
            if (fTrans)
            {
                expService.commitTransaction();
                fTrans = false;
            }
        }
        finally
        {
            FlowManager.get().flowObjectModified();
            if (fTrans)
            {
                expService.rollbackTransaction();
            }
        }
        rs.close();
    }

    public String getFCSAnalysisName(FlowWell well) throws SQLException
    {
        FlowSchema schema = new FlowSchema(null, getContainer());
        ExpDataTable table = schema.createFCSFileTable("fcsFiles");
        ColumnInfo colRowId = table.getColumn(ExpDataTable.Column.RowId);
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(colRowId, well.getRowId());
        FieldSubstitution fs = getFCSAnalysisNameExpr();
        if (fs == null)
            return well.getName();
        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(table, Arrays.asList(fs.getFieldKeys()));
        ResultSet rs = Table.select(table, new ArrayList<ColumnInfo>(columns.values()), filter, null);
        try
        {
            if (rs.next())
            {
                return fs.eval(columns, rs);
            }
        }
        finally
        {
            rs.close();
        }
        return well.getName();
    }

    public String getFCSAnalysisFilterString()
    {
        return (String) getProperty(FlowProperty.FCSAnalysisFilter);
    }

    public SimpleFilter getFCSAnalysisFilter()
    {
        SimpleFilter ret = new SimpleFilter();
        String value = getFCSAnalysisFilterString();
        if (value != null)
        {
            ActionURL url = new ActionURL();
            url.setRawQuery(value);
            ret.addUrlFilters(url, null);
        }
        return ret;
    }

    public void setFCSAnalysisFilter(User user, String value) throws SQLException
    {
        setProperty(user, FlowProperty.FCSAnalysisFilter.getPropertyDescriptor(), value);
    }

    public String getICSMetadataString()
    {
        return (String)getProperty(FlowProperty.ICSMetadata);
    }

    public void setICSMetadata(User user, String value) throws SQLException
    {
        setProperty(user, FlowProperty.ICSMetadata.getPropertyDescriptor(), value);
    }

    public ICSMetadata getICSMetadata()
    {
        String metadata = getICSMetadataString();
        if (metadata == null || metadata.length() == 0)
            return null;
        return ICSMetadata.fromXmlString(metadata);
    }

    public String getProtocolSettingsDescription()
    {
        List<String> parts = new ArrayList<String>();
        if (getSampleSetJoinFields().size() != 0)
        {
            parts.add("Sample set join fields");
        }
        if (getFCSAnalysisFilterString() != null)
        {
            parts.add("FCSAnalysis filter");
        }
        if (getFCSAnalysisNameExpr() != null)
        {
            parts.add("FCSAnalysis name setting");
        }
        if (getICSMetadataString() != null)
        {
            parts.add("ICS Metadata");
        }
        if (parts.size() == 0)
            return null;
        StringBuilder ret = new StringBuilder("Protocol Settings (");
        if (parts.size() ==1)
        {
            ret.append(parts.get(0));
        }
        else
        {
            for (int i = 0; i < parts.size(); i++)
            {
                if (i != 0)
                {
                    if (i != parts.size() - 1)
                    {
                        ret.append(", ");
                    }
                    else
                    {
                        ret.append(" and ");
                    }
                }
                ret.append(parts.get(i));
            }
        }
        ret.append(")");
        return ret.toString();
    }

    public String getLabel()
    {
        return "Protocol '" + getName() + "'";
    }

}
