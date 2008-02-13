/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.util.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * MascotPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class MascotPipelineJob extends AbstractMS2SearchPipelineJob implements MascotSearchTask.JobSupport
{
    enum Pipelines
    {
        sample, fraction, fractionGroup;

        public TaskId getTaskId()
        {
            return new TaskId(getClass().getEnclosingClass(), toString());
        }
    }

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
                             File fileInputXML,
                             boolean fromCluster) throws SQLException, IOException
    {
        super(protocol, MascotCPipelineProvider.name, info, name, dirSequenceRoot, fileInputXML, filesMzXML, fromCluster);

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

    public boolean isPerlClusterAware()
    {
        return true;
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
        TaskId tid = super.getTaskPipelineId();
        if (tid != null)
            return tid;

        if (getInputFiles().length > 1)
            return Pipelines.fractionGroup.getTaskId();
        if (!isSamples())
            return Pipelines.fraction.getTaskId();

        return Pipelines.sample.getTaskId();
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

    public String getXarTemplateResource()
     {
         StringBuilder templateResource = new StringBuilder("org/labkey/ms2/pipeline/mascot/templates/MS2SearchMascot");
         if (getInputFiles().length > 1)
         {
             templateResource.append("Fractions");
         }
         if (isXPressQuantitation())
         {
             templateResource.append("Xpress");
         }
         templateResource.append(".xml");

         return templateResource.toString();
     }
}