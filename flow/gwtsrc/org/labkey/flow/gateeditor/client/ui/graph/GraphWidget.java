/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.ui.graph;

import org.labkey.flow.gateeditor.client.model.GWTPoint;
import org.labkey.flow.gateeditor.client.model.GWTRectangle;
import org.labkey.flow.gateeditor.client.model.GWTGate;

abstract public class GraphWidget
{
    protected GraphWindow graphWindow;
    protected GWTPoint ptCapture;

    public GraphWidget(GraphWindow graphWindow)
    {
        this.graphWindow = graphWindow;
    }

    static native public void setCursor(String cursor)
    /*-{
         $wnd.document.body.style.cursor = cursor;
    }-*/;

    abstract public GraphWidget hitTest(GWTPoint ptScreen);

    abstract public void mouseMove(GWTPoint ptScreen);

    abstract public void mouseDown(GWTPoint ptScreen);

    abstract public void mouseUp(GWTPoint ptScreen);

    abstract public void updateDisplay();

    abstract public GWTRectangle getBoundingRect();

    public GWTPoint getOffset(GWTPoint point)
    {
        return constrainOffset(new GWTPoint(point.x - ptCapture.x, point.y - ptCapture.y));
    }

    public GWTPoint constrainOffset(GWTPoint point)
    {
        GWTPoint pt = new GWTPoint(point.x, point.y);
        GWTRectangle rcBounds = getBoundingRect();
        GWTRectangle rcData = new GWTRectangle(graphWindow.graphInfo.rcData);
        rcData.translate(graphWindow.image.getAbsoluteLeft(), graphWindow.image.getAbsoluteTop());
        pt.x = Math.max(pt.x, rcData.x -rcBounds.x - 1);
        pt.x = Math.min(pt.x, rcData.x + rcData.width - rcBounds.x - rcBounds.width);
        pt.y = Math.max(pt.y, rcData.y -rcBounds.y);
        pt.y = Math.min(pt.y, rcData.y + rcData.height - rcBounds.y - rcBounds.height + 1);
        return pt;
    }

    public void capture(GWTPoint point)
    {
        ptCapture = point;
        graphWindow.capture(this);
    }

    public void releaseCapture()
    {
        ptCapture = null;
        graphWindow.releaseCapture(this);
    }

    public boolean hasCapture()
    {
        return ptCapture != null && graphWindow.capture == this;
    }

    public GWTGate getGate()
    {
        return graphWindow.getGate();
    }
}
