package org.labkey.elispot.pipeline;

import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Jun 14, 2012
 */
public class ElispotPipelineProvider extends PipelineProvider
{
    public static final String NAME = "ElispotPipelineProvider";

    public ElispotPipelineProvider(Module parent)
    {
        super(NAME, parent);
    }

    @Override
    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
    }
}
