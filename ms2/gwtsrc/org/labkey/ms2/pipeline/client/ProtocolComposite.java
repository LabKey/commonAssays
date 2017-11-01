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

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.api.gwt.client.util.StringUtils;

import java.util.Collections;
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
    Label textBoxLabel;
    Label descriptionLabel;
    private static final String NEW_PROTOCOL = "<new protocol>";
    private List<String> _protocols;


    public ProtocolComposite()
    {
        protocolListBox.setVisibleItemCount(1);
        protocolDescHtml.setWordWrap(true);
        protocolDescHtml.setStylePrimaryName("labkey-read-only");
        textBoxLabel = new Label("Name");
        descriptionLabel = new Label("Description");

        instance.setStylePrimaryName("lk-fields-table");
        instance.setWidget(0,0,protocolListBox);
        instance.getFlexCellFormatter().setColSpan(0,0,2);

        instance.setWidget(1,0,textBoxLabel);
        instance.getCellFormatter().setStylePrimaryName(1,0, "labkey-form-label-nowrap");
        instance.setWidget(1,1,protocolNameTextBox);

        instance.setWidget(2,0,descriptionLabel);
        instance.getCellFormatter().setStylePrimaryName(2,0, "labkey-form-label-nowrap");
        instance.getCellFormatter().setHorizontalAlignment(2,0,HasHorizontalAlignment.ALIGN_LEFT);
        instance.setWidget(2,1,protocolDescTextArea);

        textBoxLabel.setStylePrimaryName(LABEL_STYLE_NAME);
        descriptionLabel.setStylePrimaryName(LABEL_STYLE_NAME);

        initWidget(instance);
    }


    public Widget getLabel()
    {
        Label listBoxLabel = new Label("Analysis protocol");
        listBoxLabel.setStylePrimaryName(LABEL_STYLE_NAME);
        return listBoxLabel;
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
        if (protocolListBox.getItemCount() == 0 && (protocols == null || protocols.isEmpty()))
        {
            protocolListBox.clear();
            protocolListBox.addItem(NEW_PROTOCOL, "new");
            return;
        }

        if(protocols != null && protocolListBox.getItemCount() != protocols.size() + 1)
        {
            _protocols = protocols;
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

    /** We need to create a unique protocol name. We will append "_1", or change an existing "_X" to increment the X */
    public void copy()
    {
        String protocolName = protocolNameTextBox.getText();

        if(protocolName == null || protocolName.length() == 0 )
        {
            setDefault("");
            return;
        }

        int index = protocolName.lastIndexOf("_");
        String prefix = protocolName + "_";

        int versionInt = 1;

        // See if the current protocol name ends with "_X"
        if( index > 0 && index != protocolName.length() - 1)
        {
            String versionString = protocolName.substring(index + 1);

            try
            {
                // See if X is a number
                versionInt = Integer.parseInt(versionString) + 1;

                // Remove the X from the prefix
                prefix = protocolName.substring(0, index + 1);
            }
            catch(NumberFormatException ignored)
            {
            }
        }

        // Keep incrementing the number on the suffix until we have a unique name
        while (_protocols != null && _protocols.contains(prefix + versionInt))
        {
            versionInt++;
        }

        setDefault(prefix + versionInt);
    }

    public void setFocus(boolean hasFocus)
    {
        protocolNameTextBox.setFocus(hasFocus);
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        return "";
    }
}
