package org.labkey.ms2.pipeline.client.tandem;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import org.labkey.ms2.pipeline.client.InputXmlComposite;

/**
 * User: billnelson@uky.edu
 * Date: Apr 18, 2008
 */

/**
 * <code>XtandemInputXmlComposite</code>
 */
public class XtandemInputXmlComposite extends InputXmlComposite
{
    public Widget getLabel(String style)
    {
        labelWidget = new Label("X! Tandem XML:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }
}
