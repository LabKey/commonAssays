/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: 9/16/13
 */
public class CometSearchProtocol extends AbstractMS2SearchProtocol<CometPipelineJob>
{
    public CometSearchProtocol(String name, String description, String xml)
    {
        super(name, description, xml);
    }

    public AbstractFileAnalysisProtocolFactory getFactory()
    {
        return CometSearchProtocolFactory.get();
    }

    public CometPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                              PipeRoot root, List<File> filesInput,
                                              File fileParameters, @Nullable Map<String, String> variableMap
    ) throws IOException
    {
        return new CometPipelineJob(this, info, root, getName(), getDirSeqRoot(),
                filesInput, fileParameters);
    }

    public void validate(PipeRoot root) throws PipelineValidationException
    {
        String[] dbNames = getDbNames();
        if(dbNames == null || dbNames.length == 0)
            throw new IllegalArgumentException("A sequence database must be selected.");

        File fileSequenceDB = new File(getDirSeqRoot(), dbNames[0]);
        if (!fileSequenceDB.exists())
            throw new IllegalArgumentException("Sequence database '" + dbNames[0] + "' is not found in local FASTA root.");

        super.validate(root);
    }
}
