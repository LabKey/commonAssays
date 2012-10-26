/*
 * Copyright (c) 2005-2012 LabKey Corporation
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

import org.labkey.api.pipeline.ParamParser;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

/**
 * XTandemSearchProtocolFactory class
 * <p/>
 * Created: Oct 7, 2005
 *
 * @author bmaclean
 */
public class XTandemSearchProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    private static final XTandemSearchProtocolFactory instance = new XTandemSearchProtocolFactory();

    public static XTandemSearchProtocolFactory get()
    {
        return instance;
    }

    private XTandemSearchProtocolFactory()
    {
        // Use the get() function.
    }

    public String getName()
    {
        return "xtandem";
    }

    public String getParametersFileName()
    {
        return "tandem.xml";
    }

    public String getLegacyDefaultParametersFileName()
    {
        return "default_input.xml";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/tandem/XTandemDefaults.xml";
    }

    public XTandemSearchProtocol createProtocolInstance(String name, String description, String xml)
    {
        return new XTandemSearchProtocol(name, description, xml);
    }

    protected AbstractMS2SearchProtocol createProtocolInstance(ParamParser parser)
    {
        parser.removeInputParameter("protein, taxon");

        return super.createProtocolInstance(parser);
    }
}
