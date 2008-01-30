/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.AbstractFileAnalysisProvider;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.pipeline.TaskPipeline;

import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.util.Map;

/**
 * <code>AbstractMS2SearchPipelineProvider</code>
 */
abstract public class AbstractMS2SearchPipelineProvider
        extends AbstractFileAnalysisProvider<AbstractMS2SearchProtocolFactory>
{
    public AbstractMS2SearchPipelineProvider(String name)
    {
        super(name);
    }

    public void initSystemDirectory(File rootDir, File systemDir)
    {
        AbstractMS2SearchProtocolFactory factory = getProtocolFactory();
        if (factory != null)
            factory.initSystemDirectory(rootDir, systemDir);
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory(TaskPipeline pipeline)
    {
        // MS2 search providers all support only one protocol factory each.
        return getProtocolFactory();
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory(File fileParams)
    {
        AbstractMS2SearchProtocolFactory factory = getProtocolFactory();
        if (factory.isProtocolTypeFile(fileParams))
            return factory;
        return null; 
    }

    abstract public AbstractMS2SearchProtocolFactory getProtocolFactory();

    abstract public void ensureEnabled() throws PipelineProtocol.PipelineValidationException;

    abstract public Map<String, String[]> getSequenceFiles(URI sequenceRoot) throws IOException;

    abstract public String getHelpTopic();
}
