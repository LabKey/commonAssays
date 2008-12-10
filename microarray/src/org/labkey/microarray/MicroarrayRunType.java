/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

/**
 * User: jeckels
* Date: Feb 7, 2008
*/
public class MicroarrayRunType extends ExperimentRunType
{
    public static final MicroarrayRunType INSTANCE = new MicroarrayRunType();

    private MicroarrayRunType()
    {
        super("Microarray", MicroarraySchema.SCHEMA_NAME, MicroarraySchema.TABLE_RUNS);
    }

    public Priority getPriority(ExpProtocol protocol)
    {
        if (MicroarrayAssayProvider.PROTOCOL_PREFIX.equals(new Lsid(protocol.getLSID()).getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
