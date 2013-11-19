/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
package org.labkey.ms2.pipeline.sequest;

import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * SequestPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class SequestPipelineJob extends AbstractMS2SearchPipelineJob
{
    private static final TaskId TASK_ID = new TaskId(SequestPipelineJob.class);

    public SequestPipelineJob(SequestSearchProtocol protocol,
                              ViewBackgroundInfo info,
                              PipeRoot root,
                              String name,
                              File dirSequenceRoot,
                              List<File> filesMzXML,
                              File fileInputXML
    ) throws IOException
    {
        super(protocol, SequestPipelineProvider.name, info, root, name, dirSequenceRoot, fileInputXML, filesMzXML);

        header("Sequest search for " + getBaseName());
        writeInputFilesToLog();
    }

    public SequestPipelineJob(SequestPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);
    }

    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new SequestPipelineJob(this, file);
    }

    public TaskId getTaskPipelineId()
    {
        return TASK_ID;
    }

    public boolean isRefreshRequired()
    {
        return true;
    }

    public File getSearchNativeOutputFile()
    {
        return SequestSearchTask.getNativeOutputFile(getAnalysisDirectory(), getBaseName(), getGZPreference());
    }
}
