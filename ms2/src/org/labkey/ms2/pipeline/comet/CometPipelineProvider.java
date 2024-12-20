/*
 * Copyright (c) 2013-2018 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.PipelineController;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: September 17, 2013
 */
public class CometPipelineProvider extends AbstractMS2SearchPipelineProvider<CometSearchTask.Factory>
{
    private static final String ACTION_LABEL = "Comet Peptide Search";

    public static final String NAME = "Comet";

    public CometPipelineProvider(Module owningModule)
    {
        super(NAME, owningModule, CometSearchTask.Factory.class);
    }

    @Override
    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        return "comet.xml".equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    @Override
    public void updateFilePropertiesEnabled(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        String actionId = getActionId();
        addAction(actionId, getTaskPipeline(CometPipelineJob.TASK_ID).getAnalyzeURL(context.getContainer(), directory.getRelativePath(), null), ACTION_LABEL,
                directory, directory.listPaths(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    protected String getActionId()
    {
        // Retain old GWT action class as the action ID to preserve file browser button configuration
        return createActionId("org.labkey.ms2.pipeline.PipelineController$SearchCometAction", ACTION_LABEL);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        if (isEnabled())
        {
            String actionId = getActionId();
            return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
        }
        return super.getDefaultActionConfigSkipModuleEnabledCheck(container);
    }

    @Override
    @NotNull
    public HttpView<Object> createSetupWebPart(Container container)
    {
        return new SetupWebPart();
    }

    static class SetupWebPart extends WebPartView<Object>
    {
        public SetupWebPart()
        {
            super(FrameType.DIV);
        }

        @Override
        protected void renderView(Object model, PrintWriter out)
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

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return CometSearchProtocolFactory.get();
    }

    @Override
    public List<String> getSequenceDbDirList(Container container, File sequenceRoot)
    {
        return MS2PipelineManager.getSequenceDirList(sequenceRoot, "");
    }

    @Override
    public String getHelpTopic()
    {
        return "pipelineComet";
    }

    @Override
    public void ensureEnabled(Container container)
    {
    }
}
