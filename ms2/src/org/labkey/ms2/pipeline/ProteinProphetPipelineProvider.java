/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.FileType;
import org.labkey.ms2.MS2Controller;

import java.io.File;
import java.util.List;

/**
 * User: jeckels
 * Date: Feb 17, 2006
 */
public class ProteinProphetPipelineProvider extends PipelineProvider
{
    static final String NAME = "ProteinProphet";

    public ProteinProphetPipelineProvider()
    {
        super(NAME);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, List<FileEntry> entries)
    {
        for (FileEntry entry : entries)
        {
            if (!entry.isDirectory())
                continue;

            addFileActions(MS2Controller.ImportProteinProphetAction.class, "Import ProteinProphet",
                    entry, entry.listFiles(new ProteinProphetFilenameFilter()));
        }
    }


    private static class ProteinProphetFilenameFilter extends FileEntryFilter
    {
        public boolean accept(File f)
        {
            FileType fileType = TPPTask.getProtXMLFileType(f);
            if (fileType != null)
            {
                File parent = f.getParentFile();
                String basename = fileType.getBaseName(f);
                
                return !fileExists(AbstractMS2SearchProtocol.FT_SEARCH_XAR.newFile(parent, basename));
            }

            return false;
        }
    }

}
