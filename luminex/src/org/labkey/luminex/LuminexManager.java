package org.labkey.luminex;

import org.apache.commons.collections4.map.HashedMap;
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
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
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
import org.labkey.luminex.query.LuminexDataTable;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
            public String getInfo(@Nullable LuminexSingleExclusionCommand command)
            {
                String info = getDescription().toLowerCase();
                if (command != null)
                {
                    info = command.getCommand().toUpperCase() + " " + info + " ("
                            + (command.getDescription() == null ? "" : "Description: " + command.getDescription() + ", ")
                            + "Type: " + command.getType() + ")";
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
                    row.put("Type", form.getType());
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
            public String getInfo(@Nullable LuminexSingleExclusionCommand command)
            {
                // for titration exclusions, null command means that we have > 1 command in this job
                String info = "MULTIPLE " + getDescription().toLowerCase() + "s";
                if (command != null)
                {
                    info = command.getCommand().toUpperCase() + " " + getDescription().toLowerCase()
                            + " (" + command.getDescription() + ")";
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
            public String getInfo(@Nullable LuminexSingleExclusionCommand command)
            {
                String info = getDescription().toLowerCase();
                if (command != null)
                {
                    info = command.getCommand().toUpperCase() + " " + info;
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

        public String getDescription()
        {
            return _description;
        }

        public abstract String getTableName();
        public abstract String getInfo(@Nullable LuminexSingleExclusionCommand command);
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

    private Map<Integer, ExpData> getReplacementInputMap(ExpRun replacedRun, ExpRun run)
    {
        //Map old data inputs to new based on filename
        //NOTE: exclusions in renamed/new files will be dropped
        Map<String, Integer> oldInputs = new HashMap<>();
        Map<Integer, ExpData> inputIdMap = new HashMap<>(); //key: oldId
        replacedRun.getDataOutputs().forEach(o ->  {
            oldInputs.put(o.getName(), o.getRowId());
            oldInputs.put(o.getFile().getName(), o.getRowId());
        });
        run.getDataOutputs().stream()
                .filter(o -> oldInputs.containsKey(o.getFile().getName()))
                .forEach( newFile ->{
                    Integer oldId = oldInputs.get(newFile.getName());
                    inputIdMap.put(oldId, newFile);
                });

        return inputIdMap;
    }

    private LuminexSingleExclusionCommand generateExclusionCommands(Map<String, Object> oldExclusion, Integer dataFileId)
    {
        LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
        command.setCommand("insert");
        command.setDescription((String) oldExclusion.get("Description"));
        command.setType((String) oldExclusion.get("type"));
        command.setComment((String) oldExclusion.get("comment"));
        command.setDataId(dataFileId);
        command.setCreated((Timestamp) oldExclusion.get("created"));
        command.setCreatedBy((Integer) oldExclusion.get("createdBy"));

        return command;
    }

    private Collection<Map<String, Object>> getWellExclusions(Set<Integer> dataIds)
    {
        if(dataIds == null || dataIds.size() == 0)
            return null;

        //Get full list of exclusions expanded per analyte
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT ea.AnalyteId, e.* FROM ")
            .append(LuminexProtocolSchema.getTableInfoWellExclusion(), "e").append(",")
            .append(LuminexProtocolSchema.getTableInfoWellExclusionAnalyte(), "ea").append(",")
            .append(LuminexProtocolSchema.getTableInfoAnalytes(), "a")
            .append(" WHERE e.RowId = ea.WellExclusionId ")
            .append(" AND ea.AnalyteId = a.RowId ")
            .append(" AND a.DataId ");

        OntologyManager.getTinfoObject().getSqlDialect().appendInClauseSql(sql, dataIds);

        return new SqlSelector(getSchema(), sql).getMapCollection();
    }

    private LuminexSingleExclusionCommand generateRunExclusionCommands(Map<Integer, ExpData> inputIdMap, Map<Integer, Analyte> analyteMap, int replacedRunId)
    {
        Collection<Map<String, Object>> exclusions = getRunExclusions(inputIdMap.keySet(), replacedRunId);

        if (exclusions == null || exclusions.size() <= 0)
            return null;

        LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
        command.setCommand("insert");

        //Map existing exclusions to new input files
        exclusions.forEach(exclusion ->
        {
            command.setComment((String) exclusion.get("comment")); //Should be the same

            Analyte analyte = analyteMap.get(exclusion.get("AnalyteId"));
            if (analyte != null)
            {
                //generate insertion command
                command.addAnalyte(analyte);
            }
        });

        return command;

    }

    private Collection<Map<String,Object>> getRunExclusions(Set<Integer> integers, int replacedRunId)
    {
        //Get full list of exclusions by analyte
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT ea.AnalyteId, e.* FROM ")
            .append(LuminexProtocolSchema.getTableInfoRunExclusion(), "e").append(",")
            .append(LuminexProtocolSchema.getTableInfoRunExclusionAnalyte(), "ea")
            .append(" WHERE e.RunId = ea.RunId")
            .append(" AND e.RunId = ?")
            .add(replacedRunId);

        return new SqlSelector(getSchema(), sql).getMapCollection();
    }

    private Collection<LuminexSingleExclusionCommand> generateWellExclusionCommands(Map<Integer, ExpData> inputIdMap, Map<Integer, Analyte> analyteMap)
    {
        Collection<Map<String, Object>> exclusions = getWellExclusions(inputIdMap.keySet());
        if(exclusions == null)
            return null;

        Map<String, LuminexSingleExclusionCommand> replacementCommands = new HashMap<>();
        //Map existing exclusions to new input files
        exclusions.forEach(exclusion ->
        {
            Integer dataId = (Integer)exclusion.get("DataId");

            //If existing file has corresponding new file
            if (inputIdMap.containsKey(dataId))
            {
                ExpData file = inputIdMap.get(dataId);
                Analyte newAnalyte = analyteMap.get(Integer.valueOf(exclusion.get("AnalyteId").toString()));

                //if file does not have a corresponding Analyte
                if (newAnalyte == null)
                    return;

                //Get existing command
                String key = createExclusionCommandKey(file.getName(), (String) exclusion.get("Description"), (String) exclusion.get("type"));
                LuminexSingleExclusionCommand command = replacementCommands.get(key);
                if (command == null)
                {
                    //generate/update insertion command
                    command = generateExclusionCommands(exclusion, file.getRowId());
                    replacementCommands.put(key, command);
                }

                //Add analyte to command
                command.addAnalyte(newAnalyte);
            }
        });

        return replacementCommands.values();
    }

    public Long getRetainedExclusionCount(Integer replacedRunId, Set<String> analyteNames, Set<String> fileNames, Set<String> types, Set<String> titrations)
    {
        Long result = getRetainedWellExclusionCount(replacedRunId, analyteNames, fileNames, types, titrations);
        result += getRetainedRunExclusionCount(replacedRunId, analyteNames);
        return result;
    }

    private Long getRetainedRunExclusionCount(Integer replacedRunId, Set<String> analyteNames)
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

    private Long getRetainedWellExclusionCount(Integer replacedRunId, Set<String> analyteNames, Set<String> fileNames, Set<String> types, Set<String> titrations)
    {
        SQLFragment sql = new SQLFragment();
        sql.append( "SELECT COUNT(DISTINCT we.RowId) FROM ")
            .append(LuminexProtocolSchema.getTableInfoWellExclusion(), "we").append(",")
            .append(LuminexProtocolSchema.getTableInfoWellExclusionAnalyte(), "wea").append(",")
            .append(LuminexProtocolSchema.getTableInfoAnalytes(), "a").append(",")
            .append(ExperimentService.get().getTinfoData(), "d")
            .append(" WHERE we.RowId = wea.WellExclusionId AND ")
            .append("wea.AnalyteId = a.RowId AND ")
            .append("we.DataId = d.RowId AND ")
            .append("d.RunId = ?").add(replacedRunId);

        //Add filename filter
        appendInClause(sql, "d.Name ", fileNames, "\n");

        //Add analyte filter
        appendInClause(sql, "a.Name ", analyteNames, "\n");

        //Add titration filter
        appendInClause(sql, "(we.Description IS NULL OR we.Description ", titrations, "\n");

        //Add type filter: Null is for titrations
        appendInClause(sql, "(we.type IS NULL OR we.type ", types, ")\n");

        return new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getObject(Long.class);
    }

    private SQLFragment appendInClause(SQLFragment sql, String columnExpression, Set set, String closeOutString)
    {
        //Add filename filter
        if (set != null && set.size() > 0)
        {
            sql.append(" AND ").append(columnExpression);
            OntologyManager.getTinfoObject().getSqlDialect().appendInClauseSql(sql, set);
            sql.append(closeOutString);
        }

        return sql;
    }

    public void retainExclusions(User user, Container container, ExpRun replacedRun, ExpRun run, LuminexAssayProvider provider) throws ValidationException
    {
        //Map existing Files & analytes to new run
        Map<Integer, ExpData> inputIdMap = getReplacementInputMap(replacedRun, run);
        Map<Integer, Analyte> analyteMap = getAnalyteMap(replacedRun, run);

        Collection<LuminexSingleExclusionCommand> wellExclusionCommands = generateWellExclusionCommands(inputIdMap, analyteMap);
        LuminexSingleExclusionCommand runExclusionCommands = generateRunExclusionCommands(inputIdMap, analyteMap, replacedRun.getRowId());

        try
        {
            //Copy WellExclusions and TitrationExclusions
            if(wellExclusionCommands != null)
                createExclusions(user, container, wellExclusionCommands, run.getRowId(), ExclusionType.WellExclusion, run.getProtocol(), provider, false, null);

            //Copy AnalyteExclusions
            if (runExclusionCommands != null)
                createExclusions(user, container, Collections.singletonList(runExclusionCommands), run.getRowId(), ExclusionType.RunExclusion, run.getProtocol(), provider, false, null);
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

    private Map<Integer,Analyte> getAnalyteMap(ExpRun replacedRun, ExpRun run)
    {
        List<Analyte> replacedAnalytes = getAnalytes(replacedRun);
        Map<String, Integer> analyteNameMap = new HashedMap<>();
        replacedAnalytes.forEach(a -> analyteNameMap.put(a.getName(), a.getRowId()));
        List<Analyte> newAnalytes = getAnalytes(run);
        Map<Integer, Analyte> results = new HashMap<>();
        newAnalytes.forEach(analyte ->
        {
            if (analyteNameMap.containsKey(analyte.getName()))
            {
                results.put(analyteNameMap.get(analyte.getName()), analyte);
            }
        });

        return results;
    }

    public void createExclusions(User user, Container c, Collection<LuminexSingleExclusionCommand> commands, Integer runId,
                                 ExclusionType _exclusionType, ExpProtocol protocol, AssayProvider assayProvider,
                                 boolean rerunTransform, Logger logger)
    throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, InvalidKeyException
    {
        if (logger == null)
            logger = _log;

        LuminexProtocolSchema schema = new LuminexProtocolSchema(user, c, (LuminexAssayProvider)assayProvider, protocol, null);

        TableInfo tableInfo = schema.getTable(_exclusionType.getTableName());
        if (tableInfo != null)
        {
            QueryUpdateService qus = tableInfo.getUpdateService();
            if (qus != null)
            {
                Map<Enum, Object> options = new HashMap<>();
                options.put(QueryUpdateService.ConfigParameters.Logger, logger);

                //TODO: not sure if this is appropriate use...
                Map<String, Object> additionalContext = Collections.singletonMap(RERUN_TRANSFORM, rerunTransform);

                for (LuminexSingleExclusionCommand command : commands)
                {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    List<Map<String, Object>> keys = new ArrayList<>();
                    BatchValidationException errors = new BatchValidationException();
                    List<Map<String, Object>> results;

                    logger.info("Starting " + command.getCommand() + " " +  _exclusionType.getDescription().toLowerCase());

                    rows.add(_exclusionType.getRowMap(command, runId, false));
                    keys.add(_exclusionType.getRowMap(command, runId, true));

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

                    logger.info("Finished " + command.getCommand() + " " +  _exclusionType.getDescription().toLowerCase());
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
        Set<String> excludedWellKeys = new HashSet<>();

        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (!(provider instanceof LuminexAssayProvider))
            throw new NotFoundException("Luminex assay provider not found");

        LuminexProtocolSchema schema = new LuminexProtocolSchema(user, container, (LuminexAssayProvider)provider, protocol, null);
        LuminexDataTable table = new LuminexDataTable(schema);

        // data file, analyte, description, and type are needed to match an existing run exclusion to data from an Excel file row
        FieldKey dataFileUrlFK = FieldKey.fromParts("Data", "DataFileUrl");
        FieldKey analyteFK = FieldKey.fromParts("Analyte", "Name");
        FieldKey descriptionFK = FieldKey.fromParts("Description");
        FieldKey typeFK = FieldKey.fromParts("Type");
        Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(table, Arrays.asList(dataFileUrlFK, analyteFK, descriptionFK, typeFK));

        SimpleFilter filter = new SimpleFilter(provider.getTableMetadata(protocol).getRunFieldKeyFromResults(), runId);
        filter.addCondition(FieldKey.fromParts(LuminexDataTable.EXCLUSION_COMMENT_COLUMN_NAME), LuminexDataTable.EXCLUSION_WELL_GROUP_COMMENT, CompareType.STARTS_WITH);
        new TableSelector(table, cols.values(), filter, null).forEachMap(row ->
        {
            String dataFileUrl = (String)row.get(cols.get(dataFileUrlFK).getAlias());
            String dataFileName = dataFileUrl.substring(dataFileUrl.lastIndexOf("/") + 1);
            String analyteName = (String)row.get(cols.get(analyteFK).getAlias());
            String description = (String)row.get(cols.get(descriptionFK).getAlias());
            String type = (String)row.get(cols.get(typeFK).getAlias());
            excludedWellKeys.add(createWellExclusionKey(dataFileName, analyteName, description, type));
        });

        return excludedWellKeys;
    }

    /** Create a simple object to use as a key, combining the three properties */
    public String createExclusionCommandKey(String dataFileName, String description, String type)
    {
        return dataFileName  + "|" + description + "|" + type;
    }

    /** Refine the key object with the individual analyte */
    public String createWellExclusionKey(String dataFileName, String analyteName, String description, String type)
    {
        return createExclusionCommandKey(dataFileName, description, type) + "|" + analyteName;
    }
}
