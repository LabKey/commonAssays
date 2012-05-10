/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.file.PathMapper;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileType;
import org.labkey.ms2.pipeline.sequest.SequestPipelineJob;
import org.labkey.ms2.pipeline.sequest.UWSequestSearchTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * User: jeckels
 * Date: May 17, 2011
 */
public class MSDaPlLoaderTask extends PipelineJob.Task<MSDaPlLoaderTask.Factory>
{
    private static final String ACTION_NAME = "Load MSDaPl";

    public MSDaPlLoaderTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public static class Factory extends AbstractTaskFactory<MSDaPlLoaderTaskFactorySettings, Factory>
    {
        private String _baseServerURL;
        private String _username;
        private String _password;
        private Integer _projectId;
        private String _pipeline;
        private PathMapper _pathMapper;

        public Factory()
        {
            super(MSDaPlLoaderTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new MSDaPlLoaderTask(this, job);
        }

        public List<FileType> getInputTypes()
        {
            // CONSIDER: Not really the input type, but the input type for the search.
            //           Should it be null or FASTA?
            return Collections.singletonList(AbstractMS2SearchProtocol.FT_MZXML);
        }

        public String getStatusName()
        {
            return "MSDaPl";
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(ACTION_NAME);
        }

        public boolean isJobComplete(PipelineJob job)
        {
            // No way of knowing.
            return false;
        }

        public String getGroupParameterName()
        {
            return "load MSDaPl";
        }

        @Override
        protected void configure(MSDaPlLoaderTaskFactorySettings settings)
        {
            super.configure(settings);
            if (settings.getBaseServerURL() != null)
                _baseServerURL = settings.getBaseServerURL();
            if (settings.getUsername() != null)
                _username = settings.getUsername();
            if (settings.getPassword() != null)
                _password = settings.getPassword();
            if (settings.getProjectId() != null)
                _projectId = settings.getProjectId();
            if (settings.getPipeline() != null)
                _pipeline = settings.getPipeline();
            if (settings.getPathMapper() != null)
                _pathMapper = settings.getPathMapper();
        }

        public PathMapper getPathMapper()
        {
            return _pathMapper;
        }
    }

    public SequestPipelineJob getJob()
    {
        return (SequestPipelineJob)super.getJob();
    }

    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        RecordedAction action = new RecordedAction(ACTION_NAME);

        File dir = getJob().getLogFile().getParentFile();
        File sequestDir = new File(dir, "sequest");
        File decoyDir = new File(sequestDir, "decoy");
        File percolatorDir = new File(dir, "percolator");

        sequestDir.mkdir();
        decoyDir.mkdir();
        percolatorDir.mkdir();

        JSONObject postBody = new JSONObject();
        postBody.put("projectId", _factory._projectId);
        postBody.put("date", DateUtil.formatDateTime(new Date(), "yyyy-MM-dd"));
        postBody.put("pipeline", _factory._pipeline);
        String comments = getJob().getDescription();
        if (getJob().getStatusHref() != null)
        {
            comments = comments + " - " + getJob().getStatusHref().getURIString();
        }
        postBody.put("comments", comments);

        try
        {
            FileUtils.copyFileToDirectory(new File(dir, "sequest.params"), sequestDir);
            File sequestResultsFile = UWSequestSearchTask.SEQUEST_OUTPUT_FILE_TYPE.getFile(dir, getJob().getBaseName());
            FileUtils.copyFileToDirectory(sequestResultsFile, sequestDir);
            File inputFile = getJob().getInputFiles().get(0);
            if (!inputFile.getName().endsWith("ms2"))
            {
                File ms2File = new File(inputFile.getParentFile(), getJob().getBaseName() + ".ms2");
                File cms2File = new File(inputFile.getParentFile(), getJob().getBaseName() + ".cms2");
                if (ms2File.isFile())
                {
                    FileUtils.copyFileToDirectory(ms2File, sequestDir);
                }
                if (cms2File.isFile())
                {
                    FileUtils.copyFileToDirectory(ms2File, sequestDir);
                }
            }
            FileUtils.copyFileToDirectory(inputFile, sequestDir);
            FileUtils.copyFile(UWSequestSearchTask.SEQUEST_DECOY_OUTPUT_FILE_TYPE.getFile(dir, getJob().getBaseName()), new File(decoyDir, sequestResultsFile.getName()));
            FileUtils.copyFile(new File(dir, getJob().getBaseName() + ".result.xml"), new File(percolatorDir, "combined-results.xml"));

            changePermissions(sequestDir);
            changePermissions(percolatorDir);

            String localURI = getJob().getLogFile().getParentFile().getCanonicalFile().toURI().toString();
            PathMapper pathMapper = _factory.getPathMapper();
            String linuxURI = pathMapper == null ? localURI : pathMapper.remoteToLocal(localURI);
            postBody.put("dataDirectory", linuxURI.substring("file:".length()));
            HttpClient client = new HttpClient();
            client.getState().setCredentials(
                    new AuthScope(new URI(_factory._baseServerURL, false).getHost(), AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                    new UsernamePasswordCredentials(_factory._username, _factory._password));

            //send basic auth header on first request
            client.getParams().setAuthenticationPreemptive(true);
            PostMethod method = new PostMethod(_factory._baseServerURL);

            method.setRequestEntity(new StringRequestEntity(postBody.toString(), "application/json", "UTF-8"));

            getJob().getLogger().info("Submitting to MSDaPl");

            if (client.executeMethod(method) != 200)
            {
                getJob().getLogger().error("HTTP status code " +  method.getStatusCode() + " returned from MSDaPl");
                getJob().getLogger().error(method.getResponseBodyAsString());
            }
            else
            {
                getJob().getLogger().info("Submit successful: " + method.getResponseBodyAsString());                
            }
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }

        return new RecordedActionSet(action);
    }

    private void changePermissions(File file)
    {
        file.setExecutable(true, false);
        file.setWritable(true, false);
        file.setReadable(true, false);

        File[] children = file.listFiles();
        if (children != null)
        {
            for (File child : children)
            {
                changePermissions(child);
            }
        }
    }
}
