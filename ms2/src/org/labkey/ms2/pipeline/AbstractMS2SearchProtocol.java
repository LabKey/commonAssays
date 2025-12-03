/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import io.micrometer.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.ParamParser;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskFactory;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.util.FileType;
import org.labkey.api.util.massSpecDataFileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.vfs.FileLike;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory.SEQUENCE_FILE_SEPARATOR;

/**
 * <code>AbstractMS2SearchProtocol</code>
 */
abstract public class AbstractMS2SearchProtocol<JOB extends AbstractMS2SearchPipelineJob> extends AbstractFileAnalysisProtocol<JOB>
{
    public static final FileType FT_MZXML = new massSpecDataFileType();
    public static final FileType FT_SEARCH_XAR = new FileType(".search.xar.xml");
    public static final String PIPELINE_DATABASE = "pipeline, database";
    protected final Container _container;

    public AbstractMS2SearchProtocol(String name, String description, String xml, Container container)
    {
        super(name, description, xml);
        _container = container;
    }

    @Override
    public String getJoinedBaseName()
    {
        return LEGACY_JOINED_BASENAME;
    }

    public FileLike getDirSeqRoot()
    {
        return MS2PipelineManager.getSequenceDatabaseRoot(_container, true);
    }

    @Override
    public abstract JOB createPipelineJob(ViewBackgroundInfo info,
                                          PipeRoot root,
                                          List<FileLike> filesInput,
                                          FileLike fileParameters,
                                          @Nullable Map<String, String> variableMap) throws IOException;

    @Override
    protected void save(FileLike path, Map<String, String> addParams, Map<String, String> instanceParams) throws IOException
    {
        if (addParams == null)
            addParams = new HashMap<>();

        super.save(path, addParams, instanceParams);
    }

    @Override
    public List<FileType> getInputTypes()
    {
        TaskFactory<?> taskFactory = PipelineJobService.get().getTaskFactory(MS2PipelineManager.MZXML_CONVERTER_TASK_ID);
        if (taskFactory != null)
        {
            return taskFactory.getInputTypes();
        }
        return Collections.singletonList(FT_MZXML);
    }

    @Override
    public void validate(PipeRoot root) throws PipelineValidationException
    {
        super.validate(root);

        if (getDbNames().isEmpty())
        {
            throw new PipelineValidationException("Select a sequence database.");
        }
    }

    @NotNull
    protected List<String> getDbNames()
    {
        if (xml == null)
        {
            return Collections.emptyList();
        }
        ParamParser parser = parse();
        String names = parser.getInputParameter(PIPELINE_DATABASE);
        if (StringUtils.isEmpty(names))
        {
            return Collections.emptyList();
        }
        return Arrays.asList(names.split(SEQUENCE_FILE_SEPARATOR));
    }
}
