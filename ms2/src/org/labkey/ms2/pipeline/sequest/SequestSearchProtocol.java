/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

package org.labkey.ms2.pipeline.sequest;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.net.URI;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 11:10:20 AM
 */
public class SequestSearchProtocol extends AbstractMS2SearchProtocol<SequestPipelineJob>
{
    private static Logger _log = Logger.getLogger(SequestSearchProtocol.class);

    public SequestSearchProtocol(String name, String description, String xml)
    {
        super(name, description, xml);
    }

    public AbstractFileAnalysisProtocolFactory getFactory()
    {
        return SequestSearchProtocolFactory.get();
    }

    public SequestPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                File[] filesInput,
                                                File fileParameters
    )
            throws SQLException, IOException
    {
        return new SequestPipelineJob(this, info, getName(), getDirSeqRoot(),
                filesInput, fileParameters);
    }

    public void validate(URI uriRoot) throws PipelineValidationException
    {
        String[] dbNames = getDbNames();
        if(dbNames == null || dbNames.length == 0)
            throw new IllegalArgumentException("A sequence database must be selected.");

        File fileSequenceDB = new File(getDirSeqRoot(), dbNames[0]);
        if (!fileSequenceDB.exists())
            throw new IllegalArgumentException("Sequence database '" + dbNames[0] + "' is not found in local FASTA root.");

        super.validate(uriRoot);
    }
}
