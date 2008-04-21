package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;
import org.labkey.api.gwt.client.util.StringUtils;

/**
 * User: billnelson@uky.edu
 * Date: Apr 8, 2008
 */
public abstract class InputXmlComposite extends SearchFormComposite
{
    private TextAreaWrapable inputXmlTextArea = new TextAreaWrapable();
    private Hidden inputXmlHidden = new Hidden();
    private HTML inputXmlHtml = new HTML();
    private HorizontalPanel instance = new HorizontalPanel();
    public static final String DEFAULT_XML = "<?xml version=\"1.0\"?>\n" +
                "<bioml>\n" +
                "<!-- Override default parameters here. -->\n" +
                "</bioml>";

    public InputXmlComposite()
    {
        super();
        init();
    }

    public void setDefault()
    {
        inputXmlTextArea.setText(DEFAULT_XML);
    }

    public void update(String text)
    {
        if(text.equals(""))
            text = DEFAULT_XML;
        inputXmlTextArea.setText(text);
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

    public void init()
    {
        inputXmlTextArea.setVisibleLines(10);
        inputXmlTextArea.setWrap("OFF");
        instance.add(inputXmlTextArea);
        initWidget(instance);
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
