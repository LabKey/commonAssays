/*
 * Copyright (c) 2005-2015 LabKey Corporation
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

import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

/**
 * @author jeckels
 */
public class FractionRollupProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    private static final FractionRollupProtocolFactory instance = new FractionRollupProtocolFactory();

    public static FractionRollupProtocolFactory get()
    {
        return instance;
    }

    private FractionRollupProtocolFactory()
    {
        // Use the get() function.
    }

    @Override
    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/rollup/FractionRollupDefaults.xml";
    }

    public String getName()
    {
        return "rollup";
    }

    public FractionRollupProtocol createProtocolInstance(String name, String description, String xml)
    {
        return new FractionRollupProtocol(name, description, xml);
    }
}
