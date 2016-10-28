/*
 * Copyright (c) 2006-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.mascot;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * MascotPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class MascotPipelineJob extends AbstractMS2SearchPipelineJob implements MascotSearchTask.JobSupport
{
    private static final TaskId TASK_ID = new TaskId(MascotPipelineJob.class);

    private String _mascotServer;
    private String _mascotHTTPProxy;
    private String _mascotUserAccount;
    private String _mascotUserPassword;

    public MascotPipelineJob(MascotSearchProtocol protocol,
                             ViewBackgroundInfo info,
                             PipeRoot root,
                             String name,
                             File dirSequenceRoot,
                             List<File> filesMzXML,
                             File fileInputXML) throws IOException
    {
        super(protocol, MascotCPipelineProvider.name, info, root, name, dirSequenceRoot, fileInputXML, filesMzXML);

        MascotConfig config = MascotConfig.findMascotConfig(info.getContainer());
        _mascotServer = config.getMascotServer();
        _mascotHTTPProxy = config.getMascotHTTPProxy();
        _mascotUserAccount = config.getMascotUserAccount();
        _mascotUserPassword = config.getMascotUserPassword();

        header("Mascot search for " + getBaseName());
        writeInputFilesToLog();
    }

    public MascotPipelineJob(MascotPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);

        _mascotServer = job._mascotServer;
        _mascotHTTPProxy = job._mascotHTTPProxy;
        _mascotUserAccount = job._mascotUserAccount;
        _mascotUserPassword = job._mascotUserPassword;
    }

    public String getMascotServer()
    {
        return _mascotServer;
    }

    public String getMascotHTTPProxy()
    {
        return _mascotHTTPProxy;
    }

    public String getMascotUserAccount()
    {
        return _mascotUserAccount;
    }

    public String getMascotUserPassword()
    {
        return _mascotUserPassword;
    }

    public TaskId getTaskPipelineId()
    {
        return TASK_ID;
    }

    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new MascotPipelineJob(this, file);
    }

    public File getSearchNativeSpectraFile()
    {
        return MascotSearchTask.getNativeSpectraFile(getAnalysisDirectory(), getBaseName());
    }

    public File getSearchNativeOutputFile()
    {
        return MascotSearchTask.getNativeOutputFile(getAnalysisDirectory(), getBaseName());
    }
}