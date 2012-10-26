/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 10:59:40 AM
 */
public class SequestSearchProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    private static final SequestSearchProtocolFactory instance = new SequestSearchProtocolFactory();

    public static SequestSearchProtocolFactory get()
    {
        return instance;
    }

    private SequestSearchProtocolFactory()
    {
        // Use the get() function.
    }

    public String getName()
    {
        return "sequest";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/sequest/SequestDefaults.xml";
    }

    public AbstractMS2SearchProtocol createProtocolInstance(String name, String description, String xml)
    {
        return new SequestSearchProtocol(name, description, xml);
    }

}
