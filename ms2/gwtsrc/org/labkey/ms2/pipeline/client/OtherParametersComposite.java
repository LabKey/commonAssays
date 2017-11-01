/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.api.gwt.client.ui.FontButton;
import org.labkey.api.gwt.client.ui.HelpPopup;
import org.labkey.api.gwt.client.ui.ImageButton;
import org.labkey.api.gwt.client.ui.PropertiesEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jan 30, 2012
 */
public class OtherParametersComposite extends SearchFormComposite
{
    protected FlexTable _table = new FlexTable();
    Map<String, TextBox> _inputs = new LinkedHashMap<String, TextBox>();
    private TextBox _newParameterNameTextBox = new TextBox();
    private ImageButton _newParameterButton = new ImageButton("Add");
    private ChangeHandler _handler;
    public static final String BLANK_PARAMETER_NAME_PROMPT = "<Type parameter name>";
    private final List<SearchFormComposite> _uiElements;
    private final InputXmlComposite _inputXmlComposite;

    public OtherParametersComposite(List<SearchFormComposite> uiElements, InputXmlComposite inputXmlComposite)
    {
        _uiElements = uiElements;
        _inputXmlComposite = inputXmlComposite;
        _newParameterNameTextBox.addBlurHandler(new BlurHandler()
        {
            public void onBlur(BlurEvent event)
            {
                if (_newParameterNameTextBox.getText().trim().isEmpty())
                {
                    _newParameterNameTextBox.setText(BLANK_PARAMETER_NAME_PROMPT);
                    DOM.setStyleAttribute(_newParameterNameTextBox.getElement(), "color", "grey");
                }
            }
        });

        _newParameterButton.addClickHandler(new ClickHandler()
        {
            public void onClick(ClickEvent event)
            {
                addParameter();
            }
        });
        _newParameterNameTextBox.addKeyPressHandler(new KeyPressHandler()
        {
            public void onKeyPress(KeyPressEvent event)
            {
                if (KeyCodes.KEY_ENTER == event.getNativeEvent().getKeyCode())
                {
                    addParameter();
                }
            }
        });

        _newParameterNameTextBox.addFocusHandler(new FocusHandler()
        {
            public void onFocus(FocusEvent event)
            {
                if (BLANK_PARAMETER_NAME_PROMPT.equals(_newParameterNameTextBox.getText()))
                {
                    _newParameterNameTextBox.setText("");
                }
                DOM.setStyleAttribute(_newParameterNameTextBox.getElement(), "color", "");
            }
        });
        _newParameterNameTextBox.setText(BLANK_PARAMETER_NAME_PROMPT);
        DOM.setStyleAttribute(_newParameterNameTextBox.getElement(), "color", "grey");
        _newParameterNameTextBox.setWidth("20em");

        _table.setStylePrimaryName("lk-fields-table");
        _table.setCellPadding(1);
        initWidget(_table);
    }

    private void addParameter()
    {
        String name = _newParameterNameTextBox.getText().trim();
        if (!name.isEmpty() && !name.equals(BLANK_PARAMETER_NAME_PROMPT))
        {
            if (isHandledParameter(name))
            {
                new Search.ErrorDialogBox("Please set the '" + name + "' parameter by using the user interface above");
                return;
            }
            if (!_inputs.containsKey(name))
            {
                Label label = new Label(name);
                _table.setWidget(_table.getRowCount() - 1, 0, label);
                _table.getCellFormatter().setStylePrimaryName(_table.getRowCount() - 1, 0, "labkey-form-label-nowrap");
                TextBox textBox = createParameterValueTextBox();
                _inputs.put(name, textBox);
                _table.setWidget(_table.getRowCount() - 1, 1, textBox);
                _table.setWidget(_table.getRowCount() - 1, 2, createDeleteButton(name));
                _table.setWidget(_table.getRowCount(), 0, _newParameterNameTextBox);
                _table.setWidget(_table.getRowCount() - 1, 1, _newParameterButton);
                _newParameterNameTextBox.setText("");
                _newParameterNameTextBox.setText(BLANK_PARAMETER_NAME_PROMPT);
                DOM.setStyleAttribute(_newParameterNameTextBox.getElement(), "color", "grey");
                _handler.onChange(null);
                textBox.setFocus(true);
            }
            else
            {
                new Search.ErrorDialogBox("Duplicate parameter name");
            }
        }
    }

    @Override
    public void setWidth(String width)
    {
    }

    @Override
    public Widget getLabel()
    {
        Label label = new Label("Other parameters");
        label.setStyleName(LABEL_STYLE_NAME);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(new HelpPopup("Other parameters", "The names and values of other <a href=\"https://www.labkey.org/wiki/home/Documentation/page.view?name=pipelineParams\" target=\"_blank\">analysis parameters</a> that are not controlled by the above inputs."));
        return panel;
    }

    @Override
    public String validate()
    {
        return "";
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        for (Map.Entry<String, TextBox> entry : _inputs.entrySet())
        {
            params.setInputParameter(entry.getKey(), entry.getValue().getText());
        }
    }

    @Override
    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);
        createTable();
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        _inputs.clear();
        for (Map.Entry<String, String> entry : params.getParameters().entrySet())
        {
            if (!isHandledParameter(entry.getKey()))
            {
                TextBox valueBox = createParameterValueTextBox();
                valueBox.setText(entry.getValue());
                _inputs.put(entry.getKey(), valueBox);
            }
        }
        createTable();
        return "";
    }

    private boolean isHandledParameter(String name)
    {
        for (SearchFormComposite uiElement : _uiElements)
        {
            if (uiElement.isHandledParameterName(name))
            {
                return true;
            }
        }
        return false;
    }


    private TextBox createParameterValueTextBox()
    {
        TextBox valueBox = new TextBox();
        valueBox.setWidth("20em");
        valueBox.addChangeHandler(_handler);
        return valueBox;
    }

    private void createTable()
    {
        int row = 0;
        _table.removeAllRows();
        for (final Map.Entry<String, TextBox> entry : _inputs.entrySet())
        {
            Label label = new Label(entry.getKey());

            _table.setWidget(row, 0, label);
            _table.getCellFormatter().setStylePrimaryName(row, 0, "labkey-form-label-nowrap");

            if (readOnly)
            {
                _table.setText(row, 1, entry.getValue().getText());
            }
            else
            {
                _table.setWidget(row, 1, entry.getValue());
                FontButton deleteButton = createDeleteButton(entry.getKey());
                _table.setWidget(row, 2, deleteButton);
            }
            row++;
        }

        if (!readOnly)
        {
            _table.setWidget(++row, 0, _newParameterNameTextBox);
            _table.setWidget(row, 1, _newParameterButton);
        }

    }

    private FontButton createDeleteButton(final String paramName)
    {
        return PropertiesEditor.getDeleteButton(paramName, new ClickHandler()
        {
            public void onClick(ClickEvent event)
            {
                _inputXmlComposite.params.removeInputParameter(paramName);
                _inputs.remove(paramName);
                createTable();
                _handler.onChange(null);
            }
        });
    }

    public void setName(String name)
    {

    }

    public String getName()
    {
        return null;
    }

    public void addChangeListener(ChangeHandler handler)
    {
        _handler = handler;
    }
}
