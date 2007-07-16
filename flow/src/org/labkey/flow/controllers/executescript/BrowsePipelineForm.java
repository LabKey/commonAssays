package org.labkey.flow.controllers.executescript;

import org.labkey.api.pipeline.browse.BrowseForm;
import org.labkey.api.pipeline.browse.FileFilter;
import org.labkey.api.pipeline.browse.BrowseFile;
import org.labkey.api.view.ViewURLHelper;

import java.util.Map;
import java.util.LinkedHashMap;

public class BrowsePipelineForm extends BrowseForm
{
    public String getActionText()
    {
        return "Upload Workspace";
    }

    public ViewURLHelper getActionURL()
    {
        return getContainer().urlFor(AnalysisScriptController.Action.uploadWorkspaceBrowse);
    }


    public Map<String, ? extends FileFilter> getFileFilterOptions()
    {
        LinkedHashMap<String, FileFilter> ret = new LinkedHashMap();
        ret.put("xml", new FileFilter("XML files") {

            public boolean accept(BrowseFile file)
            {
                return file.getName().endsWith(".xml");
            }
        });
        ret.put("all", FileFilter.allFiles);
        return ret;
    }
}
