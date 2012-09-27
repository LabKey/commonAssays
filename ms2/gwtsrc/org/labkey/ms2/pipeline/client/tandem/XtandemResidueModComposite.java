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

package org.labkey.ms2.pipeline.client.tandem;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import org.labkey.ms2.pipeline.client.ResidueModComposite;
import org.labkey.ms2.pipeline.client.Search;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>XtandemResidueModComposite</code>
 */
public class XtandemResidueModComposite extends ResidueModComposite
{
    public XtandemResidueModComposite(Search searchForm)
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
        staticFlexTable.setWidget(1, 0, newStaticButton);
        dynamicFlexTable.setWidget(0, 0, modDynamicListBox);
        dynamicFlexTable.setWidget(0, 2, dynamicPanel);
        dynamicFlexTable.setWidget(0, 1, addDynamicButton);
        dynamicFlexTable.setWidget(1, 0, newDynamicButton);
        instance.setWidget(modTabPanel);
    }

    public void update(Map mod0Map, Map mod1Map)
    {
        setListBoxMods(mod0Map, modStaticListBox);
        setListBoxMods(mod1Map, modDynamicListBox);
    }

    public Map getModMap(int modType)
    {
        if(modType == STATIC)
            return getListBoxMap(modStaticListBox);
        else if(modType == DYNAMIC)
            return getListBoxMap(modDynamicListBox);
        return null;
    }

    protected String validate(ListBox box, int modType)
    {
        Map<String, String> modMap = getListBoxMap(box);
        ListBox defaultModListBox;
        if(modType == STATIC) defaultModListBox = modStaticListBox;
        else defaultModListBox = modDynamicListBox;

        for(Map.Entry<String, String> entry : modMap.entrySet())
        {
            String error = validateModification(entry.getKey(), entry.getValue(), defaultModListBox);
            if (error != null)
            {
                return error;
            }
        }
        return "";
    }
}
