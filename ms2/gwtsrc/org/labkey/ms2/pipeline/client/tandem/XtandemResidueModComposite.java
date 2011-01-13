/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
        Map modMap = getListBoxMap(box);
        ListBox defaultModListBox;
        if(modType == STATIC) defaultModListBox = modStaticListBox;
        else defaultModListBox = modDynamicListBox;
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
