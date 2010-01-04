/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import java.io.File;
import java.io.IOException;

/**
 * MascotPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class MascotPipelineJob extends AbstractMS2SearchPipelineJob implements MascotSearchTask.JobSupport
{
    private static TaskId _tid = new TaskId(MascotPipelineJob.class);

    private String _mascotServer;
    private String _mascotHTTPProxy;
    private String _mascotUserAccount;
    private String _mascotUserPassword;
    private String _mascotSequenceDB;
    private String _mascotSequenceRelease;

    public MascotPipelineJob(MascotSearchProtocol protocol,
                             ViewBackgroundInfo info,
                             String name,
                             File dirSequenceRoot,
                             File filesMzXML[],
                             File fileInputXML
    ) throws IOException
    {
        super(protocol, MascotCPipelineProvider.name, info, name, dirSequenceRoot, fileInputXML, filesMzXML);

        AppProps appProps = AppProps.getInstance();
        _mascotServer = appProps.getMascotServer();
        _mascotHTTPProxy = appProps.getMascotHTTPProxy();
        _mascotUserAccount = appProps.getMascotUserAccount();
        _mascotUserPassword = appProps.getMascotUserPassword();

        header("Mascot search for " + getBaseName());
    }

    public MascotPipelineJob(MascotPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);

        _mascotServer = job._mascotServer;
        _mascotHTTPProxy = job._mascotHTTPProxy;
        _mascotUserAccount = job._mascotUserAccount;
        _mascotUserPassword = job._mascotUserPassword;
        _mascotSequenceDB = job._mascotSequenceDB;
        _mascotSequenceRelease = job._mascotSequenceRelease;
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

    public void setMascotSequenceDB(String sequenceDB)
    {
        _mascotSequenceDB = sequenceDB;
    }

    public void setMascotSequenceRelease(String sequenceRelease)
    {
        _mascotSequenceRelease = sequenceRelease;
    }

    public String getSearchEngine()
    {
        return "mascot";
    }

    public TaskId getTaskPipelineId()
    {
        return _tid;
    }

    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new MascotPipelineJob(this, file);
    }

    public File[] getSequenceFiles()
    {
        if (_mascotSequenceDB == null || _mascotSequenceRelease == null)
            return super.getSequenceFiles();

        return new File[] { MS2PipelineManager.getLocalMascotFile(getSequenceRootDirectory().getAbsolutePath(),
                _mascotSequenceDB, _mascotSequenceRelease) };
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