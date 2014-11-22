/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
import org.labkey.api.gwt.client.ui.HelpPopup;

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
    protected Map<String, List<String>> enzymes = new HashMap<String, List<String>>();
    protected Label enzymeReadOnly = new Label();

    public EnzymeComposite()
    {
        enzymeListBox.setVisibleItemCount(1);
        enzymeReadOnly.setStylePrimaryName("labkey-read-only");
        instance.add(enzymeListBox);
        initWidget(instance);
    }

    public void update(Map<String, List<String>> enzymeMap)
    {
        if(enzymeMap == null) return;
        Set<String> keySet =  enzymeMap.keySet();
        ArrayList<String> sorted = new ArrayList<String>(keySet);
        Collections.sort(sorted);
        enzymeListBox.clear();
        enzymes = enzymeMap;

        for(String name : sorted)
        {
            List<String> value = enzymeMap.get(name);
            // Add just the "canonical" enzyme signature
            enzymeListBox.addItem(name, value.get(0));
        }
        setSelectedEnzymeByName("Trypsin");
    }


    public void setWidth(String width)
    {
        instance.setWidth(width);
        enzymeListBox.setWidth(width);
        enzymeReadOnly.setWidth(width);
    }

    public Widget getLabel()
    {
        Label label = new Label("Enzyme");
        label.setStylePrimaryName(LABEL_STYLE_NAME);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(new HelpPopup("Enzyme", "Short peptides are generated from longer protein sequences by the use of either chemical or enzymatic cleavage. Both types of cleavage tend to have preferred sites of cleavage, based on the residues on either side of the peptide bond to be cleaved. Proteomics experiments frequently use enzymes with very strong sequence specificity, to limit the number of peptides generated and the assure that there are a reasonable number of peptides in the length range most useful for protein identification. <div style=\"font-size: smaller\">Text taken from the <a href=\"http://www.thegpm.org/tandem/api/pcs.html\" target=\"_blank\">X!Tandem documentation.</a></div>"));
        return panel;
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
        try
        {
            new Enzyme(enzymeSignature);
        }
        catch(EnzymeParseException e)
        {
            return e.getMessage();
        }
        return findEnzyme(enzymeSignature);
    }

    private String findEnzyme(String enzymeSignature)
    {
        int enzCount = enzymeListBox.getItemCount();
        boolean foundEnz = false;

        String canonicalSignature = null;

        for (Map.Entry<String, List<String>> entry : enzymes.entrySet())
        {
            if (entry.getValue().contains(enzymeSignature))
            {
                canonicalSignature = entry.getValue().get(0);
                break;
            }
        }
        if(canonicalSignature == null)
            return "The enzyme '" + enzymeSignature + "' was not found.";

        for(int i = 0; i < enzCount; i++)
        {
            try
            {
                String listBoxValue = enzymeListBox.getValue(i);
                // Create to see if it parses
                new Enzyme(listBoxValue);
                if(canonicalSignature.equals(listBoxValue))
                {
                    enzymeListBox.setSelectedIndex(i);
                    foundEnz = true;
                }
            }
            catch(EnzymeParseException e)
            {
                return e.getMessage();
            }
        }
        if(!foundEnz)
            return "The enzyme '" + enzymeSignature + "' was not found.";
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

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        params.setInputParameter(ParameterNames.ENZYME, getSelectedEnzyme());
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        String enzyme = params.getInputParameter(ParameterNames.ENZYME);
        if(enzyme == null || enzyme.equals(""))
        {
            enzyme = getSelectedEnzyme();
            if(enzyme == null || enzyme.equals(""))
            {
                return "";
            }
            else
            {
                try
                {
                    params.setInputParameter(ParameterNames.ENZYME, enzyme);
                }
                catch(SearchFormException e)
                {
                    return "Cannot set the enzyme in XML: " + e.getMessage();
                }
            }
        }
        else if(!enzyme.equals(getSelectedEnzyme()))
        {
            return setSelectedEnzyme(enzyme);
        }
        return "";
    }

    @Override
    public Set<String> getHandledParameterNames()
    {
        return Collections.singleton(ParameterNames.ENZYME);
    }
}
