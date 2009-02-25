/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

package org.labkey.flow.controllers.run;

import org.labkey.flow.data.*;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowSchema;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;

public class RunForm extends FlowQueryForm
{
    private int _runid = 0;
    private FlowRun _run = null;

    public void setRunId(int id)
    {
        if (id != _runid)
            _run = null;
        _runid = id;
    }

    public int getRunId()
    {
        return _runid;
    }

    protected FlowSchema createSchema()
    {
        FlowSchema ret = (FlowSchema)super.createSchema();
        ret.setRun(getRun());
        return ret;
    }

    public FlowRun getRun()
    {
        if (null == _run && 0 != _runid)
            _run = FlowRun.fromRunId(_runid);
        return _run;
    }

    public QuerySettings createQuerySettings(UserSchema schema)
    {
        QuerySettings ret = super.createQuerySettings(schema);
        if (ret.getQueryName() == null && getRun() != null)
        {
            String queryName = getRun().getDefaultQuery().toString();
            ret.setQueryName(queryName);
        }
        return ret;
    }
}
