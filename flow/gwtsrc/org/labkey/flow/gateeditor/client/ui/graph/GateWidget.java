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

import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;

public class GateWidget extends GraphWidget
{
    Handle[] handles;
    boolean addingPoints;

    public GateWidget(GraphWindow graphWindow)
    {
        super(graphWindow);
        handles = new Handle[0];
    }

    public String urlGraphWindow(GWTGraphInfo graphInfo)
    {
        String ret = graphInfo.graphURL;
        if (getGate() instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) getGate();
            for (int i = 0; i < polygon.getArrX().length; i ++)
            {
                ret += "&ptX=" + polygon.getArrX()[i];
                ret += "&ptY=" + polygon.getArrY()[i];
            }
            if (addingPoints)
            {
                ret += "&open=true";
            }
        }
        else if (getGate() instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) getGate();
            ret += "&ptX=" + interval.getMinValue();
            ret += "&ptX=" + interval.getMaxValue();
        }
        return ret;
    }

    public void updateDisplay()
    {
        if (graphWindow.graphInfo == null)
            return;
        graphWindow.image.setUrl(urlGraphWindow(graphWindow.graphInfo));
        graphWindow.image.setVisible(true);
        int newSize = 0;
        if (getGate() instanceof GWTIntervalGate)
        {
            newSize = 2;
        }
        if (getGate() instanceof GWTPolygonGate)
        {
            newSize = ((GWTPolygonGate) getGate()).getArrX().length;
        }
        resizeHandleArray(newSize);
        for (int i = 0; i < handles.length; i ++)
        {
            handles[i].updateDisplay();
        }
    }

    public boolean contains(GWTPoint pt)
    {
        pt = graphWindow.screenToValue(pt);
        if (getGate() instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) getGate();
            return interval.getMinValue() <= pt.x && pt.x < interval.getMaxValue() ||
                    interval.getMaxValue() <= pt.x && pt.x < interval.getMinValue();
        }
        else if (getGate() instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) getGate();
            return polygon.contains(pt);
        }
        return false;
    }

    public GraphWidget hitTest(GWTPoint ptScreen)
    {
        GraphWidget ret;
        if (getGate().isOpen())
        {
            if (handles.length > 2)
            {
                return handles[0].hitTest(ptScreen);
            }
            return null;
        }
        for (int i = 0; i < handles.length; i ++)
        {
            ret = handles[i].hitTest(ptScreen);
            if (ret != null)
                return ret;
        }
        if (contains(ptScreen))
        {
            return this;
        }
        return null;
    }

    private void updateGate(GWTPoint ptScreen)
    {
        GWTPoint ptOffset = getOffset(ptScreen);
        GWTGate gate = getGate();
        if (getGate() instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) getGate();
            GWTPoint minPt = new GWTPoint(interval.getMinValue(), 0);
            GWTPoint maxPt = new GWTPoint(interval.getMaxValue(), 0);
            minPt = graphWindow.valueToScreen(minPt);
            maxPt = graphWindow.valueToScreen(maxPt);
            minPt.x += ptOffset.x;
            maxPt.x += ptOffset.x;
            minPt = graphWindow.screenToValue(minPt);
            maxPt = graphWindow.screenToValue(maxPt);
            interval = new GWTIntervalGate(interval.getAxis(), minPt.x, maxPt.x);
            gate = interval;
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            double[] arrX = new double[polygon.length()];
            double[] arrY = new double[polygon.length()];
            for (int i = 0; i < polygon.length(); i++)
            {
                GWTPoint point = new GWTPoint(polygon.getArrX()[i], polygon.getArrY()[i]);
                point = graphWindow.valueToScreen(point);
                point.x += ptOffset.x;
                point.y += ptOffset.y;
                point = graphWindow.screenToValue(point);
                arrX[i] = point.x;
                arrY[i] = point.y;
            }
            polygon = new GWTPolygonGate(polygon.getXAxis(), arrX, polygon.getYAxis(), arrY);
            gate = polygon;
        }
        gate.setDirty(true);
        graphWindow.getEditor().getState().setGate(gate);
        ptCapture = ptScreen;
    }

    public void offsetPosition(GWTPoint point)
    {
        for (int i = 0; i < handles.length; i ++)
        {
            handles[i].offsetPosition(point);
        }
    }

    public void mouseMove(GWTPoint ptScreen)
    {
        setCursor("move");
        if (hasCapture())
        {
            offsetPosition(getOffset(ptScreen));
        }
    }

    public void mouseDown(GWTPoint ptScreen)
    {
        capture(ptScreen);
    }

    public void mouseUp(GWTPoint ptScreen)
    {
        if (hasCapture())
        {
            updateGate(ptScreen);
            releaseCapture();
        }
    }

    private void resizeHandleArray(int size)
    {
        if (size == handles.length)
            return;
        Handle[] newHandles = new Handle[size];
        for (int i = 0; i < handles.length && i < size; i ++)
        {
            newHandles[i] = handles[i];
        }
        for (int i = size; i < handles.length; i ++)
        {
            handles[i].remove();
        }
        for (int i = handles.length; i < size; i ++)
        {
            newHandles[i] = new Handle(this, i);
        }
        handles = newHandles;
    }

    public GWTRectangle getBoundingRect()
    {
        GWTGate gate = getGate();
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            GWTRectangle ret = new GWTRectangle(graphWindow.valueToScreen(new GWTPoint(interval.getMinValue(), 0)),
                    graphWindow.valueToScreen(new GWTPoint(interval.getMaxValue(), 0)));
            ret.y = graphWindow.graphInfo.rcData.y + graphWindow.image.getAbsoluteTop();
            ret.height = graphWindow.graphInfo.rcData.height;
            return ret;
        }
        if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            GWTRectangle rcValue = polygon.getBoundingRectangle();
            return graphWindow.valueToScreen(rcValue);
        }
        return null;
    }
}
