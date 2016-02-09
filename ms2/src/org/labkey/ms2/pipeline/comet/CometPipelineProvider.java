/*
 * Copyright (c) 2013-2016 LabKey Corporation
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

package org.labkey.ms2.pipeline.comet;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.PipelineController;
import org.labkey.ms2.pipeline.SearchFormUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: September 17, 2013
 */
public class CometPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    private static final String ACTION_LABEL = "Comet Peptide Search";

    public static final String NAME = "Comet";

    public CometPipelineProvider(Module owningModule)
    {
        super(NAME, owningModule);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        return "comet.xml".equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(PipelineController.SearchCometAction.class, ACTION_LABEL);
        addAction(actionId, PipelineController.SearchCometAction.class, ACTION_LABEL,
            directory, directory.listFiles(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        String actionId = createActionId(PipelineController.SearchCometAction.class, ACTION_LABEL);
        return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
    }

    public HttpView getSetupWebPart(Container container)
    {
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
            html.append("<table><tr><td style=\"font-weight:bold;\">Comet specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetCometDefaultsAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                .append(" - Specify the default XML parameters file for Comet.</td></tr></table>");
            out.write(html.toString());
        }
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return CometSearchProtocolFactory.get();
    }

    public List<String> getSequenceDbPaths(File sequenceRoot) throws IOException
    {
        return MS2PipelineManager.addSequenceDbPaths(sequenceRoot, "", new ArrayList<>());
    }

    public List<String> getSequenceDbDirList(Container container, File sequenceRoot) throws IOException
    {
        return MS2PipelineManager.getSequenceDirList(sequenceRoot, "");
    }

    public List<String> getTaxonomyList(Container container) throws IOException
    {
        //Comet does not support Mascot style taxonomy.
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
        return "pipelineComet";
    }

    public void ensureEnabled(Container container) throws PipelineValidationException
    {
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
