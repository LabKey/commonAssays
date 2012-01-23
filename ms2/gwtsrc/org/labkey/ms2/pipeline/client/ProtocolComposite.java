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

package org.labkey.ms2.pipeline.client;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;
import org.labkey.api.gwt.client.util.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Mar 25, 2008
 */
public class ProtocolComposite extends SearchFormComposite
{
    private FlexTable instance = new FlexTable();
    private ListBox protocolListBox = new ListBox();
    private TextBox protocolNameTextBox = new TextBox();
    private TextArea protocolDescTextArea = new TextArea();
    private HTML protocolDescHtml = new HTML();
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
        textBoxLabel = new Label("Name");
        descriptionLabel = new Label("Description");
        
        instance.setWidget(0,0,protocolListBox);
        instance.getFlexCellFormatter().setColSpan(0,0,2);

        instance.setWidget(1,0,textBoxLabel);
        instance.getCellFormatter().setStylePrimaryName(1,0, "labkey-form-label-nowrap");
        instance.setWidget(1,1,protocolNameTextBox);

        instance.setWidget(2,0,descriptionLabel);
        instance.getCellFormatter().setStylePrimaryName(2,0, "labkey-form-label-nowrap");
        instance.getCellFormatter().setHorizontalAlignment(2,0,HasHorizontalAlignment.ALIGN_LEFT);
        instance.setWidget(2,1,protocolDescTextArea);

        listBoxLabel = new Label("Analysis protocols");
        listBoxLabel.addStyleName("labkey-strong");
        labelWidget = new VerticalPanel();
        ((VerticalPanel)labelWidget).add(listBoxLabel);
        initWidget(instance);
    }


    public Widget getLabel(String style)
    {
        setLabelStyle(style);
        return labelWidget;
    }

    private void setLabelStyle(String style)
    {
        labelWidget.setStylePrimaryName(style);
        listBoxLabel.setStylePrimaryName(style);
        textBoxLabel.setStylePrimaryName(style);
        descriptionLabel.setStylePrimaryName(style);
    }

    public void addChangeHandler(ChangeHandler handler)
    {
        protocolListBox.addChangeHandler(handler);
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
        
        protocolListBox.setWidth("100%");
        protocolNameTextBox.setWidth("100%");
        protocolDescTextArea.setWidth("100%");
        protocolDescHtml.setWidth("100%");
        instance.getColumnFormatter().setWidth(0,"2%");
        instance.getColumnFormatter().setWidth(1,"98%");
    }

    public void update(List<String> protocolList, String defaultProtocol, String textArea)
    {
        setProtocolListBoxContents(protocolList, defaultProtocol);
        protocolDescTextArea.setText(textArea);
    }

    public void setProtocolListBoxContents(List<String> protocols, String defaultProtocol)
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
            for (String protocol : protocols)
            {
                if (protocol.equals(NEW_PROTOCOL))
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
        else if(!found && !defaultProtocol.equals("new"))
        {
            protocolListBox.setSelectedIndex(0);
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

        if(readOnly)
        {
            
            protocolNameTextBox.setVisible(false);
            textBoxLabel.setVisible(false);
            instance.getCellFormatter().removeStyleName(1, 0, "labkey-form-label-nowrap");
            instance.remove(protocolDescTextArea);
            instance.setWidget(2,1,protocolDescHtml);
            if (protocolDescTextArea.getText() != null && !protocolDescTextArea.getText().trim().equals(""))
            {
                protocolDescHtml.setHTML(StringUtils.filter(protocolDescTextArea.getText(), true));
            }
            else
            {
                protocolDescHtml.setHTML("<em>" + StringUtils.filter("<none given>") + "</em>");
            }

//            index = instance.getWidgetIndex(protocolNameTextBox);
//            protocolNameHidden.setValue(protocolNameTextBox.getText());
//            protocolNameHidden.setName(protocolNameTextBox.getName());
//            if(index != -1)
//            {
//                instance.remove(protocolNameTextBox);
//                ((VerticalPanel)labelWidget).remove(textBoxLabel);
//                instance.insert(protocolNameHidden, index);
//            }
//            index = instance.getWidgetIndex(protocolDescTextArea);
//            protocolDescHidden.setValue(protocolDescTextArea.getText());
//            protocolDescHtml.setHTML(StringUtils.filter(protocolDescTextArea.getText(), true));
//            protocolDescHidden.setName(protocolDescTextArea.getName());
//            if(index != -1)
//            {
//                instance.remove(protocolDescTextArea);
//                instance.add(protocolDescHidden);
//                instance.insert(protocolDescHtml, index);
//            }
        }
        else
        {
            instance.getCellFormatter().setStylePrimaryName(1,0, "labkey-form-label-nowrap");
            //instance.getCellFormatter().setStylePrimaryName(2,0, "labkey-form-label-nowrap");
            protocolNameTextBox.setVisible(true);
            instance.remove(protocolDescHtml);
            instance.setWidget(2,1,protocolDescTextArea);
            //protocolDescHtml.setHTML("");
            textBoxLabel.setVisible(true);

//            index = instance.getWidgetIndex(protocolNameHidden);
//            if(index != -1)
//            {
//                instance.remove(protocolNameHidden);
//                instance.insert(protocolNameTextBox, index);
//                ((VerticalPanel)labelWidget).insert(textBoxLabel,index);
//            }
//            index = instance.getWidgetIndex(protocolDescHtml);
//            if(index != -1)
//            {
//                instance.remove(protocolDescHidden);
//                instance.remove(protocolDescHtml);
//                instance.insert(protocolDescTextArea, index);
//            }
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
                    if (!Character.isLetterOrDigit(ch) && ch != '_'  && ch != ' ')
                    {
                        return "The name '" + protocolNameTextBox.getText() + "' is not a valid protocol name.";
                    }
                }
            }
        }
        return "";
    }

    public void newProtocol()
    {
        setDefault("");
        protocolDescTextArea.setText("");
    }

    public void copy()
    {
        String suffix = "_Copy";
        String pName = protocolNameTextBox.getText();

        if(pName == null || pName.length() == 0 )
        {
            setDefault("");
            return;
        }

        StringBuffer protocolName = new StringBuffer(pName);
        int index = protocolName.lastIndexOf(suffix);

        if( index > 0 && index != protocolName.length()-1)
        {
            String versionString = protocolName.substring(index + suffix.length());
            int versionInt = 0;

            try
            {
                versionInt = Integer.parseInt(versionString);
            }
            catch(NumberFormatException e)
            {
                protocolName.append(suffix + "1");
                setDefault(protocolName.toString());
                return;
            }

            versionInt++;
            protocolName.replace(index + suffix.length(), protocolName.length(), Integer.toString(versionInt));

        }
        else
        {
            protocolName.append(suffix + "1");
        }
        setDefault(protocolName.toString());
    }

    public void setFocus(boolean hasFocus)
    {
        protocolNameTextBox.setFocus(hasFocus);
    }

}
