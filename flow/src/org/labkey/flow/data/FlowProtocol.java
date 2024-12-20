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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
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
import org.labkey.api.exp.api.ExpSampleType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.api.SampleTypeService;
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
import org.labkey.api.query.QueryParam;
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
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.script.KeywordsJob;

import jakarta.servlet.http.HttpServletRequest;
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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FlowProtocol extends FlowObject<ExpProtocol>
{
    static private final Logger _log = LogManager.getLogger(FlowProtocol.class);
    static protected final String DEFAULT_PROTOCOL_NAME = "Flow";
    static private final String SAMPLETYPE_NAME = "Samples";

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

    /**
     * Returns the sample type in scope
     */
    public ExpSampleType getSampleType(User user)
    {
        return SampleTypeService.get().getSampleType(getContainer(), user, SAMPLETYPE_NAME);
    }

    /**
     * Construct a container filter appropriate for the sample type in scope and user permissions
     */
    public ContainerFilter getContainerFilter(ExpSampleType sampleType, User user)
    {
        return new ContainerFilter.SimpleContainerFilter(ExpSchema.getSearchContainers(getContainer(), sampleType, null, user));
    }

    public ActionURL getSampleTypeDetailsURL(ExpSampleType sampleType, Container container)
    {
        ActionURL url = sampleType.detailsURL();

        // set the container filter if the sample type is scoped to the project
        if (!sampleType.getContainer().equals(container) && sampleType.getContainer().isProject())
        {
            url.setContainer(getContainer());
            url.addParameter("Material." + QueryParam.containerFilterName, ContainerFilter.Type.CurrentPlusProject.name());
        }
        return url;
    }

    /**
     * Returns a map of Sample property to FieldKey relative to the FCSFile table.
     */
    public Map<String, FieldKey> getSampleTypeJoinFields()
    {
        String prop = (String) getProperty(FlowProperty.SampleTypeJoin.getPropertyDescriptor());

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

    public String getSampleTypeLSID(User user)
    {
        String propValue = (String) getProperty(ExperimentProperty.SampleTypeLSID.getPropertyDescriptor());
        if (propValue != null)
            return propValue;

        // get lsid for sample type with name "Samples"
        ExpSampleType sampleType = getSampleType(user);
        if (sampleType != null)
            return sampleType.getLSID();

        return null;
    }

    public void setSampleTypeJoinFields(User user, Map<String, FieldKey> values) throws Exception
    {
        List<String> strings = new ArrayList<>();
        for (Map.Entry<String, FieldKey> entry : values.entrySet())
        {
            strings.add(PageFlowUtil.encode(entry.getKey()) + "=" + PageFlowUtil.encode(entry.getValue().toString()));
        }
        String value = StringUtils.join(strings.iterator(), "&");
        setProperty(user, FlowProperty.SampleTypeJoin.getPropertyDescriptor(), value);
        setProperty(user, ExperimentProperty.SampleTypeLSID.getPropertyDescriptor(), getSampleTypeLSID(user));
        FlowManager.get().flowObjectModified();
    }

    public ActionURL urlCreateSampleType()
    {
        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getCreateSampleTypeURL(getContainer());
        url.addParameter("name", SAMPLETYPE_NAME);
        url.addParameter("nameReadOnly", true);
        return url;
    }

    public ActionURL urlUploadSamples()
    {
        return PageFlowUtil.urlProvider(ExperimentUrls.class).getImportSamplesURL(getContainer(), SAMPLETYPE_NAME);
    }

    public ActionURL urlShowSamples()
    {
        return urlFor(ProtocolController.ShowSamplesAction.class);
    }

    public Map<SampleKey, ExpMaterial> getSampleMap(User user)
    {
        ExpSampleType st = getSampleType(user);
        if (st == null)
            return Collections.emptyMap();
        Set<String> propertyNames = getSampleTypeJoinFields().keySet();
        if (propertyNames.size() == 0)
            return Collections.emptyMap();
        SamplesSchema schema = new SamplesSchema(user, getContainer());

        ContainerFilter cf = getContainerFilter(st, user);
        TableInfo sampleTable = schema.getTable(st, cf);
        List<ColumnInfo> selectedColumns = new ArrayList<>();
        ColumnInfo colRowId = sampleTable.getColumn(ExpMaterialTable.Column.RowId.toString());
        selectedColumns.add(colRowId);
        for (String propertyName : propertyNames)
        {
            ColumnInfo lookupColumn = sampleTable.getColumn(propertyName);
            if (lookupColumn != null)
                selectedColumns.add(lookupColumn);
            else
                _log.warn("Flow sample join property '" + propertyName + "' not found on SampleType");
        }

        Map<Integer, ExpMaterial> materialMap = new HashMap<>();
        List<? extends ExpMaterial> materials = getSamples(st, user);
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

    /**
     * Returns the samples for this sample type, if the sample type is scoped at the project
     * level and the user has read access then both current and project level samples will be
     * returned
     */
    public List<? extends ExpMaterial> getSamples(ExpSampleType sampleType, User user)
    {
        ContainerFilter cf = new ContainerFilter.SimpleContainerFilter(ExpSchema.getSearchContainers(getContainer(), sampleType, null, user));
        List<ExpMaterial> samples = new ArrayList<>();

        cf.getIds().forEach(id -> samples.addAll(sampleType.getSamples(ContainerManager.getForId(id))));

        return samples;
    }

    public int updateSampleIds(User user)
    {
        _log.info("updateSampleIds: protocol=" + this.getName() + ", folder=" + this.getContainerPath());

        ExperimentService svc = ExperimentService.get();
        Map<String, FieldKey> joinFields = getSampleTypeJoinFields();
        _log.debug("joinFields: " + joinFields);

        Map<SampleKey, ExpMaterial> sampleMap = getSampleMap(user);
        _log.debug("sampleMap=" + sampleMap.size());

        ExpSampleType st = getSampleType(user);
        _log.debug("sampleType=" + (st == null ? "<none>" : st.getName()) + ", lsid=" + (st == null ? "<none>" : st.getLSID()));

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
                    if (material.getCpasType() == null || !Objects.equals(material.getCpasType(), st.getLSID()))
                    {
                        _log.debug("   sample's sampletype isn't ours: " + material.getCpasType());
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

    public static class FCSFilesGroupedBySample
    {
        public Map<Integer, Map<FieldKey, Object>> samples;
        public Map<Integer, Map<FieldKey, Object>> fcsFiles;
        public List<FieldKey> sampleFields;
        public Collection<FieldKey> fcsFileFields;
        public Map<Integer, Pair<Integer, String>> fcsFileRuns;
        public Map<Integer, List<Integer>> linkedSampleIdToFcsFileIds;
        public int linkedFcsFileCount;
        public List<Integer> unlinkedFcsFileIds;
        public List<Integer> unlinkedSampleIds;

    }

    // CONSIDER: Use a fancy NestableQueryView to group FCSFiles by Sample
    public FCSFilesGroupedBySample getFCSFilesGroupedBySample(User user, Container c)
    {
        var joinFields = this.getSampleTypeJoinFields();
        var sampleFields = joinFields.keySet().stream().map(FieldKey::fromParts).collect(toList());
        var fcsFileFields = joinFields.values();

        FlowSchema schema = new FlowSchema(user, c);
        String sql = "SELECT\n" +
                "  __FCSFiles.RowId As FCSFileRowId,\n" +
                "  __FCSFiles.Run.RowId AS FCSFileRunId,\n" +
                "  __FCSFiles.Run.Name AS FCSFileRunName,\n" +
                "  M.RowId AS SampleRowId,\n" +
                "FROM __FCSFiles\n" +
                "FULL OUTER JOIN __Samples M ON\n" +
                "__FCSFiles.Sample = M.RowId\n" +
                "ORDER BY M.Name, __FCSFiles.RowId";

        ExpSampleType sampleType = getSampleType(user);
        ContainerFilter cf = getContainerFilter(sampleType, user);
        UserSchema userSchema = QueryService.get().getUserSchema(user, getContainer(), SamplesSchema.SCHEMA_NAME);
        TableInfo sampleTable = userSchema.getTable(SAMPLETYPE_NAME, cf);
        TableInfo fcsTable = schema.getTable(FlowTableType.FCSFiles, null);

        Map<String, TableInfo> tableMap = new HashMap<>();
        tableMap.put("__FCSFiles", fcsTable);
        tableMap.put("__Samples", sampleTable);

        List<Integer> sampleIds = new ArrayList<>();
        List<Integer> fcsFileIds = new ArrayList<>();
        Map<Integer, List<Integer>> samplesToFcsFiles = new LinkedHashMap<>();
        List<Integer> unlinkedFcsFileIds = new ArrayList<>();
        Map<Integer, Pair<Integer, String>> fcsFileRuns = new HashMap<>();
        int linkedFcsFileCount = 0;

        try (TableResultSet rs = (TableResultSet)QueryService.get().select(schema, sql, tableMap, false, false))
        {
            for (Map<String, Object> row : rs)
            {
                Integer sampleRowId = (Integer) row.get("SampleRowId");
                Integer fcsFileRowId = (Integer) row.get("FCSFileRowId");
                Integer fcsFileRunId = (Integer) row.get("FCSFileRunId");
                String fcsFileRunName = (String) row.get("FCSFileRunName");

                if (sampleRowId != null)
                {
                    sampleIds.add(sampleRowId);
                    if (fcsFileRowId != null)
                    {
                        var fcsFiles = samplesToFcsFiles.computeIfAbsent(sampleRowId, k -> new ArrayList<>());
                        fcsFiles.add(fcsFileRowId);
                        linkedFcsFileCount++;
                    }
                }
                else
                {
                    if (fcsFileRowId != null)
                        unlinkedFcsFileIds.add(fcsFileRowId);
                }

                if (fcsFileRowId != null)
                {
                    fcsFileIds.add(fcsFileRowId);
                    if (fcsFileRunId != null)
                    {
                        fcsFileRuns.put(fcsFileRowId, Pair.of(fcsFileRunId, fcsFileRunName));
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        List<Integer> unlinkedSampleIds = new ArrayList<>(sampleIds);
        unlinkedSampleIds.removeAll(samplesToFcsFiles.keySet());

        var rowIdFieldKey = FieldKey.fromParts("RowId");
        var nameFieldKey = FieldKey.fromParts("Name");

        Map<Integer, Map<FieldKey, Object>> samples = new HashMap<>();
        var sampleColumns = new HashSet<FieldKey>();
        sampleColumns.add(rowIdFieldKey);
        sampleColumns.add(nameFieldKey);
        sampleColumns.addAll(sampleFields);
        var sampleColumnMap = QueryService.get().getColumns(sampleTable, sampleColumns);
        try (var results = new TableSelector(sampleTable, sampleColumnMap.values(), new SimpleFilter(rowIdFieldKey, sampleIds, CompareType.IN), null).getResults())
        {
            while (results.next())
            {
                var row = results.getFieldKeyRowMap();
                Integer sampleId = (Integer)row.get(rowIdFieldKey);
                samples.put(sampleId, new HashMap<>(row));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        Map<Integer, Map<FieldKey, Object>> fcsFiles = new HashMap<>();
        var fcsFileColumns = new HashSet<FieldKey>();
        fcsFileColumns.add(rowIdFieldKey);
        fcsFileColumns.add(nameFieldKey);
        fcsFileColumns.addAll(fcsFileFields);
        var fcsFilesTable = schema.createFCSFileTable("FCSFiles", null);
        var fcsFileColumnMap = QueryService.get().getColumns(fcsFilesTable, fcsFileColumns);
        try (var results = new TableSelector(fcsFilesTable, fcsFileColumnMap.values(), new SimpleFilter(rowIdFieldKey, fcsFileIds, CompareType.IN), null).getResults())
        {
            while (results.next())
            {
                var row = results.getFieldKeyRowMap();
                Integer fcsFileId = (Integer) row.get(rowIdFieldKey);
                fcsFiles.put(fcsFileId, new HashMap<>(row));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        ;

        var ret = new FCSFilesGroupedBySample();
        ret.fcsFileRuns = fcsFileRuns;
        ret.linkedFcsFileCount = linkedFcsFileCount;
        ret.linkedSampleIdToFcsFileIds = samplesToFcsFiles;
        ret.unlinkedFcsFileIds = unlinkedFcsFileIds;
        ret.unlinkedSampleIds = unlinkedSampleIds;
        ret.fcsFiles = fcsFiles;
        ret.samples = samples;
        ret.sampleFields = sampleFields;
        ret.fcsFileFields = fcsFileFields;
        return ret;
    }


    public SampleKey makeSampleKey(String runName, String fileName, AttributeSet attrs)
    {
        Collection<FieldKey> fields = getSampleTypeJoinFields().values();
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
        FlowSchema schema = new FlowSchema(User.getSearchUser(), getContainer());
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
        if (getSampleTypeJoinFields().size() != 0)
        {
            parts.add("Sample type join fields");
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

            // create sample type
            assertNull(protocol.getSampleType(user));
            String sampleTypeLSID = protocol.getSampleTypeLSID(user);
            assertNull(sampleTypeLSID);

            List<GWTPropertyDescriptor> props = List.of(
                    new GWTPropertyDescriptor("Name", "string"),
                    new GWTPropertyDescriptor("ExprName", "string"),
                    new GWTPropertyDescriptor("WellId", "string"),
                    new GWTPropertyDescriptor("PTID", "string")
            );
            ExpSampleType st = SampleTypeService.get().createSampleType(c, user, SAMPLETYPE_NAME, null,
                    props, List.of(), -1,-1,-1,-1,null);
            assertNotNull(protocol.getSampleType(user));

            sampleTypeLSID = protocol.getSampleTypeLSID(user);
            assertNotNull(sampleTypeLSID);

            // add join fields
            assertEquals(0, protocol.getSampleTypeJoinFields().size());
            protocol.setSampleTypeJoinFields(user, Map.of(
                    "ExprName", FieldKey.fromParts("Keyword", "EXPERIMENT NAME"),
                    "WellId", FieldKey.fromParts("Keyword", "WELL ID")
            ));

            // import samples:
            //   Name  PTID  WellId  ExprName
            //   one   p01   E01     L02-060329-PV1-R1
            //   two   p02   E02     L02-060329-PV1-R1
            UserSchema schema = QueryService.get().getUserSchema(user, c, "samples");
            TableInfo table = schema.getTable(SAMPLETYPE_NAME);

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
            DomainProperty exprNameProp = st.getDomain().getPropertyByName("ExprName");
            DomainProperty wellIdProp = st.getDomain().getPropertyByName("WellId");
            DomainProperty ptidProp = st.getDomain().getPropertyByName("PTID");

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
            Map<? extends ExpMaterial, String> materialInputs = afterSampleImportRun.getExperimentRun().getMaterialInputs();
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
