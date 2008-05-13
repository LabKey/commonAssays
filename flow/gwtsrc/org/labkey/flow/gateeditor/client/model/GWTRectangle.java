/*
 * Copyright (c) 2007-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
