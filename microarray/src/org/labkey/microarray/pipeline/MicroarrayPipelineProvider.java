/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

package org.labkey.microarray.pipeline;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.view.ViewContext;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.MicroarrayController;
import org.labkey.microarray.MicroarrayModule;

import java.util.Collections;
import java.util.List;

public class MicroarrayPipelineProvider extends AssayPipelineProvider
{
    public static final String NAME = "Array";
    private static final String IMPORT_IMAGES_BUTTON_NAME = "Import Images";

    public MicroarrayPipelineProvider(MicroarrayAssayProvider assayProvider)
    {
        super(NAME, MicroarrayModule.class, ArrayPipelineManager.getMageFileFilter(), assayProvider, "Import MAGE-ML");
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        super.updateFileProperties(context, pr, directory, includeAll);

        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createImportImagesActionId();
        addAction(actionId, MicroarrayController.ImportImageFilesAction.class, IMPORT_IMAGES_BUTTON_NAME,
                directory, directory.listFiles(ArrayPipelineManager.getImageFileFilter()), true, true, includeAll);
    }

    private String createImportImagesActionId()
    {
        return createActionId(MicroarrayController.ImportImageFilesAction.class, IMPORT_IMAGES_BUTTON_NAME);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfig()
    {
        String actionId = createImportImagesActionId();
        return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, IMPORT_IMAGES_BUTTON_NAME, true));
    }
}

