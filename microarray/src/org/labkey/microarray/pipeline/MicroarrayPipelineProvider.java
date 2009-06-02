/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.ACL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.MicroarrayController;

import java.io.File;
import java.util.List;


public class MicroarrayPipelineProvider extends PipelineProvider
{
    public static final String NAME = "Array";

    public MicroarrayPipelineProvider()
    {
        super(NAME);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, List<FileEntry> entries)
    {
        if (!context.hasPermission(ACL.PERM_INSERT))
            return;

        PipeRoot root = PipelineService.get().findPipelineRoot(context.getContainer());
        for (FileEntry entry : entries)
        {
            File[] files = entry.listFiles(ArrayPipelineManager.getImageFileFilter());
            if (files != null)
                addAction(MicroarrayController.ImportImageFilesAction.class, "Import Images",
                        entry, files);

            files = entry.listFiles(ArrayPipelineManager.getMageFileFilter());
            if (files != null && files.length > 0)
            {
                List<ExpProtocol> assays = AssayService.get().getAssayProtocols(context.getContainer());
                NavTree navTree = new NavTree("Import MAGE-ML");
                for (ExpProtocol protocol : assays)
                {
                    if (AssayService.get().getProvider(protocol) instanceof MicroarrayAssayProvider)
                    {
                        ActionURL url = MicroarrayController.getUploadRedirectAction(context.getContainer(), protocol, root.relativePath(new File(entry.getURI())));
                        NavTree child = new NavTree("Use " + protocol.getName(), url);
                        child.setId("Import MAGE-ML:Use " + protocol.getName());
                        navTree.addChild(child);
                    }
                }

                if (navTree.getChildCount() > 0)
                {
                    navTree.addSeparator();
                }

                ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getDesignerURL(context.getContainer(), MicroarrayAssayProvider.NAME, context.getActionURL());
                navTree.addChild("Create Assay Definition", url);

                entry.addAction(new FileAction(navTree, files));
            }
        }
    }

}

