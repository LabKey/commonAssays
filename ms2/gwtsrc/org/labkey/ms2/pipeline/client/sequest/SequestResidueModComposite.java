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
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

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
        SimplePanel mod0WrapperPanel = new SimplePanel();
        SimplePanel mod1WrapperPanel = new SimplePanel();
        mod0WrapperPanel.add(mod0ListBox);
        mod1WrapperPanel.add(mod1ListBox);
        FlexTable.FlexCellFormatter formatter = modFlexTable.getFlexCellFormatter();
        formatter.setRowSpan(0, 0, 2);
        formatter.setRowSpan(0, 2, 2);
        modsDeckPanel.add(mod0WrapperPanel);
        modsDeckPanel.add(mod1WrapperPanel);
        modsDeckPanel.showWidget(0);
        modFlexTable.setWidget(0, 0, modsDeckPanel);
        modFlexTable.setWidget(0, 2, modTabPanel);
        modFlexTable.setWidget(0, 1, addButton);
        modFlexTable.setWidget(1, 0, newButton);
        instance.setWidget(modFlexTable);
    }

        public void update(Map mod0Map, Map mod1Map)
    {
        setListBoxMods(mod0Map, mod0ListBox);
        setListBoxMods(mod1Map, mod1ListBox);
    }

    public Map getModMap(int modType)
    {
        if(modType == STATIC)
            return getListBoxMap(mod0ListBox);
        else if(modType == DYNAMIC)
            return getListBoxMap(mod1ListBox);
        return null;
    }
    
    protected String validate(ListBox box, int modType)
        {
            Map modMap = getListBoxMap(box);
            ListBox defaultModListBox;
            if(modType == STATIC) defaultModListBox = mod0ListBox;
            else defaultModListBox = mod1ListBox;
            Set keys = modMap.keySet();

            for(Iterator it = keys.iterator(); it.hasNext();)
            {
                String modName = (String)it.next();
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
                float massF;

                try
                {
                    massF = Float.parseFloat(mass);
                }
                catch (NumberFormatException e)
                {
                    return "modification mass contained an invalid mass value (" + mass + ")";
                }

            }
            return "";
        }

}
