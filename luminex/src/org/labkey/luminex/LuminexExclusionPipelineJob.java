/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cnathe on 10/9/14.
 */
public class LuminexExclusionPipelineJob extends PipelineJob
{
    private LuminexSaveExclusionsForm _form;
    private ExclusionType _exclusionType;
    private Integer _runId;

    private transient AssayProvider _provider;
    private transient ExpRun _run;

    public LuminexExclusionPipelineJob(ViewBackgroundInfo info, PipeRoot root, LuminexSaveExclusionsForm form)
    {
        super(LuminexAssayProvider.NAME, info, root);

        File logFile = new File(root.getRootPath(), FileUtil.makeFileNameWithTimestamp("luminex_exclusion", "log"));
        setLogFile(logFile);

        _form = form;
        _exclusionType = LuminexExclusionPipelineJob.ExclusionType.valueOf(form.getTableName());
        _runId = form.getRunId();
    }

    @Override
    public URLHelper getStatusHref()
    {
        if (getRun() != null)
        {
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(getRun().getContainer(), getRun().getProtocol(), getRun().getRowId());
        }
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Luminex Exclusion" + (getRun() != null ? ": " + getRun().getName() : "");
    }

    @Override
    public void run()
    {
        try
        {
            setStatus(TaskStatus.running, getJobInfo());

            LuminexProtocolSchema schema = new LuminexProtocolSchema(getUser(), getContainer(),
                    (LuminexAssayProvider)getAssayProvider(), _form.getProtocol(getContainer()), null);

            TableInfo tableInfo = schema.getTable(_exclusionType.getTableName());
            if (tableInfo != null)
            {
                QueryUpdateService qus = tableInfo.getUpdateService();
                if (qus != null)
                {
                    for (LuminexSingleExclusionCommand command : _form.getCommands())
                    {
                        List<Map<String, Object>> rows = new ArrayList<>();
                        List<Map<String, Object>> keys = new ArrayList<>();
                        BatchValidationException errors = new BatchValidationException();
                        List<Map<String, Object>> results;

                        logProperties(command);
                        getLogger().info("Starting " + command.getCommand() + " " +  _exclusionType.getDescription().toLowerCase());

                        rows.add(_exclusionType.getRowMap(command, _form.getRunId(), false));
                        keys.add(_exclusionType.getRowMap(command, _form.getRunId(), true));

                        switch (command.getCommand())
                        {
                            case "insert":
                                results = qus.insertRows(getUser(), getContainer(), rows, errors, null);
                                getLogger().info(StringUtilsLabKey.pluralize(results.size(), "record") + " inserted into " + tableInfo.getName());
                                break;
                            case "update":
                                results = qus.updateRows(getUser(), getContainer(), rows, keys, null);
                                getLogger().info(StringUtilsLabKey.pluralize(results.size(), "record") + " updated in " + tableInfo.getName());
                                break;
                            case "delete":
                                results = qus.deleteRows(getUser(), getContainer(), keys, null);
                                getLogger().info(StringUtilsLabKey.pluralize(results.size(), "record") + " deleted from " + tableInfo.getName());
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid command type: " + command.getCommand());
                        }

                        if (errors.hasErrors())
                        {
                            throw errors;
                        }

                        getLogger().info("Finished " + command.getCommand() + " " +  _exclusionType.getDescription().toLowerCase());
                    }
                }
            }

            setStatus(TaskStatus.complete, getJobInfo());
        }
        catch (Exception e)
        {
            getLogger().error("The following error was generated by the exclusion:\n");
            getLogger().error(e.getMessage() + "\n");
            setStatus(TaskStatus.error, e.getMessage());
            getLogger().info("Error StackTrace", e);
        }
    }

    private void logProperties(LuminexSingleExclusionCommand exclusion)
    {
        getLogger().info("----- Exclusion Properties ---------");
        if (getRun() != null)
            getLogger().info("Assay Id: " + getRun().getName());
        getLogger().info("Run Id: " + _form.getRunId());
        if (exclusion.getDataId() != null)
            getLogger().info("Data Id: " + exclusion.getDataId());
        getLogger().info("Description: " + propertyNullToBlank(exclusion.getDescription()));
        if (exclusion.getType() != null)
            getLogger().info("Type: " + exclusion.getType());
        getLogger().info("Analytes: " + propertyNullToBlank(exclusion.getAnalyteNames()));
        getLogger().info("Comment: " + propertyNullToBlank(exclusion.getComment()));
        getLogger().info("----- End Exclusion Properties -----");
    }

    private String propertyNullToBlank(String value)
    {
        return value == null ? "[Blank]" : value;
    }

    private String getJobInfo()
    {
        // if we only have one command, use that in the info text
        LuminexSingleExclusionCommand command = null;
        if (_form.getCommands().size() == 1)
        {
            command = _form.getCommands().get(0);
        }

        return _exclusionType.getInfo(command);
    }

    private AssayProvider getAssayProvider()
    {
        if (_provider == null)
        {
            _provider = AssayService.get().getProvider(_form.getProtocol(getContainer()));
        }
        return _provider;
    }

    private ExpRun getRun()
    {
        if (_run == null)
        {
            _run = ExperimentService.get().getExpRun(_runId);
        }
        return _run;
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

        private ExclusionType(String description)
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
}
