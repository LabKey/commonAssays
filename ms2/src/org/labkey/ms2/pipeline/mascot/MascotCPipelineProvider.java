/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.PipelineController;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * MascotCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
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
        protected void renderView(Object model, PrintWriter out)
        {
            ViewContext context = getViewContext();
            if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
                return;
            StringBuilder html = new StringBuilder();
            html.append("<table><tr><td style=\"font-weight:bold;\">Mascot specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetMascotDefaultsAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for Mascot.</td></tr>");
            ActionURL configMascotURL = new ActionURL(MS2Controller.MascotConfigAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(configMascotURL.getLocalURIString()).append("\">Configure Mascot Server</a>")
                    .append(" - Specify connection information for the Mascot Server.</td></tr>");
            html.append("</table>");
            out.write(html.toString());
        }
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return MascotSearchProtocolFactory.get();
    }

    @Override
    public List<String> getSequenceDbDirList(Container container, File sequenceRoot) throws IOException
    {
        MascotConfig config = ensureMascotConfig(container);

        MascotClientImpl mascotClient = new MascotClientImpl(config.getMascotServer(), null);
        mascotClient.setProxyURL(config.getMascotHTTPProxy());
        List<String> sequenceDBs = mascotClient.getSequenceDbList();

        if (sequenceDBs.isEmpty())
        {
            // TODO: Would be nice if the Mascot client just threw its own connectivity exception.
            String connectivityResult = mascotClient.testConnectivity(false);
            if (!"".equals(connectivityResult))
                throw new IOException(connectivityResult);
        }
        return sequenceDBs;
    }

    @NotNull
    private MascotConfig ensureMascotConfig(Container container) throws IOException
    {
        MascotConfig config = MascotConfig.findMascotConfig(container);
        if (!config.hasMascotServer())
            throw new IOException("Mascot Server has not been configured.");
        return config;
    }
    @Override
    public String getHelpTopic()
    {
        return "pipelineMascot";
    }

    @Override
    public void ensureEnabled(Container container) throws PipelineValidationException
    {
        MascotConfig config = MascotConfig.findMascotConfig(container);
        String mascotServer = config.getMascotServer();
        if ((!config.hasMascotServer() || mascotServer.isEmpty()))
            throw new PipelineValidationException("Mascot server has not been specified in site customization.");
    }
}
