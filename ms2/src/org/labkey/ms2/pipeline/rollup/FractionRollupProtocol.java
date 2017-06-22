/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.tandem.XTandemSearchTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author jeckels
 */
public class FractionRollupProtocol extends AbstractMS2SearchProtocol<FractionRollupPipelineJob>
{
    public FractionRollupProtocol(String name, String description, String xml)
    {
        super(name, description, xml);
    }

    public AbstractFileAnalysisProtocolFactory getFactory()
    {
        return FractionRollupProtocolFactory.get();
    }

    public FractionRollupPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                PipeRoot root, List<File> filesInput,
                                                File fileParameters, @Nullable Map<String, String> variableMap
    ) throws IOException
    {
        return new FractionRollupPipelineJob(this, info, root, getName(),
                filesInput, fileParameters);
    }

    @Override
    public List<FileType> getInputTypes()
    {
        return Collections.singletonList(XTandemSearchTask.getNativeFileType(FileType.gzSupportLevel.SUPPORT_GZ));
    }

    @Override
    public void validate(PipeRoot root) throws PipelineValidationException
    {
        validateProtocolName();
    }
}
