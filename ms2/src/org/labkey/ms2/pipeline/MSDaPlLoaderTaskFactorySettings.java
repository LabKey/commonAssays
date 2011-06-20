package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.TaskId;

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
    private File _baseDirectory;

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

    public File getBaseDirectory()
    {
        return _baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory)
    {
        _baseDirectory = baseDirectory;
    }
}