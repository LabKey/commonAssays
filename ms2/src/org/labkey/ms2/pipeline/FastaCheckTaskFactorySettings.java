package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.TaskId;

import java.util.List;

/**
 * User: jeckels
 * Date: May 25, 2012
 */
public class FastaCheckTaskFactorySettings extends AbstractTaskFactorySettings
{
    private String _cloneName;
    private Boolean _requireDecoyDatabase;
    private List<String> _decoyFileSuffixes;

    public FastaCheckTaskFactorySettings(String name)
    {
        super(FastaCheckTask.class, name);
    }

    public TaskId getCloneId()
    {
        return new TaskId(FastaCheckTask.class, _cloneName);
    }

    public String getCloneName()
    {
        return _cloneName;
    }

    public void setCloneName(String cloneName)
    {
        _cloneName = cloneName;
    }

    public Boolean getRequireDecoyDatabase()
    {
        return _requireDecoyDatabase;
    }

    public void setRequireDecoyDatabase(Boolean requireDecoyDatabase)
    {
        _requireDecoyDatabase = requireDecoyDatabase;
    }

    public List<String> getDecoyFileSuffixes()
    {
        return _decoyFileSuffixes;
    }

    public void setDecoyFileSuffixes(List<String> suffixes)
    {
        _decoyFileSuffixes = suffixes;
    }
}
