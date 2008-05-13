/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.controllers.executescript;

import org.labkey.api.pipeline.browse.BrowseForm;
import org.labkey.api.pipeline.browse.FileFilter;
import org.labkey.api.pipeline.browse.BrowseFile;
import org.labkey.api.view.ActionURL;

import java.util.Map;
import java.util.LinkedHashMap;

public class BrowsePipelineForm extends BrowseForm
{
    public String getActionText()
    {
        return "Upload Workspace";
    }

    public ActionURL getActionURL()
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
