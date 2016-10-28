/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

package org.labkey.flow.analysis.chart;

import org.jfree.chart.plot.*;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.ContourEntity;
import org.jfree.ui.RectangleEdge;
import org.jfree.data.contour.ContourDataset;
import org.jfree.data.Range;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import org.labkey.flow.analysis.model.Polygon;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Contour plot which allows displaying gates overlayed on the graph.
 */
public class DensityPlot extends ContourPlot
{
    static private class PolygonData
    {
        public PolygonData(String label, Polygon poly)
        {
            _label = label;
            _poly = poly;
        }
        String _label;
        Polygon _poly;
    }

    List _polyDatas = new ArrayList();

    public DensityPlot(DensityDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, ColorBar colorBar)
    {
        super(dataset, domainAxis, rangeAxis, colorBar);
    }

    protected void drawLine(Graphics2D g2, Rectangle2D dataArea, double x1, double y1, double x2, double y2)
    {
        int prevX, prevY, nextX, nextY;
        if (x1 == x2 || y1 == y2)
        {
            prevX = (int)getDomainAxis().valueToJava2D(x1, dataArea, RectangleEdge.BOTTOM);
            prevY = (int)getRangeAxis().valueToJava2D(y1, dataArea, RectangleEdge.LEFT);
            nextX = (int)getDomainAxis().valueToJava2D(x2, dataArea, RectangleEdge.BOTTOM);
            nextY = (int)getRangeAxis().valueToJava2D(y2, dataArea, RectangleEdge.LEFT);
            g2.drawLine(prevX, prevY, nextX, nextY);
            return;
        }

        int nSegments = 10;
        prevX = (int)getDomainAxis().valueToJava2D(x1, dataArea, RectangleEdge.BOTTOM);
        prevY = (int)getRangeAxis().valueToJava2D(y1, dataArea, RectangleEdge.LEFT);
        for (int i = 1; i <= nSegments; i ++)
        {
            double x = x2 * i / nSegments + x1 * (nSegments - i) / nSegments;
            double y = y2 * i / nSegments + y1 * (nSegments - i) / nSegments;
            nextX = (int)getDomainAxis().valueToJava2D(x, dataArea, RectangleEdge.BOTTOM);
            nextY = (int)getRangeAxis().valueToJava2D(y, dataArea, RectangleEdge.LEFT);
            g2.drawLine(prevX, prevY, nextX, nextY);
            prevX = nextX;
            prevY = nextY;
        }
    }

    protected void drawPolygon(Graphics2D g2, Rectangle2D dataArea, PolygonData polygon)
    {
        int len = polygon._poly.X.length;
        double x1 = polygon._poly.X[len - 1];
        double y1 = polygon._poly.Y[len - 1];
        for (int i = 0; i < len; i ++)
        {
            double x2 = polygon._poly.X[i];
            double y2 = polygon._poly.Y[i];
            drawLine(g2, dataArea, x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
    }


    public void render(Graphics2D g2, Rectangle2D dataArea, PlotRenderingInfo info, CrosshairState crosshairState)
    {
        super.render(g2, dataArea, info, crosshairState);
        g2.setColor(PlotFactory.COLOR_GATE);
        for (Iterator it = _polyDatas.iterator(); it.hasNext();)
        {
            PolygonData data = (PolygonData) it.next();
            drawPolygon(g2, dataArea, data);
        }
    }

    int floor(double d)
    {
        return (int) d;
    }

    int ceil(double d)
    {
        int f = floor(d);
        return d == f ? f : (f > 0 ? f + 1 : f - 1);
    }

    double[] getTranslatedValues(double[] values, ValueAxis axis, Rectangle2D area, RectangleEdge edge)
    {
        double[] ret = new double[values.length + 1];
        ret[0] = axis.valueToJava2D(values[0], area, edge);
        double lastValue = ret[0];
        for (int i = 1; i < values.length; i ++)
        {
            lastValue = axis.valueToJava2D(values[i], area, edge);
            ret[i] = (lastValue + ret[i - 1]) / 2;
        }
        ret[values.length] = lastValue;
        return ret;
    }

    public Range getDataRange(ValueAxis axis)
    {
        double[] values;
        if (axis == getDomainAxis())
            values = ((DensityDataset) getDataset()).getPossibleXValues();
        else if (axis == getRangeAxis())
        {
            values = ((DensityDataset) getDataset()).getPossibleYValues();
        }
        else
        {
            return null;
        }
        return new Range(values[0], values[values.length - 1]);
    }

    /**
     * Fills the plot.
     *
     * @param g2             the graphics device.
     * @param dataArea       the area within which the data is being drawn.
     * @param info           collects information about the drawing.
     * @param plot           the plot (can be used to obtain standard color information etc).
     * @param horizontalAxis the domain (horizontal) axis.
     * @param verticalAxis   the range (vertical) axis.
     * @param colorBar       the color bar axis.
     * @param contourData    the dataset.
     * @param crosshairState information about crosshairs on a plot.
     */
    public void contourRenderer(Graphics2D g2,
                                Rectangle2D dataArea,
                                PlotRenderingInfo info,
                                ContourPlot plot,
                                ValueAxis horizontalAxis,
                                ValueAxis verticalAxis,
                                ColorBar colorBar,
                                ContourDataset contourData,
                                CrosshairState crosshairState)
    {

        assert contourData instanceof DensityDataset;
        DensityDataset data = (DensityDataset) contourData;
        // setup for collecting optional entity info...
        Rectangle2D.Double entityArea = null;
        EntityCollection entities = null;
        if (info != null)
        {
            entities = info.getOwner().getEntityCollection();
        }

        Rectangle2D.Double rect = null;
        rect = new Rectangle2D.Double();

        //turn off anti-aliasing when filling rectangles
        Object antiAlias = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int[] xIndex = data.indexX();
        int[] indexX = data.getXIndices();
        boolean vertInverted = verticalAxis.isInverted();
        boolean horizInverted = false;
        if (horizontalAxis instanceof NumberAxis)
        {
            horizInverted = horizontalAxis.isInverted();
        }
        double[] arrX = data.getPossibleXValues();
        double[] arrY = data.getPossibleYValues();
        double[] arrTransX = getTranslatedValues(arrX, horizontalAxis, dataArea, RectangleEdge.BOTTOM);
        double[] arrTransY = getTranslatedValues(arrY, verticalAxis, dataArea, RectangleEdge.LEFT);

        for (int col = 0; col < arrX.length; col ++)
        {
            for (int row = 0; row < arrY.length; row ++)
            {
                int index = col * arrY.length + row;
                double z = data.getZValue(0, index);
                assert arrY[row] == data.getYValue(0, index);
                assert arrX[col] == data.getXValue(0, index);

                double transX = arrTransX[col];
                double transDX = arrTransX[col + 1] - transX;
                double transY = arrTransY[row + 1];
                double transDY = arrTransY[row] - transY;

                rect.setRect(transX, transY, transDX, transDY);
                Rectangle rectI = new Rectangle((int) transX, (int) transY,
                        ceil(transX + transDX) - floor(transX),
                        ceil(transY + transDY) - floor(transY));
                Number Z = data.getZ(0, index);
                if (Z != null)
                {
                    g2.setPaint(colorBar.getPaint(Z.doubleValue()));
                    //g2.fill(rect);
                    g2.fillRect(rectI.x, rectI.y, rectI.width, rectI.height);
                }
                else if (this.getMissingPaint() != null)
                {
                    g2.setPaint(this.getMissingPaint());
                    g2.fill(rect);
                }

                entityArea = rect;

                // add an entity for the item...
                if (entities != null)
                {
                    String tip = "";
                    if (getToolTipGenerator() != null)
                    {
                        tip = getToolTipGenerator().generateToolTip(data, index);
                    }
                    String url = null;
                    ContourEntity entity = new ContourEntity((Rectangle2D.Double) entityArea.clone(),
                            tip, url);
                    entity.setIndex(index);
                    entities.add(entity);
                }

                // do we need to update the crosshair values?
                if (plot.isDomainCrosshairLockedOnData())
                {
                    if (plot.isRangeCrosshairLockedOnData())
                    {
                        // both axes
                        crosshairState.updateCrosshairPoint(
                                data.getYValue(0, index), data.getYValue(0, index), transX, transY, PlotOrientation.VERTICAL
                        );
                    }
                    else
                    {
                        // just the horizontal axis...
                        crosshairState.updateCrosshairX(transX);
                    }
                }
                else
                {
                    if (plot.isRangeCrosshairLockedOnData())
                    {
                        // just the vertical axis...
                        crosshairState.updateCrosshairY(transY);
                    }
                }
            }
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias);

        return;

    }

    public void addPolygon(String label, Polygon poly)
    {
        _polyDatas.add(new PolygonData(label, poly));
    }
}
