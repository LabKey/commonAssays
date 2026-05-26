/*
 * Copyright (c) 2007-2026 LabKey Corporation
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
import org.labkey.api.data.PropertyManager.WritablePropertyMap;
import org.labkey.api.util.FileUtil;
import org.labkey.vfs.FileLike;
import org.labkey.vfs.FileSystemLike;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FlowSettings
{
    static private FileLike _tempAnalysisDirectory;
    static private final String PROPCAT_FLOW = "flow";
    static private final String PROPNAME_WORKINGDIRECTORY = "workingDirectory";
    static private final String PROPNAME_DELETE_FILES = "deleteFiles";

    static private FileLike getTempAnalysisDirectory()
    {
        if (_tempAnalysisDirectory != null)
            return _tempAnalysisDirectory;
        try
        {
            FileLike ret = FileUtil.createTempDirectoryFileLike("FlowAnalysis");
            FileLike file = ret.resolveChild("FlowAnalysis.tmp");
            if (!ret.exists())
            {
                FileUtil.mkdir(ret);
            }

            // Clean-up any existing prior analysis file
            file.delete();
            _tempAnalysisDirectory = ret;
            return ret;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the flow analysis working directory.
     * Note: This may be outside the FileLike/FileSystemLike paradigm, it is either a temp directory or a value set by the admins
     *
     * @return File object representing the Flow analysis working directory.
     */
    static public FileLike getWorkingDirectory()
    {
        //Get admin provided setting if it exists
        String path = getWorkingDirectoryPath();
        if (path != null)
            return FileSystemLike.wrapFile(new File(path));

        // Otherwise default to the
        return getTempAnalysisDirectory();
    }

    static public String getWorkingDirectoryPath()
    {
        Container container = ContainerManager.getRoot();
        Map<String, String> map = PropertyManager.getProperties(container, PROPCAT_FLOW);
        return map.get(PROPNAME_WORKINGDIRECTORY);
    }

    /**
     * Save the Flow Analysis working directory path setting
     * Note: This may be outside the FileLike/FileSystemLike paradigm
     *
     * @param path string file path to the flow analysis working directory
     */
    static public void setWorkingDirectoryPath(String path)
    {
        Container container = ContainerManager.getRoot();
        WritablePropertyMap map = PropertyManager.getWritableProperties(container, PROPCAT_FLOW, path != null);
        if (map == null)
        {
            assert path == null;
            return;
        }
        map.put(PROPNAME_WORKINGDIRECTORY, path);
        map.save();
    }

    public static void setDeleteFiles(boolean deleteFiles)
    {
        Container container = ContainerManager.getRoot();
        WritablePropertyMap map = PropertyManager.getWritableProperties(container, PROPCAT_FLOW, !deleteFiles);
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
