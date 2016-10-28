/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ViewContext;
import org.labkey.microarray.controllers.MicroarrayController;
import org.labkey.microarray.MicroarrayModule;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;

public class GeneDataPipelineProvider extends PipelineProvider
{
    public static final String NAME = "GeneData";
    private static final String BUTTON_NAME = "Analyze in GeneData";

    private static final String GENE_DATA_BASE_URL_PARAMETER_NAME = "org.labkey.microarray.geneDataBaseURL";
    private static final String GENE_DATA_FILE_ROOT_PARAMETER_NAME = "org.labkey.microarray.geneDataFileRoot";

    private static final Logger LOG = Logger.getLogger(GeneDataPipelineProvider.class);

    public GeneDataPipelineProvider(MicroarrayModule module)
    {
        super(NAME, module);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), ReadPermission.class))
        {
            return;
        }

        if (getGeneDataBaseURL() == null || getGeneDataFileRoot() == null)
        {
            return;
        }

        String actionId = createActionId();
        addAction(actionId, MicroarrayController.GeneDataAnalysisAction.class, BUTTON_NAME,
                directory, directory.listFiles(new FileFilter()
                {
                    @Override
                    public boolean accept(File pathname)
                    {
                        return pathname.isFile();
                    }
                }), true, false, includeAll);
    }

    private String createActionId()
    {
        return createActionId(MicroarrayController.GeneDataAnalysisAction.class, BUTTON_NAME);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        if (getGeneDataBaseURL() == null || getGeneDataFileRoot() == null)
        {
            return Collections.emptyList();
        }

        String actionId = createActionId();
        return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, BUTTON_NAME, true));
    }

    private static String getConfigParameter(String parameterName)
    {
        ServletContext context = ModuleLoader.getServletContext();
        if (context != null)
        {
            return context.getInitParameter(parameterName);
        }
        return null;
    }

    public static String getGeneDataBaseURL()
    {
        return getConfigParameter(GENE_DATA_BASE_URL_PARAMETER_NAME);
    }

    /** @return the file to the gene data root file, if configured and it exists; null otherwise */
    public static File getGeneDataFileRoot()
    {
        String path = getConfigParameter(GENE_DATA_FILE_ROOT_PARAMETER_NAME);
        if (path == null)
        {
            return null;
        }
        File f = new File(path);
        if (NetworkDrive.exists(f))
        {
            return f;
        }
        else
        {
            LOG.warn("No such directory '" + path + "' specified by '" + GENE_DATA_FILE_ROOT_PARAMETER_NAME + "'");
            return null;
        }
    }
}
