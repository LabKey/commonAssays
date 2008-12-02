/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.controllers.well;

import org.labkey.flow.data.FlowWell;
import org.labkey.api.view.ViewFormData;

import java.util.Map;

public class EditWellForm extends ViewFormData
{
    private FlowWell _well;
    public String ff_name;
    public String[] ff_keywordName;
    public String[] ff_keywordValue;
    public String ff_comment;

    public void setWell(FlowWell well) throws Exception
    {
        _well = well;
        if (ff_keywordName == null)
        {
            Map.Entry<String, String>[] entries = well.getKeywords().entrySet().toArray(new Map.Entry[0]);
            ff_keywordName = new String[entries.length];
            ff_keywordValue = new String[entries.length];
            for (int i = 0; i < entries.length; i ++)
            {
                ff_keywordName[i] = entries[i].getKey();
                ff_keywordValue[i] = entries[i].getValue();
            }
        }
        if (ff_comment == null)
        {
            ff_comment = well.getComment();
        }
        if (ff_name == null)
        {
            ff_name = well.getName();
        }
    }

    public FlowWell getWell()
    {
        return _well;
    }

    public void setFf_comment(String comment)
    {
        ff_comment = comment == null ? "" : comment;
    }
    public void setFf_name(String name)
    {
        ff_name = name == null ? "" : name;
    }

    public void setFf_keywordName(String[] names)
    {
        ff_keywordName = names;
    }

    public void setFf_keywordValue(String[] values)
    {
        ff_keywordValue = values;
    }
}
