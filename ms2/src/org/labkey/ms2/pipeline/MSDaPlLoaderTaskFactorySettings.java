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

import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.file.PathMapper;

/**
 * User: jeckels
 * Date: May 17, 2011
 */
public class MSDaPlLoaderTaskFactorySettings extends AbstractTaskFactorySettings
{
    private String _cloneName;
    private String _submitURL;
    private String _statusBaseURL;
    private String _projectDetailsURL;
    private String _checkAccessURL;
    private String _retryURL;
    private String _checkFastaURL;
    private String _username;
    private String _password;
    private String _pipeline;
    private PathMapper _pathMapper;

    public MSDaPlLoaderTaskFactorySettings(String name)
    {
        super(MSDaPlLoaderTask.class, name);
    }

    public TaskId getCloneId()
    {
        return new TaskId(MSDaPlLoaderTask.class, _cloneName);
    }

    public String getCloneName()
    {
        return _cloneName;
    }

    public void setCloneName(String cloneName)
    {
        _cloneName = cloneName;
    }

    public String getSubmitURL()
    {
        return _submitURL;
    }

    public void setSubmitURL(String submitURL)
    {
        _submitURL = submitURL;
    }

    public String getStatusBaseURL()
    {
        return _statusBaseURL;
    }

    public void setStatusBaseURL(String statusBaseURL)
    {
        _statusBaseURL = statusBaseURL;
    }

    public String getUsername()
    {
        return _username;
    }

    public void setUsername(String username)
    {
        _username = username;
    }

    public String getPassword()
    {
        return _password;
    }

    public void setPassword(String password)
    {
        _password = password;
    }

    public String getPipeline()
    {
        return _pipeline;
    }

    public void setPipeline(String pipeline)
    {
        _pipeline = pipeline;
    }

    public PathMapper getPathMapper()
    {
        return _pathMapper;
    }

    public void setPathMapper(PathMapper pathMapper)
    {
        _pathMapper = pathMapper;
    }

    public String getProjectDetailsURL()
    {
        return _projectDetailsURL;
    }

    public void setProjectDetailsURL(String projectDetailsURL)
    {
        _projectDetailsURL = projectDetailsURL;
    }

    public String getCheckAccessURL()
    {
        return _checkAccessURL;
    }

    public void setCheckAccessURL(String checkAccessURL)
    {
        _checkAccessURL = checkAccessURL;
    }

    public String getRetryURL()
    {
        return _retryURL;
    }

    public void setRetryURL(String retryURL)
    {
        _retryURL = retryURL;
    }

    public String getCheckFastaURL()
    {
        return _checkFastaURL;
    }

    public void setCheckFastaURL(String checkFastaURL)
    {
        _checkFastaURL = checkFastaURL;
    }
}
