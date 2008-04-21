package org.labkey.ms2.pipeline.client.mascot;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import org.labkey.ms2.pipeline.client.InputXmlComposite;

/**
 * User: billnelson@uky.edu
 * Date: Apr 18, 2008
 */

/**
 * <code>MascotInputXmlComposite</code>
 */
public class MascotInputXmlComposite extends InputXmlComposite
{

    public Widget getLabel(String style)
    {
        labelWidget = new Label("Mascot XML:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }
}
