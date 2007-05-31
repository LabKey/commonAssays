package org.labkey.flow.gateeditor.client.ui;

import org.labkey.flow.gateeditor.client.model.GWTPoint;
import com.google.gwt.user.client.ui.Image;

public class ActiveGate
{
    String xAxis;
    String yAxis;
    ActivePoint[] points;

    public class ActivePoint
    {
        int index;
        GWTPoint point;
        Image image;
    }


}
