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

import org.labkey.ms2.pipeline.BioMLInputParser;
import org.labkey.ms2.pipeline.XTandemInputParser;

import java.io.File;

/**
 * XTandemSearchProtocolFactory class
 * <p/>
 * Created: Oct 7, 2005
 *
 * @author bmaclean
 */
public class XTandemSearchProtocolFactory extends AbstractMS2SearchProtocolFactory<XTandemSearchProtocol>
{
    public static XTandemSearchProtocolFactory instance = new XTandemSearchProtocolFactory();

    public static XTandemSearchProtocolFactory get()
    {
        return instance;
    }

    public String getName()
    {
        return "xtandem";
    }

    public String getParametersFileName()
    {
        return "tandem.xml";
    }

    public String getDefaultParametersFileName()
    {
        return "default_input.xml";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/XTandemDefaults.xml";
    }

    public BioMLInputParser createInputParser()
    {
        return new XTandemInputParser();
    }

    public XTandemSearchProtocol createProtocolInstance(String name, String description, String[] dbNames, String xml)
    {
        return new XTandemSearchProtocol(name, description, dbNames, xml);
    }

    public XTandemSearchProtocol createProtocolInstance(String name, String description, File dirSeqRoot,
                                                        String dbPath, String[] dbNames, String xml)
    {
        if (dbPath != null && dbPath.length() > 0)
        {
            String[] dbPaths = new String[dbNames.length];
            for (int i = 0; i < dbNames.length; i++)
                dbPaths[i] = dbPath + dbNames[i];
            dbNames = dbPaths;
        }

        return createProtocolInstance(name, description, dbNames, xml);
    }

    protected XTandemSearchProtocol createProtocolInstance(BioMLInputParser parser)
    {
        parser.removeInputParameter("protein, taxon");

        return super.createProtocolInstance(parser);
    }
}
