/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protocol;

import org.apache.log4j.Logger;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.XTandemPipelineJob;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * XTandemSearchProtocol class
 * <p/>
 * Created: Oct 7, 2005
 *
 * @author bmaclean
 */
public class XTandemSearchProtocol extends MS2SearchPipelineProtocol
{
    private static Logger _log = Logger.getLogger(XTandemSearchProtocol.class);

    public XTandemSearchProtocol(String name, String description, String[] dbNames, String xml)
    {
        super(name, description, dbNames, xml);
    }

    public AbstractMS2SearchProtocolFactory getFactory()
    {
        return XTandemSearchProtocolFactory.get();
    }

    public AbstractMS2SearchPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                          File dirSequenceRoot,
                                                          File[] mzXMLFiles,
                                                          File fileParameters,
                                                          boolean fromCluster)
            throws SQLException, IOException
    {
        return new XTandemPipelineJob(info, getName(), dirSequenceRoot, mzXMLFiles, fileParameters, fromCluster);
    }
}
