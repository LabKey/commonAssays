/*
 * Copyright (c) 2013-2026 LabKey Corporation
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
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.writer.HtmlWriter;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.MS2PipelineProvider;
import org.labkey.ms2.pipeline.MS2PipelineProvider.Setting;
import org.labkey.ms2.pipeline.PipelineController;

import java.util.Collections;
import java.util.List;

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
        protected void renderView(Object model, HtmlWriter out)
        {
            ViewContext context = getViewContext();
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetCometDefaultsAction.class, context.getContainer());
            MS2PipelineProvider.renderSettings(context, "Comet", out, new Setting(setDefaultsURL, "Comet"));
        }
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return CometSearchProtocolFactory.get();
    }
}
