/*
 * Copyright (c) 2012 LabKey Corporation
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
