package org.labkey.flow.gateeditor.client.ui.graph;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.GateCallback;
import org.labkey.flow.gateeditor.client.FlowUtil;
import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;
import org.labkey.flow.gateeditor.client.ui.*;
import org.labkey.flow.gateeditor.client.model.*;

public class GraphWindow extends GateComponent
{
    static final private String loadingText = "Loading graph...";
    VerticalPanel widget;
    GraphImage image;
    Label status;
    GWTGraphInfo graphInfo;
    GateWidget gateWidget;
    GraphWidget capture;
    UpdateGraphTimer timer = new UpdateGraphTimer();

    public void capture(GraphWidget o)
    {
        this.capture = o;
    }

    public void releaseCapture(GraphWidget o)
    {
        if (o == this.capture)
        {
            this.capture = null;
        }
    }

    public GWTGraphInfo getGraphInfo()
    {
        return graphInfo;
    }

    public void setGraphInfo(GWTGraphInfo graphInfo)
    {
        this.graphInfo = graphInfo;
        if (graphInfo == null)
        {
            this.image.setVisible(false);
            return;
        }
        status.setText(loadingText);
        gateWidget.updateDisplay();
    }

    GateEditorListener listener = new GateEditorListener()
    {
        public void onWellChanged()
        {
            updateGraph();
        }

        public void onGateChanged()
        {
            updateGraph();
        }

        public void onYAxisChanged()
        {
            updateGraph();
        }

        public void onCompMatrixChanged()
        {
            updateGraph();
        }
    };
    LoadListener imageLoadListener = new LoadListener()
    {
        public void onError(Widget sender)
        {
            status.setText("Error");
        }

        public void onLoad(Widget sender)
        {
            status.setText("");
        }
    };

    MouseListener mouseListener = new _MouseListener();


    public GraphWindow(GateEditor editor)
    {
        super(editor);
        widget = new VerticalPanel();
        Image spacer = new Image();
        spacer.setHeight("1px");
        spacer.setWidth("300px");
        spacer.setUrl(FlowUtil._gif());
        widget.add(spacer);
        image = new GraphImage();
        image.addMouseListener(mouseListener);
        image.setVisible(false);
        status = new Label();

        widget.add(image);
        image.addLoadListener(imageLoadListener);
        widget.add(status);
        widget.setWidth("310px");
        editor.addListener(listener);
        gateWidget = new GateWidget(this);
    }

    public Widget getWidget()
    {
        return widget;
    }

    GWTGraphOptions getGraphOptions()
    {
        GWTWell well = getWell();
        GWTGate gate = getGate();
        GWTPopulation population = getPopulation();

        if (well == null || gate == null || population == null)
        {
            return null;
        }

        GWTGraphOptions options = new GWTGraphOptions();
        options.editingMode = editor.getState().getEditingMode();
        options.script = getScript();
        options.well = well;
        options.height = 300;
        options.width = 300;
        options.subset = population.getFullName();
        if (gate instanceof GWTIntervalGate)
        {
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            options.xAxis = interval.getAxis();
            options.yAxis = editor.getState().getYAxis();
            if ("".equals(options.yAxis))
            {
                options.yAxis = null;
            }
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            options.xAxis = polygon.getXAxis();
            options.yAxis = polygon.getYAxis();
        }
        options.compensationMatrix = getCompensationMatrix();
        return options;
    }

    private void updateGraph()
    {
        if (timer.isScheduled())
            return;
        timer.schedule(10);
    }
    private void doUpdateGraph()
    {
        GWTGraphOptions options = getGraphOptions();
        if (options == null)
        {
            this.image.setVisible(false);
            return;
        }
        if (this.graphInfo != null)
        {
            if (graphInfo.graphOptions.equals(options))
            {
                gateWidget.updateDisplay();
                return;
            }
        }
        status.setText(loadingText);
        editor.getService().getGraphInfo(options, new GateCallback() {

            public void onSuccess(Object result)
            {
                GWTGraphInfo graphInfo = (GWTGraphInfo) result;
                if (graphInfo.graphOptions.equals(getGraphOptions()))
                {
                    setGraphInfo((GWTGraphInfo) result);
                }
            }
        });
    }

    public GWTRectangle getBoundingRectangle(GWTGate gate)
    {
        if (gate instanceof GWTIntervalGate)
        {
            GWTRectangle ret = new GWTRectangle();
            GWTIntervalGate interval = (GWTIntervalGate) gate;
            GWTPoint ptMin = new GWTPoint(interval.getMinValue(), 0);
            ptMin = graphInfo.toScreen(ptMin);
            GWTPoint ptMax = new GWTPoint(interval.getMaxValue(), 0);
            ptMax = graphInfo.toScreen(ptMax);
            ret.x = ptMin.x + image.getAbsoluteLeft();
            ret.width = ptMax.x - ptMin.x;
            ret.y = image.getAbsoluteTop();
            ret.height = graphInfo.rcChart.height;
            return ret;
        }
        else if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polygon = (GWTPolygonGate) gate;
            GWTRectangle ret = polygon.getBoundingRectangle();
            ret.setTopLeft(graphInfo.toScreen(ret.getTopLeft()));
            ret.setBottomRight(graphInfo.toScreen(ret.getBottomRight()));
            ret.translate(image.getAbsoluteLeft(), image.getAbsoluteTop());
            return ret;
        }
        return new GWTRectangle();
    }

    public GWTPoint valueToScreen(GWTPoint point)
    {
        GWTPoint ret = graphInfo.toScreen(point);
        ret.x += image.getAbsoluteLeft();
        ret.y += image.getAbsoluteTop();
        return ret;
    }

    public GWTRectangle valueToScreen(GWTRectangle rectangle)
    {
        GWTPoint topLeft = new GWTPoint(rectangle.x, rectangle.y + rectangle.height);
        GWTPoint bottomRight = new GWTPoint(rectangle.x + rectangle.width, rectangle.y);
        return new GWTRectangle(valueToScreen(topLeft), valueToScreen(bottomRight));
    }

    public GWTPoint screenToValue(GWTPoint point)
    {
        GWTPoint ret = new GWTPoint(point.x - image.getAbsoluteLeft(), point.y - image.getAbsoluteTop());
        return graphInfo.toValue(ret);
    }

    public GraphWidget hitTest(GWTPoint point)
    {
        if (capture != null)
        {
            return capture;
        }
        return gateWidget.hitTest(point);
    }

    class _MouseListener implements MouseListener
    {
        private GWTPoint toScreen(Widget sender, int x, int y)
        {
            return new GWTPoint(x + sender.getAbsoluteLeft(), y + sender.getAbsoluteTop());
        }

        private boolean isOpen()
        {
            GWTGate gate = getGate();
            return gate instanceof GWTPolygonGate && ((GWTPolygonGate) gate).isOpen();
        }

        private boolean isClosingPoint(GWTPoint ptScreen)
        {
            if (!isOpen())
                return false;
            if (gateWidget.handles.length == 0)
                return false;
            Handle handle = gateWidget.handles[0];
            return handle.getPosition().contains(ptScreen);
        }

        public void onMouseDown(Widget sender, int x, int y)
        {
            if (isReadOnly())
                return;
            GWTPoint ptScreen = toScreen(sender, x, y);
            if (isOpen())
            {
                GWTPoint ptValue = screenToValue(ptScreen);
                GWTPolygonGate polygon = (GWTPolygonGate) getGate();
                if (isClosingPoint(ptScreen))
                {
                    GWTPolygonGate newGate = new GWTPolygonGate(polygon.getXAxis(), polygon.getArrX(), polygon.getYAxis(), polygon.getArrY());
                    getEditor().getState().setGate(newGate);
                    return;
                }
                double[] arrX = new double[polygon.length() + 1];
                double[] arrY = new double[polygon.length() + 1];
                for (int i = 0; i < polygon.length(); i ++)
                {
                    arrX[i] = polygon.getArrX()[i];
                    arrY[i] = polygon.getArrY()[i];
                }
                arrX[arrX.length - 1] = ptValue.x;
                arrY[arrY.length - 1] = ptValue.y;
                GWTPolygonGate newGate = new GWTPolygonGate(polygon.getXAxis(), arrX, polygon.getYAxis(), arrY);
                newGate.setOpen(true);
                newGate.setDirty(true);
                getEditor().getState().setGate(newGate);
                return;
            }
            GraphWidget hitTest = hitTest(ptScreen);
            if (hitTest != null)
            {
                hitTest.mouseDown(ptScreen);
            }
        }

        public void onMouseEnter(Widget sender)
        {
        }

        public void onMouseLeave(Widget sender)
        {
        }

        public void onMouseMove(Widget sender, int x, int y)
        {
            if (isReadOnly())
                return;
            GWTPoint ptScreen = toScreen(sender, x, y);
            if (isOpen())
            {
                if (isClosingPoint(ptScreen))
                {
                    GraphWidget.setCursor("pointer");
                }
                else
                {
                    GraphWidget.setCursor("crosshair");
                }
                return;
            }

            GraphWidget.setCursor("auto");

            GraphWidget hitTest = hitTest(ptScreen);
            if (hitTest != null)
            {
                hitTest.mouseMove(ptScreen);
            }
        }


        public void onMouseUp(Widget sender, int x, int y)
        {
            if (isReadOnly())
                return;
            GWTPoint ptScreen = toScreen(sender, x, y);
            GraphWidget hitTest = hitTest(ptScreen);
            if (hitTest != null)
            {
                hitTest.mouseUp(ptScreen);
            }
        }
    }

    class UpdateGraphTimer extends Timer
    {
        boolean scheduled;
        public void schedule(int delayMillis)
        {
            super.schedule(delayMillis);
            scheduled = true;
        }


        public void cancel()
        {
            super.cancel();
            scheduled = false;
        }

        public boolean isScheduled()
        {
            return scheduled;
        }


        public void scheduleRepeating(int periodMillis)
        {
            throw new UnsupportedOperationException();
        }

        public void run()
        {
            scheduled = false;
            doUpdateGraph();
        }
    }
}
