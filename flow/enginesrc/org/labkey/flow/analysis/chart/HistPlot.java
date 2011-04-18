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

package org.labkey.flow.analysis.chart;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.Range;

import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;

public class HistPlot extends XYPlot
{
    List<Range> _gates = new ArrayList();
    public HistPlot(XYDataset dataset, ValueAxis xAxis, ValueAxis yAxis)
    {
        super(dataset, xAxis, yAxis, new XYBarRenderer());
        getRenderer().setSeriesVisibleInLegend(false);

    }

    public void addGate(Range range)
    {
        _gates.add(range);
    }

    boolean renderSeries(Graphics2D g2, Rectangle2D dataArea, int series, PlotRenderingInfo info, CrosshairState crosshairState)
    {
        List<Double> yValues = new ArrayList();
        List<Integer> xValues = new ArrayList();

        XYDataset dataset = getDataset();
        int lastX = -1;
        int count = 0;
        double total = 0;
        int itemCount = dataset.getItemCount(series);
        if (itemCount == 0)
            return false;

        count = 1;
        total = dataset.getYValue(series, 0);
        lastX = (int) Math.round(getDomainAxis().valueToJava2D(dataset.getXValue(series, 0), dataArea, getDomainAxisEdge()));
        for (int item = 1; item < itemCount; item ++)
        {
            int x = (int) Math.round(getDomainAxis().valueToJava2D(dataset.getXValue(series, item), dataArea, getDomainAxisEdge()));
            double value = dataset.getYValue(series, item);
            if (x != lastX)
            {
                xValues.add(lastX);
                yValues.add(total / count);
                count = 1;
                total = value;
                lastX = x;
            }
            else
            {
                total += value;
                count ++;
            }
        }
        xValues.add(lastX);
        yValues.add(total / count);

        assert xValues.size() == yValues.size();
        g2.setPaint(Color.BLACK);
        g2.setStroke(new BasicStroke());
        for (int i = 1; i < xValues.size(); i ++)
        {
            int x1 = xValues.get(i - 1);
            int x2 = xValues.get(i);
            int y1 = (int) Math.round(getRangeAxis().valueToJava2D(yValues.get(i - 1), dataArea, getRangeAxisEdge()));
            int y2 = (int) Math.round(getRangeAxis().valueToJava2D(yValues.get(i), dataArea, getRangeAxisEdge()));

            Shape line;
            if (getOrientation() == PlotOrientation.VERTICAL)
            {
                line = new Line2D.Double(x1, y1, x2, y2);
            }
            else
            {
                return false;
            }
            g2.draw(line);
        }
        return true;
    }

    public boolean render(Graphics2D g2, Rectangle2D dataArea, int series, PlotRenderingInfo info, CrosshairState crosshairState)
    {
        renderSeries(g2, dataArea, series, info, crosshairState);
        g2.setColor(PlotFactory.COLOR_GATE);
        g2.setStroke(new BasicStroke());

        if (_gates.size() == 1)
        {
            Range gate = _gates.get(0);
            int min = (int) getDomainAxis().valueToJava2D(gate.getLowerBound(), dataArea, getDomainAxisEdge());
            int max = (int) getDomainAxis().valueToJava2D(gate.getUpperBound(), dataArea, getDomainAxisEdge());
            g2.drawLine(min, (int) dataArea.getMinY(), min + 1, (int) dataArea.getMaxY());
            g2.drawLine(max - 1, (int) dataArea.getMinY(), max, (int) dataArea.getMaxY());
        }
        else
        {
            for (int i = 0; i < _gates.size(); i ++)
            {
                Range gate =  _gates.get(i);
                int min = (int) getDomainAxis().valueToJava2D(gate.getLowerBound(), dataArea, getDomainAxisEdge());
                int max = (int) getDomainAxis().valueToJava2D(gate.getUpperBound(), dataArea, getDomainAxisEdge());
                int y = ((int) dataArea.getMinY()) + 10 + 4 * i;
                g2.drawLine(min, y, max, y);
                g2.drawLine(min, y - 1, min, y + 1);
                g2.drawLine(max, y - 1, max, y + 1);
            }
        }
        return true;
    }
}
