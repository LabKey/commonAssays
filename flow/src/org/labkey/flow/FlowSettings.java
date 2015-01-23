/*
 * Copyright (c) 2006-2015 LabKey Corporation
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

package org.labkey.flow;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FlowSettings
{
    static private File _tempAnalysisDirectory;
    static private final String PROPCAT_FLOW = "flow";
    static private final String PROPNAME_WORKINGDIRECTORY = "workingDirectory";
    static private final String PROPNAME_NORMALIZATION_ENABLED = "normalizationEnabled";
    static private final String PROPNAME_DELETE_FILES = "deleteFiles";

    static private File getTempAnalysisDirectory()
    {
        if (_tempAnalysisDirectory != null)
            return _tempAnalysisDirectory;
        File file;
        try
        {
            file = File.createTempFile("FlowAnalysis", "tmp");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        File ret = new File(file.getParentFile(), "FlowAnalysis");
        if (!ret.exists())
        {
            ret.mkdir();
        }
        file.delete();
        _tempAnalysisDirectory = ret;
        return ret;
    }

    static public File getWorkingDirectory()
    {
        String path = getWorkingDirectoryPath();
        if (path != null)
            return new File(path);
        return getTempAnalysisDirectory();
    }

    static public String getWorkingDirectoryPath()
    {
        Container container = ContainerManager.getRoot();
        Map<String, String> map = PropertyManager.getProperties(container, PROPCAT_FLOW);
        return map.get(PROPNAME_WORKINGDIRECTORY);
    }

    static public void setWorkingDirectoryPath(String path)
    {
        Container container = ContainerManager.getRoot();
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(container, PROPCAT_FLOW, path != null);
        if (map == null)
        {
            assert path == null;
            return;
        }
        map.put(PROPNAME_WORKINGDIRECTORY, path);
        map.save();
    }

    static public boolean isNormalizationEnabled()
    {
        Container container = ContainerManager.getRoot();
        Map<String, String> map = PropertyManager.getProperties(container, PROPCAT_FLOW);
        String value = StringUtils.trimToNull(map.get(PROPNAME_NORMALIZATION_ENABLED));
        if (value == null)
            return false;

        return Boolean.valueOf(value);
    }

    static public void setNormalizationEnabled(Boolean enabled)
    {
        Container container = ContainerManager.getRoot();
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(container, PROPCAT_FLOW, enabled != null);
        if (map == null)
        {
            assert enabled == null;
            return;
        }
        if (enabled == null)
            map.remove(PROPNAME_NORMALIZATION_ENABLED);
        else
            map.put(PROPNAME_NORMALIZATION_ENABLED, String.valueOf(enabled));
        map.save();
    }

    public static void setDeleteFiles(boolean deleteFiles)
    {
        Container container = ContainerManager.getRoot();
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(container, PROPCAT_FLOW, !deleteFiles);
        if (map == null)
            return;

        if (deleteFiles)
            map.remove(PROPNAME_DELETE_FILES);
        else
            map.put(PROPNAME_DELETE_FILES, "false");
        map.save();
    }

    /** Defaults to 'true' if no value has been set. */
    static public boolean isDeleteFiles()
    {
        Container container = ContainerManager.getRoot();
        Map<String, String> map = PropertyManager.getProperties(container, PROPCAT_FLOW);
        String value = StringUtils.trimToNull(map.get(PROPNAME_DELETE_FILES));
        if (value == null)
            return true;

        return Boolean.valueOf(value);
    }
}
