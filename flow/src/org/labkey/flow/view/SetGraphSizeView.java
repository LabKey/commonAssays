package org.labkey.flow.view;

import org.labkey.api.jsp.JspLoader;
import org.labkey.api.view.JspView;

public class SetGraphSizeView extends JspView
{
    public SetGraphSizeView()
    {
        super(JspLoader.createPage("/org/labkey/flow/view/setGraphSize.jsp"));
    }
}
