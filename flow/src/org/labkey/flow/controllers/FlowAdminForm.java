/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.flow.controllers;

import org.labkey.flow.FlowSettings;

public class FlowAdminForm
{
    private String _workingDirectory;
    private boolean _deleteFiles;
    private boolean _normalizationEnabled;

    public FlowAdminForm()
    {
        _workingDirectory = FlowSettings.getWorkingDirectoryPath();
        _deleteFiles = FlowSettings.isDeleteFiles();
        _normalizationEnabled = FlowSettings.isNormalizationEnabled();
    }

    public void setWorkingDirectory(String path)
    {
        _workingDirectory = path;
    }

    public String getWorkingDirectory()
    {
        return _workingDirectory;
    }

    public boolean isDeleteFiles()
    {
        return _deleteFiles;
    }

    public void setDeleteFiles(boolean deleteFiles)
    {
        _deleteFiles = deleteFiles;
    }

    public boolean isNormalizationEnabled()
    {
        return _normalizationEnabled;
    }

    public void setNormalizationEnabled(boolean normalizationEnabled)
    {
        _normalizationEnabled = normalizationEnabled;
    }
}
