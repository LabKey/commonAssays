/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
package org.labkey.test.ms2.params;

import org.labkey.test.pipeline.PipelineWebTestBase;

/**
 * <code>MS2EmailSuccessParams</code>
 */
public class MS2EmailSuccessParams extends MS2TestParams
{
    public MS2EmailSuccessParams(PipelineWebTestBase test, String dataPath, String protocolName,
                                 String... sampleNames)
    {
        super(test, dataPath, protocolName, sampleNames);
    }

    public void validate()
    {
        validateEmailSuccess();
    }
}
