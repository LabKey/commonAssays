package org.labkey.flow.analysis.chart;

import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.plot.Plot;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Subclass which draws as small of a colorbar as possible.
 */
public class DensityColorBar extends ColorBar
    {
    public DensityColorBar(String label)
        {
        super(label);
        }

    public AxisSpace reserveSpace(Graphics2D g2, Plot plot, Rectangle2D plotArea, Rectangle2D dataArea, RectangleEdge edge, AxisSpace space)
        {
        return new AxisSpace();
        }

        public double draw(Graphics2D g2, double cursor, Rectangle2D plotArea, Rectangle2D dataArea, Rectangle2D reservedArea, RectangleEdge edge)
        {
            // no-op
            return cursor;
        }
    }
