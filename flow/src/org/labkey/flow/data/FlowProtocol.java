/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableResultSet;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.SamplesSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowSchema;

import javax.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FlowProtocol extends FlowObject<ExpProtocol>
{
    static private final Logger _log = Logger.getLogger(FlowProtocol.class);
    static protected final String DEFAULT_PROTOCOL_NAME = "Flow";
    static final private String SAMPLESET_NAME = "Samples";

    static public String getProtocolLSIDPrefix()
    {
        // See ExperimentServiceImpl.getNamespacePrefix(ExpProtocolImpl.class)
        return "Protocol";
    }

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

    static public boolean isDefaultProtocol(ExpProtocol protocol)
    {
        return protocol != null &&
                getProtocolLSIDPrefix().equals(protocol.getLSIDNamespacePrefix()) &&
                DEFAULT_PROTOCOL_NAME.equals(protocol.getName());
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
        if (!ret.getContainer().hasPermission(user, ReadPermission.class))
        {
            throw new UnauthorizedException();
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
        return urlFor(ProtocolController.ShowProtocolAction.class);
    }

    public ActionURL urlDownload()
    {
        throw new UnsupportedOperationException();
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
        String prop = (String) getProperty(FlowProperty.SampleSetJoin.getPropertyDescriptor());

        if (prop == null)
            return Collections.emptyMap();

        String[] values = StringUtils.split(prop, "&");
        Map<String, FieldKey> ret = new LinkedHashMap<>();

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
        String propValue = (String) getProperty(ExperimentProperty.SampleSetLSID.getPropertyDescriptor());
        if (propValue != null)
            return propValue;

        return ExperimentService.get().generateLSID(getContainer(), ExpSampleSet.class, SAMPLESET_NAME);
    }

    public void setSampleSetJoinFields(User user, Map<String, FieldKey> values) throws Exception
    {
        List<String> strings = new ArrayList<>();
        for (Map.Entry<String, FieldKey> entry : values.entrySet())
        {
            strings.add(PageFlowUtil.encode(entry.getKey()) + "=" + PageFlowUtil.encode(entry.getValue().toString()));
        }
        String value = StringUtils.join(strings.iterator(), "&");
        setProperty(user, FlowProperty.SampleSetJoin.getPropertyDescriptor(), value);
        setProperty(user, ExperimentProperty.SampleSetLSID.getPropertyDescriptor(), getSampleSetLSID());
        FlowManager.get().flowObjectModified();
    }

    public ActionURL urlUploadSamples(boolean importMoreSamples)
    {
        ActionURL ret = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowUploadMaterialsURL(getContainer());
        ret.addParameter("name", SAMPLESET_NAME);
        ret.addParameter("nameReadOnly", "true");
        if (importMoreSamples)
        {
            ret.addParameter("importMoreSamples", "true");
        }
        return ret;
    }

    public ActionURL urlShowSamples(boolean unlinkedOnly)
    {
        ActionURL ret = urlFor(ProtocolController.ShowSamplesAction.class);
        if (unlinkedOnly)
            ret.addParameter("unlinkedOnly", true);
        return ret;
    }

    public Map<SampleKey, ExpMaterial> getSampleMap(User user) throws SQLException
    {
        ExpSampleSet ss = getSampleSet();
        if (ss == null)
            return Collections.emptyMap();
        Set<String> propertyNames = getSampleSetJoinFields().keySet();
        if (propertyNames.size() == 0)
            return Collections.emptyMap();
        SamplesSchema schema = new SamplesSchema(user, getContainer());

        ExpMaterialTable sampleTable = schema.getSampleTable(ss);
        List<ColumnInfo> selectedColumns = new ArrayList<>();
        ColumnInfo colRowId = sampleTable.getColumn(ExpMaterialTable.Column.RowId.toString());
        selectedColumns.add(colRowId);
        for (String propertyName : propertyNames)
        {
            ColumnInfo lookupColumn = sampleTable.getColumn(propertyName);
            if (lookupColumn != null)
                selectedColumns.add(lookupColumn);
        }
        Map<SampleKey, ExpMaterial> ret = new HashMap<>();

        try (ResultSet rsSamples = new TableSelector(sampleTable, selectedColumns, null, null).getResultSet())
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

        return ret;
    }

    public int updateSampleIds(User user) throws SQLException
    {
        ExperimentService svc = ExperimentService.get();
        Map<String, FieldKey> joinFields = getSampleSetJoinFields();
        Map<SampleKey, ExpMaterial> sampleMap = getSampleMap(user);
        ExpSampleSet ss = getSampleSet();

        FlowSchema schema = new FlowSchema(user, getContainer());
        TableInfo fcsFilesTable = schema.getTable("FCSFiles");
        List<FieldKey> fields = new ArrayList<>();
        FieldKey fieldRowId = new FieldKey(null, "RowId");
        FieldKey fieldSampleRowId = new FieldKey(null, "Sample");
        fields.add(fieldRowId);
        fields.add(fieldSampleRowId);
        fields.addAll(joinFields.values());
        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(fcsFilesTable, fields);
        ColumnInfo colRowId = columns.get(fieldRowId);
        ColumnInfo colSampleId = columns.get(fieldSampleRowId);
        int ret = 0;

        try (ResultSet rs = new TableSelector(fcsFilesTable, new ArrayList<>(columns.values()), null, null).getResultSet();
             DbScope.Transaction transaction = svc.ensureTransaction())
        {
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
                if (Objects.equals(newSampleId, oldSampleId))
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
            transaction.commit();
        }
        return ret;
    }

    public int getUnlinkedSampleCount()
    {
        ExpSampleSet ss = getSampleSet();
        if (ss == null)
            return 0;

        return getUnlinkedSampleCount(ss.getSamples());
    }

    // TODO: Remove me as I am very expensive
    public static int getUnlinkedSampleCount(List<? extends ExpMaterial> samples)
    {
        if (samples == null)
            return 0;

        int count = 0;
        for (ExpMaterial material : samples)
        {
            List<FlowFCSFile> fcsFiles = getFCSFiles(material);
            if (fcsFiles.size() == 0)
                count++;
        }
        return count;
    }

    public static List<FlowFCSFile> getFCSFiles(ExpMaterial material)
    {
        if (material == null)
            return Collections.emptyList();

        List<? extends ExpProtocolApplication> apps = material.getTargetApplications();
        if (apps == null || apps.size() == 0)
            return Collections.emptyList();

        ArrayList<FlowFCSFile> result = new ArrayList<>();
        for (ExpProtocolApplication app : apps)
        {
            FlowDataObject.addDataOfType(app.getOutputDatas(), FlowDataType.FCSFile, result);
        }
        return result;
    }

    // CONSIDER: Use a fancy NestableQueryView to group FCSFiles by Sample
    public static Map<Pair<Integer, String>, List<Pair<Integer, String>>> getFCSFilesGroupedBySample(User user, Container c)
    {
        Map<Pair<Integer, String>, List<Pair<Integer,String>>> ret = new LinkedHashMap<>();

        FlowSchema schema = new FlowSchema(user, c);
        String sql = "SELECT " +
                "FCSFiles.RowId As FCSFileRowId,\n" +
                "FCSFiles.Name As FCSFileName,\n" +
                "M.RowId AS SampleRowId,\n" +
                "M.Name AS SampleName\n" +
                "FROM FCSFiles\n" +
                "FULL OUTER JOIN exp.Materials M ON\n" +
                "FCSFiles.Sample = M.RowId\n" +
                "ORDER BY M.Name";
        try (TableResultSet rs = (TableResultSet)QueryService.get().select(schema, sql))
        {
            for (Map<String, Object> row : rs)
            {
                Integer sampleRowId = (Integer) row.get("SampleRowId");
                String sampleName = (String) row.get("SampleName");
                Pair<Integer, String> samplePair = Pair.of(sampleRowId, sampleName);
                List<Pair<Integer, String>> fcsFiles = ret.get(samplePair);
                if (fcsFiles == null)
                    ret.put(samplePair, fcsFiles = new ArrayList<>());

                Integer fcsFileRowId = (Integer) row.get("FCSFIleRowId");
                String fcsFileName = (String) row.get("FCSFileName");
                Pair<Integer, String> fcsFilePair = Pair.of(fcsFileRowId, fcsFileName);
                fcsFiles.add(fcsFilePair);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
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
        ExperimentService expService = ExperimentService.get();
        FieldSubstitution fs = getFCSAnalysisNameExpr();
        if (fs == null)
        {
            fs = FlowProtocol.getDefaultFCSAnalysisNameExpr();
        }
        fs.insertParent(FieldKey.fromParts("FCSFile"));
        FlowSchema schema = new FlowSchema(user, getContainer());
        ExpDataTable table = schema.createFCSAnalysisTable("FCSAnalysis", FlowDataType.FCSAnalysis, false);
        Map<FieldKey, ColumnInfo> columns = new HashMap<>();
        ColumnInfo colRowId = table.getColumn(ExpDataTable.Column.RowId);
        columns.put(new FieldKey(null, "RowId"), colRowId);
        columns.putAll(QueryService.get().getColumns(table, Arrays.asList(fs.getFieldKeys())));

        try (DbScope.Transaction transaction = expService.ensureTransaction();
             ResultSet rs = new TableSelector(table, new ArrayList<>(columns.values()), null, null).getResultSet())
        {
            while (rs.next())
            {
                int rowid = ((Number) colRowId.getValue(rs)).intValue();
                FlowObject obj = FlowDataObject.fromRowId(rowid);
                if (obj instanceof FlowFCSAnalysis)
                {
                    ExpData data = ((FlowFCSAnalysis) obj).getData();
                    String name = fs.eval(columns, rs);
                    if (!Objects.equals(name, data.getName()))
                    {
                        data.setName(name);
                        data.save(user);
                    }
                }
            }
            transaction.commit();
        }
        finally
        {
            FlowManager.get().flowObjectModified();
        }
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
		ArrayList<ColumnInfo> sel = new ArrayList<>(columns.values());
		sel.add(colRowId);

        try (ResultSet rs = new TableSelector(table, sel, filter, null).getResultSet())
        {
            if (rs.next())
            {
                return fs.eval(columns, rs);
            }
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
        FlowManager.get().flowObjectModified();
    }

    public boolean hasICSMetadata()
    {
        String metadata = getICSMetadataString();
        return metadata != null && metadata.length() > 0;
    }

    @Nullable
    public ICSMetadata getICSMetadata()
    {
        String metadata = getICSMetadataString();
        if (metadata == null || metadata.length() == 0)
            return null;
        return ICSMetadata.fromXmlString(metadata);
    }

    public String getProtocolSettingsDescription()
    {
        List<String> parts = new ArrayList<>();
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
