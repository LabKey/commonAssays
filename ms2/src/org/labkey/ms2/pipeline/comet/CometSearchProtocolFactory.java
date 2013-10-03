/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

/**
 * User: jeckels
 * Date: 9/16/13
 */
public class CometSearchProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    private static final CometSearchProtocolFactory INSTANCE = new CometSearchProtocolFactory();

    public static CometSearchProtocolFactory get()
    {
        return INSTANCE;
    }

    private CometSearchProtocolFactory()
    {
        // Use the get() function.
    }

    public String getName()
    {
        return "comet";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/comet/CometDefaults.xml";
    }

    public AbstractMS2SearchProtocol createProtocolInstance(String name, String description, String xml)
    {
        return new CometSearchProtocol(name, description, xml);
    }
}
