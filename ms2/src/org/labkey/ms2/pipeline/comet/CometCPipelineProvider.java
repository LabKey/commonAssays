/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
package org.labkey.ms2.pipeline.comet;

import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * CometCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class CometCPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    public static String name = "Comet";

    public CometCPipelineProvider()
    {
        super(name);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        if ("comet.def".equals(name))
            return true;

        return super.isStatusViewableFile(container, name, basename);
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
        // Never actually create a protocol based job.
        return null;
    }

    public void ensureEnabled() throws PipelineProtocol.PipelineValidationException
    {
        // Nothing to do.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public String getHelpTopic()
    {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public List<String> getSequenceDbPaths(URI sequenceRoot) throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public List<String> getSequenceDbDirList(URI sequenceRoot) throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public List<String> getTaxonomyList() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support Mascot style taxonomy.");
    }

    public Map<String, String> getEnzymes() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public Map<String, String> getResidue0Mods() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public Map<String, String> getResidue1Mods() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }
}
