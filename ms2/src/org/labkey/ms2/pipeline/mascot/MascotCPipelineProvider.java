/*
 * Copyright (c) 2007-2026 LabKey Corporation
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
package org.labkey.ms2.pipeline.mascot;

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
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.MS2PipelineProvider;
import org.labkey.ms2.pipeline.MS2PipelineProvider.Setting;
import org.labkey.ms2.pipeline.PipelineController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MascotCPipelineProvider extends AbstractMS2SearchPipelineProvider<MascotSearchTask.Factory>
{
    public static String name = "Mascot";
    private static final String ACTION_LABEL = "Mascot Peptide Search";

    public MascotCPipelineProvider(Module owningModule)
    {
        super(name, owningModule, MascotSearchTask.Factory.class);
    }

    @Override
    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        if ("mascot.xml".equals(name))
            return true;

        return super.isStatusViewableFile(container, name, basename);
    }

    @Override
    public void updateFilePropertiesEnabled(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!MascotConfig.findMascotConfig(context.getContainer()).hasMascotServer())
            return;

        String actionId = getActionId();
        addAction(actionId, getTaskPipeline(MascotPipelineJob.TASK_ID).getAnalyzeURL(context.getContainer(), null, null), ACTION_LABEL,
                directory, directory.listPaths(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    protected String getActionId()
    {
        // Retain old GWT action class as the action ID to preserve file browser button configuration
        return createActionId("org.labkey.ms2.pipeline.PipelineController$SearchMascotAction", ACTION_LABEL);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        if (isEnabled() && MascotConfig.findMascotConfig(container).hasMascotServer())
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

    private static class SetupWebPart extends WebPartView<Object>
    {
        public SetupWebPart()
        {
            super(FrameType.DIV);
        }

        @Override
        protected void renderView(Object model, HtmlWriter out)
        {
            ViewContext context = getViewContext();
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetMascotDefaultsAction.class, context.getContainer());
            ActionURL configMascotURL = new ActionURL(MS2Controller.MascotConfigAction.class, context.getContainer());
            MS2PipelineProvider.renderSettings(context, "Mascot", out,
                new Setting(setDefaultsURL, "Mascot"),
                new Setting(configMascotURL, "Configure Mascot Server", "Specify connection information for the Mascot Server.")
            );
        }
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return MascotSearchProtocolFactory.get();
    }

    @NotNull
    private MascotConfig ensureMascotConfig(Container container) throws IOException
    {
        MascotConfig config = MascotConfig.findMascotConfig(container);
        if (!config.hasMascotServer())
            throw new IOException("Mascot Server has not been configured.");
        return config;
    }
}
