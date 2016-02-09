/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

package org.labkey.ms2.pipeline.sequest;

import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskFactory;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.*;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.ms2.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Aug 24, 2006
 * Time: 12:45:45 PM
 */
public class SequestPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    private static final String ACTION_LABEL = "Sequest Peptide Search";

    public static String name = "Sequest";

    public SequestPipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        return "sequest.xml".equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    private boolean hasSequest()
    {
        for (TaskFactory taskFactory : PipelineJobService.get().getTaskFactories(null))
        {
            if (taskFactory instanceof AbstractSequestSearchTaskFactory)
            {
                AbstractSequestSearchTaskFactory sequestFactory = (AbstractSequestSearchTaskFactory)taskFactory;
                if (sequestFactory.getLocation() != null || sequestFactory.getSequestInstallDir() != null)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!hasSequest())
            return;
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(PipelineController.SearchSequestAction.class, ACTION_LABEL);
        addAction(actionId, PipelineController.SearchSequestAction.class, ACTION_LABEL,
            directory, directory.listFiles(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        if (hasSequest())
        {
            String actionId = createActionId(PipelineController.SearchSequestAction.class, ACTION_LABEL);
            return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
        }
        return super.getDefaultActionConfigSkipModuleEnabledCheck(container);
    }

    public HttpView getSetupWebPart(Container container)
    {
        if (!hasSequest())
            return null;
        return new SetupWebPart();
    }

    class SetupWebPart extends WebPartView
    {
        public SetupWebPart()
        {
            super(FrameType.DIV);
        }

        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            ViewContext context = getViewContext();
            if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
                return;
            StringBuilder html = new StringBuilder();
            html.append("<table><tr><td style=\"font-weight:bold;\">Sequest specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetSequestDefaultsAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                .append(" - Specify the default XML parameters file for Sequest.</td></tr></table>");
            out.write(html.toString());
        }
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return SequestSearchProtocolFactory.get();
    }

    public List<String> getSequenceDbPaths(File sequenceRoot) throws IOException
    {
        return MS2PipelineManager.addSequenceDbPaths(sequenceRoot, "", new ArrayList<String>());
    }

    public List<String> getSequenceDbDirList(Container container, File sequenceRoot) throws IOException
    {
        return MS2PipelineManager.getSequenceDirList(sequenceRoot, "");
    }

    public List<String> getTaxonomyList(Container container) throws IOException
    {
        //"Sequest does not support Mascot style taxonomy.
        return null;
    }

    public Map<String, List<String>> getEnzymes(Container container) throws IOException
    {
        return SearchFormUtil.getDefaultEnzymeMap();
    }

    public Map<String, String> getResidue0Mods(Container container) throws IOException
    {
        return SearchFormUtil.getDefaultStaticMods();
    }

    public Map<String, String> getResidue1Mods(Container container) throws IOException
    {
        return SearchFormUtil.getDefaultDynamicMods();
    }

    public String getHelpTopic()
    {
        return "pipelineSequest";
    }

    public void ensureEnabled(Container container) throws PipelineValidationException
    {
        if (!hasSequest())
            throw new PipelineValidationException("Sequest server has not been specified in site customization.");
    }

    public boolean supportsDirectories()
    {
        return true;
    }

    public boolean remembersDirectories()
    {
        return false;
    }

    public boolean hasRemoteDirectories()
    {
        return false;
    }

}
