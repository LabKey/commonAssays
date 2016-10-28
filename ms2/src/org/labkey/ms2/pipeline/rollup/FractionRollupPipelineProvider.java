/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.rollup;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.pipeline.AbstractMS2PipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.PipelineController;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Accepts .xtan.xml files and shows a pipeline action letting the user combine the files into a single
 * set of combined search results.
 * @author jeckels
 */
public class FractionRollupPipelineProvider extends AbstractMS2PipelineProvider
{
    public static final String NAME = "FractionRollup";
    private static final String ACTION_LABEL = "Fraction Rollup Analysis";

    public FractionRollupPipelineProvider(Module owningModule)
    {
        super(NAME, owningModule);
    }

    @Override
    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        String nameParameters = FractionRollupProtocolFactory.get().getParametersFileName();
        return nameParameters.equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    @Override
    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(PipelineController.FractionRollupAction.class, ACTION_LABEL);
        addAction(actionId, PipelineController.FractionRollupAction.class, ACTION_LABEL,
                directory, directory.listFiles(new MS2PipelineManager.XtanXmlFileFilter()), true, true, includeAll);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        String actionId = createActionId(PipelineController.FractionRollupAction.class, ACTION_LABEL);
        return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
    }

    @Override
    public AbstractFileAnalysisProtocolFactory getProtocolFactory(TaskPipeline pipeline)
    {
        return FractionRollupProtocolFactory.get();
    }

    @Override
    public AbstractFileAnalysisProtocolFactory getProtocolFactory(File file)
    {
        return FractionRollupProtocolFactory.get();
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return FractionRollupProtocolFactory.get();
    }

    @Override
    public String getHelpTopic()
    {
        return "pipelineparams";
    }

    @Override
    public boolean isSearch()
    {
        return false;
    }

    @Override
    public boolean dbExists(Container container, File sequenceRoot, String s)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ensureEnabled(Container container) throws PipelineValidationException
    {
        // This is always available
    }
}

