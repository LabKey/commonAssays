package org.labkey.ms2.pipeline.client.mascot;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HasText;
import org.labkey.ms2.pipeline.client.InputXmlComposite;
import org.labkey.ms2.pipeline.client.SearchFormException;
import org.labkey.ms2.pipeline.client.ParamParser;

/**
 * User: billnelson@uky.edu
 * Date: Apr 18, 2008
 */

/**
 * <code>MascotInputXmlComposite</code>
 */
public class MascotInputXmlComposite extends InputXmlComposite
{

    public String update(String text)
    {
        if(params == null)
            params = new MascotParamParser(inputXmlTextArea);
        return super.update(text);
    }
    public Widget getLabel(String style)
    {
        labelWidget = new Label("Mascot XML:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    public void setTaxonomy(String name) throws SearchFormException
    {
        params.setTaxonomy(name);
    }

    private class MascotParamParser extends ParamParser
    {
        private MascotParamParser(HasText xml)
        {
            super(xml);
            ENZYME = "mascot, enzyme";
            STATIC_MOD = "mascot, fixed modifications";
            DYNAMIC_MOD = "mascot, variable modifications";
        }
    }
}
