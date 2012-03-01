package org.labkey.ms2.protein;

import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.ViewContext;

/**
 * User: jeckels
 * Date: Feb 29, 2012
 */
public class ProteinAnnotationPipelineProvider extends PipelineProvider
{

    public static final String NAME = "ProteinAnnotation";

    public ProteinAnnotationPipelineProvider(Module module)
    {
        super(NAME, module);
    }

    @Override
    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {

    }
}
