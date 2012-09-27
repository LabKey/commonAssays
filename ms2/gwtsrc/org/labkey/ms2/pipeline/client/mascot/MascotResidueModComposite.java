/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

package org.labkey.ms2.pipeline.client.mascot;

import org.labkey.ms2.pipeline.client.ResidueModComposite;
import org.labkey.ms2.pipeline.client.Search;

import java.util.*;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.FlexTable;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>MascotResidueModComposite</code>
 */
public class MascotResidueModComposite extends ResidueModComposite
{
    public MascotResidueModComposite(Search searchForm)
    {
        this.searchForm = searchForm;
        FlexTable.FlexCellFormatter staticFormatter = staticFlexTable.getFlexCellFormatter();
        staticFormatter.setRowSpan(0, 0, 2);
        staticFormatter.setRowSpan(0, 2, 2);
        FlexTable.FlexCellFormatter dynamicFormatter = dynamicFlexTable.getFlexCellFormatter();
        dynamicFormatter.setRowSpan(0, 0, 2);
        dynamicFormatter.setRowSpan(0, 2, 2);
        staticFlexTable.setWidget(0, 0, modStaticListBox);
        staticFlexTable.setWidget(0, 2, staticPanel);
        staticFlexTable.setWidget(0, 1, addStaticButton);
        dynamicFlexTable.setWidget(0, 0, modDynamicListBox);
        dynamicFlexTable.setWidget(0, 2, dynamicPanel);
        dynamicFlexTable.setWidget(0, 1, addDynamicButton);
        instance.setWidget(modTabPanel);
    }

        public String validate()
    {
        String error = validate(staticListBox);
        if(error.length()> 0) return error;
        error = validate(dynamicListBox);
        return error;
    }

    String validate(ListBox box)
    {
        Map<String, String> modMap = getListBoxMap(box);

        for(String modName : modMap.keySet())
        {
            if(find(modName, modStaticListBox) == -1) return "modification mass contained an invalid value(" + modName + ").";
        }
        return "";
    }

    public Map<String, String> getModMap(int modType)
    {
        return getListBoxMap(modStaticListBox);
    }

    public void update(Map<String, String> mod0Map, Map<String, String> mod1Map)
    {
        setListBoxMods(mod0Map, modStaticListBox);
        //there the same in mascot
        setListBoxMods(mod0Map, modDynamicListBox);
    }

    protected String validate(ListBox box, int modType)
    {
        Map<String, String> modMap = getListBoxMap(box);

        for(Map.Entry<String, String> entry : modMap.entrySet())
        {
            String error = validateModification(entry.getKey(), entry.getValue(), modStaticListBox);
            if (error != null)
            {
                return error;
            }
        }
        return "";
    }
}
