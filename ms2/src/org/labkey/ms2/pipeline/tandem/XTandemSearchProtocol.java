/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.net.URI;

/**
 * XTandemSearchProtocol class
 * <p/>
 * Created: Oct 7, 2005
 *
 * @author bmaclean
 */
public class XTandemSearchProtocol extends AbstractMS2SearchProtocol<XTandemPipelineJob>
{
    private static Logger _log = Logger.getLogger(XTandemSearchProtocol.class);

    public XTandemSearchProtocol(String name, String description, String xml)
    {
        super(name, description, xml);
    }

    public AbstractFileAnalysisProtocolFactory getFactory()
    {
        return XTandemSearchProtocolFactory.get();
    }

    public XTandemPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                File[] filesInput,
                                                File fileParameters
    )
            throws IOException
    {
        return new XTandemPipelineJob(this, info, getName(), getDirSeqRoot(),
                filesInput, fileParameters);
    }

    public void validate(URI uriRoot) throws PipelineValidationException
    {
        super.validate(uriRoot);
    }
}
