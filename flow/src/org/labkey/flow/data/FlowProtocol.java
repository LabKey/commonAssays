/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.CaseInsensitiveHashMap;
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
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.api.SampleSetService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.ExperimentProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.exp.query.SamplesSchema;
import org.labkey.api.gwt.client.model.GWTPropertyDescriptor;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryRowReference;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.TestContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.script.KeywordsJob;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FlowProtocol extends FlowObject<ExpProtocol>
{
    static private final Logger _log = Logger.getLogger(FlowProtocol.class);
    static protected final String DEFAULT_PROTOCOL_NAME = "Flow";
    static private final String SAMPLESET_NAME = "Samples";

    static private final boolean DEFAULT_CASE_SENSITIVE_KEYWORDS = true;
    static private final boolean DEFAULT_CASE_SENSITIVE_STATS_AND_GRAPHS = false;

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
                ret.setProperty(user, ExperimentProperty.PROTOCOLIMPLEMENTATION.getPropertyDescriptor(), FlowProtocolImplementation.NAME);
            FlowProtocolStep.initProtocol(user, ret);
            return ret;
        }

        try (var ignore = SpringActionController.ignoreSqlUpdates())
        {
            ExpProtocol protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ExperimentRun, DEFAULT_PROTOCOL_NAME);
            protocol.save(user);
            protocol.setProperty(user, ExperimentProperty.PROTOCOLIMPLEMENTATION.getPropertyDescriptor(), FlowProtocolImplementation.NAME);
            ret = new FlowProtocol(protocol);
            FlowProtocolStep.initProtocol(user, ret);
            return ret;
        }
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

    public static FlowProtocol fromURLRedirectIfNull(User user, ActionURL url, HttpServletRequest request)
    {
        FlowProtocol protocol = fromURL(user, url, request);
        if (protocol == null)
            throw new RedirectException(url.clone().setAction(FlowController.BeginAction.class));

        return protocol;
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

    // For serialzation
    protected FlowProtocol() {}

    public FlowProtocol(ExpProtocol protocol)
    {
        super(protocol);
    }

    public ExpProtocol getProtocol()
    {
        return getExpObject();
    }

    @Override
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

    @Override
    public FlowObject getParent()
    {
        return null;
    }

    @Override
    public ActionURL urlShow()
    {
        return urlFor(ProtocolController.ShowProtocolAction.class);
    }

    @Override
    public ActionURL urlDownload()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryRowReference getQueryRowReference()
    {
        return new QueryRowReference(getContainer(), ExpSchema.SCHEMA_EXP, ExpSchema.TableType.Protocols.name(), FieldKey.fromParts("RowId"), getProtocol().getRowId());
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

    public ActionURL urlCreateSampleSet()
    {
        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getCreateSampleSetURL(getContainer());
        url.addParameter("name", SAMPLESET_NAME);
        url.addParameter("nameReadOnly", true);
        return url;
    }

    public ActionURL urlUploadSamples()
    {
        return PageFlowUtil.urlProvider(ExperimentUrls.class).getImportSamplesURL(getContainer(), SAMPLESET_NAME);
    }

    public ActionURL urlShowSamples()
    {
        return urlFor(ProtocolController.ShowSamplesAction.class);
    }

    public Map<SampleKey, ExpMaterial> getSampleMap(User user)
    {
        ExpSampleSet ss = getSampleSet();
        if (ss == null)
            return Collections.emptyMap();
        Set<String> propertyNames = getSampleSetJoinFields().keySet();
        if (propertyNames.size() == 0)
            return Collections.emptyMap();
        SamplesSchema schema = new SamplesSchema(user, getContainer());

        ExpMaterialTable sampleTable = schema.getSampleTable(ss, null);
        List<ColumnInfo> selectedColumns = new ArrayList<>();
        ColumnInfo colRowId = sampleTable.getColumn(ExpMaterialTable.Column.RowId.toString());
        selectedColumns.add(colRowId);
        for (String propertyName : propertyNames)
        {
            ColumnInfo lookupColumn = sampleTable.getColumn(propertyName);
            if (lookupColumn != null)
                selectedColumns.add(lookupColumn);
            else
                _log.warn("Flow sample join property '" + propertyName + "' not found on SampleSet");
        }

        Map<Integer, ExpMaterial> materialMap = new HashMap<>();
        List<? extends ExpMaterial> materials = ss.getSamples(getContainer());
        for (ExpMaterial material : materials)
        {
            materialMap.put(material.getRowId(), material);
        }

        Map<SampleKey, ExpMaterial> ret = new HashMap<>();
        try (ResultSet rsSamples = new TableSelector(sampleTable, selectedColumns, null, null).getResultSet())
        {
            while (rsSamples.next())
            {
                int rowId = ((Number) colRowId.getValue(rsSamples)).intValue();
                ExpMaterial sample = materialMap.get(rowId);
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
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        return ret;
    }

    public int updateSampleIds(User user)
    {
        _log.info("updateSampleIds: protocol=" + this.getName() + ", folder=" + this.getContainerPath());

        ExperimentService svc = ExperimentService.get();
        Map<String, FieldKey> joinFields = getSampleSetJoinFields();
        _log.debug("joinFields: " + joinFields);

        Map<SampleKey, ExpMaterial> sampleMap = getSampleMap(user);
        _log.debug("sampleMap=" + sampleMap.size());

        ExpSampleSet ss = getSampleSet();
        _log.debug("sampleSet=" + (ss == null ? "<none>" : ss.getName()) + ", lsid=" + (ss == null ? "<none>" : ss.getLSID()));

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

        int fcsFileCount = 0;
        int unchanged = 0;
        int linked = 0;

        List<? extends ExpData> fcsFiles = ExperimentService.get().getExpDatas(getContainer(), FlowDataType.FCSFile, null);
        Map<Integer, ExpData> fcsFileMap = new HashMap<>();
        for (ExpData fcsFile : fcsFiles)
        {
            fcsFileMap.put(fcsFile.getRowId(), fcsFile);
        }

        try (ResultSet rs = new TableSelector(fcsFilesTable, new ArrayList<>(columns.values()), null, null).getResultSet();
             DbScope.Transaction transaction = svc.ensureTransaction())
        {
            _log.debug("entered transaction");
            transaction.addCommitTask(() -> {
                _log.debug("update flow object on tx post-commit");
                FlowManager.get().flowObjectModified();
            }, DbScope.CommitTaskOption.POSTCOMMIT);

            Set<ExpRun> fcsFileRuns = new HashSet<>();
            while (rs.next())
            {
                Number fcsFileId = ((Number) colRowId.getValue(rs));
                ExpData fcsFile = fcsFileMap.get(fcsFileId);
                _log.debug("-- fcsFileId=" + fcsFileId + ", fcsFile=" + fcsFile);
                if (fcsFile == null)
                    continue;
                fcsFileCount++;
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
                _log.debug("   sampleKey=" + key);

                ExpMaterial sample = sampleMap.get(key);
                Integer newSampleId = sample == null ? null : sample.getRowId();
                Object oldSampleId = colSampleId.getValue(rs);
                _log.debug("   newSampleId=" + newSampleId + ", oldSampleId=" + oldSampleId);
                if (Objects.equals(newSampleId, oldSampleId))
                {
                    unchanged++;
                    _log.debug("   unchanged");
                    continue;
                }
                ExpProtocolApplication app = fcsFile.getSourceApplication();
                if (app == null)
                {
                    // This will happen for orphaned FCSFiles (where the ExperimentRun has been deleted).
                    _log.debug("   orphaned FCSFile");
                    continue;
                }
                _log.debug("   protocol app=" + app.getName());

                boolean changed = false;
                boolean found = false;
                for (ExpMaterial material : app.getInputMaterials())
                {
                    if (material.getCpasType() == null || !Objects.equals(material.getCpasType(), ss.getLSID()))
                    {
                        _log.debug("   sample's sampleset isn't ours: " + material.getCpasType());
                        continue;
                    }
                    if (sample != null)
                    {
                        _log.debug("   found previously linked sample, no change");
                        if (material.equals(sample))
                        {
                            found = true;
                            linked++;
                            break;
                        }
                    }
                    _log.debug("   found previously linked sample no longer needed, remove = " + material.getName());
                    app.removeMaterialInput(user, material);
                    changed = true;
                }
                if (!found && sample != null)
                {
                    _log.debug("   didn't find previously linked sample, add");
                    app.addMaterialInput(user, sample, null);
                    linked++;
                    changed = true;
                }

                if (changed)
                {
                    ExpRun fcsFileRun = app.getRun();
                    fcsFileRuns.add(fcsFileRun);
                }
            }

            if (!fcsFileRuns.isEmpty())
            {
                _log.info(fcsFileRuns.size() + " runs changed, syncing edges");
                ExperimentService.get().syncRunEdges(fcsFileRuns);
            }

            if (!transaction.isAborted())
            {
                _log.debug("commit...");
                transaction.commit();
            }
            else
            {
                _log.debug("tx aborted, not committing");
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        _log.debug("fcsFileCount=" + fcsFileCount + ", sampleCount=" + sampleMap.size() + ", linked=" + linked + ", unchanged=" + unchanged);
        return linked;
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
        ExpDataTable table = schema.createFCSAnalysisTable("FCSAnalysis", null, FlowDataType.FCSAnalysis, false);
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
        ExpDataTable table = schema.createFCSFileTable("fcsFiles", null);
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

    // Filter columns are relative to the FCSFiles table
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

    public boolean isCaseSensitiveKeywords()
    {
        Boolean value = (Boolean)getProperty(FlowProperty.CaseSensitiveKeywords.getPropertyDescriptor());
        return value != null ? value.booleanValue() : DEFAULT_CASE_SENSITIVE_KEYWORDS;
    }

    public void setCaseSensitiveKeywords(User user, boolean caseSensitive) throws SQLException
    {
        setProperty(user, FlowProperty.CaseSensitiveKeywords.getPropertyDescriptor(), caseSensitive);
    }

    public boolean isCaseSensitiveStatsAndGraphs()
    {
        Boolean value = (Boolean)getProperty(FlowProperty.CaseSensitiveStatsAndGraphs.getPropertyDescriptor());
        return value != null ? value.booleanValue() : DEFAULT_CASE_SENSITIVE_STATS_AND_GRAPHS;
    }

    public void setCaseSensitiveStatsAndGraphs(User user, boolean caseSensitive) throws SQLException
    {
        setProperty(user, FlowProperty.CaseSensitiveStatsAndGraphs.getPropertyDescriptor(), caseSensitive);
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
            parts.add("Metadata");
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

    @Override
    public String getLabel()
    {
        return "Protocol '" + getName() + "'";
    }

    public static class TestCase
    {
        private Container c;
        private User user;

        @Before
        public void setup()
        {
            JunitUtil.deleteTestContainer();
            c = JunitUtil.getTestContainer();
            user = TestContext.get().getUser();
        }

        @Test
        public void testSampleJoin() throws Exception
        {
            FlowProtocol protocol = ensureForContainer(user, c);
            PipeRoot root = PipelineService.get().findPipelineRoot(c);

            // import some FCS files
            ViewBackgroundInfo info = new ViewBackgroundInfo(c, user, null);
            File dir = JunitUtil.getSampleData(null, "flow/flowjoquery/microFCS");
            KeywordsJob job = new KeywordsJob(info, protocol, List.of(dir), null, root);
            List<FlowRun> runs = job.go();
            assertNotNull(runs);
            assertEquals(1, runs.size());

            FlowRun run = runs.get(0);
            int runId = run.getRunId();
            FlowFCSFile[] fcsFiles = run.getFCSFiles();
            assertEquals(2, fcsFiles.length);
            assertEquals(0, fcsFiles[0].getSamples().size());
            assertEquals(0, fcsFiles[1].getSamples().size());

            // add join fields
            assertEquals(0, protocol.getSampleSetJoinFields().size());
            protocol.setSampleSetJoinFields(user, Map.of(
                    "ExprName", FieldKey.fromParts("Keyword", "EXPERIMENT NAME"),
                    "WellId", FieldKey.fromParts("Keyword", "WELL ID")
            ));

            // create sample set
            assertNull(protocol.getSampleSet());
            String sampleSetLSID = protocol.getSampleSetLSID();
            assertNull(SampleSetService.get().getSampleSet(sampleSetLSID));

            List<GWTPropertyDescriptor> props = List.of(
                    new GWTPropertyDescriptor("Name", "string"),
                    new GWTPropertyDescriptor("ExprName", "string"),
                    new GWTPropertyDescriptor("WellId", "string"),
                    new GWTPropertyDescriptor("PTID", "string")
            );
            ExpSampleSet ss = SampleSetService.get().createSampleSet(c, user, SAMPLESET_NAME, null,
                    props, List.of(), -1,-1,-1,-1,null);
            assertNotNull(protocol.getSampleSet());

            // import samples:
            //   Name  PTID  WellId  ExprName
            //   one   p01   E01     L02-060329-PV1-R1
            //   two   p02   E02     L02-060329-PV1-R1
            UserSchema schema = QueryService.get().getUserSchema(user, c, "samples");
            TableInfo table = schema.getTable(SAMPLESET_NAME);

            BatchValidationException errors = new BatchValidationException();
            QueryUpdateService qus = table.getUpdateService();
            List<Map<String, Object>> rows = qus.insertRows(user, c, List.of(
                    CaseInsensitiveHashMap.of(
                            "Name", "one",
                            "ExprName", "L02-060329-PV1-R1",
                            "WellId", "E01",
                            "PTID", "p01"),
                    CaseInsensitiveHashMap.of(
                            "Name", "two",
                            "ExprName", "L02-060329-PV1-R1",
                            "WellId", "E02",
                            "PTID", "p02")
            ), errors, null, null);
            if (errors.hasErrors())
                throw errors;

            // verify - FCSFile linked to sample
            DomainProperty exprNameProp = ss.getDomain().getPropertyByName("ExprName");
            DomainProperty wellIdProp = ss.getDomain().getPropertyByName("WellId");
            DomainProperty ptidProp = ss.getDomain().getPropertyByName("PTID");

            ExpMaterial toBeDeleted = null;
            FlowRun afterSampleImportRun = FlowRun.fromRunId(runId);
            for (FlowFCSFile file : afterSampleImportRun.getFCSFiles())
            {
                List<? extends ExpMaterial> samples = file.getSamples();
                assertEquals(1, samples.size());
                ExpMaterial sample = samples.get(0);

                String WELL_ID = file.getKeyword("WELL ID");
                String wellId = (String)sample.getProperty(wellIdProp);
                assertEquals(WELL_ID, wellId);

                if ("E01".equals(wellId))
                    assertEquals("one", sample.getName());
                else if ("E02".equals(wellId))
                    assertEquals("two", sample.getName());

                if (toBeDeleted == null)
                    toBeDeleted = sample;
            }

            // verify - Samples aren't added as inputs to the FCSFile's run, just the individual Keywords protocol applications
            Map<ExpMaterial, String> materialInputs = afterSampleImportRun.getExperimentRun().getMaterialInputs();
            assertEquals(0, materialInputs.size());
            assertEquals(2, sumInteralMaterialInputs(afterSampleImportRun));

            // delete one sample
            String toBeDeletedWellId = (String)toBeDeleted.getProperty(wellIdProp);
            toBeDeleted.delete(user);

            // verify - FCSFile no longer linked
            FlowRun afterSampleDeletedRun = FlowRun.fromRunId(runId);
            assertEquals(1, sumInteralMaterialInputs(afterSampleDeletedRun));
            for (FlowFCSFile file : afterSampleImportRun.getFCSFiles())
            {
                String WELL_ID = file.getKeyword("WELL ID");
                if (WELL_ID.equals(toBeDeletedWellId))
                    assertEquals(0, file.getSamples().size());
                else
                    assertEquals(1, file.getSamples().size());
            }
        }

        private int sumInteralMaterialInputs(FlowRun run)
        {
            return run
                .getExperimentRun()
                .getProtocolApplications()
                .stream().mapToInt(p -> p.getMaterialInputs().size()).sum();
        }
    }
}
