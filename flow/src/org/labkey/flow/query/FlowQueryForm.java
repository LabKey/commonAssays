/*
 * Copyright (c) 2006-2007 LabKey Corporation
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

package org.labkey.flow.query;

import org.labkey.api.query.QueryForm;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;
import org.labkey.api.util.UnexpectedException;

public class FlowQueryForm extends QueryForm
{
    public String getSchemaName()
    {
        return FlowSchema.SCHEMANAME;
    }

    protected FlowSchema createSchema()
    {
        try
        {
            return new FlowSchema(getViewContext());
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }
}
