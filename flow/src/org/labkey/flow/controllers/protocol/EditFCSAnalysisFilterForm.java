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

package org.labkey.flow.controllers.protocol;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.UnauthorizedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditFCSAnalysisFilterForm extends ProtocolForm
{
    public FieldKey[] ff_field;
    public String[] ff_op;
    public String[] ff_value;

    public void init() throws UnauthorizedException
    {
        List<FieldKey> fields = new ArrayList<>();
        List<String> ops = new ArrayList<>();
        List<String> values = new ArrayList<>();
        String prop = getProtocol().getFCSAnalysisFilterString();
        for (Map.Entry<String, String> entry : PageFlowUtil.fromQueryString(prop))
        {
            String[] parts = StringUtils.split(entry.getKey(), "~");
            if (parts.length != 2)
            {
                continue;
            }
            fields.add(FieldKey.fromString(parts[0]));
            ops.add(parts[1]);
            values.add(entry.getValue());
        }
        ff_field = fields.toArray(new FieldKey[fields.size()]);
        ff_op = ops.toArray(new String[ops.size()]);
        ff_value = values.toArray(new String[ops.size()]);
    }

    public void setFf_field(String[] fields)
    {
        ff_field = new FieldKey[fields.length];
        for (int i = 0; i < fields.length; i ++)
        {
            if (StringUtils.isEmpty(fields[i]))
                continue;
            ff_field[i] = FieldKey.fromString(fields[i]);
        }
    }

    public void setFf_op(String[] ops)
    {
        ff_op = ops;
    }
    public void setFf_value(String[] values)
    {
        ff_value = values;
    }

    public String getFilterValue()
    {
        List<String> clauses = new ArrayList();
        for (int i = 0; i < ff_field.length; i ++)
        {
            if (ff_field[i] == null)
                continue;
            String clause = PageFlowUtil.encode(ff_field[i].toString()) + "~" + ff_op[i] + "=" + PageFlowUtil.encode(ff_value[i]);
            clauses.add(clause);
        }
        if (clauses.size() == 0)
            return null;
        return StringUtils.join(clauses.iterator(), "&");
    }
}
