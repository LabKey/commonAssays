/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowWell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditWellForm extends ViewForm
{
    private List<FlowWell> _wells = new ArrayList<>();
    private FlowWell _well;
    public String ff_name;
    public String[] ff_keywordName;
    public String[] ff_keywordValue;
    public String[] ff_keywordError;
    public String ff_comment;
    public String editWellReturnUrl;
    public boolean ff_isBulkEdit;

    public void setWell(FlowWell well)
    {
        _well = well;
        if (well != null)
        {
            if (ff_keywordName == null)
            {
                Map.Entry<String, String>[] entries = well.getKeywords().entrySet().toArray(new Map.Entry[0]);
                ff_keywordName = new String[entries.length];
                ff_keywordValue = new String[entries.length];
                ff_keywordError = new String[entries.length];
                for (int i = 0; i < entries.length; i ++)
                {
                    ff_keywordName[i] = entries[i].getKey();
                    ff_keywordValue[i] = entries[i].getValue();
                    ff_keywordError[i] = null;
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
    }

    public FlowWell getWell()
    {
        return _well;
    }


    public void setWells(List<FlowWell> wells, boolean isBulkEdit){
        _wells = wells;
        if (wells != null && wells.size() > 0)
        {
            setFormKeywords(wells);

            if (!isBulkEdit)
            {
                if (ff_comment == null)
                {
                    ff_comment = wells.get(0).getComment();
                }
                if (ff_name == null)
                {
                    ff_name = wells.get(0).getName();
                }
            }
        }
    }

    private void setFormKeywords(List<FlowWell> wells)
    {
        if (ff_keywordName == null)
        {
            Map.Entry<String, String>[] entries = wells.get(0).getKeywords().entrySet().toArray(new Map.Entry[0]);
            ff_keywordName = new String[entries.length];
            ff_keywordValue = new String[entries.length];
            ff_keywordError = new String[entries.length];
            for (int i = 0; i < entries.length; i++)
            {
                ff_keywordName[i] = entries[i].getKey();
                if(!ff_isBulkEdit)
                {
                    ff_keywordValue[i] = entries[i].getValue();
                }
            }
        }
    }

    public List<FlowWell> getWells()
    {
        return _wells;
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
    public void setFf_keywordError(String[] values)
    {
        ff_keywordError = values;
    }

    public void setFf_isBulkEdit(boolean ff_isBulkEdit)
    {
        this.ff_isBulkEdit = ff_isBulkEdit;
    }
}
