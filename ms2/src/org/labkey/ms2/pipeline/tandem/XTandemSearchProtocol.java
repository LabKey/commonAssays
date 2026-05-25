/*
 * Copyright (c) 2007-2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.vfs.FileLike;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * XTandemSearchProtocol class
 * <p/>
 * Created: Oct 7, 2005
 *
 * @author bmaclean
 */
public class XTandemSearchProtocol extends AbstractMS2SearchProtocol<XTandemPipelineJob>
{
    public XTandemSearchProtocol(String name, String description, String xml, Container container)
    {
        super(name, description, xml, container);
    }

    @Override
    public XTandemSearchProtocolFactory getFactory()
    {
        return XTandemSearchProtocolFactory.get();
    }

    @Override
    public XTandemPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                PipeRoot root, List<FileLike> filesInput,
                                                FileLike fileParameters, @Nullable Map<String, String> variableMap
    ) throws IOException
    {
        return new XTandemPipelineJob(this, info, root, getName(),
                filesInput, fileParameters);
    }
}
