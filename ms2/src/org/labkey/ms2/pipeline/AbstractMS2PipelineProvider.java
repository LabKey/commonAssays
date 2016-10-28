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
package org.labkey.ms2.pipeline;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProvider;

import java.io.File;
import java.io.IOException;

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

    /** @throws org.labkey.api.pipeline.PipelineValidationException if the provider should not be available on the current server
     * @param container*/
    abstract public void ensureEnabled(Container container) throws PipelineValidationException;

    abstract public AbstractMS2SearchProtocolFactory getProtocolFactory();

    /** @return the name of the help topic that the user can consult for guidance on setting parameters */
    abstract public String getHelpTopic();

    /** @return true if this will be a full-blown search, false if it's operating on already searched-data */
    abstract public boolean isSearch();

    abstract public boolean dbExists(Container container, File sequenceRoot, String dbName) throws IOException;
}
