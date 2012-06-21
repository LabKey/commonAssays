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
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.file.PathMapper;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.pipeline.client.ParameterNames;
import org.labkey.ms2.pipeline.sequest.SequestPipelineJob;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: May 17, 2011
 */
public class MSDaPlLoaderTask extends PipelineJob.Task<MSDaPlLoaderTask.Factory>
{
    private static final String ACTION_NAME = "Load MSDaPl";

    private static final String PROJECT_ID_PARAMETER_NAME = "msdapl, project id";

    public MSDaPlLoaderTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public static class Factory extends AbstractTaskFactory<MSDaPlLoaderTaskFactorySettings, Factory>
    {
        private String _submitURL;
        private String _statusBaseURL;
        private String _checkFastaURL;
        private String _projectDetailsURL;
        private String _checkAccessURL;
        private String _retryURL;
        private String _username;
        private String _password;
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

        @Override
        public boolean isJoin()
        {
            return true;
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
        public void validateParameters(PipelineJob job) throws PipelineValidationException
        {
            if (_submitURL == null)
            {
                throw new PipelineValidationException("No submitURL set for MSDaPl configuration in ms2Config.xml");
            }
            if (_checkAccessURL == null)
            {
                throw new PipelineValidationException("No checkAccessURL set for MSDaPl configuration in ms2Config.xml");
            }
            if (_retryURL == null)
            {
                throw new PipelineValidationException("No retryURL set for MSDaPl configuration in ms2Config.xml");
            }
            if (_projectDetailsURL == null)
            {
                throw new PipelineValidationException("No projectDetailsURL set for MSDaPl configuration in ms2Config.xml");
            }
            if (_statusBaseURL == null)
            {
                throw new PipelineValidationException("No statusBaseURL set for MSDaPl configuration in ms2Config.xml");
            }
            if (_checkFastaURL == null)
            {
                throw new PipelineValidationException("No checkFastaURL set for MSDaPl configuration in ms2Config.xml");
            }

            int projectId = getProjectId(job.getParameters());

            try
            {
                // First validate that the project exists
                validateProjectId(projectId);
                validateUserAccess(projectId, job.getUser().getEmail());
                validateFasta(job.getParameters().get(ParameterNames.SEQUENCE_DB));
            }
            catch (HttpException e)
            {
                throw new PipelineValidationException(e);
            }
            catch (IOException e)
            {
                throw new PipelineValidationException(e);
            }
        }

        private void validateProjectId(int projectId) throws IOException, PipelineValidationException
        {
            String url = _projectDetailsURL + "/" + projectId;
            HttpClient client = new HttpClient();
            GetMethod method = new GetMethod(url);
            int statusCode = client.executeMethod(method);
            if (statusCode == 404)
            {
                throw new PipelineValidationException("No such MSDaPl project id: " + projectId);
            }
            else if (statusCode != 200)
            {
                throw new PipelineValidationException("Unexpected MSDaPl status code when issuing request to '" + url + "': " + statusCode);
            }
        }

        private void validateUserAccess(int projectId, String email) throws IOException, PipelineValidationException
        {
            String url = _checkAccessURL + "?projectId=" + projectId + "&userEmail=" + PageFlowUtil.encode(email);
            HttpClient client = new HttpClient();
            GetMethod method = new GetMethod(url);
            int statusCode = client.executeMethod(method);
            if (statusCode == 404)
            {
                throw new PipelineValidationException("Got a 404 response code from MSDaPl. User account for '" + email+  "' most likely does not exist");
            }
            if (statusCode != 200)
            {
                throw new PipelineValidationException("Unexpected MSDaPl status code when issuing request to '" + url + "': " + statusCode);
            }
            String response = method.getResponseBodyAsString();
            if (!response.toLowerCase().startsWith("access allowed"))
            {
                throw new PipelineValidationException(email + " does not have access to MSDaPl project " + projectId + ". Message was: '" + response + "'");
            }
        }

        private void validateFasta(String fastaName) throws IOException, PipelineValidationException
        {
            String url = _checkFastaURL + "?name=" + PageFlowUtil.encode(fastaName);
            HttpClient client = new HttpClient();
            GetMethod method = new GetMethod(url);
            int statusCode = client.executeMethod(method);
            if (statusCode != 200)
            {
                throw new PipelineValidationException("Unexpected MSDaPl status code when issuing request to '" + url + "': " + statusCode);
            }
            String response = method.getResponseBodyAsString();
            if (!response.toLowerCase().startsWith("found"))
            {
                throw new PipelineValidationException("Unknown FASTA file - not yet imported into YRC. Message was: '" + response + "'");
            }
        }

        @Override
        protected void configure(MSDaPlLoaderTaskFactorySettings settings)
        {
            super.configure(settings);
            if (settings.getSubmitURL() != null)
                _submitURL = trimURL(settings.getSubmitURL());
            if (settings.getStatusBaseURL() != null)
                _statusBaseURL = trimURL(settings.getStatusBaseURL());
            if (settings.getCheckAccessURL() != null)
                _checkAccessURL = trimURL(settings.getCheckAccessURL());
            if (settings.getRetryURL() != null)
                _retryURL = trimURL(settings.getRetryURL());
            if (settings.getProjectDetailsURL() != null)
                _projectDetailsURL = trimURL(settings.getProjectDetailsURL());
            if (settings.getCheckFastaURL() != null)
                _checkFastaURL = trimURL(settings.getCheckFastaURL());
            if (settings.getUsername() != null)
                _username = settings.getUsername();
            if (settings.getPassword() != null)
                _password = settings.getPassword();
            if (settings.getPipeline() != null)
                _pipeline = settings.getPipeline();
            if (settings.getPathMapper() != null)
                _pathMapper = settings.getPathMapper();
        }

        private String trimURL(String url)
        {
            while (url != null && url.endsWith("/"))
            {
                // Strip off any trailing slashes
                url = url.substring(0, url.length() - 1);
            }
            return url;
        }

        public PathMapper getPathMapper()
        {
            return _pathMapper;
        }
    }

    public static int getProjectId(Map<String, String> params) throws PipelineValidationException
    {
        String paramValue = params.get(PROJECT_ID_PARAMETER_NAME);
        if (paramValue == null)
        {
            throw new PipelineValidationException("No value specified for '" + PROJECT_ID_PARAMETER_NAME + "'");
        }
        try
        {
            return Integer.parseInt(paramValue);
        }
        catch (NumberFormatException e)
        {
            throw new PipelineValidationException("Expected a number for '" + PROJECT_ID_PARAMETER_NAME + "', but got: " + paramValue);
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

        JSONObject postBody = new JSONObject();
        try
        {
            postBody.put("projectId", getProjectId(getJob().getParameters()));
        }
        catch (PipelineValidationException e)
        {
            throw new PipelineJobException(e);
        }
        postBody.put("date", DateUtil.formatDateTime(new Date(), "yyyy-MM-dd"));
        postBody.put("pipeline", _factory._pipeline);
        String comments = getJob().getDescription();
        if (getJob().getStatusHref() != null)
        {
            comments = comments + " - " + getJob().getStatusHref().getURIString();
        }
        postBody.put("comments", comments);
        postBody.put("userEmail", getJob().getUser().getEmail());

        try
        {
            changePermissions(dir);

            String localURI = getJob().getLogFile().getParentFile().getCanonicalFile().toURI().toString();
            PathMapper pathMapper = _factory.getPathMapper();
            String linuxURI = pathMapper == null ? localURI : pathMapper.remoteToLocal(localURI);
            postBody.put("dataDirectory", linuxURI.substring("file:".length()));
            HttpClient client = new HttpClient();
            client.getState().setCredentials(
                    new AuthScope(new URI(_factory._submitURL, false).getHost(), AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                    new UsernamePasswordCredentials(_factory._username, _factory._password));

            //send basic auth header on first request
            client.getParams().setAuthenticationPreemptive(true);
            PostMethod method = new PostMethod(_factory._submitURL);

            method.setRequestEntity(new StringRequestEntity(postBody.toString(), "application/json", "UTF-8"));

            getJob().getLogger().info("Submitting to MSDaPl server: " + _factory._submitURL);

            if (client.executeMethod(method) != HttpServletResponse.SC_OK)
            {
                getJob().error("Submit to MSDaPl failed. HTTP status code " +  method.getStatusCode());
            }
            else
            {
                String responseMessage = method.getResponseBodyAsString();
                getJob().getLogger().info("Submit successful: " + responseMessage);
                if (responseMessage != null && responseMessage.toLowerCase().contains("id:"))
                {
                    try
                    {
                        long jobId = Long.parseLong(responseMessage.substring(responseMessage.toLowerCase().indexOf("id:") + "id:".length()).trim());
                        waitForLoad(jobId);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new PipelineJobException("Could not parse job ID from MSDaPl");
                    }
                }
                else
                {
                    throw new PipelineJobException("Unexpected response message: " + responseMessage);
                }
            }
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }

        return new RecordedActionSet(action);
    }

    private void waitForLoad(long jobId) throws PipelineJobException, IOException
    {
        int sleeps = 0;
        String statusMessage;
        do
        {
            try
            {
                if (sleeps++ % 10 == 0)
                {
                    // Write a message every five minutes to show that we're still working
                    getJob().getLogger().info("Polling MSDaPl for status");
                }

                // Wait 30 seconds before polling again
                Thread.sleep(30 * 1000);
            }
            catch (InterruptedException ignored) {}
            // Update status so we'll see if we've been cancelled
            getJob().setStatus("WAITING FOR MSDaPl LOAD");
            HttpClient client = new HttpClient();
            String url = _factory._statusBaseURL + "/" + jobId;
            client.getState().setCredentials(
                    new AuthScope(new URI(url, false).getHost(), AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                    new UsernamePasswordCredentials(_factory._username, _factory._password));
            client.getParams().setAuthenticationPreemptive(true);
            GetMethod method = new GetMethod(url);
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpServletResponse.SC_OK)
            {
                throw new PipelineJobException("Unexpected status code when querying MSDaPl status: " + statusCode + ", message: " + method.getResponseBodyAsString());
            }
            statusMessage = method.getResponseBodyAsString();
            if (statusMessage.toLowerCase().contains("error"))
            {
                throw new PipelineJobException("MSDaPl reported an error: " + statusMessage);
            }
        }
        while (!"Complete".equalsIgnoreCase(statusMessage));
    }

    private void changePermissions(File file)
    {
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
