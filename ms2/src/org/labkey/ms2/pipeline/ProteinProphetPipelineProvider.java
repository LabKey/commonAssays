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

package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.FileType;
import org.labkey.api.module.Module;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.ms2.MS2Controller;

import java.io.File;

/**
 * User: jeckels
 * Date: Feb 17, 2006
 */
public class ProteinProphetPipelineProvider extends PipelineProvider
{
    static final String NAME = "ProteinProphet";

    public ProteinProphetPipelineProvider(Module owningModule)
    {
        super(NAME, owningModule);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }
        
        String actionId = createActionId(MS2Controller.ImportProteinProphetAction.class, "Import ProteinProphet Results");
        addAction(actionId, MS2Controller.ImportProteinProphetAction.class, "Import ProteinProphet Results",
                directory, directory.listFiles(new ProteinProphetFilenameFilter()), true, true, includeAll);
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
