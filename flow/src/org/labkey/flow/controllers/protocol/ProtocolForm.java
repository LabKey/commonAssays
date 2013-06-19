/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.flow.query.FlowSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProtocolForm extends ViewForm
{
    private FlowProtocol _protocol;

    public FlowProtocol getProtocol() throws UnauthorizedException
    {
        if (_protocol != null)
            return _protocol;
        _protocol = FlowProtocol.fromURL(getUser(), getViewContext().getActionURL(), getRequest());
        return _protocol;
    }

    public Map<FieldKey, String> getKeywordFieldMap()
    {
        Map<FieldKey, String> options = new LinkedHashMap<>();
        options.put(FieldKey.fromParts("Name"), "FCS file name");
        options.put(FieldKey.fromParts("Run", "Name"), "Run name");
        FlowSchema schema = new FlowSchema(getUser(), getContainer());
        ExpDataTable table = schema.createFCSFileTable(null);
        FlowPropertySet fps = new FlowPropertySet(table);
        FieldKey keyKeyword = FieldKey.fromParts("Keyword");
        for (String keyword : fps.getVisibleKeywords())
        {
            options.put(new FieldKey(keyKeyword, keyword), keyword);
        }
        return options;
    }
}
