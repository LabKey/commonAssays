/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.flow.webparts;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowProtocolStep;

public class AnalysisScriptTypeColumn extends DataColumn
{
    public AnalysisScriptTypeColumn(ColumnInfo column)
    {
        super(column);
    }

    public boolean isSortable()
    {
        return false;
    }

    public boolean isFilterable()
    {
        return false;
    }

    @Override @NotNull
    public String getFormattedValue(RenderContext ctx)
    {
        Object value = getBoundColumn().getValue(ctx);
        if (!(value instanceof Number))
        {
            return "#ERROR#";
        }
        int id = ((Number) value).intValue();
        FlowScript script = FlowScript.fromScriptId(id);
        if (script == null)
        {
            return "#NOT FOUND#";
        }
        String ret = "";
        String and = "";
        if (script.hasStep(FlowProtocolStep.calculateCompensation))
        {
            ret += and;
            ret += "Compensation";
            and = " and ";
        }
        if (script.hasStep(FlowProtocolStep.analysis))
        {
            ret += and;
            ret += "Analysis";
            and = " and ";
        }
        return ret;
    }
}
