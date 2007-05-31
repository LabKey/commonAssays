package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.gateeditor.client.model.GWTRectangle;
import org.labkey.flow.gateeditor.client.model.GWTRange;
import org.labkey.flow.gateeditor.client.model.GWTPoint;

public class GWTGraphInfo implements IsSerializable
{
    public GWTGraphOptions graphOptions;
    public GWTRectangle rcChart;
    public GWTRectangle rcData;
    public GWTRange rangeX;
    public GWTRange rangeY;
    public String graphURL;

    public GWTPoint toScreen(GWTPoint point)
    {
        return new GWTPoint(rangeX.toScreen(point.x, rcData.width) + rcData.x,
                rcData.y + rcData.height - rangeY.toScreen(point.y, rcData.height));
    }

    public GWTPoint toValue(GWTPoint point)
    {
        return new GWTPoint(rangeX.toValue(point.x - rcData.x, rcData.width),
                rangeY.toValue(rcData.y + rcData.height - point.y, rcData.height));
    }
}
