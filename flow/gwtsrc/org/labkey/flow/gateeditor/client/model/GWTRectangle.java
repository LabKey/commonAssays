package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTRectangle implements IsSerializable
{
    public double x;
    public double y;
    public double height;
    public double width;

    public GWTRectangle(GWTPoint topLeft, GWTPoint bottomRight)
    {
        setTopLeft(topLeft);
        setBottomRight(bottomRight);
    }

    public GWTRectangle()
    {

    }

    public GWTRectangle(GWTRectangle that)
    {
        this.x = that.x;
        this.y = that.y;
        this.height = that.height;
        this.width = that.width;
    }

    public boolean contains(GWTPoint point)
    {
        return x <= point.x && point.x < x + width &&
                y <= point.y && point.y < y + height;
    }

    public GWTPoint getTopLeft()
    {
        return new GWTPoint(x, y);
    }

    public GWTPoint getBottomRight()
    {
        return new GWTPoint(x + width, y + height);
    }

    public void setTopLeft(GWTPoint point)
    {
        x = point.x;
        y = point.y;
    }

    public void setBottomRight(GWTPoint point)
    {
        width = point.x - x;
        height = point.y - y;
    }

    public void translate(double x, double y)
    {
        this.x += x;
        this.y += y;
    }
}
