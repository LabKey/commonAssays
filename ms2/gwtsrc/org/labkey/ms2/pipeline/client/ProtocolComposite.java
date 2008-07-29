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

import java.util.*;

import org.labkey.api.gwt.client.util.StringUtils;

/**
 * User: billnelson@uky.edu
 * Date: Mar 25, 2008
 */
public class ProtocolComposite extends SearchFormComposite implements SourcesChangeEvents
{
    private VerticalPanel instance = new VerticalPanel();
    private ListBox protocolListBox = new ListBox();
    private TextBox protocolNameTextBox = new TextBox();
    private TextArea protocolDescTextArea = new TextArea();
    private Hidden protocolNameHidden = new Hidden();
    private HTML protocolDescHtml = new HTML();
    private Hidden protocolDescHidden = new Hidden();
    Label listBoxLabel;
    Label textBoxLabel;
    Label descriptionLabel;
    private static final String NEW_PROTOCOL = "<new protocol>";


    public ProtocolComposite()
    {
        super();
        init();
    }

    public void init()
    {
        protocolListBox.setVisibleItemCount(1);
        protocolDescHtml.setWordWrap(true);
        protocolDescHtml.setStylePrimaryName("labkey-read-only");
        instance.add(protocolListBox);
        instance.add(protocolNameTextBox);
        instance.add(protocolDescTextArea);
        initLabel();
        initWidget(instance);
    }

    private void initLabel()
    {
        listBoxLabel = new Label("Analysis protocols:");
        listBoxLabel.addStyleName("labkey-list-box");
        textBoxLabel = new Label("Protocol name:");
        descriptionLabel = new Label("Protocol description:");
        labelWidget = new VerticalPanel();
        ((VerticalPanel)labelWidget).add(listBoxLabel);
        ((VerticalPanel)labelWidget).add(textBoxLabel);
        ((VerticalPanel)labelWidget).add(descriptionLabel);
    }

    public Widget getLabel(String style)
    {
        setLabelStyle(style);
        return labelWidget;
    }

    private void setLabelStyle(String style)
    {
        listBoxLabel.setStylePrimaryName(style);
        textBoxLabel.setStylePrimaryName(style);
        descriptionLabel.setStylePrimaryName(style);
    }

    public void addChangeListener(ChangeListener changeListener)
    {
        protocolListBox.addChangeListener(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener)
    {
        protocolListBox.removeChangeListener(changeListener);
    }

    public void setName(String name)
    {
        protocolListBox.setName(name);
        protocolNameTextBox.setName(name + "Name");
        protocolDescTextArea.setName(name + "Description");
    }

    public String getName()
    {
        return protocolListBox.getName();
    }

    public void setWidth(String width)
    {
        instance.setWidth(width);
        protocolListBox.setWidth(width);
        protocolNameTextBox.setWidth(width);
        protocolDescTextArea.setWidth(width);
        protocolDescHtml.setWidth(width);
    }

    public void update(List protocolList, String defaultProtocol, String textArea)
    {
        setProtocolListBoxContents(protocolList, defaultProtocol);
        protocolDescTextArea.setText(textArea);
    }

    public void setProtocolListBoxContents(List protocols, String defaultProtocol)
    {
        if(protocolListBox.getItemCount()== 0 )
        {
            if(protocols == null || protocols.size() == 0)
            {
                protocolListBox.clear();
                protocolListBox.addItem(NEW_PROTOCOL, "new");
                return;
            }
            protocolListBox.clear();
            Collections.sort(protocols);
            protocols.add(0,NEW_PROTOCOL);
            for(Iterator it = protocols.iterator(); it.hasNext();)
            {
                String protocol = (String)it.next();
                if(protocol.equals(NEW_PROTOCOL))
                {
                   protocolListBox.addItem(protocol, "new");
                }
                else
                {
                    protocolListBox.addItem(protocol, protocol);
                }
            }
        }
        setDefault(defaultProtocol);
    }

    public void setDefault(String defaultProtocol)
    {
        if(defaultProtocol == null || defaultProtocol.length() == 0)
        {
            protocolNameTextBox.setText("");
            defaultProtocol = "new";
        }
        int protocolCount = protocolListBox.getItemCount();
        boolean found = false;
        for(int i = 0; i < protocolCount; i++)
        {
            if(protocolListBox.getValue(i).equals(defaultProtocol))
            {
                found = true;
                protocolListBox.setSelectedIndex(i);
                break;
            }
        }
        if(found && !defaultProtocol.equals("new"))
        {
            protocolNameTextBox.setText(defaultProtocol);
        }
        else
        {
            protocolNameTextBox.setText("");   
        }
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);
        int index = 0;

        if(readOnly)
        {
            index = instance.getWidgetIndex(protocolNameTextBox);
            protocolNameHidden.setValue(protocolNameTextBox.getText());
            protocolNameHidden.setName(protocolNameTextBox.getName());
            if(index != -1)
            {
                instance.remove(protocolNameTextBox);
                ((VerticalPanel)labelWidget).remove(textBoxLabel);
                instance.insert(protocolNameHidden, index);
            }
            index = instance.getWidgetIndex(protocolDescTextArea);
            protocolDescHidden.setValue(protocolDescTextArea.getText());
            protocolDescHtml.setHTML(StringUtils.filter(protocolDescTextArea.getText(), true));
            protocolDescHidden.setName(protocolDescTextArea.getName());
            if(index != -1)
            {
                instance.remove(protocolDescTextArea);
                instance.add(protocolDescHidden);
                instance.insert(protocolDescHtml, index);
            }
        }
        else
        {
            index = instance.getWidgetIndex(protocolNameHidden);
            if(index != -1)
            {
                instance.remove(protocolNameHidden);
                instance.insert(protocolNameTextBox, index);
                ((VerticalPanel)labelWidget).insert(textBoxLabel,index);
            }
            index = instance.getWidgetIndex(protocolDescHtml);
            if(index != -1)
            {
                instance.remove(protocolDescHidden);
                instance.remove(protocolDescHtml);
                instance.insert(protocolDescTextArea, index);
            }
        }
    }

    public void setVisibleLines(int lines)
    {
        protocolDescTextArea.setVisibleLines(lines);
    }

    public String getSelectedProtocolValue()
    {
        try
        {
            return protocolListBox.getValue(protocolListBox.getSelectedIndex());
        }
        catch(IndexOutOfBoundsException e)
        {
            return "";
        }

    }

    public String validate()
    {
        if(protocolNameTextBox.getText().equalsIgnoreCase("default"))
            return "Sorry, default is a reserved protocol name. Please choose another.";
        String selectedProtocol = getSelectedProtocolValue();
        if(selectedProtocol.equals("new"))
        {
            if(protocolNameTextBox.getText().equals(""))
            {
                return "Missing protocol name.";
            }
            else
            {
                for (int i = 0; i < protocolNameTextBox.getText().length(); i++)
                {
                    char ch = protocolNameTextBox.getText().charAt(i);
                    if (!Character.isLetterOrDigit(ch) && ch != '_')
                    {
                        return "The name '" + protocolNameTextBox.getText() + "' is not a valid protocol name.";
                    }
                    if (ch == ' ')
                    {
                        return "The cluster pipeline does not currently support spaces in search protocol names.";
                    }
                }
            }
        }
        return "";
    }

    public boolean existingProtocol()
    {
        if(!getSelectedProtocolValue().equals("new"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void newProtocol()
    {
        setDefault("");
        protocolDescTextArea.setText("");
    }

    public void copy()
    {
        setDefault("");   
    }

    public void setFocus(boolean hasFocus)
    {
        protocolNameTextBox.setFocus(hasFocus);
    }
}
