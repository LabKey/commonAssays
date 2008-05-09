package org.labkey.ms2.pipeline.client.sequest;

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
 * <code>SequestInputXmlComposite</code>
 */
public class SequestInputXmlComposite extends InputXmlComposite
{
    public String update(String text)
    {
        if(params == null)
            params = new SequestParamParser(inputXmlTextArea);
        return super.update(text);
    }

    public Widget getLabel(String style)
    {
        labelWidget = new Label("Sequest XML:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    private class SequestParamParser extends ParamParser
    {
        private SequestParamParser(HasText xml)
        {
            super(xml);
        }
    }
}
