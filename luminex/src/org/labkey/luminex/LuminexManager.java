/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.WellExclusion;
import org.labkey.luminex.query.LuminexDataTable;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LuminexManager
{
    private static final LuminexManager instance = new LuminexManager();
    private static final Logger _log = Logger.getLogger(LuminexManager.class);
    public static final String SCHEMA_NAME = "luminex";
    public static final String RERUN_TRANSFORM = "rerunTransform";


    static public LuminexManager get()
    {
        return instance;
    }

    public String getSchemaName()
    {
        return SCHEMA_NAME;
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }

    public enum ExclusionType
    {
        WellExclusion("Replicate Group Exclusion")
        {
            @Override
            public String getTableName()
            {
                return "WellExclusion";
            }

            @Override
            public String getInfo(List<LuminexSingleExclusionCommand> commands)
            {
                StringBuilder sb = new StringBuilder();
                if (commands.size() > 1)
                {
                    sb.append("MULTIPLE " + getDescription(commands.get(0)).toLowerCase() + "s");
                }
                else if (commands.size() == 1)
                {
                    LuminexSingleExclusionCommand command = commands.get(0);
                    sb.append(command.getCommand().toUpperCase()).
                            append(" ").
                            append(getDescription(command).toLowerCase()).
                            append(" (Description: ").
                            append(command.getDescription()).
                            append(", Type: ").
                            append(command.getType());

                    if (command.getWell() != null)
                        sb.append(", Well: ").append(command.getWell());
                    sb.append(")");
                }
                else
                    sb.append(getDescription(null).toLowerCase());

                return sb.toString();
            }

            @Override
            public Map<String, Object> getRowMap(LuminexSingleExclusionCommand form, Integer runId, boolean keysOnly)
            {
                Map<String, Object> row = new HashMap<>();
                if (!keysOnly)
                {
                    row = form.getBaseRowMap();
                    row.put("Type", form.getType());
                    row.put("Well", form.getWell());
                }
                row.put("RowId", form.getKey());
                return row;
            }

            @Override
            public String getDescription(@Nullable LuminexSingleExclusionCommand command)
            {
                if (command != null && command.getWell() != null)
                {
                    // single well exclusion
                    return "Well Exclusion";
                }
                return super.getDescription(command);
            }
        },
        SinglepointUnknownExclusion("Singlepoint Unknown Exclusion")
        {
            @Override
            public String getTableName()
            {
                return "WellExclusion";
            }

            @Override
            public String getInfo(List<LuminexSingleExclusionCommand> commands)
            {
                // for singlepoint unknown exclusions, null command means that we have > 1 command in this job
                String info = "MULTIPLE " + getDescription(null).toLowerCase() + "s";
                if (commands.size() == 1)
                {
                    LuminexSingleExclusionCommand command = commands.get(0);
                    info = command.getCommand().toUpperCase() + " " + getDescription(command).toLowerCase()
                            + " (Description: " + command.getDescription() + ", Dilution: " + command.getDilution() + ")";
                }
                return info;
            }

            @Override
            public Map<String, Object> getRowMap(LuminexSingleExclusionCommand form, Integer runId, boolean keysOnly)
            {
                Map<String, Object> row = new HashMap<>();
                if (!keysOnly)
                {
                    row = form.getBaseRowMap();
                    row.put("Dilution", form.getDilution());
                }
                row.put("RowId", form.getKey());
                return row;
            }
        },
        TitrationExclusion("Titration Exclusion")
        {
            @Override
            public String getTableName()
            {
                return "WellExclusion";
            }

            @Override
            public String getInfo(List<LuminexSingleExclusionCommand> commands)
            {
                // for titration exclusions, null command means that we have > 1 command in this job
                String info = "MULTIPLE " + getDescription(null).toLowerCase() + "s";
                if (commands.size() == 1)
                {
                    LuminexSingleExclusionCommand command = commands.get(0);
                    info = command.getCommand().toUpperCase() + " " + getDescription(command).toLowerCase()
                            + " (Description: " + command.getDescription() + ")";
                }
                return info;
            }

            @Override
            public Map<String, Object> getRowMap(LuminexSingleExclusionCommand form, Integer runId, boolean keysOnly)
            {
                Map<String, Object> row = new HashMap<>();
                if (!keysOnly)
                {
                    row = form.getBaseRowMap();
                }
                row.put("RowId", form.getKey());
                return row;
            }
        },
        RunExclusion("Analyte Exclusion")
        {
            @Override
            public String getTableName()
            {
                return "RunExclusion";
            }

            @Override
            public String getInfo(List<LuminexSingleExclusionCommand> commands)
            {
                String info = getDescription(null).toLowerCase();
                if (commands.size() == 1)
                {
                    info = commands.get(0).getCommand().toUpperCase() + " " + info;
                }
                return info;
            }

            @Override
            public Map<String, Object> getRowMap(LuminexSingleExclusionCommand form, Integer runId, boolean keysOnly)
            {
                Map<String, Object> row = new HashMap<>();
                if (!keysOnly)
                {
                    row = form.getBaseRowMap();
                }
                row.put("RunId", runId);
                return row;
            }
        };

        private String _description;

        ExclusionType(String description)
        {
            _description = description;
        }

        public String getDescription(@Nullable LuminexSingleExclusionCommand command)
        {
            return _description;
        }

        public abstract String getTableName();
        public abstract String getInfo(List<LuminexSingleExclusionCommand> commands);
        public abstract Map<String, Object> getRowMap(LuminexSingleExclusionCommand form, Integer runId, boolean keysOnly);
    }

    public Analyte[] getAnalytes(int dataRowId)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("DataId"), dataRowId);
        Sort sort = new Sort("RowId");
        return new TableSelector(LuminexProtocolSchema.getTableInfoAnalytes(), filter, sort).getArray(Analyte.class);
    }

    public List<Analyte> getAnalytes(ExpRun run)
    {
        List<Analyte> analytes = new ArrayList<>();

        run.getDataOutputs().forEach(o->
            analytes.addAll(
                new TableSelector(LuminexProtocolSchema.getTableInfoAnalytes(), new SimpleFilter(
                        FieldKey.fromParts("DataId"), o.getRowId()), new Sort("RowId")
                ).getCollection(Analyte.class)
            ));

        return analytes;
    }

    public long getExclusionCount(int rowId)
    {
        long exclusionCount = new TableSelector(LuminexProtocolSchema.getTableInfoRunExclusion(), new SimpleFilter(FieldKey.fromParts("RunId"), rowId), null).getRowCount();
        exclusionCount += new TableSelector(LuminexProtocolSchema.getTableInfoWellExclusion(), new SimpleFilter(FieldKey.fromParts("DataId", "RunId"), rowId), null).getRowCount();
        return exclusionCount;
    }

    private Map<Integer, ExpData> getReplacementInputMap(ExpProtocol protocol, ExpRun replacedRun, ExpRun run)
    {
        //Map old data inputs to new based on dataFileHeaderKey
        //NOTE: exclusions in renamed/new files will be dropped
        Map<String, Integer> oldInputs = new HashMap<>();
        Map<Integer, ExpData> inputIdMap = new HashMap<>(); //key: oldId
        replacedRun.getDataOutputs().forEach(o ->  {
            oldInputs.put(getDataFileHeaderKey(protocol, o), o.getRowId());
        });
        run.getDataOutputs().stream()
                .filter(newFile -> oldInputs.containsKey(getDataFileHeaderKey(protocol, newFile)))
                .forEach( newFile -> {
                    Integer oldId = oldInputs.get(getDataFileHeaderKey(protocol, newFile));
                    inputIdMap.put(oldId, newFile);
                });

        return inputIdMap;
    }

    private LuminexSingleExclusionCommand generateExclusionCommands(Map<String, Object> oldExclusion, Integer dataFileId)
    {
        LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
        command.setCommand("insert");
        command.setDataId(dataFileId);
        command.setDescription((String) oldExclusion.get("Description"));
        command.setDilution(oldExclusion.get("dilution") != null ? Double.parseDouble(oldExclusion.get("dilution").toString()) : null);
        command.setType((String) oldExclusion.get("type"));
        command.setComment((String) oldExclusion.get("comment"));
        command.setCreated((Timestamp) oldExclusion.get("created"));
        command.setCreatedBy((Integer) oldExclusion.get("createdBy"));
        if (oldExclusion.get("well") != null)
            command.setWell((String)oldExclusion.get("well"));
        return command;
    }

    private Collection<Map<String, Object>> getWellExclusions(Set<Integer> dataIds)
    {
        if(dataIds == null || dataIds.size() == 0)
            return null;

        //Get full list of exclusions expanded per analyte
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT a.Name AS AnalyteName\n")
            .append(", e.* \n")
            .append("FROM ").append(LuminexProtocolSchema.getTableInfoWellExclusion(), "e").append(",")
            .append(LuminexProtocolSchema.getTableInfoWellExclusionAnalyte(), "ea").append(",")
            .append(ExperimentService.get().getTinfoData(), "d").append(",")
            .append(LuminexProtocolSchema.getTableInfoAnalytes(), "a")
            .append(" WHERE e.RowId = ea.WellExclusionId ")
            .append(" AND ea.AnalyteId = a.RowId ")
            .append(" AND d.RowId = e.DataId")
            .append(" AND a.DataId ");

        OntologyManager.getTinfoObject().getSqlDialect().appendInClauseSql(sql, dataIds);

        return new SqlSelector(getSchema(), sql).getMapCollection();
    }

    private LuminexSingleExclusionCommand generateRunExclusionCommands(Map<Integer, ExpData> inputIdMap, Map<String, Analyte> analyteMap, int replacedRunId)
    {
        Collection<Map<String, Object>> exclusions = getRunExclusions(inputIdMap.keySet(), replacedRunId);

        if (exclusions.size() <= 0)
            return null;

        LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
        command.setCommand("insert");

        //Map existing exclusions to new input files
        exclusions.forEach(exclusion ->
        {
            //Should be the same for all exclusions
            command.setComment((String) exclusion.get("comment"));
            command.setCreated((Timestamp) exclusion.get("created"));
            command.setCreatedBy((Integer) exclusion.get("createdBy"));


            Analyte analyte = analyteMap.get(exclusion.get("AnalyteName"));
            if (analyte != null)
            {
                //generate insertion command
                command.addAnalyte(analyte);
            }
        });

        return !command.getAnalyteRowIds().isEmpty() ? command : null;
    }

    private Collection<Map<String,Object>> getRunExclusions(Set<Integer> integers, int replacedRunId)
    {
        //Get full list of exclusions by analyte
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT a.Name AS AnalyteName, e.* FROM ")
            .append(LuminexProtocolSchema.getTableInfoRunExclusion(), "e").append(",")
            .append(LuminexProtocolSchema.getTableInfoRunExclusionAnalyte(), "ea").append(",")
            .append(LuminexProtocolSchema.getTableInfoAnalytes(), "a")
            .append(" WHERE e.RunId = ea.RunId ")
            .append(" AND ea.AnalyteId = a.RowId")
            .append(" AND e.RunId = ?")
            .add(replacedRunId);

        return new SqlSelector(getSchema(), sql).getMapCollection();
    }

    private Collection<LuminexSingleExclusionCommand> generateWellExclusionCommands(Integer newRunId, LuminexRunContext context, Map<Integer, ExpData> inputIdMap, Map<String, Analyte> analyteMap)
    {
        Collection<Map<String, Object>> exclusions = getWellExclusions(inputIdMap.keySet());
        if(exclusions == null)
            return null;

        Set<String> wellKeys = getWellKeysForRun(newRunId, context.getProtocol(), context.getContainer(), context.getUser(), false);
        Map<String, LuminexSingleExclusionCommand> replacementCommands = new HashMap<>();
        //Map existing exclusions to new input files
        exclusions.forEach(exclusion ->
        {
            Integer dataId = (Integer)exclusion.get("DataId");
            String analyteName = (String)exclusion.get("AnalyteName");
            ExpData file = inputIdMap.get(dataId);
            Analyte newAnalyte = analyteMap.get(analyteName);

            //if file does not have a corresponding Analyte
            if (newAnalyte == null)
                return;

            String dataFileHeaderKey = getDataFileHeaderKey(context.getProtocol(), inputIdMap.get(dataId));
            String description = (String)exclusion.get("Description");  // == null for Blank control wells
            String type = (String)exclusion.get("Type"); // == null for singlepoint unknown and titration exclusions
            Double dilution = exclusion.get("Dilution") != null ? Double.parseDouble(exclusion.get("Dilution").toString()) : null; // == null for well replicate group and titration exclusions

            //Get existing command
            String key = createExclusionCommandKey(dataFileHeaderKey, description, type, dilution);
            LuminexSingleExclusionCommand command = replacementCommands.get(key);

            if (command == null)
            {
                boolean isTitrationTypeExclusion = dilution == null && type == null;
                boolean isSinglepointUnknownExclusion = dilution != null && type == null;
                boolean isWellReplicateGroupTypeExclusion = dilution == null && type != null;
                boolean hasWellKeyMatch = false;

                if (isTitrationTypeExclusion)
                    hasWellKeyMatch = wellKeys.stream().anyMatch(k -> k.startsWith(getTitrationKey(dataFileHeaderKey, analyteName, description)));
                else if (isSinglepointUnknownExclusion)
                    hasWellKeyMatch = wellKeys.stream().anyMatch(k -> k.startsWith(getTitrationKey(dataFileHeaderKey, analyteName, description)) && k.endsWith("|" + dilution + "|null"));
                else if (isWellReplicateGroupTypeExclusion)
                    hasWellKeyMatch = wellKeys.stream().anyMatch(k -> k.startsWith(getReplicateGroupKey(dataFileHeaderKey, analyteName, description, type)));

                if (hasWellKeyMatch)
                    command = generateExclusionCommands(exclusion, file.getRowId());
            }

            //Add analyte if at least one matching Well was found
            if (command != null)
            {
                command.addAnalyte(newAnalyte);
                replacementCommands.put(key, command);
            }
        });

        return replacementCommands.values();
    }

    public Long getRetainedRunExclusionCount(Integer replacedRunId, Set<String> analyteNames)
    {
        SQLFragment sql = new SQLFragment();
        sql.append( "SELECT COUNT(DISTINCT re.RunId) FROM ")
                .append(LuminexProtocolSchema.getTableInfoRunExclusion(), "re").append(",")
                .append(LuminexProtocolSchema.getTableInfoRunExclusionAnalyte(), "rea").append(",")
                .append(LuminexProtocolSchema.getTableInfoAnalytes(), "a")
                .append(" WHERE re.RunId = rea.RunId ")
                .append(" AND rea.AnalyteId = a.RowId ")
                .append(" AND re.RunId = ? ").add(replacedRunId);

        //Add analyte filter
        appendInClause(sql, "a.Name ", analyteNames, "\n");

        return new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getObject(Long.class);
    }

    public Collection<WellExclusion> getRetainedWellExclusions(Integer replacedRunId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append( "SELECT opvRSN.StringValue AS ReaderSerialNumber, opvAD.DateTimeValue AS AcquisitionDate, ")
            .append("a.Name AS Analyte, we.Description, we.Type, we.Dilution\n")
            .append("FROM ").append(LuminexProtocolSchema.getTableInfoWellExclusion(), "we").append(",")
            .append(LuminexProtocolSchema.getTableInfoWellExclusionAnalyte(), "wea").append(",")
            .append(LuminexProtocolSchema.getTableInfoAnalytes(), "a").append(",")
            .append(OntologyManager.getTinfoObjectPropertiesView(), "opvRSN").append(",")
            .append(OntologyManager.getTinfoObjectPropertiesView(), "opvAD").append(",")
            .append(ExperimentService.get().getTinfoData(), "d")
            .append(" WHERE we.RowId = wea.WellExclusionId")
            .append(" AND wea.AnalyteId = a.RowId")
            .append(" AND we.DataId = d.RowId")
            .append(" AND opvRSN.name = 'ReaderSerialNumber' AND opvRSN.Container = d.Container AND opvRSN.ObjectURI = d.LSID")
            .append(" AND opvAD.name = 'AcquisitionDate' AND opvAD.Container = d.Container AND opvAD.ObjectURI = d.LSID")
            .append(" AND d.RunId = ?").add(replacedRunId);

        return new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getCollection(WellExclusion.class);
    }

    private SQLFragment appendInClause(SQLFragment sql, String columnExpression, Set set, String closeOutString)
    {
        //Add dataFileHeaderKey filter
        if (set != null && set.size() > 0)
        {
            sql.append(" AND ").append(columnExpression);
            OntologyManager.getTinfoObject().getSqlDialect().appendInClauseSql(sql, set);
            sql.append(closeOutString);
        }

        return sql;
    }

    public void retainExclusions(LuminexRunContext uploadContext, ExpRun replacedRun, ExpRun run) throws ValidationException
    {
        //Map existing Files & analytes to new run
        Map<Integer, ExpData> inputIdMap = getReplacementInputMap(uploadContext.getProtocol(), replacedRun, run);
        Map<String, Analyte> analyteMap = getAnalyteMap(replacedRun, run);

        Collection<LuminexSingleExclusionCommand> wellExclusionCommands = generateWellExclusionCommands(run.getRowId(), uploadContext, inputIdMap, analyteMap);
        LuminexSingleExclusionCommand runExclusionCommands = generateRunExclusionCommands(inputIdMap, analyteMap, replacedRun.getRowId());

        try
        {
            //Copy WellExclusions, SinglepointUnknownExclusions, and TitrationExclusions
            if (wellExclusionCommands != null)
            {
                createExclusions(uploadContext.getUser(), uploadContext.getContainer(), wellExclusionCommands, run.getRowId(),
                        null, run.getProtocol(), uploadContext.getProvider(), false, null);
            }

            //Copy AnalyteExclusions
            if (runExclusionCommands != null)
            {
                createExclusions(uploadContext.getUser(), uploadContext.getContainer(), Collections.singletonList(runExclusionCommands),
                        run.getRowId(), ExclusionType.RunExclusion, run.getProtocol(), uploadContext.getProvider(), false, null);
            }
        }
        catch (SQLException|QueryUpdateServiceException|DuplicateKeyException|InvalidKeyException e)
        {
            ValidationException ex = new ValidationException("DB Error:" + e.getMessage());
            ex.addSuppressed(e);
            throw ex;
        }
        catch (BatchValidationException e)
        {
            ValidationException ex = new ValidationException("Failed to re-create exclusions: " + e.getMessage());
            ex.addSuppressed(e);
            throw ex;
        }

    }

    private Map<String, Analyte> getAnalyteMap(ExpRun replacedRun, ExpRun run)
    {
        List<Analyte> newAnalytes = getAnalytes(run);
        Map<String, Analyte> results = new HashMap<>();
        newAnalytes.forEach(analyte -> results.put(analyte.getName(), analyte));

        return results;
    }

    public void createExclusions(User user, Container c, Collection<LuminexSingleExclusionCommand> commands, Integer runId,
                                 @Nullable ExclusionType baseExclusionType, ExpProtocol protocol, AssayProvider assayProvider,
                                 boolean rerunTransform, Logger logger)
    throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, InvalidKeyException
    {
        if (logger == null)
            logger = _log;

        LuminexProtocolSchema schema = new LuminexProtocolSchema(user, c, (LuminexAssayProvider)assayProvider, protocol, null);

        // if the baseExclusionType is null, default to the WellExclusion table
        TableInfo tableInfo;
        if (baseExclusionType != null)
            tableInfo = schema.getTable(baseExclusionType.getTableName());
        else
            tableInfo = schema.getTable(ExclusionType.WellExclusion.getTableName());

        if (tableInfo != null)
        {
            QueryUpdateService qus = tableInfo.getUpdateService();
            if (qus != null)
            {
                Map<Enum, Object> options = new HashMap<>();
                options.put(QueryUpdateService.ConfigParameters.Logger, logger);

                Map<String, Object> additionalContext = Collections.singletonMap(RERUN_TRANSFORM, rerunTransform);

                for (LuminexSingleExclusionCommand command : commands)
                {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    List<Map<String, Object>> keys = new ArrayList<>();
                    BatchValidationException errors = new BatchValidationException();
                    List<Map<String, Object>> results;

                    // if the baseExclusionType is null, determine the type based on the command details
                    ExclusionType exclusionType = baseExclusionType;
                    if (exclusionType == null)
                        exclusionType = command.getExclusionType();

                    logger.info("Starting " + command.getCommand() + " " +  exclusionType.getDescription(command).toLowerCase());

                    rows.add(exclusionType.getRowMap(command, runId, false));
                    keys.add(exclusionType.getRowMap(command, runId, true));

                    String logVerb;
                    switch (command.getCommand())
                    {
                        case "insert":
                            results = qus.insertRows(user, c, rows, errors, options, additionalContext);
                            logVerb = " inserted into ";
                            break;
                        case "update":
                            results = qus.updateRows(user, c, rows, keys, options, additionalContext);
                            logVerb = " updated in ";
                            break;
                        case "delete":
                            results = qus.deleteRows(user, c, keys, options, additionalContext);
                            logVerb = " deleted from ";
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid command type: " + command.getCommand());
                    }

                    logger.info(StringUtilsLabKey.pluralize(results.size(), "record") + logVerb + tableInfo.getName());

                    if (errors.hasErrors())
                    {
                        throw errors;
                    }

                    logger.info("Finished " + command.getCommand() + " " +  exclusionType.getDescription(command).toLowerCase());
                }
            }
        }
    }

    public Integer getRunRowIdForUploadContext(ExpRun run, AssayRunUploadContext context)
    {
        //If not a rerun just use current run's id
        if (context.getReRunId() == null)
            return run.getRowId();

        return (context instanceof LuminexRunContext && ((LuminexRunContext) context).getRetainExclusions()) ?
            context.getReRunId() : //If we are retaining exclusions from previous run use the reRunId
            run.getRowId(); //Else use current runId
    }

    public Set<String> getWellExclusionKeysForRun(Integer runId, ExpProtocol protocol, Container container, User user)
    {
        return getWellKeysForRun(runId, protocol, container, user, true);
    }

    private Set<String> getWellKeysForRun(Integer runId, ExpProtocol protocol, Container container, User user, boolean onlyExcludedWells)
    {
        Set<String> excludedWellKeys = new HashSet<>();

        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (!(provider instanceof LuminexAssayProvider))
            throw new NotFoundException("Luminex assay provider not found");

        LuminexProtocolSchema schema = new LuminexProtocolSchema(user, container, (LuminexAssayProvider)provider, protocol, null);
        LuminexDataTable table = new LuminexDataTable(schema);

        // data file, analyte, description, dilution, and type are needed to match an existing exclusion to data from an Excel file row
        FieldKey readerSerialNumberFK = FieldKey.fromParts("Data", "ReaderSerialNumber");
        FieldKey acquisitionDateFK = FieldKey.fromParts("Data", "AcquisitionDate");
        FieldKey analyteFK = FieldKey.fromParts("Analyte", "Name");
        FieldKey descriptionFK = FieldKey.fromParts("Description");
        FieldKey typeFK = FieldKey.fromParts("Type");
        FieldKey dilutionFK = FieldKey.fromParts("Dilution");
        FieldKey wellFK = FieldKey.fromParts("Well");
        Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(table, Arrays.asList(readerSerialNumberFK, acquisitionDateFK, analyteFK, descriptionFK, dilutionFK, typeFK, wellFK));

        SimpleFilter filter = new SimpleFilter(provider.getTableMetadata(protocol).getRunFieldKeyFromResults(), runId);
        if(onlyExcludedWells)
        {
            // filter should work for either entire replicate group or single well
            filter.addCondition(FieldKey.fromParts(LuminexDataTable.EXCLUSION_COMMENT_COLUMN_NAME), LuminexDataTable.EXCLUSION_WELL_COMMENT, CompareType.STARTS_WITH);
        }

        new TableSelector(table, cols.values(), filter, null).forEachMap(row ->
        {
            String readerSerialNumber = (String)row.get(cols.get(readerSerialNumberFK).getAlias());
            Date acquisitionDate = (Date)row.get(cols.get(acquisitionDateFK).getAlias());
            String dataFileHeaderKey = getDataFileHeaderKey(readerSerialNumber, acquisitionDate);

            String analyteName = (String)row.get(cols.get(analyteFK).getAlias());
            String description = (String)row.get(cols.get(descriptionFK).getAlias());
            String type = (String)row.get(cols.get(typeFK).getAlias());
            Object dilutionObj = row.get(cols.get(dilutionFK).getAlias());
            Double dilution = dilutionObj != null ? Double.parseDouble(dilutionObj.toString()) : null;
            String well = onlyExcludedWells ? (String)row.get(cols.get(wellFK).getAlias()) : null;

            excludedWellKeys.add(createWellKey(dataFileHeaderKey, analyteName, description, type, dilution, well));
        });

        return excludedWellKeys;
    }

    /** Create a simple object to use as a key, combining the three properties */
    private String createExclusionCommandKey(String dataFileHeaderKey, String description, String type, Double dilution)
    {
        return dataFileHeaderKey  + "|" + description + "|" + type + "|" + dilution;
    }

    /** Refine the key object with the individual analyte */
    public String createWellKey(String dataFileHeaderKey, String analyteName, String description, String type, Double dilution, String well)
    {
        return getReplicateGroupKey(dataFileHeaderKey, analyteName, description, type) + dilution + "|" + well;
    }

    private String getReplicateGroupKey(String dataFileHeaderKey, String analyteName, String description, String type)
    {
        return getTitrationKey(dataFileHeaderKey, analyteName, description) + type + "|";
    }

    private String getTitrationKey(String dataFileHeaderKey, String analyteName, String description)
    {
        return dataFileHeaderKey + "|" + analyteName + "|" + description + "|";
    }

    private String getDataFileHeaderKey(ExpProtocol protocol, ExpData o)
    {
        Domain excelRunDomain =  LuminexAssayProvider.getExcelRunDomain(protocol);
        String readerSerialNumber = (String) o.getProperty(excelRunDomain.getPropertyByName("ReaderSerialNumber"));
        Date acquisitionDate = (Date) o.getProperty(excelRunDomain.getPropertyByName("AcquisitionDate"));
        return getDataFileHeaderKey(readerSerialNumber, acquisitionDate);
    }

    public String getDataFileHeaderKey(ExpProtocol protocol, File dataFile) throws ExperimentException
    {
        LuminexExcelParser parser = new LuminexExcelParser(protocol, Collections.singleton(dataFile));
        Domain excelRunDomain = LuminexAssayProvider.getExcelRunDomain(protocol);
        Map<DomainProperty, String> excelRunProps = parser.getExcelRunProps(dataFile);
        String readerSerialNumber = excelRunProps.get(excelRunDomain.getPropertyByName("ReaderSerialNumber"));
        String acquisitionDate = excelRunProps.get(excelRunDomain.getPropertyByName("AcquisitionDate"));
        return getDataFileHeaderKey(readerSerialNumber, acquisitionDate != null ? new Date(acquisitionDate) : null);
    }

    public String getDataFileHeaderKey(String readerSerialNumber, Date acquisitionDate)
    {
        return readerSerialNumber + "|" + (acquisitionDate != null ? acquisitionDate.getTime() : null);
    }
}
