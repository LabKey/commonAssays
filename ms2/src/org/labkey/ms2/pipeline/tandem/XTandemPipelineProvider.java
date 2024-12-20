/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.ms2.pipeline.tandem;

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
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class XTandemPipelineProvider extends AbstractMS2SearchPipelineProvider<XTandemSearchTask.Factory>
{
    public static String name = "X! Tandem";
    private static final String ACTION_LABEL = "X!Tandem Peptide Search";

    public XTandemPipelineProvider(Module owningModule)
    {
        super(name, owningModule, XTandemSearchTask.Factory.class);
    }

    @Override
    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
        return nameParameters.equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    @Override
    public void updateFilePropertiesEnabled(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        String actionId = getActionId();
        addAction(actionId, getTaskPipeline(XTandemPipelineJob.TASK_ID).getAnalyzeURL(context.getContainer(), directory.getRelativePath(), null), ACTION_LABEL,
                directory, directory.listPaths(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    protected String getActionId()
    {
        // Retain old GWT action class as the action ID to preserve file browser button configuration
        return createActionId("org.labkey.ms2.pipeline.PipelineController$SearchXTandemAction", ACTION_LABEL);
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

    private static class SetupWebPart extends WebPartView<Object>
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
            html.append("<table><tr><td style=\"font-weight:bold;\">X! Tandem specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetTandemDefaultsAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for X! Tandem.</td></tr></table>");
            out.write(html.toString());
        }
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return XTandemSearchProtocolFactory.get();
    }

    @Override
    public List<String> getSequenceDbDirList(Container container, File sequenceRoot)
    {
        return MS2PipelineManager.getSequenceDirList(sequenceRoot, "");
    }

    @Override
    public String getHelpTopic()
    {
        return "pipelineXTandem";
    }

    @Override
    public void ensureEnabled(Container container)
    {
        // Always enabled.
    }
}
