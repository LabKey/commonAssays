package org.labkey.flow.analysis.chart;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.Range;
import org.jfree.ui.RectangleEdge;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class HistPlot extends XYPlot
{
    List<Range> _gates = new ArrayList();
    public HistPlot(XYDataset dataset, ValueAxis xAxis, ValueAxis yAxis, XYItemRenderer renderer)
    {
        super(dataset, xAxis, yAxis, renderer);

    }

    public void addGate(Range range)
    {
        _gates.add(range);
    }

    public boolean render(Graphics2D g2, Rectangle2D dataArea, int series, PlotRenderingInfo info, CrosshairState crosshairState)
    {
        boolean ret = super.render(g2, dataArea, series, info, crosshairState);
        g2.setColor(PlotFactory.COLOR_GATE);
        g2.setStroke(new BasicStroke());

        for (int i = 0; i < _gates.size(); i ++)
        {
            Range gate =  _gates.get(i);
            int min = (int) getDomainAxis().valueToJava2D(gate.getLowerBound(), dataArea, RectangleEdge.BOTTOM);
            int max = (int) getDomainAxis().valueToJava2D(gate.getUpperBound(), dataArea, RectangleEdge.BOTTOM);
            int y = ((int) dataArea.getMinY()) + 10 + 4 * i;
            g2.drawLine(min, y, max, y);
            g2.drawLine(min, y - 1, min, y + 1);
            g2.drawLine(max, y - 1, max, y + 1);
        }
        return ret;
    }
}
