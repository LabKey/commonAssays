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

package org.labkey.ms2.pipeline.client;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;
import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 24, 2008
 */

/**
 * <code>EnzymeComposite</code>
 */
public class EnzymeComposite extends SearchFormComposite
{
    protected VerticalPanel instance = new VerticalPanel();
    protected ListBox enzymeListBox = new ListBox();
    protected Label enzymeReadOnly = new Label();

    public EnzymeComposite()
    {
        super();
        init();
    }

    public void init()
    {
        enzymeListBox.setVisibleItemCount(1);
        enzymeReadOnly.setStylePrimaryName("labkey-read-only");
        instance.add(enzymeListBox);
        initWidget(instance);
        labelWidget = new Label();
    }

    public void update(Map<String, String> enzymeMap)
    {
        if(enzymeMap == null) return;
        Set<String> keySet =  enzymeMap.keySet();
        ArrayList<String> sorted = new ArrayList<String>(keySet);
        Collections.sort(sorted);
        enzymeListBox.clear();

        for(String name : sorted)
        {
            String value = enzymeMap.get(name);
            enzymeListBox.addItem(name, value);
        }
        setSelectedEnzymeByName("Trypsin");
    }


    public void setWidth(String width)
    {
        instance.setWidth(width);
        enzymeListBox.setWidth(width);
        enzymeReadOnly.setWidth(width);
    }

    public Widget getLabel(String style)
    {
        ((Label)labelWidget).setText("Enzyme");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    public String validate()
    {
        return "";
    }

    public void setName(String s)
    {
        enzymeListBox.setName(s);
    }

    public String getName()
    {
        return enzymeListBox.getName();
    }

    public String getSelectedEnzyme()
    {
        int index = enzymeListBox.getSelectedIndex();
        if(index == -1) return "";
        return enzymeListBox.getValue(index);
    }

    public String setSelectedEnzymeByName(String enzyme)
    {

        int enzCount = enzymeListBox.getItemCount();
        boolean foundEnz = false;
        for(int i = 0; i < enzCount; i++)
        {
            if(enzyme.equals(enzymeListBox.getItemText(i)))
            {
                enzymeListBox.setSelectedIndex(i);
                foundEnz = true;
            }
        }
        if(!foundEnz)
            return "The enzyme '" + enzyme + "' was not found.";
        return "";
    }

    public String setSelectedEnzyme(String enzymeSignature)
    {
        Enzyme enzyme;
        try
        {
            enzyme = new Enzyme(enzymeSignature);
        }
        catch(EnzymeParseException e)
        {
            return e.getMessage();
        }
        return findEnzyme(enzyme);
    }

    private String findEnzyme(Enzyme origCutSite)
    {
        if(origCutSite == null) return "Cut site is equal to null.";
        int enzCount = enzymeListBox.getItemCount();
        boolean foundEnz = false;

        for(int i = 0; i < enzCount; i++)
        {
            Enzyme listCutSite;
            try
            {
                String listBoxValue = enzymeListBox.getValue(i);
                listCutSite = new Enzyme(listBoxValue);
            }
            catch(EnzymeParseException e)
            {
                return e.getMessage();
            }
            if(origCutSite.equals(listCutSite))
            {
                enzymeListBox.setSelectedIndex(i);
                foundEnz = true;
            }
        }
        if(!foundEnz)
            return "The enzyme '" + origCutSite.toString() + "' was not found.";
        return "";
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);

        if(readOnly)
        {
            int index = enzymeListBox.getSelectedIndex();
            if(index != -1)
            {
                String enzymeName = enzymeListBox.getItemText(index);
                enzymeReadOnly.setText(enzymeName);
            }
            else
            {
                enzymeReadOnly.setText(" ");   
            }
            instance.remove(enzymeListBox);
            instance.insert(enzymeReadOnly, 0);
        }
        else
        {
            instance.remove(enzymeReadOnly);
            instance.add(enzymeListBox);
        }
    }

    public void addChangeListener(ChangeHandler changeHandler)
    {
        enzymeListBox.addChangeHandler(changeHandler);
    }
}
