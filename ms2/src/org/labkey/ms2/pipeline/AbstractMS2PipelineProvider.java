package org.labkey.ms2.pipeline;

import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProvider;

/**
 * Shared between search-engine pipeline providers and ones that operate on existing search results.
 * Created by: jeckels
 * Date: 8/23/15
 */
public abstract class AbstractMS2PipelineProvider<ProtocolFactory extends AbstractFileAnalysisProtocolFactory> extends AbstractFileAnalysisProvider<ProtocolFactory, TaskPipeline>
{
    public AbstractMS2PipelineProvider(String name, Module owningModule)
    {
        super(name, owningModule);
    }

    /** @throws org.labkey.api.pipeline.PipelineValidationException if the provider should not be available on the current server */
    abstract public void ensureEnabled() throws PipelineValidationException;

    abstract public AbstractMS2SearchProtocolFactory getProtocolFactory();

    /** @return the name of the help topic that the user can consult for guidance on setting parameters */
    abstract public String getHelpTopic();
}
