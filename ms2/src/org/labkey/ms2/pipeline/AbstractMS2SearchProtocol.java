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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskFactory;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.util.FileType;
import org.labkey.api.util.massSpecDataFileType;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>AbstractMS2SearchProtocol</code>
 */
abstract public class AbstractMS2SearchProtocol<JOB extends AbstractMS2SearchPipelineJob> extends AbstractFileAnalysisProtocol<JOB>
{
    public static final FileType FT_MZXML = new massSpecDataFileType();
    public static final FileType FT_SEARCH_XAR = new FileType(".search.xar.xml");

    private File _dirSeqRoot;
    private String _dbPath;
    private String[] _dbNames;

    public AbstractMS2SearchProtocol(String name, String description, String xml)
    {
        super(name, description, xml);
    }

    @Override
    public String getJoinedBaseName()
    {
        return LEGACY_JOINED_BASENAME;
    }

    public File getDirSeqRoot()
    {
        return _dirSeqRoot;
    }

    public void setDirSeqRoot(File dirSeqRoot)
    {
        _dirSeqRoot = dirSeqRoot;
    }

    public String getDbPath()
    {
        return _dbPath;
    }

    public void setDbPath(String dbPath)
    {
        _dbPath = dbPath;
    }

    public String[] getDbNames()
    {
        return _dbNames;
    }

    public void setDbNames(String[] dbNames)
    {
        _dbNames = dbNames;
    }

    public abstract JOB createPipelineJob(ViewBackgroundInfo info,
                                          PipeRoot root,
                                          List<File> filesInput,
                                          File fileParameters,
                                          @Nullable Map<String, String> variableMap) throws IOException;

    @Override
    protected void save(File file, Map<String, String> addParams, Map<String, String> instanceParams) throws IOException
    {
        if (addParams == null)
            addParams = new HashMap<>();

        StringBuffer dbs = new StringBuffer();
        for (String dbName : _dbNames)
        {
            if (dbs.length() > 0)
                dbs.append(';');
            dbs.append(dbName);
        }
        addParams.put("pipeline, database", dbs.toString());

        super.save(file, addParams, instanceParams);        
    }

    public List<FileType> getInputTypes()
    {
        TaskFactory taskFactory = PipelineJobService.get().getTaskFactory(MS2PipelineManager.MZXML_CONVERTER_TASK_ID);
        if (taskFactory != null)
        {
            return taskFactory.getInputTypes();
        }
        return Collections.singletonList(FT_MZXML);
    }

    public void validate(PipeRoot root) throws PipelineValidationException
    {
        super.validate(root);

        if (_dbNames.length == 0 || _dbNames[0] == null || _dbNames[0].length() == 0)
            throw new PipelineValidationException("Select a sequence database.");
    }
}
