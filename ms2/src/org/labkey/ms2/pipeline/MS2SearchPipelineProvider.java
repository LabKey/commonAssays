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

import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.ms2.protocol.MS2SearchPipelineProtocol;
import org.labkey.ms2.protocol.AbstractMS2SearchProtocolFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * <code>MS2SearchPipelineProvider</code>
 */
public interface MS2SearchPipelineProvider
{
    String getName();

    void ensureEnabled() throws PipelineValidationException;

    AbstractMS2SearchProtocolFactory getProtocolFactory();

    Map<String, String[]> getSequenceFiles(URI sequenceRoot) throws IOException;

    String getHelpTopic();
}
