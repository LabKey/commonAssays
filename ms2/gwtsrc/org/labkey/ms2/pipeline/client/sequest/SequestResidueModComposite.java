/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.ms2.pipeline.client.sequest;

import org.labkey.ms2.pipeline.client.ResidueModComposite;
import org.labkey.ms2.pipeline.client.Search;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;

import java.util.Map;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>SequestResidueModComposite</code>
 */
public class SequestResidueModComposite extends ResidueModComposite
{
    
    public SequestResidueModComposite(Search searchForm)
    {
        super();
        this.searchForm = searchForm;
        init();
    }

    public void init()
    {
        super.init();
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

        public void update(Map<String, String> mod0Map, Map<String, String> mod1Map)
    {
        setListBoxMods(mod0Map, modStaticListBox);
        setListBoxMods(mod1Map, modDynamicListBox);
    }

    public Map<String, String> getModMap(int modType)
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

            for(String modName : modMap.keySet())
            {
                if(find(modName, defaultModListBox) != -1) continue;
                if (modName.charAt(modName.length() - 2) != '@' && modName.length() > 3)
                {
                    return "modification mass contained an invalid value(" + modName + ").";
                }
                char residue = modName.charAt(modName.length() - 1);
                if (!isValidResidue(residue))
                {
                    return "modification mass contained an invalid residue(" + residue + ").";
                }
                String mass = modName.substring(0, modName.length() - 2);

                try
                {
                    Float.parseFloat(mass);
                }
                catch (NumberFormatException e)
                {
                    return "modification mass contained an invalid mass value (" + mass + ")";
                }

            }
            return "";
        }

}
