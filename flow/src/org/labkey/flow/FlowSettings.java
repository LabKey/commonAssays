/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;

import java.io.File;
import java.util.Map;

public class FlowSettings
{
    static private File _tempAnalysisDirectory;
    static private final String PROPCAT_FLOW = "flow";
    static private final String PROPNAME_WORKINGDIRECTORY = "workingDirectory";

    static private File getTempAnalysisDirectory() throws Exception
    {
        if (_tempAnalysisDirectory != null)
            return _tempAnalysisDirectory;
        File file = File.createTempFile("FlowAnalysis", "tmp");
        File ret = new File(file.getParentFile(), "FlowAnalysis");
        if (!ret.exists())
        {
            ret.mkdir();
        }
        file.delete();
        _tempAnalysisDirectory = ret;
        return ret;
    }

    static public File getWorkingDirectory() throws Exception
    {
        String path = getWorkingDirectoryPath();
        if (path != null)
            return new File(path);
        return getTempAnalysisDirectory();
    }

    static public String getWorkingDirectoryPath()
    {
        Container container = ContainerManager.getRoot();
        Map<String, String> map = PropertyManager.getProperties(container.getId(), PROPCAT_FLOW);
        return map.get(PROPNAME_WORKINGDIRECTORY);
    }

    static public void setWorkingDirectoryPath(String path) throws Exception
    {
        Container container = ContainerManager.getRoot();
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(container.getId(), PROPCAT_FLOW, path != null);
        if (map == null)
        {
            assert path == null;
            return;
        }
        map.put(PROPNAME_WORKINGDIRECTORY, path);
        PropertyManager.saveProperties(map);
    }

    static public boolean getDeleteFiles()
    {
        return false;
    }
}
