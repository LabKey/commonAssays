/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.pipeline.*;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.module.Module;
import org.labkey.ms2.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class XTandemPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    public static String name = "X! Tandem";
    private static final String ACTION_LABEL = "X!Tandem Peptide Search";

    public XTandemPipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
        return nameParameters.equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(PipelineController.SearchXTandemAction.class, ACTION_LABEL);
        addAction(actionId, PipelineController.SearchXTandemAction.class, ACTION_LABEL,
                directory, directory.listFiles(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        String actionId = createActionId(PipelineController.SearchXTandemAction.class, ACTION_LABEL);
        return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
    }

    public HttpView getSetupWebPart(Container container)
    {
        return new SetupWebPart();
    }

    private static class SetupWebPart extends WebPartView
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
            html.append("<table><tr><td style=\"font-weight:bold;\">X! Tandem specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetTandemDefaultsAction.class, context.getContainer());  // TODO: Should be method in PipelineController
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for X! Tandem.</td></tr></table>");
            out.write(html.toString());
        }
    }

    public boolean supportsDirectories()
    {
        return true;
    }

    public boolean remembersDirectories()
    {
        return true;
    }

    public boolean hasRemoteDirectories()
    {
        return false;
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return XTandemSearchProtocolFactory.get();
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
        //"X! Tandem does not support Mascot style taxonomy.
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
        return "pipelineXTandem";
    }

    public void ensureEnabled(Container container) throws PipelineValidationException
    {
        // Always enabled.
    }
}
