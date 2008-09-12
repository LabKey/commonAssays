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

import com.google.gwt.user.client.ui.*;
import org.labkey.api.gwt.client.ui.ImageButton;

import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 30, 2008
 */

/**
 * <code>ResidueModComposite</code>
 */
public abstract class ResidueModComposite extends SearchFormComposite implements SourcesClickEvents
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
    char[] validResidues = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N','O', 'P', 'Q', 'R', 'S', 'T', 'V', 'W','X', 'Y', 'Z',']','['};


    
    public ResidueModComposite()
    {
        super();
    }

    public void init()
    {
        modStaticListBox.setVisibleItemCount(3);
        modDynamicListBox.setVisibleItemCount(3);
        staticListBox.setVisibleItemCount(3);
        dynamicListBox.setVisibleItemCount(3);
        staticPanel.add(staticListBox);
        staticPanel.add(deleteStaticButton);
        dynamicPanel.add(dynamicListBox);
        dynamicPanel.add(deleteDynamicButton);
        modTabPanel.add(staticFlexTable, "Fixed");
        modTabPanel.add(dynamicFlexTable, "Variable");
        modTabPanel.selectTab(0);
        readOnlyPanel.add(staticReadOnlyLabel);
        readOnlyPanel.add(dynamicReadOnlyLabel);
        labelWidget = new Label();         
        initWidget(instance);
    }

    public void setWidth(String width)
    {
        int intWidth = 0;
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
            intWidth = Integer.parseInt(num.toString());
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

    public Widget getLabel(String style)
    {
        ((Label)labelWidget).setText("Residue modifications:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    public String validate()
    {
        String error = validate(staticListBox, STATIC);
        if(error.length()> 0) return error;
        error = validate(dynamicListBox, DYNAMIC);
        if(error.length()> 0) return error;
        Map modMap = getStaticMods();
        Collection values = modMap.values();
        ArrayList al = new ArrayList();

        for(Iterator it = values.iterator();it.hasNext(); )
        {
            String sig = (String)it.next();
            Character res = new Character(sig.charAt(sig.length()-1));
            if(al.contains(res)) return "Two static residue modifications for the same residue.";
            al.add(res);

        }
        return "";
    }

    abstract protected String validate(ListBox box, int modType);

    protected boolean isValidResidue(char res)
    {
        for(int i = 0; i < validResidues.length; i++)
        {
            if(res == validResidues[i]) return true;
        }
        return false;
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

    public void addClickListener(ClickListener clickListener) {
        //Not needed
    }

    public void removeClickListener(ClickListener clickListener) {
        //Not needed.
    }

    public Map getStaticMods()
    {
        return getListBoxMap(staticListBox);
    }

    public Map getDynamicMods()
    {
        return getListBoxMap(dynamicListBox);
    }

    public void setSelectedStaticMods(Map staticMods)
    {
        setListBoxMods(staticMods, staticListBox);
    }

    public void setSelectedDynamicMods(Map dynamicMods)
    {
        setListBoxMods(dynamicMods, dynamicListBox);
    }

    protected void setListBoxMods(Map modMap, ListBox box)
    {
        if(modMap == null) return;
        Set keySet =  modMap.keySet();
        ArrayList sorted = new ArrayList(keySet);
        Collections.sort(sorted);
        box.clear();

        for(Iterator it = sorted.iterator(); it.hasNext();)
        {
            String name = (String)it.next();
            String value = (String)modMap.get(name);
            box.addItem(name, value);
        }
    }

    protected Map getListBoxMap(ListBox box)
    {
        Map modMap = new HashMap();
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
                searchForm.clearDisplay();
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
                searchForm.clearDisplay();
                searchForm.appendError(error);
                searchForm.setSearchButtonEnabled(false);
            }
            else
            {
                searchForm.clearDisplay();
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
                        searchForm.clearDisplay();
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
                        searchForm.clearDisplay();
                        searchForm.appendError(error);
                        searchForm.setSearchButtonEnabled(false);
                    }
                    else
                    {
                        searchForm.clearDisplay();
                        searchForm.setSearchButtonEnabled(true);
                    }

                }
            });
            table.setWidget(2, 1, new ImageButton("Cancel") {
                public void onClick(Widget sender)
                {
                    dialog.hide();
                    dialog = null;
                    searchForm.clearDisplay();
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
            String res = residues.getItemText(index);
            StringBuffer sb = new StringBuffer();
            sb.append(wt);
            sb.append("@");
            sb.append(res);
            int foundIndex = find(sb.toString(), box);
            if(foundIndex == -1)
            {
                box.insertItem(sb.toString(), sb.toString(), 0);
            }
        }

        private void loadResidues(ListBox box)
        {
            for(int i = 0; i < validResidues.length; i++ )
            {
                box.addItem(Character.toString(validResidues[i]));
            }
            box.addItem("A");
            box.addItem("B");
            box.addItem("C");
            box.addItem("D");
            box.addItem("E");
            box.addItem("F");
            box.addItem("G");
            box.addItem("H");
            box.addItem("I");
            box.addItem("K");
            box.addItem("L");
            box.addItem("M");
            box.addItem("N");
            box.addItem("O");
            box.addItem("P");
            box.addItem("Q");
            box.addItem("R");
            box.addItem("S");
            box.addItem("T");
            box.addItem("V");
            box.addItem("W");
            box.addItem("X");
            box.addItem("Y");
            box.addItem("Z");
            box.addItem("[");
            box.addItem("]");
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
            Map modsMap = getListBoxMap(staticListBox);
            Set mods = modsMap.keySet();
            StringBuffer sb = new StringBuffer();
            sb.append("Fixed Modifications: ");
            int count = 0;
            for(Iterator it = mods.iterator(); it.hasNext(); count++)
            {
                if(count > 1) sb.append(", ");
                sb.append((String)it.next());

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

    abstract public void update(Map mod0Map, Map mod1Map);

    abstract public Map getModMap(int modType);

}
