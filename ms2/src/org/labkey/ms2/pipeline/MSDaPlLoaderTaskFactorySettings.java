/*
 * Copyright (c) 2011 LabKey Corporation
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

import java.io.File;

/**
 * User: jeckels
 * Date: May 17, 2011
 */
public class MSDaPlLoaderTaskFactorySettings extends AbstractTaskFactorySettings
{
    private String _cloneName;
    private String _baseServerURL;
    private String _username;
    private String _password;
    private Integer _projectId;
    private String _pipeline;
    private PathMapper _pathMapper;

    public MSDaPlLoaderTaskFactorySettings(String name)
    {
        this(MSDaPlLoaderTask.class, name);
    }

    public MSDaPlLoaderTaskFactorySettings(Class namespaceClass, String name)
    {
        super(namespaceClass, name);
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

    public String getBaseServerURL()
    {
        return _baseServerURL;
    }

    public void setBaseServerURL(String baseServerURL)
    {
        _baseServerURL = baseServerURL;
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

    public Integer getProjectId()
    {
        return _projectId;
    }

    public void setProjectId(Integer projectId)
    {
        _projectId = projectId;
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
}