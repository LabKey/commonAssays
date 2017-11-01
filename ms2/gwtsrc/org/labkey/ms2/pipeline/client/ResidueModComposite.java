/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import com.google.gwt.user.client.ui.*;
import org.labkey.api.gwt.client.ui.HelpPopup;
import org.labkey.api.gwt.client.ui.ImageButton;

import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>ResidueModComposite</code>
 */
public abstract class ResidueModComposite extends SearchFormComposite
{
    protected Search searchForm;
    protected SimplePanel instance = new SimplePanel();
    protected TabPanel modTabPanel = new TabPanel();
    protected FlexTable staticFlexTable = new FlexTable();
    protected FlexTable dynamicFlexTable = new FlexTable();
    protected ListBox modStaticListBox = new ListBox(false);
    protected ListBox modDynamicListBox = new ListBox(false);
    protected ListBox staticListBox = new ListBox();
    protected ListBox dynamicListBox = new ListBox();
    protected HorizontalPanel staticPanel = new HorizontalPanel();
    protected HorizontalPanel dynamicPanel = new HorizontalPanel();
    protected AddButton addStaticButton = new AddButton(STATIC);
    protected AddButton addDynamicButton = new AddButton(DYNAMIC);
    protected DeleteButton deleteDynamicButton = new DeleteButton(DYNAMIC);
    protected DeleteButton deleteStaticButton = new DeleteButton(STATIC);
    protected NewButton newStaticButton = new NewButton(STATIC);
    protected NewButton newDynamicButton = new NewButton(DYNAMIC);
    protected VerticalPanel readOnlyPanel = new VerticalPanel();
    protected Label staticReadOnlyLabel = new Label();
    protected Label dynamicReadOnlyLabel = new Label();
    public  static final int STATIC = 0;
    public  static final int DYNAMIC = 1;
    private static final Map<Character, String> VALID_RESIDUES;

    static
    {
        Map<Character, String> residues = new LinkedHashMap<Character, String>();
        residues.put('A', "A - Alanine");
        residues.put('B', "B - Asparagine");
        residues.put('C', "C - Cysteine");
        residues.put('D', "D - Aspartic acid");
        residues.put('E', "E - Glutamic acid");
        residues.put('F', "F - Phenylalanine");
        residues.put('G', "G - Glycine");
        residues.put('H', "H - Histidine");
        residues.put('I', "I - Isoleucine");
        residues.put('K', "K - Lysine");
        residues.put('L', "L - Leucine");
        residues.put('M', "M - Methionine");
        residues.put('N', "N - Asparagine");
        residues.put('O', "O - Pyrrolysine");
        residues.put('P', "P - Proline");
        residues.put('Q', "Q - Glutamine");
        residues.put('R', "R - Argine");
        residues.put('S', "S - Serine");
        residues.put('T', "T - Threonine");
        residues.put('V', "V - Valine");
        residues.put('W', "W - Tryptophan");
        residues.put('X', "X - All residues");
        residues.put('Y', "Y - Tyrosine");
        residues.put('Z', "Z - Glutamine");
        residues.put(']', "C-Terminus");
        residues.put('[', "N-Terminus");

        VALID_RESIDUES = Collections.unmodifiableMap(residues);
    };


    
    public ResidueModComposite()
    {
        modStaticListBox.setVisibleItemCount(3);
        modStaticListBox.setWidth("250px");
        modDynamicListBox.setVisibleItemCount(3);
        modDynamicListBox.setWidth("250px");
        staticListBox.setWidth("250px");
        staticListBox.setVisibleItemCount(3);
        dynamicListBox.setWidth("250px");
        dynamicListBox.setVisibleItemCount(3);
        staticPanel.add(staticListBox);
        staticPanel.add(deleteStaticButton);
        dynamicPanel.add(dynamicListBox);
        dynamicPanel.add(deleteDynamicButton);
        staticFlexTable.setStylePrimaryName("lk-fields-table");
        modTabPanel.add(staticFlexTable, "Fixed");
        dynamicFlexTable.setStylePrimaryName("lk-fields-table");
        modTabPanel.add(dynamicFlexTable, "Variable");
        modTabPanel.selectTab(0);
        readOnlyPanel.add(staticReadOnlyLabel);
        readOnlyPanel.add(dynamicReadOnlyLabel);
        initWidget(instance);
    }

    public void setWidth(String width)
    {
        StringBuffer num = new StringBuffer();
        StringBuffer type = new StringBuffer();
        StringBuffer endWidth = new StringBuffer();
        StringBuffer centerWidth = new StringBuffer();
        StringBuffer listWidth = new StringBuffer();
        for(int i = 0; i < width.length(); i++)
        {
            char widthChar = width.charAt(i);
            if(Character.isDigit(widthChar))
            {
                num.append(widthChar);
            }
            else
            {
                type.append(widthChar);
            }
        }
        try
        {
            int intWidth = Integer.parseInt(num.toString());
            endWidth.append(Integer.toString((intWidth/9) * 4));
            endWidth.append(type);
            centerWidth.append(Integer.toString(intWidth/9));
            centerWidth.append(type);
            listWidth.append(Integer.toString(((intWidth/9) * 4)-60));
            modTabPanel.setWidth(endWidth.toString());
            staticFlexTable.getColumnFormatter().setWidth(0, endWidth.toString());
            staticFlexTable.getColumnFormatter().setWidth(1, centerWidth.toString());
            staticFlexTable.getColumnFormatter().setWidth(2, endWidth.toString());
            dynamicFlexTable.getColumnFormatter().setWidth(0, endWidth.toString());
            dynamicFlexTable.getColumnFormatter().setWidth(1, centerWidth.toString());
            dynamicFlexTable.getColumnFormatter().setWidth(2, endWidth.toString());
            modStaticListBox.setWidth(endWidth.toString());
            modDynamicListBox.setWidth(endWidth.toString());
            dynamicPanel.setWidth(endWidth.toString());
            staticPanel.setWidth(endWidth.toString());
            staticListBox.setWidth(listWidth.toString());
            dynamicListBox.setWidth(listWidth.toString());
        }
        catch(NumberFormatException e)
        {}
    }

    public Widget getLabel()
    {
        Label label = new Label("Residue modifications");
        label.setStylePrimaryName(LABEL_STYLE_NAME);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(new HelpPopup("Residue modifications",
                "<p>Residue modifications, often referred to as <a href=\"http://en.wikipedia.org/wiki/Posttranslational_modification\" target=\"_blank\">posttranslational modifications</a> (PTM) are the chemical modifications of a protein after its translation. Each modification will alter the mass of its associated amino acid.</p>" +
                "<p><a href=\"http://www.thegpm.org/tandem/api/rmm.html\" target=\"_blank\">Fixed modifications</a> are expected to be present for every amino acid of that type.</p>" +
                "<p><a href=\"http://www.thegpm.org/tandem/api/refpmm.html\" target=\"_blank\">Variable modifications</a> may or may not be present for a given amino acid.</p>" +
                "<p>'[' represents an N-terminus modification, and ']' represents a C-terminus modification.</p>"));
        return panel;
    }

    public String validate()
    {
        String error = validate(staticListBox, STATIC);
        if(error.length()> 0) return error;
        error = validate(dynamicListBox, DYNAMIC);
        if(error.length()> 0) return error;
        Map<String, String> modMap = getStaticMods();
        ArrayList<Character> al = new ArrayList<Character>();

        for(String sig : modMap.values())
        {
            Character res = new Character(sig.charAt(sig.length()-1));
            if(al.contains(res)) return "Two static residue modifications for the same residue.";
            al.add(res);

        }
        return "";
    }

    abstract protected String validate(ListBox box, int modType);

    protected boolean isValidResidue(char res)
    {
        return VALID_RESIDUES.containsKey(res);
    }

    public void clear()
    {
        staticListBox.clear();
        dynamicListBox.clear();
    }

    

    public void setName(String s)
    {
        //not yet
    }

    public String getName()
    {
        return null;  //Not yet
    }

    public Map<String, String> getStaticMods()
    {
        return getListBoxMap(staticListBox);
    }

    public Map<String, String> getDynamicMods()
    {
        return getListBoxMap(dynamicListBox);
    }

    public void setSelectedStaticMods(Map<String, String> staticMods)
    {
        setListBoxMods(staticMods, staticListBox);
    }

    public void setSelectedDynamicMods(Map<String, String> dynamicMods)
    {
        setListBoxMods(dynamicMods, dynamicListBox);
    }

    protected void setListBoxMods(Map<String, String> modMap, ListBox box)
    {
        if(modMap == null) return;
        Set<String> keySet =  modMap.keySet();
        ArrayList<String> sorted = new ArrayList<String>(keySet);
        Collections.sort(sorted);
        box.clear();

        for(Iterator it = sorted.iterator(); it.hasNext();)
        {
            String name = (String)it.next();
            String value = modMap.get(name);
            box.addItem(name, value);
        }
    }

    protected Map<String, String> getListBoxMap(ListBox box)
    {
        Map<String, String> modMap = new HashMap<String, String>();
        int modCount = box.getItemCount();
        for(int i = 0;i < modCount;i++)
        {
            String key = box.getItemText(i);
            String value = box.getValue(i);
            modMap.put(key, value);
        }
        return modMap;
    }

    private class AddButton extends ImageButton
    {
        private int tabIndex;

        AddButton(int tabIndex)
        {
            super("Add>");
            this.tabIndex = tabIndex;
        }

        public void onClick(Widget sender)
        {
            ListBox tabBox = getTabListBox(tabIndex);
            ListBox defaultModListBox = getDefaultsListBox(tabIndex);
            int modIndex = defaultModListBox.getSelectedIndex();
            if(modIndex != -1)
            {
                String text = defaultModListBox.getItemText(modIndex);
                String value = defaultModListBox.getValue(modIndex);
                if(find(text, tabBox) == -1)
                {
                    tabBox.insertItem(text, value, 0);
                }
            }
            String error = searchForm.syncForm2Xml();
            if(error.length() > 0)
            {
                searchForm.clearErrors();
                searchForm.appendError(error);
                searchForm.setSearchButtonEnabled(false);
            }
        }
    }

    public class NewButton extends ImageButton
    {
        private int tabIndex;
        NewButton(int tabIndex)
        {
            super("New");
            this.tabIndex = tabIndex;
        }

        public void onClick(Widget sender)
        {
            new NewModDialogBox(tabIndex);
        }
    }

    private class DeleteButton extends ImageButton
    {
        private int tabIndex;
        DeleteButton(int tabIndex)
        {
            super("Remove");
            this.tabIndex = tabIndex;
        }

        public void onClick(Widget sender)
        {
            ListBox box = getTabListBox(tabIndex);

            int boxIndex = box.getSelectedIndex();
            if(boxIndex != -1)
            {
                box.removeItem(boxIndex);
            }
            String error = searchForm.syncForm2Xml();
            if(error.length() > 0)
            {
                searchForm.clearErrors();
                searchForm.appendError(error);
                searchForm.setSearchButtonEnabled(false);
            }
            else
            {
                searchForm.clearErrors();
                searchForm.setReadOnly(false);
            }
        }
    }

    private class NewModDialogBox
    {
        private TextBox molWt = new TextBox();
        private ListBox residues = new ListBox();
        private DialogBox dialog = new DialogBox();
        private final int tabIndex;

        public NewModDialogBox(int index)
        {
            this.tabIndex = index;
            loadResidues(residues);
            dialog.setText("Create new residue modification");
            FlexTable table = new FlexTable();
            table.setStylePrimaryName("lk-fields-table");
            table.setWidget(0, 0, new Label("Residue"));
            table.setWidget(0, 1, residues);
            table.setWidget(1, 0, new Label("Weight"));
            table.setWidget(1, 1, molWt);
            table.setWidget(2, 0, new ImageButton("Enter"){
                public void onClick(Widget sender)
                {
                    String error = "";
                    String wt = molWt.getText();
                    try
                    {
                        Float.parseFloat(wt);
                    }
                    catch (NumberFormatException e)
                    {
                        error = "modification mass contained an invalid mass value (" + wt + ")";
                        searchForm.clearErrors();
                        searchForm.appendError(error);
                        searchForm.setSearchButtonEnabled(false);
                        molWt.setText("");
                        return;
                    }
                    add2List(tabIndex);
                    searchForm.syncForm2Xml();
                    dialog.hide();
                    dialog = null;
                    error = validate();
                    if(error.length() > 0)
                    {
                        searchForm.clearErrors();
                        searchForm.appendError(error);
                        searchForm.setSearchButtonEnabled(false);
                    }
                    else
                    {
                        searchForm.clearErrors();
                        searchForm.setSearchButtonEnabled(true);
                    }

                }
            });
            table.setWidget(2, 1, new ImageButton("Cancel") {
                public void onClick(Widget sender)
                {
                    dialog.hide();
                    dialog = null;
                    searchForm.clearErrors();
                    searchForm.setSearchButtonEnabled(true);
                }
            });
            dialog.setWidget(table);
            dialog.center();
            molWt.setFocus(true);
        }

        private void add2List(int tabIndex)
        {
            add2List(getTabListBox(tabIndex));
        }

        private void add2List(ListBox box)
        {
            String wt = molWt.getText();
            int index = residues.getSelectedIndex();
            String res = residues.getValue(index);
            StringBuilder sb = new StringBuilder();
            sb.append(wt);
            sb.append("@");
            sb.append(res);
            String mod = sb.toString();
            String name = convertToDisplayValue(mod);
            int foundIndex = find(name, box);
            if(foundIndex == -1)
            {
                box.insertItem(name, mod, 0);
            }
        }

        private void loadResidues(ListBox box)
        {
            for (Map.Entry<Character, String> entry : VALID_RESIDUES.entrySet())
            {
                box.addItem(entry.getValue(), entry.getKey().toString());
            }
        }
    }

//    private int getTabIndex()
//    {
//        DeckPanel deck = modTabPanel.getDeckPanel();
//        return deck.getVisibleWidget();
//    }

    private ListBox getTabListBox(int tabIndex)
    {
        if(tabIndex == STATIC)
            return staticListBox;
        else
            return dynamicListBox;
    }

    private ListBox getDefaultsListBox(int tabIndex)
    {
        if(tabIndex == STATIC)
            return modStaticListBox;
        else
            return modDynamicListBox;
    }

    protected int find(String text, ListBox box)
    {
        if(text == null || box == null) return -1;
        for(int i = 0; i < box.getItemCount(); i++)
        {
            if(text.equals(box.getItemText(i)))
            {
                return i;
            }
        }
        return -1;
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);
        if(readOnly)
        {
            Map<String, String> modsMap = getListBoxMap(staticListBox);
            Set<String> mods = modsMap.keySet();
            StringBuilder sb = new StringBuilder();
            sb.append("Fixed Modifications: ");
            int count = 0;
            for(Iterator it = mods.iterator(); it.hasNext(); count++)
            {
                if(count > 1) sb.append(", ");
                sb.append((String)it.next());

            }
            if (mods.isEmpty())
            {
                sb.append(" <none>");
            }
            staticReadOnlyLabel.setText(sb.toString());
            modsMap = getListBoxMap(dynamicListBox);
            mods = modsMap.keySet();
            sb.delete(0, sb.length());
            sb.append("Variable Modifications: ");
            count = 0;
            for(Iterator it = mods.iterator(); it.hasNext(); count++)
            {
                if(count > 1) sb.append(", ");
                sb.append((String)it.next());
            }
            if (mods.isEmpty())
            {
                sb.append(" <none>");
            }
            dynamicReadOnlyLabel.setText(sb.toString());
            instance.remove(modTabPanel);
            instance.setWidget(readOnlyPanel);
        }
        else
        {
            boolean removed = instance.remove(readOnlyPanel);
            if(removed)
                instance.add(modTabPanel);
        }
    }

    abstract public void update(Map<String, String> mod0Map, Map<String, String> mod1Map);

    abstract public Map<String, String> getModMap(int modType);

    public void setStaticMods(Map<String, String> mods, ParamParser params) throws SearchFormException
    {
        if(mods.size() == 0)
        {
            params.removeInputParameter(ParameterNames.STATIC_MOD);
            return;
        }
        StringBuilder valuesString = new StringBuilder();
        for(String mod : mods.values())
        {
            if(valuesString.length() > 0)
                valuesString.append(",");
            valuesString.append(mod);
        }
        params.setInputParameter(ParameterNames.STATIC_MOD, valuesString.toString());
    }

    public void setDynamicMods(Map<String, String> mods, ParamParser params) throws SearchFormException
    {
        if(mods.size() == 0)
        {
            params.removeInputParameter(ParameterNames.DYNAMIC_MOD);
            return;
        }
        StringBuilder valuesString = new StringBuilder();
        for(String mod : mods.values())
        {
            if(valuesString.length() > 0)
                valuesString.append(",");
            valuesString.append(mod);
        }
        params.setInputParameter(ParameterNames.DYNAMIC_MOD, valuesString.toString());
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        setStaticMods(getStaticMods(), params);
        setDynamicMods(getDynamicMods(), params);
    }

    private Map<String, String> mods2Map(String mods, Map<String, String> knownMods)
    {
        if(knownMods == null || mods == null) return null;
        Map<String, String> returnMap = new HashMap<String, String>();
        if(mods.length() == 0) return returnMap;
        String[] modsArray = mods.split(",");
        List<String> modsList = new ArrayList<String>();
        for (String mod : modsArray)
        {
            String checkMod = mod.trim();
            if (checkMod.length() > 0)
                modsList.add(checkMod);
        }

        for (Map.Entry<String, String> knownModEntry : knownMods.entrySet())
        {
            String[] sites = knownModEntry.getValue().split(",");
            boolean found;
            for (int i = 0; i < sites.length; i++)
            {
                found = false;
                for (String mod : modsList)
                {
                    if (mod.equals(sites[i]))
                    {
                        found = true;
                        if (i == (sites.length - 1))
                        {
                            returnMap.put(knownModEntry.getKey(), knownModEntry.getValue());
                            for (String site : sites)
                            {
                                modsList.remove(site);
                            }
                        }
                        break;
                    }
                }
                if (!found) break;
            }
        }
        for (String mod : modsList)
        {
            returnMap.put(convertToDisplayValue(mod), mod);
        }
        return returnMap;
    }

    private String convertToDisplayValue(String mod)
    {
        if (mod.length() > 2 && mod.charAt(mod.length() - 2) == '@')
        {
            String prefix = mod.substring(0, mod.length() - 1);
            String suffix = mod.substring(mod.length() - 1).toUpperCase();
            if (VALID_RESIDUES.containsKey(suffix.charAt(0)))
            {
                suffix = VALID_RESIDUES.get(suffix.charAt(0));
            }
            return prefix + suffix;
        }
        return mod;
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        Map<String, String> staticMods = mods2Map(params.getInputParameter(ParameterNames.STATIC_MOD), getModMap(ResidueModComposite.STATIC));
        setSelectedStaticMods(staticMods);

        Map<String, String> dynamicMods = mods2Map(params.getInputParameter(ParameterNames.DYNAMIC_MOD), getModMap(DYNAMIC));
        setSelectedDynamicMods(dynamicMods);
        try
        {
           setStaticMods(staticMods, params);
           setDynamicMods(dynamicMods, params);
        }
        catch(SearchFormException e)
        {
          return "Trouble adding residue modification params to input XML.\n" + e.getMessage();
        }

        return validate();
    }

    @Override
    public Set<String> getHandledParameterNames()
    {
        return new HashSet<String>(Arrays.asList(ParameterNames.STATIC_MOD, ParameterNames.DYNAMIC_MOD));
    }

    protected String validateModification(String modName, String modValue, ListBox modListBox)
    {
        if(find(modName, modListBox) != -1) return null;
        if (modValue.length() < 2)
        {
            return "invalid modification specified: '" + modValue + "'";
        }
        if (modValue.charAt(modValue.length() - 2) != '@' && modValue.length() > 3)
        {
            return "modification mass contained an invalid value(" + modValue + ").";
        }
        char residue = modValue.charAt(modValue.length() - 1);
        if (!isValidResidue(residue))
        {
            return "modification mass contained an invalid residue(" + residue + ").";
        }
        String mass = modValue.substring(0, modValue.length() - 2);
        float massF;

        try
        {
            massF = Float.parseFloat(mass);
        }
        catch (NumberFormatException e)
        {
            return "modification mass contained an invalid mass value (" + mass + ")";
        }
        return null;
    }
}
