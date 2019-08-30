package org.labkey.flow.view;

import org.labkey.api.jsp.JspLoader;
import org.labkey.api.view.JspView;

public class SetGraphSizeView extends JspView
{
    public SetGraphSizeView()
    {
        super(JspLoader.createPage((String)null, "/org/labkey/flow/view/setGraphSize.jsp"));
    }
}
