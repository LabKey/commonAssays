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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import org.labkey.api.gwt.client.util.StringUtils;

/**
 * User: billnelson@uky.edu
 * Date: Apr 8, 2008
 */
public abstract class InputXmlComposite extends SearchFormComposite
{
    protected TextAreaWrapable inputXmlTextArea = new TextAreaWrapable();
    protected Hidden inputXmlHidden = new Hidden();
    protected HTML inputXmlHtml = new HTML();
    protected HorizontalPanel instance = new HorizontalPanel();
    protected ParamParser params;
    public static final String DEFAULT_XML = "<?xml version=\"1.0\"?>\n" +
                "<bioml>\n" +
                "<!-- Override default parameters here. -->\n" +
                "</bioml>";

    public InputXmlComposite()
    {
        inputXmlTextArea.setVisibleLines(10);
        inputXmlTextArea.setWrap("OFF");
        inputXmlHtml.setStylePrimaryName("labkey-read-only");
        instance.add(inputXmlTextArea);
        initWidget(instance);
    }

    public void setDefault()
    {
        update(DEFAULT_XML);
    }

    public String update(String text)
    {
        if(text.equals(""))
            text = DEFAULT_XML;
        inputXmlTextArea.setText(text);
        return validate();
    }

    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        if(readOnly)
        {
            instance.remove(inputXmlTextArea);
            String text = inputXmlTextArea.getText();
            inputXmlHidden.setName(getName());
            inputXmlHidden.setValue(text);
            inputXmlHtml.setHTML(StringUtils.filter(text,true));
            instance.add(inputXmlHidden);
            instance.add(inputXmlHtml);
        }
        else
        {
            instance.remove(inputXmlHidden);
            instance.remove(inputXmlHtml);
            instance.add(inputXmlTextArea);
        }

    }

    public String getName()
    {
        return inputXmlTextArea.getName();
    }

    public void setName(String name)
    {
        inputXmlTextArea.setName(name);
    }

    public void setWidth(String width)
    {
        instance.setWidth(width);
        inputXmlTextArea.setWidth(width);
        inputXmlHtml.setWidth(width);
    }

    public String validate()
    {
        return params.validate();
    }

    public void addChangeListener(ChangeHandler changeHandler)
    {
        inputXmlTextArea.addChangeHandler(changeHandler);
    }

    public void writeXml() throws SearchFormException
    {
        params.writeXml();
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        throw new UnsupportedOperationException();
    }

    private class TextAreaWrapable extends TextArea
    {
        public void setWrap(String wrapOption)
        {
                Element textArea = getElement();
                DOM.setElementAttribute(textArea,"wrap",wrapOption);

        }
    }
}
