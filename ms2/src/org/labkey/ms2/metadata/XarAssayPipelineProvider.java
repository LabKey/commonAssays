/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.ms2.metadata;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.NavTree;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import java.io.File;
import java.util.List;

/**
 * User: peter@labkey.com
 * Date: Aug 24, 2006
 * Time: 12:45:45 PM
 */
public class XarAssayPipelineProvider extends PipelineProvider
{
    public static String name = "XarAssay";
    public static String PIPELINE_BUTTON_TEXT = "Create Assay Run";

    public XarAssayPipelineProvider()
    {
        super(name);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, List<FileEntry> entries)
    {
        for (FileEntry entry : entries)
        {
            if (!entry.isDirectory())
            {
                continue;
            }

            File[] files = entry.listFiles(FILE_FILTER);
            if (files != null && files.length > 0)
            {
                PipeRoot root = PipelineService.get().findPipelineRoot(context.getContainer());
                List<ExpProtocol> assays = AssayService.get().getAssayProtocols(context.getContainer());

                NavTree navTree = new NavTree("Describe Samples with Assay");

                for (ExpProtocol protocol : assays)
                {
                    if (AssayService.get().getProvider(protocol) instanceof XarAssayProvider)
                    {
                        ActionURL url = new ActionURL(XarAssayController.UploadRedirectAction.class, context.getContainer());
                        url.addParameter("protocolId", protocol.getRowId());
                        url.addParameter("path", root.relativePath(new File(entry.getURI())));
                        navTree.addChild("Use " + protocol.getName(), url);
                        navTree.setId("Describe Samples:Use " + protocol.getName());
                    }
                }

                if (navTree.getChildCount() > 0)
                {
                    navTree.addSeparator();
                }

                ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getDesignerURL(context.getContainer(), XarAssayProvider.NAME);
                navTree.addChild("Create Assay Definition", url);

                entry.addAction(new FileAction(navTree, files));
            }
        }
    }

    public static final PipelineProvider.FileEntryFilter FILE_FILTER = new PipelineProvider.FileEntryFilter()
    {
        public boolean accept(File f)
        {
            // TODO:  If no corresponding mzXML file, show raw files.
            return isMzXMLFile(f);
        }
    };

    public static boolean isMzXMLFile(File file)
    {
        return MS2PipelineManager.isMzXMLFile(file);
    }

}
