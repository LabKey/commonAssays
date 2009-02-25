/*
 * Copyright (c) 2009 LabKey Corporation
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

import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowScript;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ActionURL;

/**
 * User: kevink
 * Date: Feb 19, 2009
 */
public class RunsForm extends FlowQueryForm
{
    private FlowExperiment _experiment;
    private FlowScript _script;
    private int _experimentId;
    private int _scriptId;

    public int getExperimentId()
    {
        return _experimentId;
    }

    public void setExperimentId(int experimentId)
    {
        _experimentId = experimentId;
    }

    public int getScriptId()
    {
        return _scriptId;
    }

    public void setScriptId(int scriptId)
    {
        _scriptId = scriptId;
    }

    protected FlowSchema createSchema()
    {
        FlowSchema ret = (FlowSchema)super.createSchema();
        ret.setExperiment(getExperiment());
//        ret.setScript(getScript());
        return ret;
    }

    public FlowExperiment getExperiment()
    {
        if (null == _experiment && 0 != _experimentId)
            _experiment = FlowExperiment.fromExperimentId(_experimentId);
        return _experiment;
    }

    public FlowScript getScript()
    {
        if (null == _script && 0 != _scriptId)
            _script = FlowScript.fromScriptId(_scriptId);
        return _script;
    }

    public QuerySettings createQuerySettings(UserSchema schema)
    {
        QuerySettings ret = super.createQuerySettings(schema);
        if (ret.getQueryName() == null)
        {
            ret.setQueryName(FlowTableType.Runs.toString());
        }

        // adding the filter doesn't work because of bug 6520
        // 6520 : can't set filter/sort on QuerySettings progamatically
        if (getScript() != null)
        {
            ActionURL sortFilter = getScript().getRunsUrl(ret.getSortFilterURL());
            ret.setSortFilterURL(sortFilter);
        }

        return ret;
    }

}
