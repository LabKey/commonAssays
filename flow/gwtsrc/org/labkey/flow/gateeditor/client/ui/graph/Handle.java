package org.labkey.flow.gateeditor.client.ui.graph;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;
import org.labkey.flow.gateeditor.client.FlowUtil;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;
import org.labkey.flow.gateeditor.client.model.*;

public class Handle extends GraphWidget
{
    GateWidget gateWidget;
    Image image;
    int index;

    public Handle(GateWidget gateWidget, int index)
    {
        super(gateWidget.graphWindow);
        this.gateWidget = gateWidget;
        this.index = index;
        this.image = new TransparentEventImage(gateWidget.graphWindow.image);
        image.setUrl(FlowUtil.flowResource("handle.gif"));
        graphWindow.widget.add(image);
    }

    void moveTo(GWTRectangle rect)
    {
        if (rect == null)
        {
            image.setVisible(false);
            return;
        }
        Element element = image.getElement();
        DOM.setStyleAttribute(element, "zIndex", "1");
        DOM.setStyleAttribute(element, "position", "absolute");
        DOM.setStyleAttribute(element, "left", ((int) rect.x) + "px");
        DOM.setStyleAttribute(element, "top", ((int) rect.y) + "px");
        DOM.setStyleAttribute(element, "width", ((int) rect.width) + "px");
        DOM.setStyleAttribute(element, "height", ((int) rect.height) + "px");
        image.setVisible(true);
    }

    public GWTPoint getPoint()
    {
        GWTGate gate = getGate();
        GWTPoint ret;
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            if (index == 0)
            {
                ret = new GWTPoint(interval.getMinValue(), 0);
            }
            else if (index == 1)
            {
                ret = new GWTPoint(interval.getMaxValue(), 0);
            }
            else
            {
                return null;
            }
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            if (index >= polygon.getArrX().length)
            {
                return null;
            }
            ret = new GWTPoint(polygon.getArrX()[index], polygon.getArrY()[index]);
        }
        else
        {
            return null;
        }

        return graphWindow.valueToScreen(ret);
    }

    public void remove()
    {
        image.removeFromParent();
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public GWTRectangle getPosition()
    {
        GWTPoint pt = getPoint();
        if (pt == null)
            return null;
        GWTRectangle ret = new GWTRectangle();
        GWTGraphInfo info = graphWindow.getGraphInfo();
        if (info == null)
            return null;
        if (info.graphOptions.isHistogram())
        {
            ret.x = pt.x;
            ret.width = 1;
            ret.y = graphWindow.image.getAbsoluteTop() + info.rcChart.y;
            ret.height = info.rcChart.height;
        }
        else
        {
            ret.x = pt.x - 1;
            ret.width = 3;
            ret.y = pt.y - 1;
            ret.height = 3;
        }
        return ret;
    }

    public GWTRectangle getBoundingRect()
    {
        return new GWTRectangle(getPoint(), getPoint());
    }

    public Image getImage()
    {
        return image;
    }

    public void offsetPosition(GWTPoint offset)
    {
        GWTRectangle position = getPosition();
        position.translate(offset.x, offset.y);
        moveTo(position);
    }

    public void updateDisplay()
    {
        moveTo(getPosition());
    }

    public GraphWidget hitTest(GWTPoint ptScreen)
    {
        GWTRectangle rect = getPosition();
        if (rect == null)
            return null;
        if (rect.contains(ptScreen))
            return this;
        return null;
    }

    public void updateGate(GWTPoint ptScreen)
    {
        GWTPoint ptOffset = getOffset(ptScreen);
        GWTPoint newScreen = new GWTPoint(getPoint().x + ptOffset.x, getPoint().y + ptOffset.y);
        GWTPoint newValue = graphWindow.screenToValue(newScreen);
        GWTGate gate = getGate();
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            double otherValue = index == 0 ? interval.getMaxValue() : interval.getMinValue();
            double minValue = Math.min(newValue.x, otherValue);
            double maxValue = Math.max(newValue.x, otherValue);
            interval = new GWTIntervalGate(interval.getAxis(), minValue, maxValue);
            gate = interval;
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            double[] arrX = new double[polygon.length()];
            double[] arrY = new double[polygon.length()];
            for (int i = 0; i < polygon.length(); i++)
            {
                arrX[i] = polygon.getArrX()[i];
                arrY[i] = polygon.getArrY()[i];
            }
            arrX[index] = newValue.x;
            arrY[index] = newValue.y;
            polygon = new GWTPolygonGate(polygon.getXAxis(), arrX, polygon.getYAxis(), arrY);
            gate = polygon;
        }
        gate.setDirty(true);
        graphWindow.getEditor().getState().setGate(gate);
        ptCapture = ptScreen;
    }

    public void mouseMove(GWTPoint ptScreen)
    {
        if (getGate().isOpen())
        {
            setCursor("pointer");
            return;
        }
        setCursor("crosshair");
        if (hasCapture())
        {
            offsetPosition(getOffset(ptScreen));
        }
    }

    public void mouseDown(GWTPoint ptScreen)
    {
        if (getGate().isOpen())
        {
            return;
        }
        capture(ptScreen);
    }

    public void mouseUp(GWTPoint ptScreen)
    {
        if (hasCapture())
        {
            updateGate(ptScreen);
        }
        releaseCapture();
        updateDisplay();
    }
}
