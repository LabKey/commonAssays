package org.labkey.ms2.pipeline;

import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProvider;

/**
 * Created by: jeckels
 * Date: 8/23/15
 */
public abstract class AbstractMS2PipelineProvider<ProtocolFactory extends AbstractFileAnalysisProtocolFactory> extends AbstractFileAnalysisProvider<ProtocolFactory, TaskPipeline>
{
    public AbstractMS2PipelineProvider(String name, Module owningModule)
    {
        super(name, owningModule);
    }

    abstract public void ensureEnabled() throws PipelineValidationException;

    abstract public AbstractMS2SearchProtocolFactory getProtocolFactory();

    abstract public String getHelpTopic();
}
