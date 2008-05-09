package org.labkey.ms2.pipeline.client.tandem;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HasText;
import org.labkey.ms2.pipeline.client.InputXmlComposite;
import org.labkey.ms2.pipeline.client.ParamParser;

/**
 * User: billnelson@uky.edu
 * Date: Apr 18, 2008
 */

/**
 * <code>XtandemInputXmlComposite</code>
 */
public class XtandemInputXmlComposite extends InputXmlComposite
{

    public String update(String text)
    {
        if(params == null)
            params = new XtandemParamParser(inputXmlTextArea);
        return super.update(text);
    }

    public Widget getLabel(String style)
    {
        labelWidget = new Label("X! Tandem XML:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    private class XtandemParamParser extends ParamParser
    {
        private XtandemParamParser(HasText xml)
        {
            super(xml);
        }
    }
}
