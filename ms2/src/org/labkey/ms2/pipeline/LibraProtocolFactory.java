/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;

/**
 * User: jeckels
 * Date: May 2, 2011
 */
public class LibraProtocolFactory extends AbstractFileAnalysisProtocolFactory
{
    @Override
    public AbstractFileAnalysisProtocol createProtocolInstance(String name, String description, String xml)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return "libra";
    }
}
