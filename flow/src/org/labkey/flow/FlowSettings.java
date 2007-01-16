package org.labkey.flow;

import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
        Map<String, Object> map = PropertyManager.getProperties(container.getId(), PROPCAT_FLOW, false);
        if (map != null)
            return (String) map.get(PROPNAME_WORKINGDIRECTORY);
        return null;
    }

    static public void setWorkingDirectoryPath(String path) throws Exception
    {
        Container container = ContainerManager.getRoot();
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(0, container.getId(), PROPCAT_FLOW, path != null);
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
