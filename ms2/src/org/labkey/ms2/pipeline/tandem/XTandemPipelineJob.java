/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
package org.labkey.ms2.pipeline.tandem;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.log4j.Logger;
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * XTandemPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class XTandemPipelineJob extends AbstractMS2SearchPipelineJob implements XTandemSearchTask.JobSupport
{
    private static final Logger LOG = getJobLogger(XTandemPipelineJob.class);
    protected static final TaskId TASK_ID = new TaskId(XTandemPipelineJob.class);

    @Override
    public Logger getClassLogger()
    {
        return LOG;
    }

    @JsonCreator
    protected XTandemPipelineJob(@JsonProperty("_dirSequenceRoot") File dirSequenceRoot)
    {
        super(dirSequenceRoot);
    }

    public XTandemPipelineJob(XTandemSearchProtocol protocol,
                              ViewBackgroundInfo info,
                              PipeRoot root,
                              String name,
                              File dirSequenceRoot,
                              List<File> filesMzXML,
                              File fileInputXML) throws IOException
    {
        super(protocol, XTandemPipelineProvider.name, info, root, name, dirSequenceRoot, fileInputXML, filesMzXML);

        header("X! Tandem search for " + getBaseName());
        writeInputFilesToLog();
    }

    public XTandemPipelineJob(XTandemPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);        
    }

    @Override
    public TaskId getTaskPipelineId()
    {
        return TASK_ID;
    }

    @Override
    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new XTandemPipelineJob(this, file);
    }

    /** Currently prophet analysis enabled for native, comet, and k-store algorithms */
    @Override
    public boolean isProphetEnabled()
    {
        String paramScore = getParameters().get("scoring, algorithm");
        if (paramScore == null || paramScore.length() == 0)
            paramScore = "native";

        return ("native".equals(paramScore) ||
                "comet".equals(paramScore) ||
                "k-score".equals(paramScore));        
    }

    // if fire does not exist, will append .gz if config indicates preference for gzipped outputs
    @Override
    public File getSearchNativeOutputFile()
    {
        return XTandemSearchTask.getNativeOutputFile(getAnalysisDirectory(), getBaseName(), getGZPreference());
    }
}